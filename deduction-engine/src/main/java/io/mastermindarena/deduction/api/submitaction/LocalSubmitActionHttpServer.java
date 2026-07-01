package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;
import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.infrastructure.preferences.PreferencesIdempotencyStore;
import io.mastermindarena.deduction.infrastructure.preferences.PreferencesMatchStateStore;
import io.mastermindarena.deduction.infrastructure.preferences.PreferencesWorkflowEventSink;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;

public final class LocalSubmitActionHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final Preferences preferencesRoot;

    private LocalSubmitActionHttpServer(HttpServer server, Preferences preferencesRoot) {
        this.server = server;
        this.preferencesRoot = preferencesRoot;
    }

    public static LocalSubmitActionHttpServer create(LocalSubmitActionRuntimeConfig config) {
        Objects.requireNonNull(config, "config is required");

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
            ObjectMapper objectMapper = new ObjectMapper();
            Preferences preferencesRoot = Preferences.userRoot().node(config.persistenceNode());

            PreferencesMatchStateStore stateStore = new PreferencesMatchStateStore(preferencesRoot);
            stateStore.findById(config.seedMatchId()).orElseGet(() -> {
                MatchRuntimeState seeded = new MatchRuntimeState(
                        config.seedMatchId(),
                        1,
                        0,
                        true,
                        List.of(config.seedActorId(), config.seedOpponentId()),
                        0,
                        "IN_PROGRESS",
                        null,
                        null
                );
                stateStore.save(seeded);
                return seeded;
            });

            PreferencesWorkflowEventSink eventSink = new PreferencesWorkflowEventSink(preferencesRoot);
            RuleSet ruleSet = input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
            SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                    stateStore,
                    eventSink,
                    ruleSet,
                    new ActionResolutionContractValidator(),
                    new PreferencesIdempotencyStore(preferencesRoot)
            );
            LocalSubmitActionEndpoint endpoint = new LocalSubmitActionEndpoint(new SubmitActionApplicationService(orchestrator));

            server.createContext(config.path(), exchange -> handleSubmitAction(exchange, endpoint, objectMapper, config.authBearerToken()));
            server.createContext("/health", exchange -> handleHealth(exchange, objectMapper));
            server.setExecutor(null);
            return new LocalSubmitActionHttpServer(server, preferencesRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create local SubmitAction HTTP server", e);
        }
    }

    private static void handleSubmitAction(
            HttpExchange exchange,
            LocalSubmitActionEndpoint endpoint,
            ObjectMapper objectMapper,
            String bearerToken
    ) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (!LocalSubmitActionEndpoint.METHOD.equalsIgnoreCase(exchange.getRequestMethod())) {
                logStructured("request_rejected", path, exchange.getRequestMethod(), 405, "METHOD_NOT_ALLOWED");
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            String expected = "Bearer " + bearerToken;
            if (!expected.equals(authorization)) {
                logStructured("request_rejected", path, exchange.getRequestMethod(), 401, "UNAUTHORIZED");
                writeJson(exchange, objectMapper, 401, new ErrorBody("UNAUTHORIZED"));
                return;
            }

            SubmitActionHttpRequest request = objectMapper.readValue(exchange.getRequestBody(), SubmitActionHttpRequest.class);
            SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(request);
            logStructured("submit_action", path, exchange.getRequestMethod(), response.statusCode(), response.body().accepted() ? "ACCEPTED" : response.body().rejectionCode());
            writeJson(exchange, objectMapper, response.statusCode(), response.body());
        } catch (RuntimeException ex) {
            logStructured("request_rejected", exchange.getRequestURI().getPath(), exchange.getRequestMethod(), 400, "INVALID_REQUEST");
            writeJson(exchange, objectMapper, 400, new ErrorBody("INVALID_REQUEST"));
        }
    }

    private static void handleHealth(HttpExchange exchange, ObjectMapper objectMapper) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                logStructured("request_rejected", exchange.getRequestURI().getPath(), exchange.getRequestMethod(), 405, "METHOD_NOT_ALLOWED");
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            logStructured("health", exchange.getRequestURI().getPath(), exchange.getRequestMethod(), 200, "UP");
            writeJson(exchange, objectMapper, 200, new HealthBody("UP"));
        }
    }

    private static void writeJson(HttpExchange exchange, ObjectMapper objectMapper, int statusCode, Object body) throws IOException {
        byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, json.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(json);
        }
    }

    public void start() {
        server.start();
        logStructured("server_started", "n/a", "N/A", 0, "STARTED");
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public Preferences preferencesRoot() {
        return preferencesRoot;
    }

    @Override
    public void close() {
        stop();
    }

    private record ErrorBody(String code) {
    }

    private record HealthBody(String status) {
    }

    private static void logStructured(String event, String path, String method, int statusCode, String outcome) {
        String escapedEvent = escapeJson(event);
        String escapedPath = escapeJson(path);
        String escapedMethod = escapeJson(method);
        String escapedOutcome = escapeJson(outcome);
        System.out.println(String.format(Locale.ROOT,
                "{\"event\":\"%s\",\"path\":\"%s\",\"method\":\"%s\",\"statusCode\":%d,\"outcome\":\"%s\"}",
                escapedEvent,
                escapedPath,
                escapedMethod,
                statusCode,
                escapedOutcome));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

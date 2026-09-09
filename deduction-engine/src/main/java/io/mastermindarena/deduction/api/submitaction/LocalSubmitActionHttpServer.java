package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcIdempotencyStore;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcMatchStateStore;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcPersistenceContext;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcSchemaMigrator;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcWorkflowEventSink;
import io.mastermindarena.deduction.api.websocket.runtime.WebSocketBroadcastService;
import io.mastermindarena.deduction.api.websocket.runtime.WebSocketSessionRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class LocalSubmitActionHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final RuntimeMetrics metrics;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final WebSocketBroadcastService webSocketBroadcastService;

    private LocalSubmitActionHttpServer(
            HttpServer server,
            RuntimeMetrics metrics,
            WebSocketSessionRegistry webSocketSessionRegistry,
            WebSocketBroadcastService webSocketBroadcastService
    ) {
        this.server = server;
        this.metrics = metrics;
        this.webSocketSessionRegistry = webSocketSessionRegistry;
        this.webSocketBroadcastService = webSocketBroadcastService;
    }

    private record CorsPolicy(
            Set<String> allowedOrigins,
            boolean wildcardOrigin,
            String allowMethods,
            String allowHeaders,
            int maxAgeSeconds
    ) {
    }

    public static LocalSubmitActionHttpServer create(LocalSubmitActionRuntimeConfig config) {
        Objects.requireNonNull(config, "config is required");

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
            ObjectMapper objectMapper = new ObjectMapper();
                RuntimeMetrics metrics = new RuntimeMetrics();
            JdbcPersistenceContext persistenceContext = new JdbcPersistenceContext(
                    config.dbUrl(),
                    config.dbUser(),
                    config.dbPassword(),
                    config.dbSchema()
            );
            new JdbcSchemaMigrator(persistenceContext).migrateToLatest();

            JdbcMatchStateStore stateStore = new JdbcMatchStateStore(persistenceContext);
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

            JdbcWorkflowEventSink eventSink = new JdbcWorkflowEventSink(persistenceContext);
            WebSocketSessionRegistry webSocketSessionRegistry = new WebSocketSessionRegistry();
            WebSocketBroadcastService webSocketBroadcastService = new WebSocketBroadcastService(eventSink, webSocketSessionRegistry);
            RuleSet ruleSet = new LocalMastermindRuleSet();
            SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                    stateStore,
                    eventSink,
                    ruleSet,
                    new ActionResolutionContractValidator(),
                    new JdbcIdempotencyStore(persistenceContext)
            );
            LocalSubmitActionEndpoint endpoint = new LocalSubmitActionEndpoint(new SubmitActionApplicationService(orchestrator));
                LocalMatchStateEndpoint matchStateEndpoint = new LocalMatchStateEndpoint(stateStore);
                CorsPolicy corsPolicy = createCorsPolicy(config);

            server.createContext(config.path(), exchange -> handleSubmitAction(
                    exchange,
                    endpoint,
                    objectMapper,
                    config.apiKeyHeaderName(),
                    config.apiKeyValue(),
                    config.requestIdHeaderName(),
                    corsPolicy,
                    metrics
            ));
                server.createContext(LocalMatchStateEndpoint.PATH, exchange -> handleMatchState(
                    exchange,
                    matchStateEndpoint,
                    objectMapper,
                    config.apiKeyHeaderName(),
                    config.apiKeyValue(),
                    config.requestIdHeaderName(),
                    corsPolicy,
                    metrics
                ));
                server.createContext("/health", exchange -> handleHealth(exchange, objectMapper, config.requestIdHeaderName(), corsPolicy, metrics));
            server.setExecutor(null);
            return new LocalSubmitActionHttpServer(server, metrics, webSocketSessionRegistry, webSocketBroadcastService);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create local SubmitAction HTTP server", e);
        }
    }

    private static void handleSubmitAction(
            HttpExchange exchange,
            LocalSubmitActionEndpoint endpoint,
            ObjectMapper objectMapper,
            String apiKeyHeaderName,
            String apiKeyValue,
            String requestIdHeaderName,
                CorsPolicy corsPolicy,
            RuntimeMetrics metrics
    ) throws IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(exchange, requestIdHeaderName);
        setResponseRequestIdHeader(exchange, requestIdHeaderName, requestId);
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!applyCors(exchange, corsPolicy)) {
                logStructured("request_rejected", path, method, 403, "CORS_ORIGIN_NOT_ALLOWED", requestId, elapsedMillis(startNanos), metrics.record(403, elapsedMillis(startNanos)));
                writeJson(exchange, objectMapper, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED"));
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!LocalSubmitActionEndpoint.METHOD.equalsIgnoreCase(exchange.getRequestMethod())) {
                logStructured("request_rejected", path, method, 405, "METHOD_NOT_ALLOWED", requestId, elapsedMillis(startNanos), metrics.record(405, elapsedMillis(startNanos)));
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            String providedApiKey = exchange.getRequestHeaders().getFirst(apiKeyHeaderName);
            if (!secureEquals(apiKeyValue, providedApiKey)) {
                logStructured("request_rejected", path, method, 401, "UNAUTHORIZED", requestId, elapsedMillis(startNanos), metrics.record(401, elapsedMillis(startNanos)));
                writeJson(exchange, objectMapper, 401, new ErrorBody("UNAUTHORIZED"));
                return;
            }

            SubmitActionHttpRequest request = objectMapper.readValue(exchange.getRequestBody(), SubmitActionHttpRequest.class);
            SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(request);
            long elapsedMillis = elapsedMillis(startNanos);
            logStructured(
                    "submit_action",
                    path,
                    method,
                    response.statusCode(),
                    response.body().accepted() ? "ACCEPTED" : response.body().rejectionCode(),
                    requestId,
                    elapsedMillis,
                    metrics.record(response.statusCode(), elapsedMillis)
            );
            writeJson(exchange, objectMapper, response.statusCode(), response.body());
        } catch (RuntimeException ex) {
            long elapsedMillis = elapsedMillis(startNanos);
            logStructured(
                    "request_rejected",
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestMethod(),
                    400,
                    "INVALID_REQUEST",
                    requestId,
                    elapsedMillis,
                    metrics.record(400, elapsedMillis)
            );
            writeJson(exchange, objectMapper, 400, new ErrorBody("INVALID_REQUEST"));
        }
    }

    private static void handleHealth(
            HttpExchange exchange,
            ObjectMapper objectMapper,
            String requestIdHeaderName,
                CorsPolicy corsPolicy,
            RuntimeMetrics metrics
    ) throws IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(exchange, requestIdHeaderName);
        setResponseRequestIdHeader(exchange, requestIdHeaderName, requestId);
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!applyCors(exchange, corsPolicy)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 403, "CORS_ORIGIN_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(403, elapsedMillis));
                writeJson(exchange, objectMapper, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED"));
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 405, "METHOD_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(405, elapsedMillis));
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            long elapsedMillis = elapsedMillis(startNanos);
            RuntimeMetricsSnapshot snapshot = metrics.record(200, elapsedMillis);
            logStructured("health", path, method, 200, "UP", requestId, elapsedMillis, snapshot);
            writeJson(exchange, objectMapper, 200, new HealthBody("UP", snapshot.requestsOk(), snapshot.requestsKo(), snapshot.averageLatencyMs()));
        }
    }

    private static void handleMatchState(
            HttpExchange exchange,
            LocalMatchStateEndpoint endpoint,
            ObjectMapper objectMapper,
            String apiKeyHeaderName,
            String apiKeyValue,
            String requestIdHeaderName,
                CorsPolicy corsPolicy,
            RuntimeMetrics metrics
    ) throws IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(exchange, requestIdHeaderName);
        setResponseRequestIdHeader(exchange, requestIdHeaderName, requestId);
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!applyCors(exchange, corsPolicy)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 403, "CORS_ORIGIN_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(403, elapsedMillis));
                writeJson(exchange, objectMapper, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED"));
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!LocalMatchStateEndpoint.METHOD.equalsIgnoreCase(method)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 405, "METHOD_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(405, elapsedMillis));
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            String providedApiKey = exchange.getRequestHeaders().getFirst(apiKeyHeaderName);
            if (!secureEquals(apiKeyValue, providedApiKey)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 401, "UNAUTHORIZED", requestId, elapsedMillis, metrics.record(401, elapsedMillis));
                writeJson(exchange, objectMapper, 401, new ErrorBody("UNAUTHORIZED"));
                return;
            }

            Map<String, String> queryParams = queryParams(exchange.getRequestURI().getRawQuery());
            String matchId = queryParams.get("matchId");
            String actorId = queryParams.get("actorId");
            MatchStateHttpResponse.Envelope response = endpoint.getMatchState(matchId, actorId);
            long elapsedMillis = elapsedMillis(startNanos);

            if (response.statusCode() == 200) {
                logStructured("match_state", path, method, 200, "FOUND", requestId, elapsedMillis, metrics.record(200, elapsedMillis));
                writeJson(exchange, objectMapper, 200, response.body());
                return;
            }

            if (response.statusCode() == 404) {
                logStructured("request_rejected", path, method, 404, "MATCH_NOT_FOUND", requestId, elapsedMillis, metrics.record(404, elapsedMillis));
                writeJson(exchange, objectMapper, 404, new ErrorBody("MATCH_NOT_FOUND"));
                return;
            }

            logStructured("request_rejected", path, method, 400, "INVALID_MATCH_ID", requestId, elapsedMillis, metrics.record(400, elapsedMillis));
            writeJson(exchange, objectMapper, 400, new ErrorBody("INVALID_MATCH_ID"));
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
        logStructured("server_started", "n/a", "N/A", 0, "STARTED", "n/a", 0L, metrics.snapshot());
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    RuntimeMetricsSnapshot metricsSnapshot() {
        return metrics.snapshot();
    }

    @Override
    public void close() {
        stop();
    }

    private record ErrorBody(String code) {
    }

    private record HealthBody(
            String status,
            long requestsOk,
            long requestsKo,
            long averageLatencyMs
    ) {
    }

    private static void logStructured(
            String event,
            String path,
            String method,
            int statusCode,
            String outcome,
            String requestId,
            long durationMs,
            RuntimeMetricsSnapshot metrics
    ) {
        String escapedEvent = escapeJson(event);
        String escapedPath = escapeJson(path);
        String escapedMethod = escapeJson(method);
        String escapedOutcome = escapeJson(outcome);
        String escapedRequestId = escapeJson(requestId);
        System.out.println(String.format(Locale.ROOT,
                "{\"event\":\"%s\",\"path\":\"%s\",\"method\":\"%s\",\"statusCode\":%d,\"outcome\":\"%s\",\"requestId\":\"%s\",\"durationMs\":%d,\"requestsTotal\":%d,\"requestsOk\":%d,\"requestsKo\":%d,\"averageLatencyMs\":%d}",
                escapedEvent,
                escapedPath,
                escapedMethod,
                statusCode,
                escapedOutcome,
                escapedRequestId,
                durationMs,
                metrics.requestsTotal(),
                metrics.requestsOk(),
                metrics.requestsKo(),
                metrics.averageLatencyMs()));
    }

    private static String resolveRequestId(HttpExchange exchange, String requestIdHeaderName) {
        String provided = exchange.getRequestHeaders().getFirst(requestIdHeaderName);
        if (provided == null || provided.isBlank()) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        }
        return provided;
    }

    private static void setResponseRequestIdHeader(HttpExchange exchange, String requestIdHeaderName, String requestId) {
        exchange.getResponseHeaders().set(requestIdHeaderName, requestId);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean secureEquals(String expected, String provided) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static CorsPolicy createCorsPolicy(LocalSubmitActionRuntimeConfig config) {
        String rawOrigins = config.corsAllowedOrigins();
        boolean wildcard = rawOrigins.trim().equals("*");
        Set<String> origins = wildcard
                ? Set.of("*")
                : Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        return new CorsPolicy(
                origins,
                wildcard,
                config.corsAllowedMethods(),
                config.corsAllowedHeaders(),
                config.corsMaxAgeSeconds()
        );
    }

    private static boolean applyCors(HttpExchange exchange, CorsPolicy policy) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return true;
        }

        boolean originAllowed = policy.wildcardOrigin() || policy.allowedOrigins().contains(requestOrigin);
        if (!originAllowed) {
            return false;
        }

        String responseOrigin = policy.wildcardOrigin() ? "*" : requestOrigin;
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", responseOrigin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", policy.allowMethods());
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", policy.allowHeaders());
        exchange.getResponseHeaders().set("Access-Control-Max-Age", Integer.toString(policy.maxAgeSeconds()));
        exchange.getResponseHeaders().add("Vary", "Origin");
        return true;
    }

    private static Map<String, String> queryParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        Map<String, String> params = new LinkedHashMap<>();
        Arrays.stream(rawQuery.split("&"))
                .filter(token -> !token.isBlank())
                .forEach(token -> {
                    String[] keyValue = token.split("=", 2);
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = keyValue.length > 1
                            ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                            : "";
                    params.put(key, value);
                });

        return Map.copyOf(params);
    }

    record RuntimeMetricsSnapshot(long requestsTotal, long requestsOk, long requestsKo, long averageLatencyMs) {
    }

    private static final class RuntimeMetrics {
        private final AtomicLong requestsTotal = new AtomicLong();
        private final AtomicLong requestsOk = new AtomicLong();
        private final AtomicLong requestsKo = new AtomicLong();
        private final AtomicLong latencyTotalMs = new AtomicLong();

        RuntimeMetricsSnapshot record(int statusCode, long latencyMs) {
            requestsTotal.incrementAndGet();
            if (statusCode >= 200 && statusCode < 400) {
                requestsOk.incrementAndGet();
            } else {
                requestsKo.incrementAndGet();
            }
            latencyTotalMs.addAndGet(Math.max(latencyMs, 0L));
            return snapshot();
        }

        RuntimeMetricsSnapshot snapshot() {
            long total = requestsTotal.get();
            long ok = requestsOk.get();
            long ko = requestsKo.get();
            long avg = total == 0L ? 0L : latencyTotalMs.get() / total;
            return new RuntimeMetricsSnapshot(total, ok, ko, avg);
        }
    }
}

package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcIdempotencyStore;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcMatchStateStore;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcRoomDirectory;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcPersistenceContext;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcSchemaMigrator;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcWorkflowEventSink;
import io.mastermindarena.deduction.api.websocket.runtime.WebSocketBroadcastService;
import io.mastermindarena.deduction.api.websocket.runtime.WebSocketSessionRegistry;
import io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.CorsPolicy;
import io.mastermindarena.deduction.api.submitaction.HttpObservability.RuntimeMetrics;
import io.mastermindarena.deduction.api.submitaction.HttpObservability.RuntimeMetricsSnapshot;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.createCorsPolicy;
import static io.mastermindarena.deduction.api.submitaction.HttpObservability.logStructured;

public final class LocalSubmitActionHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final RuntimeMetrics metrics;

    private LocalSubmitActionHttpServer(HttpServer server, RuntimeMetrics metrics) {
        this.server = server;
        this.metrics = metrics;
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
            JdbcMatchStateStore stateStore = new JdbcMatchStateStore(persistenceContext);
            initializeDatabase(persistenceContext, stateStore, config);
            JdbcRoomDirectory rooms = new JdbcRoomDirectory(persistenceContext);

            JdbcWorkflowEventSink eventSink = new JdbcWorkflowEventSink(persistenceContext);
            WebSocketSessionRegistry webSocketSessionRegistry = new WebSocketSessionRegistry();
            new WebSocketBroadcastService(eventSink, webSocketSessionRegistry);
            SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                    stateStore,
                    eventSink,
                    new JdbcIdempotencyStore(persistenceContext)
            );
            LocalSubmitActionEndpoint endpoint = new LocalSubmitActionEndpoint(new SubmitActionApplicationService(orchestrator));
            LocalMatchStateEndpoint matchStateEndpoint = new LocalMatchStateEndpoint(stateStore);
            CorsPolicy corsPolicy = createCorsPolicy(config);

            server.createContext(config.path(), exchange -> GameHttpHandlers.handleSubmitAction(
                    exchange,
                    endpoint,
                    rooms,
                    objectMapper,
                    config.apiKeyHeaderName(),
                    config.apiKeyValue(),
                    config.requestIdHeaderName(),
                    corsPolicy,
                    metrics
            ));
            server.createContext(LocalMatchStateEndpoint.PATH, exchange -> GameHttpHandlers.handleMatchState(
                    exchange,
                    matchStateEndpoint,
                    rooms,
                    objectMapper,
                    config.apiKeyHeaderName(),
                    config.apiKeyValue(),
                    config.requestIdHeaderName(),
                    corsPolicy,
                    metrics
            ));
            server.createContext("/local/rooms", exchange -> RoomHttpHandlers.handleRooms(exchange, rooms, objectMapper, config.apiKeyHeaderName(), config.apiKeyValue(), config.requestIdHeaderName(), corsPolicy));
            server.createContext("/local/rooms/join", exchange -> RoomHttpHandlers.handleRoomJoin(exchange, rooms, objectMapper, config.apiKeyHeaderName(), config.apiKeyValue(), config.requestIdHeaderName(), corsPolicy));
            server.createContext("/local/rooms/resume", exchange -> RoomHttpHandlers.handleRoomResume(exchange, rooms, objectMapper, config.apiKeyHeaderName(), config.apiKeyValue(), config.requestIdHeaderName(), corsPolicy));
            server.createContext("/health", exchange -> GameHttpHandlers.handleHealth(exchange, objectMapper, config.requestIdHeaderName(), corsPolicy, metrics));
            server.setExecutor(null);
            return new LocalSubmitActionHttpServer(server, metrics);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create local SubmitAction HTTP server", e);
        }
    }

    private static void initializeDatabase(
            JdbcPersistenceContext persistenceContext,
            JdbcMatchStateStore stateStore,
            LocalSubmitActionRuntimeConfig config
    ) {
        IllegalStateException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                new JdbcSchemaMigrator(persistenceContext).migrateToLatest();
                stateStore.findById(config.seedMatchId()).orElseGet(() -> {
                    MatchRuntimeState seeded = new MatchRuntimeState(
                            config.seedMatchId(),
                            List.of(config.seedActorId(), config.seedOpponentId()),
                            0L
                    );
                    stateStore.save(seeded);
                    return seeded;
                });
                return;
            } catch (IllegalStateException failure) {
                lastFailure = failure;
                if (attempt < 3) {
                    pauseBeforeDatabaseRetry(attempt);
                }
            }
        }
        throw new IllegalStateException("Unable to initialize JDBC persistence after retries", lastFailure);
    }

    private static void pauseBeforeDatabaseRetry(int attempt) {
        try {
            Thread.sleep(250L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while initializing JDBC persistence", interrupted);
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

}

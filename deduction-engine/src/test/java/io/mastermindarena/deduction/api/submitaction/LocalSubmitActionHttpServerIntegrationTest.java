package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSubmitActionHttpServerIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void p5a_serverBootsWithExternalizedConfig() {
        LocalSubmitActionRuntimeConfig config = config("p5a_boot", "test-api-key", "boot-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();
            assertTrue(server.port() > 0);
        }
    }

    @Test
    void p5a_submitActionNominalOverHttpReturns200() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p5a_nominal", "test-api-key", "http-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            SubmitActionHttpRequest request = new SubmitActionHttpRequest(
                    "http-match",
                    "p1",
                    0L,
                    "http-idem-1",
                    "payload"
            );

            HttpURLConnection connection = (HttpURLConnection) URI
                    .create("http://localhost:" + server.port() + config.path())
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", "test-api-key");
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));

            assertEquals(200, connection.getResponseCode());

            SubmitActionHttpResponse response = objectMapper.readValue(connection.getInputStream(), SubmitActionHttpResponse.class);
            assertTrue(response.accepted());
            assertEquals("http-match", response.matchId());
            assertEquals("IN_PROGRESS", response.status());
        }
    }

    @Test
    void p5c_healthEndpointReturns200() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p5c_health", "test-api-key", "health-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            HttpURLConnection connection = (HttpURLConnection) URI
                    .create("http://localhost:" + server.port() + "/health")
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("GET");

            assertEquals(200, connection.getResponseCode());
        }
    }

    @Test
    void p5b_submitActionUnauthorizedReturns401() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p5b_unauthorized", "secret-api-key", "http-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            SubmitActionHttpRequest request = new SubmitActionHttpRequest("http-match", "p1", 0L, "unauth-1", "payload");
            HttpURLConnection connection = (HttpURLConnection) URI
                    .create("http://localhost:" + server.port() + config.path())
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", "wrong-api-key");
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));

            assertEquals(401, connection.getResponseCode());
        }
    }

    @Test
    void p6a_submitActionUnauthorizedWhenApiKeyMissingReturns401() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p6a_missing_key", "secret-api-key", "http-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            SubmitActionHttpRequest request = new SubmitActionHttpRequest("http-match", "p1", 0L, "missing-key-1", "payload");
            HttpURLConnection connection = (HttpURLConnection) URI
                    .create("http://localhost:" + server.port() + config.path())
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));

            assertEquals(401, connection.getResponseCode());
        }
    }

    @Test
    void p6b_submitActionAuthorizedPersistsStateInJdbcStore() throws IOException, SQLException {
        LocalSubmitActionRuntimeConfig config = config("p6b_nominal_persistent", "secret-api-key", "persistent-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            SubmitActionHttpRequest request = new SubmitActionHttpRequest("persistent-match", "p1", 0L, "persistent-1", "payload");
            HttpURLConnection connection = (HttpURLConnection) URI
                    .create("http://localhost:" + server.port() + config.path())
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", "secret-api-key");
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));

            assertEquals(200, connection.getResponseCode());
        }

        try (Connection connection = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
             PreparedStatement statement = connection.prepareStatement("SELECT payload FROM public.match_state WHERE match_id = ?")) {
            statement.setString(1, "persistent-match");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertTrue(resultSet.getString(1).contains("\t1\t"));
            }
        }
    }

    @Test
    void p6b_restartReloadsStateAndIdempotency() throws IOException {
        String dbName = "p6b_restart_" + UUID.randomUUID().toString().replace("-", "");
        LocalSubmitActionRuntimeConfig config = configForDb(dbName, "secret-api-key", "restart-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();
            SubmitActionHttpResponse first = postSubmitAction(server.port(), config.path(), "secret-api-key",
                    new SubmitActionHttpRequest("restart-match", "p1", 0L, "idem-restart-1", "payload"));
            assertTrue(first.accepted());
            assertEquals(1L, first.version());
        }

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();
            SubmitActionHttpResponse replay = postSubmitAction(server.port(), config.path(), "secret-api-key",
                    new SubmitActionHttpRequest("restart-match", "p1", 0L, "idem-restart-1", "payload"));
            assertTrue(replay.accepted());
            assertEquals(1L, replay.version());

            HttpURLConnection conflictConnection = openSubmitActionConnection(server.port(), config.path(), "secret-api-key");
            conflictConnection.getOutputStream().write(objectMapper.writeValueAsBytes(
                    new SubmitActionHttpRequest("restart-match", "p1", 0L, "idem-restart-2", "payload")));
            assertEquals(409, conflictConnection.getResponseCode());
        }
    }

    @Test
    void p6b_httpFlowNonRegressionWithJdbcPersistence() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p6b_http_non_regression", "secret-api-key", "non-reg-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            SubmitActionHttpResponse accepted = postSubmitAction(server.port(), config.path(), "secret-api-key",
                    new SubmitActionHttpRequest("non-reg-match", "p1", 0L, "non-reg-1", "payload"));
            assertTrue(accepted.accepted());
            assertEquals("IN_PROGRESS", accepted.status());

            HttpURLConnection unauthorizedConnection = openSubmitActionConnection(server.port(), config.path(), "wrong-key");
            unauthorizedConnection.getOutputStream().write(objectMapper.writeValueAsBytes(
                    new SubmitActionHttpRequest("non-reg-match", "p1", 1L, "non-reg-2", "payload")));
            assertEquals(401, unauthorizedConnection.getResponseCode());

            HttpURLConnection health = (HttpURLConnection) URI
                    .create("http://localhost:" + server.port() + "/health")
                    .toURL()
                    .openConnection();
            health.setRequestMethod("GET");
            assertEquals(200, health.getResponseCode());
        }
    }

    @Test
    void p6a_runtimeConfigReadsSystemPropertiesWithExpectedResolutionRule() {
        System.setProperty("submitAction.port", "9191");
        System.setProperty("submitAction.path", "/submit-action");
        System.setProperty("submitAction.auth.apiKeyHeader", "X-Test-Api-Key");
        System.setProperty("submitAction.auth.apiKeyValue", "configured-api-key");
        System.setProperty("submitAction.observability.requestIdHeader", "X-Correlation-Id");
        System.setProperty("submitAction.db.url", "jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require");
        System.setProperty("submitAction.db.user", "postgres");
        System.setProperty("submitAction.db.password", "password");
        System.setProperty("submitAction.db.schema", "public");
        System.setProperty("submitAction.seed.matchId", "config-match");
        System.setProperty("submitAction.seed.actorId", "actor-a");
        System.setProperty("submitAction.seed.opponentId", "actor-b");

        try {
            LocalSubmitActionRuntimeConfig config = LocalSubmitActionRuntimeConfig.fromEnvironment();

            assertEquals(9191, config.port());
            assertEquals("/submit-action", config.path());
            assertEquals("X-Test-Api-Key", config.apiKeyHeaderName());
            assertEquals("configured-api-key", config.apiKeyValue());
            assertEquals("X-Correlation-Id", config.requestIdHeaderName());
            assertEquals("jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require", config.dbUrl());
            assertEquals("postgres", config.dbUser());
            assertEquals("password", config.dbPassword());
            assertEquals("public", config.dbSchema());
            assertEquals("system property first, then known environment aliases, then default value", LocalSubmitActionRuntimeConfig.resolutionRule());
        } finally {
            System.clearProperty("submitAction.port");
            System.clearProperty("submitAction.path");
            System.clearProperty("submitAction.auth.apiKeyHeader");
            System.clearProperty("submitAction.auth.apiKeyValue");
            System.clearProperty("submitAction.observability.requestIdHeader");
            System.clearProperty("submitAction.db.url");
            System.clearProperty("submitAction.db.user");
            System.clearProperty("submitAction.db.password");
            System.clearProperty("submitAction.db.schema");
            System.clearProperty("submitAction.seed.matchId");
            System.clearProperty("submitAction.seed.actorId");
            System.clearProperty("submitAction.seed.opponentId");
        }
    }

    @Test
    void p6c_requestCorrelationAndBasicMetricsAreAvailable() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p6c_observability", "secret-api-key", "obs-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            HttpURLConnection acceptedConnection = openSubmitActionConnection(server.port(), config.path(), "secret-api-key");
            acceptedConnection.setRequestProperty("X-Request-Id", "req-accepted-1");
            acceptedConnection.getOutputStream().write(objectMapper.writeValueAsBytes(
                    new SubmitActionHttpRequest("obs-match", "p1", 0L, "obs-idem-1", "payload")));
            assertEquals(200, acceptedConnection.getResponseCode());
            assertEquals("req-accepted-1", acceptedConnection.getHeaderField("X-Request-Id"));

            HttpURLConnection unauthorizedConnection = openSubmitActionConnection(server.port(), config.path(), "wrong-key");
            unauthorizedConnection.getOutputStream().write(objectMapper.writeValueAsBytes(
                    new SubmitActionHttpRequest("obs-match", "p1", 1L, "obs-idem-2", "payload")));
            assertEquals(401, unauthorizedConnection.getResponseCode());
            String generatedRequestId = unauthorizedConnection.getHeaderField("X-Request-Id");
            assertTrue(generatedRequestId != null);
            assertFalse(generatedRequestId.isBlank());

            LocalSubmitActionHttpServer.RuntimeMetricsSnapshot snapshot = server.metricsSnapshot();
            assertEquals(2L, snapshot.requestsTotal());
            assertEquals(1L, snapshot.requestsOk());
            assertEquals(1L, snapshot.requestsKo());
            assertTrue(snapshot.averageLatencyMs() >= 0L);
        }
    }

    @Test
    void p6d_matchStateEndpointReturns200ForExistingMatch() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p6d_match_state", "secret-api-key", "state-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            HttpURLConnection connection = openMatchStateConnection(server.port(), "state-match", "secret-api-key");
            assertEquals(200, connection.getResponseCode());

            MatchStateHttpResponse response = objectMapper.readValue(connection.getInputStream(), MatchStateHttpResponse.class);
            assertEquals("state-match", response.matchId());
            assertEquals("IN_PROGRESS", response.status());
            assertEquals(0L, response.version());
            assertEquals("p1", response.currentActorId());
        }
    }

    @Test
    void p6d_matchStateEndpointReturns404WhenMatchIsUnknown() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p6d_match_state_404", "secret-api-key", "known-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            HttpURLConnection connection = openMatchStateConnection(server.port(), "missing-match", "secret-api-key");
            assertEquals(404, connection.getResponseCode());
        }
    }

    @Test
    void p6d_matchStateEndpointReturns401WhenApiKeyIsInvalid() throws IOException {
        LocalSubmitActionRuntimeConfig config = config("p6d_match_state_401", "secret-api-key", "state-match");

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();

            HttpURLConnection connection = openMatchStateConnection(server.port(), "state-match", "wrong-api-key");
            assertEquals(401, connection.getResponseCode());
        }
    }

            @Test
            void p6d_matchStateEndpointReturnsActorAwareActionLogAndVisibleSecretCode() throws IOException {
            LocalSubmitActionRuntimeConfig config = config("p6d_match_state_actor_view", "secret-api-key", "actor-view-match");

            try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
                server.start();

                postSubmitAction(server.port(), config.path(), "secret-api-key",
                    new SubmitActionHttpRequest(
                        "actor-view-match",
                        "p1",
                        0L,
                        "actor-view-secret",
                        Map.of("type", "SECRET_CODE_SET", "secretCode", List.of("7", "K", "9", "P"))
                    ));

                postSubmitAction(server.port(), config.path(), "secret-api-key",
                    new SubmitActionHttpRequest(
                        "actor-view-match",
                        "p1",
                        1L,
                        "actor-view-guess",
                        Map.of("type", "SUBMIT_GUESS", "guess", List.of("7", "K", "2", "X"))
                    ));

                HttpURLConnection ownerConnection = openMatchStateConnection(server.port(), "actor-view-match", "p1", "secret-api-key");
                assertEquals(200, ownerConnection.getResponseCode());
                MatchStateHttpResponse ownerResponse = objectMapper.readValue(ownerConnection.getInputStream(), MatchStateHttpResponse.class);
                assertEquals(List.of("7", "K", "9", "P"), ownerResponse.visibleSecretCode());
                assertEquals(2, ownerResponse.actionLog().size());
                assertEquals("SECRET_CODE_SET", ownerResponse.actionLog().get(0).actionType());
                assertEquals("SUBMIT_GUESS", ownerResponse.actionLog().get(1).actionType());

                HttpURLConnection opponentConnection = openMatchStateConnection(server.port(), "actor-view-match", "p2", "secret-api-key");
                assertEquals(200, opponentConnection.getResponseCode());
                MatchStateHttpResponse opponentResponse = objectMapper.readValue(opponentConnection.getInputStream(), MatchStateHttpResponse.class);
                assertTrue(opponentResponse.visibleSecretCode().isEmpty());
                assertEquals(1, opponentResponse.actionLog().size());
                assertEquals("SUBMIT_GUESS", opponentResponse.actionLog().get(0).actionType());
            }
            }

    private LocalSubmitActionRuntimeConfig config(String dbNamePrefix, String apiKey, String matchId) {
        String dbName = dbNamePrefix + "_" + UUID.randomUUID().toString().replace("-", "");
        return configForDb(dbName, apiKey, matchId);
    }

    private LocalSubmitActionRuntimeConfig configForDb(String dbName, String apiKey, String matchId) {
        return new LocalSubmitActionRuntimeConfig(
                0,
                "/submit-action",
                "X-API-Key",
                apiKey,
                "X-Request-Id",
                "http://localhost:5173",
                "GET,POST,OPTIONS",
                "Content-Type,X-API-Key,X-Request-Id",
                600,
                "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa",
                "",
                "public",
                matchId,
                "p1",
                "p2"
        );
    }

    private HttpURLConnection openSubmitActionConnection(int port, String path, String apiKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI
                .create("http://localhost:" + port + path)
                .toURL()
                .openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-API-Key", apiKey);
        return connection;
    }

    private HttpURLConnection openMatchStateConnection(int port, String matchId, String apiKey) throws IOException {
        return openMatchStateConnection(port, matchId, null, apiKey);
    }

    private HttpURLConnection openMatchStateConnection(int port, String matchId, String actorId, String apiKey) throws IOException {
        String uri = "http://localhost:" + port + LocalMatchStateEndpoint.PATH + "?matchId=" + matchId;
        if (actorId != null && !actorId.isBlank()) {
            uri += "&actorId=" + actorId;
        }

        HttpURLConnection connection = (HttpURLConnection) URI
                .create(uri)
                .toURL()
                .openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-API-Key", apiKey);
        return connection;
    }

    private SubmitActionHttpResponse postSubmitAction(int port, String path, String apiKey, SubmitActionHttpRequest request) throws IOException {
        HttpURLConnection connection = openSubmitActionConnection(port, path, apiKey);
        connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));
        assertEquals(200, connection.getResponseCode());
        return objectMapper.readValue(connection.getInputStream(), SubmitActionHttpResponse.class);
    }
}

package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mastermindarena.deduction.infrastructure.preferences.PreferencesMatchStateStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSubmitActionHttpServerIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void p5a_serverBootsWithExternalizedConfig() {
        LocalSubmitActionRuntimeConfig config = new LocalSubmitActionRuntimeConfig(
                0,
                "/submit-action",
            "test-token",
            "/io/mastermindarena/test/p5a/boot",
                "boot-match",
                "p1",
                "p2"
        );

        try (LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config)) {
            server.start();
            assertTrue(server.port() > 0);
        }
    }

    @Test
    void p5a_submitActionNominalOverHttpReturns200() throws IOException {
        LocalSubmitActionRuntimeConfig config = new LocalSubmitActionRuntimeConfig(
                0,
                "/submit-action",
            "test-token",
            "/io/mastermindarena/test/p5a/nominal",
                "http-match",
                "p1",
                "p2"
        );

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
                connection.setRequestProperty("Authorization", "Bearer test-token");
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
        LocalSubmitActionRuntimeConfig config = new LocalSubmitActionRuntimeConfig(
                0,
                "/submit-action",
                "test-token",
                "/io/mastermindarena/test/p5c/health",
                "health-match",
                "p1",
                "p2"
        );

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
        LocalSubmitActionRuntimeConfig config = new LocalSubmitActionRuntimeConfig(
                0,
                "/submit-action",
                "secret-token",
                "/io/mastermindarena/test/p5b/unauthorized",
                "http-match",
                "p1",
                "p2"
        );

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
            connection.setRequestProperty("Authorization", "Bearer wrong-token");
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));

            assertEquals(401, connection.getResponseCode());
        }
    }

    @Test
    void p5b_submitActionAuthorizedPersistsState() throws IOException, BackingStoreException {
        String persistenceNode = "/io/mastermindarena/test/p5b/persistent";
        Preferences.userRoot().node(persistenceNode).removeNode();

        LocalSubmitActionRuntimeConfig config = new LocalSubmitActionRuntimeConfig(
                0,
                "/submit-action",
                "secret-token",
                persistenceNode,
                "persistent-match",
                "p1",
                "p2"
        );

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
            connection.setRequestProperty("Authorization", "Bearer secret-token");
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));

            assertEquals(200, connection.getResponseCode());
        }

        PreferencesMatchStateStore reloadedStore = new PreferencesMatchStateStore(Preferences.userRoot().node(persistenceNode));
        assertEquals(1L, reloadedStore.findById("persistent-match").orElseThrow().version());
        assertEquals("IN_PROGRESS", reloadedStore.findById("persistent-match").orElseThrow().status());
    }
}

package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.IdempotencyStore;
import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServer;
import io.mastermindarena.deduction.api.submitaction.LocalSubmitActionRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in tests: use a disposable PostgreSQL database, never production. */
class PostgresWorkflowTest {
    private JdbcPersistenceContext context;
    private JdbcRoomDirectory rooms;
    private static final String CODE = "[\"A\",\"B\",\"E\",\"F\"]";

    @BeforeEach void isolatedSchema() {
        String url = System.getenv("ARENA_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "ARENA_TEST_DB_URL enables isolated PostgreSQL tests");
        context = new JdbcPersistenceContext(url, System.getenv("ARENA_TEST_DB_USER"),
                System.getenv("ARENA_TEST_DB_PASSWORD"), "test_" + UUID.randomUUID().toString().replace("-", ""));
        new JdbcSchemaMigrator(context).migrateToLatest();
        rooms = new JdbcRoomDirectory(context);
    }

    @Test void credentialsAndSecretsSurviveNewInstances() {
        var host = rooms.create("Isolated room", "Alice", "shared");
        String id = host.room().roomId();
        assertThrows(IllegalStateException.class, () -> rooms.join(id, "Bob", "wrong"));
        var guest = rooms.join(id, "Bob", "shared");
        flow(context).submit(command(id, "p1", 0));
        flow(context).submit(command(id, "p2", 1));
        new JdbcSchemaMigrator(context).migrateToLatest();
        var reopened = new JdbcRoomDirectory(context);
        try (var connection = context.openConnection(); var sql = connection.createStatement();
             var rows = sql.executeQuery("SELECT count(*) FROM " + context.table("schema_version"))) {
            assertTrue(rows.next());
            assertEquals(8, rows.getInt(1));
        } catch (java.sql.SQLException failure) { fail(failure); }
        assertEquals("p2", reopened.resume(id + ".p2." + guest.token()).actorId());
        assertFalse(reopened.authorize(id, "p1", guest.token()));
        assertFalse(reopened.authorize("unknown", "p1", host.token()));
        assertThrows(IllegalStateException.class, () -> reopened.resume(id + ".p2.invalid"));
        var state = new JdbcMatchStateStore(context).findById(id).orElseThrow();
        assertEquals(2, state.version());
        assertEquals(2, state.actionLog().size());
        assertEquals("WAITING_GUESS", state.status());
        var replay = flow(context).submit(command(id, "p2", 1));
        assertEquals(2, replay.state().version());
        assertEquals(2, new JdbcMatchStateStore(context).findById(id).orElseThrow().actionLog().size());
    }

    @Test void apiRolesCannotReadPrivateTablesButJdbcOwnerCanPlay() throws Exception {
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            for (String role : java.util.List.of("anon", "authenticated")) {
                try (var lookup = connection.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname=?")) {
                    lookup.setString(1, role);
                    try (var rows = lookup.executeQuery()) {
                        if (!rows.next()) sql.execute("CREATE ROLE " + role + " NOLOGIN");
                    }
                }
                sql.execute("GRANT USAGE ON SCHEMA " + context.schema() + " TO " + role);
                sql.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + context.schema() + " TO " + role);
            }
            // Model an installation awaiting the security migration, with existing API grants.
            sql.execute("DELETE FROM " + context.table("schema_version") + " WHERE version=4");
        }
        new JdbcSchemaMigrator(context).migrateToLatest();
        var host = rooms.create("Private", "Alice", "");
        assertEquals(1, flow(context).submit(command(host.room().roomId(), "p1", 0)).state().version());
        for (String role : java.util.List.of("anon", "authenticated")) {
            for (String table : java.util.List.of("rooms", "match_state", "idempotency", "workflow_event", "schema_version")) {
                try (var connection = context.openConnection(); var sql = connection.createStatement()) {
                    try (var rows = sql.executeQuery("SELECT relrowsecurity FROM pg_class WHERE oid='" + context.table(table) + "'::regclass")) {
                        assertTrue(rows.next());
                        assertTrue(rows.getBoolean(1));
                    }
                    sql.execute("SET ROLE " + role);
                    var denied = assertThrows(java.sql.SQLException.class,
                            () -> sql.executeQuery("SELECT * FROM " + context.table(table)));
                    assertEquals("42501", denied.getSQLState());
                }
            }
        }
        // Even accidental SELECT grants cannot bypass RLS for these non-owner API roles.
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("GRANT SELECT ON " + context.table("rooms") + " TO anon");
            sql.execute("SET ROLE anon");
            try (var rows = sql.executeQuery("SELECT count(*) FROM " + context.table("rooms"))) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        }
    }

    @Test void idempotencyFailureRollsBackStateAndEvents() {
        var host = rooms.create("Rollback", "Alice", "");
        var store = new JdbcMatchStateStore(context);
        var events = new JdbcWorkflowEventSink(context);
        var realJournal = new JdbcIdempotencyStore(context);
        IdempotencyStore unavailable = new IdempotencyStore() {
            @Override public Optional<Entry> find(String key) { return realJournal.find(key); }
            @Override public void save(String key, Entry entry) { throw new IllegalStateException("test failure"); }
        };
        var flow = new SubmitActionOrchestrator(store, events, unavailable);
        assertThrows(IllegalStateException.class, () -> flow.submit(command(host.room().roomId(), "p1", 0)));
        assertEquals(0, store.findById(host.room().roomId()).orElseThrow().version());
        assertTrue(events.allEvents().isEmpty());
        assertEquals(1, flow(context).submit(command(host.room().roomId(), "p1", 0)).state().version());
    }

    @Test void correctedResultAndRematchSurviveNewJsonStoreInstances() {
        String id = rooms.create("JSON lifecycle", "Alice", "").room().roomId();
        var flow = flow(context);
        flow.submit(command(id, "p1", 0));
        flow.submit(command(id, "p2", 1));
        for (String actor : java.util.List.of("p1", "p2")) {
            var state = new JdbcMatchStateStore(context).findById(id).orElseThrow();
            flow.submit(new SubmitActionCommand(id, actor, state.version(), UUID.randomUUID().toString(), "PLAY_GUESS", CODE, null));
        }
        for (String actor : java.util.List.of("p1", "p2")) {
            var state = new JdbcMatchStateStore(context).findById(id).orElseThrow();
            String feedback = actor.equals("p2") ? "{\"bienPlaces\":4,\"malPlaces\":0}" : "{\"bienPlaces\":0,\"malPlaces\":0}";
            flow.submit(new SubmitActionCommand(id, actor, state.version(), UUID.randomUUID().toString(), "SEND_FEEDBACK", null, feedback));
        }
        var won = new JdbcMatchStateStore(context).findById(id).orElseThrow();
        assertEquals("p1", won.winnerId());
        assertEquals(1, won.scores().get("p1"));
        long feedbackVersion = won.actionLog().getLast().version();
        var correction = new SubmitActionCommand(id, "p2", won.version(), "correction", "CORRECT_FEEDBACK",
                "{\"feedbackVersion\":" + feedbackVersion + "}", "{\"bienPlaces\":3,\"malPlaces\":0}");
        flow(context).submit(correction);
        var reopened = new JdbcMatchStateStore(context).findById(id).orElseThrow();
        assertNull(reopened.winnerId());
        assertTrue(reopened.scores().isEmpty());
        assertEquals("WAITING_GUESS", reopened.status());
        assertEquals(reopened, flow(context).submit(correction).state());
        flow(context).submit(new SubmitActionCommand(id, "p2", reopened.version(), "abandon", "ABANDON", null, null));
        for (String actor : java.util.List.of("p1", "p2")) {
            var state = new JdbcMatchStateStore(context).findById(id).orElseThrow();
            flow(context).submit(new SubmitActionCommand(id, actor, state.version(), "rematch-" + actor, "REQUEST_REMATCH", null, null));
        }
        var rematch = new JdbcMatchStateStore(context).findById(id).orElseThrow();
        assertEquals(2, rematch.gameNumber());
        assertEquals(1, rematch.scores().get("p1"));
        assertTrue(rematch.actionLog().isEmpty());
    }

    @Test void twoInstancesCannotCommitTheSameVersion() throws Exception {
        String id = rooms.create("Concurrency", "Alice", "").room().roomId();
        JdbcPersistenceContext other = new JdbcPersistenceContext(System.getenv("ARENA_TEST_DB_URL"),
                System.getenv("ARENA_TEST_DB_USER"), System.getenv("ARENA_TEST_DB_PASSWORD"), context.schema());
        var first = flow(context);
        var second = flow(other);
        assertEquals(1, race(() -> submitAccepted(first, command(id, "p1", 0)),
                () -> submitAccepted(second, command(id, "p2", 0))));
        var state = new JdbcMatchStateStore(context).findById(id).orElseThrow();
        assertEquals(1, state.version());
        assertEquals(1, state.actionLog().size());
        String missing = state.actionLog().getFirst().actorId().equals("p1") ? "p2" : "p1";
        assertEquals(2, second.submit(command(id, missing, 1)).state().version());
    }

    @Test void twoConcurrentGuestsCannotTakeTheSameSeat() throws Exception {
        String id = rooms.create("Join race", "Alice", "").room().roomId();
        assertEquals(1, race(() -> joinAccepted(id, "Bob"), () -> joinAccepted(id, "Charlie")));
        assertTrue(rooms.findInfo(id).orElseThrow().joined());
    }

    @Test void realHttpRejectsImpersonationAndMalformedBodies() throws Exception {
        var config = new LocalSubmitActionRuntimeConfig(0, "/local/submit-action", "X-API-Key",
                "local-http-validation-only-123", "X-Request-Id", "http://localhost:5175",
                "GET,POST,OPTIONS", "Content-Type,X-API-Key,X-Room-Token", 60,
                System.getenv("ARENA_TEST_DB_URL"), System.getenv("ARENA_TEST_DB_USER"),
                System.getenv("ARENA_TEST_DB_PASSWORD"), context.schema(), "seed", "p1", "p2", true);
        try (var server = LocalSubmitActionHttpServer.create(config); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var json = new ObjectMapper();
            var created = http(client, base + "/local/rooms", "POST",
                    "{\"name\":\"HTTP test\",\"pseudo\":\"Alice\"}", null);
            assertEquals(201, created.statusCode());
            var admission = json.readTree(created.body());
            String id = admission.path("room").path("roomId").asText();
            String token = admission.path("token").asText();
            String stateUrl = base + "/local/match-state?matchId=" + id + "&actorId=p1";
            assertEquals(200, http(client, stateUrl, "GET", null, token).statusCode());
            assertEquals(403, http(client, stateUrl, "GET", null, "fake").statusCode());
            assertEquals(403, http(client, base + "/local/match-state?matchId=" + id + "&actorId=p2",
                    "GET", null, token).statusCode());
            var resumed = http(client, base + "/local/rooms/resume", "POST",
                    "{\"recoveryCode\":\"" + id + ".p1." + token + "\"}", null);
            assertEquals(200, resumed.statusCode());
            assertEquals("p1", json.readTree(resumed.body()).path("actorId").asText());
            var malformed = http(client, base + "/local/submit-action", "POST", "{", token);
            assertEquals(400, malformed.statusCode());
            assertEquals("INVALID_REQUEST", json.readTree(malformed.body()).path("code").asText());
            assertEquals(400, http(client, base + "/local/rooms", "POST", "x".repeat(65_537), null).statusCode());
            assertEquals(0, new JdbcMatchStateStore(context).findById(id).orElseThrow().version());
        }
    }

    @Test void realHttpKeepsPendingActionsPrivateAndRejectsForeignCorrections() throws Exception {
        var json = new ObjectMapper();
        var config = httpConfig();
        try (var server = LocalSubmitActionHttpServer.create(config); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var host = rooms.create("Confidentiality", "Alice", "");
            String id = host.room().roomId();
            var guest = rooms.join(id, "Bob", "");
            String hostView = base + "/local/match-state?matchId=" + id + "&actorId=p1";
            String guestView = base + "/local/match-state?matchId=" + id + "&actorId=p2";
            String otherCode = "[\"D\",\"D\",\"C\",\"C\"]";
            acceptedHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
            acceptedHttp(client, base, id, "p2", guest.token(), 1, "READY_SECRET", otherCode, null);
            var view = json.readTree(http(client, hostView, "GET", null, host.token()).body());
            assertEquals(json.readTree(CODE), view.path("visibleSecretCode"));
            assertTrue(view.path("opponentSecretCode").isEmpty());
            assertTrue(view.path("turns").isEmpty());
            String publicInfo = http(client, base + "/local/rooms?roomId=" + id, "GET", null, null).body();
            assertFalse(publicInfo.contains(host.token()));
            assertFalse(publicInfo.contains(guest.token()));
            assertFalse(publicInfo.contains("Secret"));

            acceptedHttp(client, base, id, "p1", host.token(), 2, "PLAY_GUESS", CODE, null);
            assertTrue(json.readTree(http(client, guestView, "GET", null, guest.token()).body()).path("turns").isEmpty());
            acceptedHttp(client, base, id, "p2", guest.token(), 3, "PLAY_GUESS", otherCode, null);
            assertEquals(2, json.readTree(http(client, guestView, "GET", null, guest.token()).body()).path("turns").size());
            String feedback = "{\"bienPlaces\":1,\"malPlaces\":0}";
            acceptedHttp(client, base, id, "p1", host.token(), 4, "SEND_FEEDBACK", null, feedback);
            assertEquals(2, json.readTree(http(client, guestView, "GET", null, guest.token()).body()).path("turns").size());
            acceptedHttp(client, base, id, "p2", guest.token(), 5, "SEND_FEEDBACK", null, feedback);
            var before = new JdbcMatchStateStore(context).findById(id).orElseThrow();
            int eventsBefore = new JdbcWorkflowEventSink(context).allEvents().size();
            var denied = submitHttp(client, base, id, "p2", guest.token(), 6, "CORRECT_FEEDBACK",
                    "{\"feedbackVersion\":5}", feedback);
            assertEquals(403, denied.statusCode());
            assertEquals("ACTOR_NOT_AUTHORIZED", json.readTree(denied.body()).path("rejectionCode").asText());
            assertFalse(json.readTree(denied.body()).path("accepted").asBoolean());
            assertEquals(403, submitHttp(client, base, id, "p1", guest.token(), 6, "CORRECT_FEEDBACK",
                    "{\"feedbackVersion\":5}", feedback).statusCode());
            assertEquals(before, new JdbcMatchStateStore(context).findById(id).orElseThrow());
            assertEquals(eventsBefore, new JdbcWorkflowEventSink(context).allEvents().size());

            acceptedHttp(client, base, id, "p2", guest.token(), 6, "PLAY_GUESS", otherCode, null);
            acceptedHttp(client, base, id, "p1", host.token(), 7, "CORRECT_FEEDBACK", "{\"feedbackVersion\":5}", feedback);
            acceptedHttp(client, base, id, "p1", host.token(), 8, "CONTEST_FEEDBACK", "{\"feedbackVersion\":6}", null);
            var pending = json.readTree(http(client, hostView, "GET", null, host.token()).body());
            for (var action : pending.path("turns")) {
                assertFalse(action.path("actorId").asText().equals("p2")
                        && action.path("actionType").asText().equals("PLAY_GUESS") && action.path("turnNumber").asInt() == 2);
            }
            assertEquals(9, pending.path("version").asInt());
            assertTrue(pending.path("opponentSecretCode").isEmpty());
            acceptedHttp(client, base, id, "p1", host.token(), 9, "PLAY_GUESS", CODE, null);
            acceptedHttp(client, base, id, "p2", guest.token(), 10, "SEND_FEEDBACK", null, feedback);
            acceptedHttp(client, base, id, "p1", host.token(), 11, "CORRECT_FEEDBACK", "{\"feedbackVersion\":5}", feedback);
            var hiddenFeedback = json.readTree(http(client, hostView, "GET", null, host.token()).body());
            assertEquals("WAITING_FEEDBACK", hiddenFeedback.path("state").asText());
            for (var action : hiddenFeedback.path("turns")) {
                assertFalse(action.path("actorId").asText().equals("p2")
                        && action.path("actionType").asText().equals("SEND_FEEDBACK") && action.path("turnNumber").asInt() == 2);
            }
            assertEquals(12, hiddenFeedback.path("version").asInt());
        }
    }

    @Test void realHttpPreservesCorsMethodsApiKeyAndRequestIdentity() throws Exception {
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var options = HttpRequest.newBuilder(URI.create(base + "/local/rooms/join"))
                    .header("Origin", "http://localhost:5175").header("X-Request-Id", "protocol-test")
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
            var preflight = client.send(options, HttpResponse.BodyHandlers.ofString());
            assertEquals(204, preflight.statusCode());
            assertEquals("", preflight.body());
            assertEquals("http://localhost:5175", preflight.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
            assertEquals("protocol-test", preflight.headers().firstValue("X-Request-Id").orElseThrow());
            var forbidden = client.send(HttpRequest.newBuilder(URI.create(base + "/health"))
                    .header("Origin", "http://untrusted.test").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, forbidden.statusCode());
            assertTrue(forbidden.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
            var unauthorized = client.send(HttpRequest.newBuilder(URI.create(base + "/local/rooms")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(401, unauthorized.statusCode());
            assertEquals(405, http(client, base + "/local/rooms/join", "GET", null, null).statusCode());
            var health = http(client, base + "/health", "GET", null, null);
            assertEquals(200, health.statusCode());
            assertEquals("UP", new ObjectMapper().readTree(health.body()).path("status").asText());
        }
    }

    @Test void realHttpReturnsJsonForInvalidBodiesAndUnavailablePersistence() throws Exception {
        var json = new ObjectMapper();
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var host = rooms.create("Error responses", "Alice", "");
            String id = host.room().roomId();
            for (String path : java.util.List.of("/local/rooms", "/local/rooms/join", "/local/rooms/resume", "/local/submit-action")) {
                for (String body : java.util.List.of("null", "[]", "true", "", "{")) {
                    var response = http(client, base + path, "POST", body, host.token());
                    assertEquals(400, response.statusCode(), path);
                    assertEquals("INVALID_REQUEST", json.readTree(response.body()).path("code").asText(), path);
                    assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
                    assertFalse(response.headers().firstValue("X-Request-Id").orElseThrow().isBlank());
                }
            }
            assertEquals(0, new JdbcMatchStateStore(context).findById(id).orElseThrow().version());
            rooms.join(id, "Bob", "");
            try (var connection = context.openConnection(); var sql = connection.createStatement()) {
                sql.execute("ALTER TABLE " + context.table("match_state") + " RENAME TO unavailable_match_state");
            }
            var brokenState = http(client, base + "/local/match-state?matchId=" + id + "&actorId=p1", "GET", null, host.token());
            assertEquals(500, brokenState.statusCode());
            assertEquals("{\"code\":\"SERVER_ERROR\"}", brokenState.body());
            var brokenSubmit = submitHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
            assertEquals(500, brokenSubmit.statusCode(), "A persistence failure is not a player rejection");
            assertEquals("{\"code\":\"SERVER_ERROR\"}", brokenSubmit.body());
            try (var connection = context.openConnection(); var sql = connection.createStatement()) {
                sql.execute("ALTER TABLE " + context.table("rooms") + " RENAME TO unavailable_rooms");
            }
            for (String path : java.util.List.of("/local/rooms", "/local/rooms?roomId=" + id)) {
                var brokenRoom = http(client, base + path, "GET", null, null);
                assertEquals(500, brokenRoom.statusCode());
                assertEquals("{\"code\":\"ROOM_SERVER_ERROR\"}", brokenRoom.body());
            }
            var brokenResume = http(client, base + "/local/rooms/resume", "POST",
                    json.writeValueAsString(java.util.Map.of("recoveryCode", id + ".p1." + host.token())), null);
            assertEquals(500, brokenResume.statusCode());
            assertEquals("{\"code\":\"ROOM_SERVER_ERROR\"}", brokenResume.body());
            assertEquals(200, http(client, base + "/health", "GET", null, null).statusCode());
        }
    }

    @Test void realHttpMasksInvalidStoredDocumentsAndRollsBackEveryWriteStage() throws Exception {
        var host = rooms.create("Persistence classification", "Alice", "");
        String id = host.room().roomId();
        rooms.join(id, "Bob", "");
        String original;
        java.time.Instant originalActivity;
        try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                "SELECT payload,updated_at FROM " + context.table("match_state") + " WHERE match_id=?")) {
            sql.setString(1, id);
            try (var rows = sql.executeQuery()) {
                rows.next(); original = rows.getString(1); originalActivity = rows.getTimestamp(2).toInstant();
            }
        }
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                    "UPDATE " + context.table("match_state") + " SET payload=? WHERE match_id=?")) {
                sql.setString(1, "{\"formatVersion\":1,\"state\":null}");
                sql.setString(2, id);
                sql.executeUpdate();
            }
            try {
                var read = http(client, base + "/local/match-state?matchId=" + id + "&actorId=p1", "GET", null, host.token());
                assertEquals(500, read.statusCode());
                assertEquals("{\"code\":\"SERVER_ERROR\"}", read.body());
                var submit = submitHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
                assertEquals(500, submit.statusCode());
                assertEquals("{\"code\":\"SERVER_ERROR\"}", submit.body());
            } finally {
                try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                        "UPDATE " + context.table("match_state") + " SET payload=? WHERE match_id=?")) {
                    sql.setString(1, original); sql.setString(2, id); sql.executeUpdate();
                }
            }
            for (String table : java.util.List.of("match_state", "workflow_event", "idempotency")) {
                try (var connection = context.openConnection(); var sql = connection.createStatement()) {
                    sql.execute("ALTER TABLE " + context.table(table) + " ADD CONSTRAINT test_write_failure CHECK (false) NOT VALID");
                }
                try {
                    var submit = submitHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
                    assertEquals(500, submit.statusCode(), table);
                    assertEquals("{\"code\":\"SERVER_ERROR\"}", submit.body());
                } finally {
                    try (var connection = context.openConnection(); var sql = connection.createStatement()) {
                        sql.execute("ALTER TABLE " + context.table(table) + " DROP CONSTRAINT test_write_failure");
                    }
                }
                try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                        "SELECT payload,updated_at FROM " + context.table("match_state") + " WHERE match_id=?")) {
                    sql.setString(1, id);
                    try (var rows = sql.executeQuery()) {
                        rows.next(); assertEquals(original, rows.getString(1));
                        assertEquals(originalActivity, rows.getTimestamp(2).toInstant());
                    }
                }
                assertTrue(new JdbcWorkflowEventSink(context).allEvents().isEmpty());
                try (var connection = context.openConnection(); var sql = connection.createStatement();
                     var rows = sql.executeQuery("SELECT count(*) FROM " + context.table("idempotency"))) {
                    rows.next(); assertEquals(0, rows.getLong(1));
                }
            }
            acceptedHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
        }
    }

    @Test void realHttpRejectsCoercedFieldsAndAmbiguousJsonWithoutMutation() throws Exception {
        var json = new ObjectMapper();
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var host = rooms.create("Strict input", "Alice", "");
            String id = host.room().roomId();
            var command = json.createObjectNode().put("matchId", id).put("actorId", "p1")
                    .put("expectedVersion", 0).put("idempotencyKey", "strict-input-operation");
            command.putObject("actionPayload").put("actionType", "READY_SECRET").put("payload", CODE);
            for (String field : java.util.List.of("expectedVersion", "matchId", "actorId", "idempotencyKey", "actionPayload")) {
                for (String value : java.util.List.of("null", "true", "[]", "{}", "1.5", "-1", "9223372036854775808", "\"\"", "\"0\"")) {
                    if (!field.equals("expectedVersion") && value.equals("\"0\"")) continue;
                    var invalid = command.deepCopy();
                    invalid.set(field, json.readTree(value));
                    assertInvalidHttp(client, base + "/local/submit-action", invalid.toString(), host.token());
                }
                var missing = command.deepCopy();
                missing.remove(field);
                assertInvalidHttp(client, base + "/local/submit-action", missing.toString(), host.token());
            }
            for (String field : java.util.List.of("actionType", "payload", "feedback")) {
                var invalid = command.deepCopy();
                ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.get("actionPayload")).put(field, 123);
                assertInvalidHttp(client, base + "/local/submit-action", invalid.toString(), host.token());
            }
            var missingAction = command.deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) missingAction.get("actionPayload")).remove("actionType");
            assertInvalidHttp(client, base + "/local/submit-action", missingAction.toString(), host.token());
            assertInvalidHttp(client, base + "/local/submit-action", command + " null", host.token());
            assertInvalidHttp(client, base + "/local/submit-action",
                    command.toString().replace("\"actionType\":\"READY_SECRET\"", "\"actionType\":\"ABANDON\",\"actionType\":\"READY_SECRET\""), host.token());
            for (String body : java.util.List.of("{\"name\":123,\"pseudo\":\"Alice\"}",
                    "{\"name\":\"Test\",\"pseudo\":true}", "{\"name\":\"Test\",\"pseudo\":\"Alice\",\"accessCode\":123}",
                    "{\"name\":\"First\",\"name\":\"Second\",\"pseudo\":\"Alice\"}",
                    "{\"name\":\"Test\",\"pseudo\":\"Alice\"} {}")) {
                assertInvalidHttp(client, base + "/local/rooms", body, null);
            }
            assertInvalidHttp(client, base + "/local/rooms/join", "{\"roomId\":123,\"pseudo\":\"Bob\"}", null);
            assertInvalidHttp(client, base + "/local/rooms/resume", "{\"recoveryCode\":123}", null);
            assertEquals(1, rooms.waitingRooms().size());
            assertEquals(0, new JdbcMatchStateStore(context).findById(id).orElseThrow().version());
            rooms.join(id, "Bob", "");
            acceptedHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
        }
    }

    private static void assertInvalidHttp(HttpClient client, String url, String body, String token) throws Exception {
        var response = http(client, url, "POST", body, token);
        assertEquals(400, response.statusCode());
        assertEquals("{\"code\":\"INVALID_REQUEST\"}", response.body());
    }

    @Test void realHttpRejectsDuplicateQueryParametersIncludingEncodedNames() throws Exception {
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var host = rooms.create("Query contract", "Alice", "");
            String id = host.room().roomId();
            for (String query : java.util.List.of("roomId=other&roomId=" + id, "roomId=" + id + "&%72oomId=" + id)) {
                var response = http(client, base + "/local/rooms?" + query, "GET", null, null);
                assertEquals(400, response.statusCode());
                assertEquals("{\"code\":\"INVALID_REQUEST\"}", response.body());
            }
            for (String query : java.util.List.of("matchId=other&matchId=" + id + "&actorId=p1",
                    "matchId=" + id + "&actorId=p2&actorId=p1",
                    "matchId=" + id + "&actorId=p1&%61ctorId=p1")) {
                var response = http(client, base + "/local/match-state?" + query, "GET", null, host.token());
                assertEquals(400, response.statusCode());
                assertEquals("{\"code\":\"INVALID_REQUEST\"}", response.body());
            }
            assertEquals(200, http(client, base + "/local/rooms?%72oomId=" + id, "GET", null, null).statusCode());
            assertEquals(200, http(client, base + "/local/match-state?matchId=" + id + "&actorId=%70%31", "GET", null, host.token()).statusCode());
            assertEquals(403, http(client, base + "/local/match-state?matchId=" + id + "&actorId=p2", "GET", null, host.token()).statusCode());
            assertEquals(0, new JdbcMatchStateStore(context).findById(id).orElseThrow().version());
        }
    }

    @Test void realHttpCountsEveryResponseExactlyOnceIncludingLobbyAndPreflights() throws Exception {
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var json = new ObjectMapper();
            var host = rooms.create("Metrics", "Alice", "");
            String id = host.room().roomId();
            long ok = 1, ko = 0;
            var initial = json.readTree(http(client, base + "/health", "GET", null, null).body());
            assertEquals(ok, initial.path("requestsOk").asLong());
            assertEquals(ko, initial.path("requestsKo").asLong());
            for (String[] spec : java.util.List.of(
                    new String[]{"/local/rooms", "GET", "", "200"},
                    new String[]{"/local/rooms?roomId=" + id, "GET", "", "200"},
                    new String[]{"/local/rooms", "POST", "{\"name\":\"New\",\"pseudo\":\"Bob\"}", "201"},
                    new String[]{"/local/rooms/join", "POST", "{\"roomId\":\"missing\",\"pseudo\":\"Bob\"}", "404"},
                    new String[]{"/local/rooms/resume", "POST", "{\"recoveryCode\":\"invalid\"}", "403"},
                    new String[]{"/local/rooms", "POST", "null", "400"},
                    new String[]{"/local/rooms/join", "GET", "", "405"},
                    new String[]{"/local/match-state?matchId=" + id + "&actorId=p2", "GET", "", "403"},
                    new String[]{"/local/submit-action", "POST", json.writeValueAsString(java.util.Map.of(
                            "matchId", id, "actorId", "p1", "expectedVersion", 0, "idempotencyKey", "metrics-test",
                            "actionPayload", java.util.Map.of("actionType", "READY_SECRET", "payload", CODE))), "409"},
                    new String[]{"/local/rooms", "OPTIONS", "", "204"},
                    new String[]{"/local/rooms/join", "OPTIONS", "", "204"},
                    new String[]{"/local/rooms/resume", "OPTIONS", "", "204"},
                    new String[]{"/local/match-state", "OPTIONS", "", "204"},
                    new String[]{"/local/submit-action", "OPTIONS", "", "204"},
                    new String[]{"/health", "OPTIONS", "", "204"})) {
                var response = http(client, base + spec[0], spec[1], spec[2].isEmpty() ? null : spec[2], host.token());
                int status = Integer.parseInt(spec[3]);
                assertEquals(status, response.statusCode(), spec[0]);
                if (status < 400) ok++; else ko++;
                // The health request itself is included, as in the existing health contract.
                ok++;
                var totals = json.readTree(http(client, base + "/health", "GET", null, null).body());
                assertEquals(ok, totals.path("requestsOk").asLong(), spec[0]);
                assertEquals(ko, totals.path("requestsKo").asLong(), spec[0]);
            }
        }
    }

    @Test void restrictedRuntimeCanPlayButCannotMigrateDeleteOrChangePermissions() throws Exception {
        String role = "runtime_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE ROLE " + role + " LOGIN PASSWORD 'runtime_validation_only' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS");
        }
        var migrator = new JdbcSchemaMigrator(context);
        assertThrows(IllegalArgumentException.class, () -> migrator.grantRuntimeAccess("anon"));
        assertThrows(IllegalArgumentException.class, () -> migrator.grantRuntimeAccess(System.getenv("ARENA_TEST_DB_USER")));
        migrator.grantRuntimeAccess(role);
        migrator.grantRuntimeAccess(role);
        var runtime = new JdbcPersistenceContext(System.getenv("ARENA_TEST_DB_URL"), role, "runtime_validation_only", context.schema());
        new JdbcSchemaMigrator(runtime).verifyLatest();
        for (String sqlText : java.util.List.of("CREATE TABLE " + context.table("forbidden") + " (id int)",
                "ALTER TABLE " + context.table("rooms") + " DISABLE ROW LEVEL SECURITY",
                "DELETE FROM " + context.table("rooms"), "DELETE FROM " + context.table("match_state"),
                "CALL " + context.table("purge_expired_matches") + "()",
                "CALL " + context.table("purge_expired_matches") + "(clock_timestamp(), 0)",
                "UPDATE " + context.table("schema_version") + " SET version=99",
                "UPDATE " + context.table("idempotency") + " SET fingerprint='changed'")) {
            try (var connection = runtime.openConnection(); var sql = connection.createStatement()) {
                assertEquals("42501", assertThrows(java.sql.SQLException.class, () -> sql.execute(sqlText)).getSQLState());
            }
        }
        var config = new LocalSubmitActionRuntimeConfig(0, "/local/submit-action", "X-API-Key",
                "local-http-validation-only-123", "X-Request-Id", "http://localhost:5175",
                "GET,POST,OPTIONS", "Content-Type,X-API-Key,X-Room-Token", 60,
                System.getenv("ARENA_TEST_DB_URL"), role, "runtime_validation_only", context.schema(), "seed", "p1", "p2", false);
        try (var server = LocalSubmitActionHttpServer.create(config); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var json = new ObjectMapper();
            var hostResponse = http(client, base + "/local/rooms", "POST", "{\"name\":\"Limited role\",\"pseudo\":\"Alice\"}", null);
            assertEquals(201, hostResponse.statusCode());
            var host = json.readTree(hostResponse.body());
            String id = host.path("room").path("roomId").asText(), token = host.path("token").asText();
            var joined = http(client, base + "/local/rooms/join", "POST", "{\"roomId\":\"" + id + "\",\"pseudo\":\"Bob\"}", null);
            assertEquals(200, joined.statusCode());
            String guestToken = json.readTree(joined.body()).path("token").asText();
            acceptedHttp(client, base, id, "p1", token, 0, "READY_SECRET", CODE, null);
            acceptedHttp(client, base, id, "p2", guestToken, 1, "READY_SECRET", CODE, null);
            acceptedHttp(client, base, id, "p2", guestToken, 2, "ABANDON", null, null);
            var read = http(client, base + "/local/match-state?matchId=" + id + "&actorId=p1", "GET", null, token);
            assertEquals(200, read.statusCode());
            assertEquals(1, json.readTree(read.body()).path("scores").path("p1").asInt());
            assertEquals(200, http(client, base + "/local/rooms/resume", "POST",
                    json.writeValueAsString(java.util.Map.of("recoveryCode", id + ".p1." + token)), null).statusCode());
        }
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("DELETE FROM " + context.table("schema_version") + " WHERE version=5");
        }
        assertThrows(IllegalStateException.class, () -> new JdbcSchemaMigrator(runtime).verifyLatest());
        assertThrows(IllegalStateException.class, () -> LocalSubmitActionHttpServer.create(config));
    }

    @Test void removedDiscoveryCommandIsInvalidBeforeAnyPersistence() throws Exception {
        var host = rooms.create("Removed command", "Alice", "");
        String id = host.room().roomId();
        rooms.join(id, "Bob", "");
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var before = new JdbcMatchStateStore(context).findById(id).orElseThrow();
            java.time.Instant activity;
            try (var connection = context.openConnection(); var query = connection.prepareStatement(
                    "SELECT updated_at FROM " + context.table("match_state") + " WHERE match_id=?")) {
                query.setString(1, id);
                try (var rows = query.executeQuery()) { rows.next(); activity = rows.getTimestamp(1).toInstant(); }
            }
            var denied = submitHttp(client, base, id, "p1", host.token(), 0, "DECLARE_DISCOVERY", null, null);
            assertEquals(400, denied.statusCode());
            assertEquals("{\"code\":\"INVALID_REQUEST\"}", denied.body());
            assertEquals(before, new JdbcMatchStateStore(context).findById(id).orElseThrow());
            assertTrue(new JdbcWorkflowEventSink(context).allEvents().isEmpty());
            try (var connection = context.openConnection(); var query = connection.prepareStatement(
                    "SELECT updated_at, (SELECT count(*) FROM " + context.table("idempotency")
                            + " WHERE match_id=?) FROM " + context.table("match_state") + " WHERE match_id=?")) {
                query.setString(1, id); query.setString(2, id);
                try (var rows = query.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals(activity, rows.getTimestamp(1).toInstant());
                    assertEquals(0, rows.getLong(2));
                }
            }
            acceptedHttp(client, base, id, "p1", host.token(), 0, "READY_SECRET", CODE, null);
        }
    }

    @Test void compactHttpReceiptStaysExactAfterLaterActionsCorrectionAndRematch() throws Exception {
        var host = rooms.create("Compact replay", "Alice", "");
        String id = host.room().roomId(), token = host.token();
        var guest = rooms.join(id, "Bob", "");
        var json = new ObjectMapper();
        String first = json.writeValueAsString(java.util.Map.of("matchId", id, "actorId", "p1",
                "expectedVersion", 0, "idempotencyKey", "original-receipt",
                "actionPayload", java.util.Map.of("actionType", "READY_SECRET", "payload", CODE)));
        try (var server = LocalSubmitActionHttpServer.create(httpConfig()); var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            var original = http(client, base + "/local/submit-action", "POST", first, token);
            assertEquals(200, original.statusCode());
            acceptedHttp(client, base, id, "p2", guest.token(), 1, "READY_SECRET", CODE, null);
            assertExactReplay(client, base, id, token, first, original.body());
            acceptedHttp(client, base, id, "p1", token, 2, "PLAY_GUESS", CODE, null);
            acceptedHttp(client, base, id, "p2", guest.token(), 3, "PLAY_GUESS", CODE, null);
            acceptedHttp(client, base, id, "p1", token, 4, "SEND_FEEDBACK", null, "{\"bienPlaces\":0,\"malPlaces\":0}");
            acceptedHttp(client, base, id, "p2", guest.token(), 5, "SEND_FEEDBACK", null, "{\"bienPlaces\":4,\"malPlaces\":0}");
            assertExactReplay(client, base, id, token, first, original.body());
            acceptedHttp(client, base, id, "p2", guest.token(), 6, "CORRECT_FEEDBACK", "{\"feedbackVersion\":6}", "{\"bienPlaces\":3,\"malPlaces\":0}");
            assertTrue(new JdbcMatchStateStore(context).findById(id).orElseThrow().scores().isEmpty());
            assertExactReplay(client, base, id, token, first, original.body());
            acceptedHttp(client, base, id, "p2", guest.token(), 7, "ABANDON", null, null);
            acceptedHttp(client, base, id, "p1", token, 8, "REQUEST_REMATCH", null, null);
            acceptedHttp(client, base, id, "p2", guest.token(), 9, "REQUEST_REMATCH", null, null);
            assertEquals(2, new JdbcMatchStateStore(context).findById(id).orElseThrow().gameNumber());
            assertExactReplay(client, base, id, token, first, original.body());
            var conflict = http(client, base + "/local/submit-action", "POST", first.replace("READY_SECRET", "PLAY_GUESS"), token);
            assertEquals(409, conflict.statusCode());
            assertEquals("IDEMPOTENCY_CONFLICT", json.readTree(conflict.body()).path("rejectionCode").asText());
            try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                    "SELECT result_payload FROM " + context.table("idempotency") + " WHERE match_id=?")) {
                sql.setString(1, id);
                try (var rows = sql.executeQuery()) {
                    int count = 0;
                    while (rows.next()) {
                        var stored = json.readTree(rows.getString(1));
                        assertEquals(2, stored.path("formatVersion").asInt());
                        assertFalse(stored.has("result"));
                        assertFalse(stored.path("receipt").has("actionLog"));
                        assertFalse(stored.path("receipt").has("action"));
                        count++;
                    }
                    assertEquals(10, count);
                }
            }
        }
    }

    private void assertExactReplay(HttpClient client, String base, String id, String token, String request, String original) throws Exception {
        var state = new JdbcMatchStateStore(context).findById(id).orElseThrow();
        int events = new JdbcWorkflowEventSink(context).allEvents().size();
        java.time.Instant activity;
        try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                "SELECT updated_at FROM " + context.table("match_state") + " WHERE match_id=?")) {
            sql.setString(1, id);
            try (var rows = sql.executeQuery()) { rows.next(); activity = rows.getTimestamp(1).toInstant(); }
        }
        var replay = http(client, base + "/local/submit-action", "POST", request, token);
        assertEquals(200, replay.statusCode());
        assertEquals(new ObjectMapper().readTree(original), new ObjectMapper().readTree(replay.body()));
        assertEquals(state, new JdbcMatchStateStore(context).findById(id).orElseThrow());
        assertEquals(events, new JdbcWorkflowEventSink(context).allEvents().size());
        try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                "SELECT updated_at FROM " + context.table("match_state") + " WHERE match_id=?")) {
            sql.setString(1, id);
            try (var rows = sql.executeQuery()) { rows.next(); assertEquals(activity, rows.getTimestamp(1).toInstant()); }
        }
        var read = http(client, base + "/local/match-state?matchId=" + id + "&actorId=p1", "GET", null, token);
        assertEquals(200, read.statusCode());
        assertEquals(state.version(), new ObjectMapper().readTree(read.body()).path("version").asLong());
    }

    private LocalSubmitActionRuntimeConfig httpConfig() {
        return new LocalSubmitActionRuntimeConfig(0, "/local/submit-action", "X-API-Key",
                "local-http-validation-only-123", "X-Request-Id", "http://localhost:5175",
                "GET,POST,OPTIONS", "Content-Type,X-API-Key,X-Room-Token", 60,
                System.getenv("ARENA_TEST_DB_URL"), System.getenv("ARENA_TEST_DB_USER"),
                System.getenv("ARENA_TEST_DB_PASSWORD"), context.schema(), "seed", "p1", "p2", true);
    }

    private static void acceptedHttp(HttpClient client, String base, String id, String actor, String token,
            long version, String action, String payload, String feedback) throws Exception {
        var response = submitHttp(client, base, id, actor, token, version, action, payload, feedback);
        assertEquals(200, response.statusCode());
        var body = new ObjectMapper().readTree(response.body());
        assertTrue(body.path("accepted").asBoolean());
        assertEquals(version + 1, body.path("version").asLong());
    }

    private static HttpResponse<String> submitHttp(HttpClient client, String base, String id, String actor,
            String token, long version, String action, String payload, String feedback) throws Exception {
        var actionBody = new java.util.LinkedHashMap<String, Object>();
        actionBody.put("actionType", action);
        actionBody.put("payload", payload);
        actionBody.put("feedback", feedback);
        String body = new ObjectMapper().writeValueAsString(java.util.Map.of("matchId", id, "actorId", actor,
                "expectedVersion", version, "idempotencyKey", UUID.randomUUID().toString(), "actionPayload", actionBody));
        return http(client, base + "/local/submit-action", "POST", body, token);
    }

    private static HttpResponse<String> http(HttpClient client, String url, String method, String body,
                                              String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("X-API-Key", "local-http-validation-only-123").header("Content-Type", "application/json");
        if (token != null) request.header("X-Room-Token", token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private boolean joinAccepted(String id, String pseudo) {
        try { rooms.join(id, pseudo, ""); return true; }
        catch (IllegalStateException rejected) {
            assertEquals("ROOM_FULL", rejected.getMessage()); return false;
        }
    }

    private static boolean submitAccepted(SubmitActionOrchestrator flow, SubmitActionCommand command) {
        try { return flow.submit(command).rejection() == null; }
        catch (IllegalStateException rejected) {
            assertEquals("VERSION_CONFLICT", rejected.getMessage()); return false;
        }
    }

    private static int race(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); return first.call(); });
            var b = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
        }
    }

    private static SubmitActionOrchestrator flow(JdbcPersistenceContext context) {
        return new SubmitActionOrchestrator(new JdbcMatchStateStore(context),
                new JdbcWorkflowEventSink(context), new JdbcIdempotencyStore(context));
    }

    private static SubmitActionCommand command(String id, String actor, long version) {
        return new SubmitActionCommand(id, actor, version, "ready-" + actor + "-" + version,
                SubmitActionCommand.READY_SECRET, CODE, null);
    }
}

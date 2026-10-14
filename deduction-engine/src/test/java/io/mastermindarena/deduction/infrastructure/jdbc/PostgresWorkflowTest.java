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
                System.getenv("ARENA_TEST_DB_PASSWORD"), context.schema(), "seed", "p1", "p2");
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

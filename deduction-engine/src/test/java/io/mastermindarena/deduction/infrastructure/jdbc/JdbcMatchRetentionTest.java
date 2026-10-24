package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in PostgreSQL tests; all schemas and credentials belong to the disposable test database. */
class JdbcMatchRetentionTest {
    private JdbcPersistenceContext context;
    private JdbcRoomDirectory rooms;
    private JdbcMatchRetention retention;

    @BeforeEach void isolatedSchema() {
        String url = System.getenv("ARENA_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "ARENA_TEST_DB_URL enables isolated PostgreSQL tests");
        context = new JdbcPersistenceContext(url, System.getenv("ARENA_TEST_DB_USER"),
                System.getenv("ARENA_TEST_DB_PASSWORD"), "retention_" + UUID.randomUUID().toString().replace("-", ""));
        new JdbcSchemaMigrator(context).migrateToLatest();
        rooms = new JdbcRoomDirectory(context);
        retention = new JdbcMatchRetention(context);
    }

    @Test void boundaryDeletesOnlyExpiredMatchesAndAllTheirPrivateData() throws Exception {
        var before = rooms.create("Before", "Alice", "");
        var exact = rooms.create("Exact", "Alice", "");
        var after = rooms.create("After", "Alice", "");
        ready(exact.room().roomId(), "p1", 0);
        ready(after.room().roomId(), "p1", 0);
        OffsetDateTime cutoff = retention.preview().cutoff();
        activity(before.room().roomId(), cutoff.minusNanos(1000));
        activity(exact.room().roomId(), cutoff);
        activity(after.room().roomId(), cutoff.plusNanos(1000));
        assertTrue(retention.preview().expiredMatches() >= 2);
        assertEquals(3, count("rooms")); // Preview never deletes.
        assertEquals(2, retention.purgeAt(cutoff));
        assertEquals(1, count("match_state"));
        assertEquals(1, count("rooms"));
        assertEquals(1, count("workflow_event"));
        assertEquals(1, count("idempotency"));
        assertEquals(8, count("schema_version"));
        assertTrue(rooms.authorize(after.room().roomId(), "p1", after.token()));
        assertEquals(1, new JdbcMatchStateStore(context).findById(after.room().roomId()).orElseThrow().version());
        assertFalse(rooms.authorize(exact.room().roomId(), "p1", exact.token()));
        assertEquals("ROOM_RESUME_INVALID", assertThrows(IllegalStateException.class,
                () -> rooms.resume(exact.room().roomId() + ".p1." + exact.token())).getMessage());
        assertTrue(new JdbcMatchStateStore(context).findById(exact.room().roomId()).isEmpty());
        assertEquals("MATCH_NOT_FOUND", assertThrows(IllegalStateException.class,
                () -> ready(exact.room().roomId(), "p1", 0)).getMessage());
        assertEquals(0, retention.purgeAt(cutoff));
    }

    @Test void readsResumeJoinAndReplayDoNotRefreshActivityButANewActionDoes() throws Exception {
        var host = rooms.create("Activity", "Alice", "");
        String id = host.room().roomId();
        ready(id, "p1", 0);
        OffsetDateTime cutoff = retention.preview().cutoff();
        activity(id, cutoff.minusHours(1));
        OffsetDateTime old = activity(id);
        rooms.join(id, "Bob", "");
        rooms.findInfo(id);
        rooms.waitingRooms();
        rooms.resume(id + ".p1." + host.token());
        new JdbcMatchStateStore(context).findById(id);
        ready(id, "p1", 0); // Exact replay of an accepted operation.
        assertEquals(old, activity(id));
        ready(id, "p2", 1);
        assertTrue(activity(id).isAfter(cutoff.plusDays(6)));
        assertEquals(0, retention.purge());
        assertEquals(2, count("idempotency"));
        assertEquals(2, count("workflow_event"));
    }

    @Test void freshActionUnderTheSharedLockCancelsAnAlreadySelectedPurgeCandidate() throws Exception {
        String id = rooms.create("Concurrent activity", "Alice", "").room().roomId();
        activity(id, retention.preview().cutoff().minusHours(1));
        try (var executor = Executors.newSingleThreadExecutor()) {
            long deleted = context.atomic(id, () -> {
                var pending = executor.submit(() -> new JdbcMatchRetention(otherContext()).purge());
                try {
                    awaitPurgeLock();
                    ready(id, "p1", 0);
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
                // Commit this action before waiting for the purger to obtain the lock.
                return pending;
            }).get(10, TimeUnit.SECONDS);
            assertEquals(0, deleted);
        }
        assertEquals(1, count("match_state"));
        assertEquals(1, new JdbcMatchStateStore(context).findById(id).orElseThrow().version());
        assertEquals(0, retention.preview().expiredMatches());
    }

    @Test void failedCascadeRollsBackTheWholeMatchAndCanBeRetried() throws Exception {
        var host = rooms.create("Rollback", "Alice", "");
        String id = host.room().roomId();
        ready(id, "p1", 0);
        activity(id, retention.preview().cutoff().minusHours(1));
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE " + context.table("purge_blocker") + " (match_id VARCHAR(255) REFERENCES "
                    + context.table("match_state") + "(match_id))");
            try (var insert = connection.prepareStatement("INSERT INTO " + context.table("purge_blocker") + " VALUES (?)")) {
                insert.setString(1, id);
                insert.executeUpdate();
            }
        }
        assertThrows(IllegalStateException.class, () -> retention.purge());
        for (String table : java.util.List.of("match_state", "rooms", "workflow_event", "idempotency")) assertEquals(1, count(table));
        assertEquals("p1", rooms.resume(id + ".p1." + host.token()).actorId());
        assertEquals(1, ready(id, "p1", 0).state().version());
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("DROP TABLE " + context.table("purge_blocker"));
        }
        assertEquals(1, retention.purge());
        for (String table : java.util.List.of("match_state", "rooms", "workflow_event", "idempotency")) assertEquals(0, count(table));
    }

    @Test void failureOnALaterMatchKeepsEarlierDeletionsCommitted() throws Exception {
        String first = rooms.create("First candidate", "Alice", "").room().roomId();
        String second = rooms.create("Second candidate", "Bob", "").room().roomId();
        var ordered = java.util.stream.Stream.of(first, second).sorted().toList();
        String deletedId = ordered.get(0);
        String blockedId = ordered.get(1);
        OffsetDateTime cutoff = retention.preview().cutoff();
        for (String id : ordered) {
            ready(id, "p1", 0);
            activity(id, cutoff.minusHours(1));
        }
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE " + context.table("purge_blocker") + " (match_id VARCHAR(255) REFERENCES "
                    + context.table("match_state") + "(match_id))");
            try (var insert = connection.prepareStatement("INSERT INTO " + context.table("purge_blocker") + " VALUES (?)")) {
                insert.setString(1, blockedId);
                insert.executeUpdate();
            }
        }
        assertThrows(IllegalStateException.class, () -> retention.purgeAt(cutoff));
        assertTrue(new JdbcMatchStateStore(context).findById(deletedId).isEmpty());
        assertEquals(1, new JdbcMatchStateStore(context).findById(blockedId).orElseThrow().version());
        for (String table : java.util.List.of("match_state", "rooms", "workflow_event", "idempotency")) {
            assertEquals(1, count(table));
            try (var connection = context.openConnection(); var query = connection.prepareStatement(
                    "SELECT count(*) FROM " + context.table(table) + " WHERE "
                            + (table.equals("rooms") ? "room_id" : "match_id") + "=?")) {
                query.setString(1, deletedId);
                try (var rows = query.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals(0, rows.getLong(1));
                }
            }
        }
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("DROP TABLE " + context.table("purge_blocker"));
        }
        assertEquals(1, retention.purgeAt(cutoff));
        for (String table : java.util.List.of("match_state", "rooms", "workflow_event", "idempotency")) assertEquals(0, count(table));
    }

    @Test void cronStyleCallUsesSevenDaysAndReturnsDeletedCount() throws Exception {
        String expired = rooms.create("Expired", "Alice", "").room().roomId();
        String fresh = rooms.create("Fresh", "Bob", "").room().roomId();
        ready(expired, "p1", 0);
        activity(expired, retention.preview().cutoff().minusHours(1));
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            try (var rows = sql.executeQuery("CALL " + context.table("purge_expired_matches") + "()")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getLong("deleted_matches"));
            }
            try (var rows = sql.executeQuery("CALL " + context.table("purge_expired_matches") + "()")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getLong("deleted_matches"));
            }
            assertEquals("22004", assertThrows(SQLException.class,
                    () -> sql.execute("CALL " + context.table("purge_expired_matches") + "(NULL, 0)")).getSQLState());
        }
        assertTrue(new JdbcMatchStateStore(context).findById(expired).isEmpty());
        assertTrue(new JdbcMatchStateStore(context).findById(fresh).isPresent());
        for (String table : java.util.List.of("workflow_event", "idempotency")) assertEquals(0, count(table));
    }

    @Test void upgradeKeepsExistingMatchesAndRevokesDefaultRoutineGrants() throws Exception {
        var host = rooms.create("V7 match", "Alice", "");
        ready(host.room().roomId(), "p1", 0);
        String role = "purge_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("DROP PROCEDURE " + context.table("purge_expired_matches") + "(timestamptz, bigint)");
            sql.execute("DELETE FROM " + context.table("schema_version") + " WHERE version=8");
            assertThrows(IllegalStateException.class, () -> new JdbcSchemaMigrator(context).verifyLatest());
            sql.execute("CREATE ROLE " + role + " NOLOGIN NOSUPERUSER NOBYPASSRLS");
            sql.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + context.schema() + " GRANT EXECUTE ON FUNCTIONS TO " + role);
            try {
                new JdbcSchemaMigrator(context).migrateToLatest();
            } finally {
                sql.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + context.schema() + " REVOKE EXECUTE ON FUNCTIONS FROM " + role);
            }
            try (var rows = sql.executeQuery("SELECT has_function_privilege('" + role + "', oid, 'EXECUTE'), prosecdef"
                    + " FROM pg_proc WHERE pronamespace = '" + context.schema() + "'::regnamespace"
                    + " AND proname='purge_expired_matches'")) {
                assertTrue(rows.next());
                assertFalse(rows.getBoolean(1));
                assertFalse(rows.getBoolean(2)); // Invoker rights, never a privilege-escalating definer.
                assertFalse(rows.next());
            }
        }
        new JdbcSchemaMigrator(context).migrateToLatest();
        new JdbcSchemaMigrator(context).verifyLatest();
        assertEquals(8, count("schema_version"));
        assertTrue(rooms.authorize(host.room().roomId(), "p1", host.token()));
        assertEquals(1, new JdbcMatchStateStore(context).findById(host.room().roomId()).orElseThrow().version());
        for (String table : java.util.List.of("match_state", "rooms", "workflow_event", "idempotency")) assertEquals(1, count(table));
    }

    @Test void nonEmptyPreviousInstallationIsRefusedWithoutMigrationOrDeletion() throws Exception {
        var host = rooms.create("Disposable previous data", "Alice", "");
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            sql.execute("DELETE FROM " + context.table("schema_version") + " WHERE version=7");
        }
        assertEquals("EMPTY_SCHEMA_REQUIRED_FOR_NEW_STORAGE", assertThrows(IllegalStateException.class,
                () -> new JdbcSchemaMigrator(context).migrateToLatest()).getMessage());
        assertEquals(7, count("schema_version"));
        assertTrue(rooms.authorize(host.room().roomId(), "p1", host.token()));
    }

    private JdbcPersistenceContext otherContext() {
        return new JdbcPersistenceContext(System.getenv("ARENA_TEST_DB_URL"), System.getenv("ARENA_TEST_DB_USER"),
                System.getenv("ARENA_TEST_DB_PASSWORD"), context.schema());
    }

    private io.mastermindarena.deduction.engine.workflow.SubmitActionResult ready(String id, String actor, long version) {
        return new SubmitActionOrchestrator(new JdbcMatchStateStore(context), new JdbcWorkflowEventSink(context),
                new JdbcIdempotencyStore(context)).submit(new SubmitActionCommand(id, actor, version,
                "ready-" + actor, "READY_SECRET", "[\"A\",\"B\",\"E\",\"F\"]", null));
    }

    private void activity(String id, OffsetDateTime time) throws SQLException {
        try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                "UPDATE " + context.table("match_state") + " SET updated_at=? WHERE match_id=?")) {
            sql.setObject(1, time);
            sql.setString(2, id);
            assertEquals(1, sql.executeUpdate());
        }
    }

    private OffsetDateTime activity(String id) throws SQLException {
        try (var connection = context.openConnection(); var sql = connection.prepareStatement(
                "SELECT updated_at FROM " + context.table("match_state") + " WHERE match_id=?")) {
            sql.setString(1, id);
            try (var rows = sql.executeQuery()) {
                assertTrue(rows.next());
                return rows.getObject(1, OffsetDateTime.class);
            }
        }
    }

    private long count(String table) throws SQLException {
        try (var connection = context.openConnection(); var sql = connection.createStatement();
             var rows = sql.executeQuery("SELECT count(*) FROM " + context.table(table))) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private void awaitPurgeLock() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try (var connection = otherContext().openConnection(); var sql = connection.createStatement();
                 var rows = sql.executeQuery("SELECT EXISTS(SELECT 1 FROM pg_locks WHERE locktype='advisory' AND NOT granted)")) {
                rows.next();
                if (rows.getBoolean(1)) return;
            }
            Thread.sleep(10);
        }
        fail("Purge did not wait for the action's match lock");
    }
}

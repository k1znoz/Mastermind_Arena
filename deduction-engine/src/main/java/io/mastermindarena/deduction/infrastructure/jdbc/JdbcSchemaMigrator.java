package io.mastermindarena.deduction.infrastructure.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class JdbcSchemaMigrator {
    private static final String V1_SCRIPT = "db/migration/V1__submit_action_init.sql";
    private static final String V2_SCRIPT = "db/migration/V2__rooms.sql";
    private static final String V3_SCRIPT = "db/migration/V3__repair_rooms.sql";
    private static final String V4_SCRIPT = "db/migration/V4__private_game_tables.sql";
    private static final String V5_SCRIPT = "db/migration/V5__json_idempotency.sql";
    private static final String V6_SCRIPT = "db/migration/V6__match_retention.sql";
    private static final String V7_SCRIPT = "db/migration/V7__compact_receipts.sql";
    private static final String V8_SCRIPT = "db/migration/V8__shared_retention.sql";

    private final JdbcPersistenceContext context;

    public JdbcSchemaMigrator(JdbcPersistenceContext context) {
        this.context = context;
    }

    public void migrateToLatest() {
        try (Connection connection = context.openConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            if (context.isPostgresUrl()) {
                statement.execute("SELECT pg_advisory_xact_lock(hashtext('mastermind_arena_schema_migration'))");
            }
            executeScript(statement, "db/migration/schema_history.sql");
            var applied = new java.util.HashSet<Integer>();
            try (var rows = statement.executeQuery("SELECT version FROM " + context.table("schema_version"))) {
                while (rows.next()) applied.add(rows.getInt(1));
            }
            if (applied.stream().anyMatch(version -> version > 8)) throw new IllegalStateException("UNSUPPORTED_SCHEMA_VERSION");
            var scripts = java.util.List.of(V1_SCRIPT, V2_SCRIPT, V3_SCRIPT, V4_SCRIPT, V5_SCRIPT, V6_SCRIPT, V7_SCRIPT, V8_SCRIPT);
            for (int index = 0; index < scripts.size(); index++) {
                int version = index + 1;
                if (applied.contains(version)) continue;
                if (version == 5) {
                    try (var rows = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM " + context.table("match_state") + ")")) {
                        rows.next();
                        if (rows.getBoolean(1)) throw new IllegalStateException("EMPTY_SCHEMA_REQUIRED_FOR_NEW_STORAGE");
                    }
                }
                if (version == 6 || version == 7) {
                    for (String table : java.util.List.of("match_state", "rooms", "idempotency", "workflow_event")) {
                        try (var rows = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM " + context.table(table) + ")")) {
                            rows.next();
                            if (rows.getBoolean(1)) throw new IllegalStateException("EMPTY_SCHEMA_REQUIRED_FOR_NEW_STORAGE");
                        }
                    }
                }
                if (version == 8) {
                    // Dollar-quoted PL/pgSQL bodies contain semicolons; execute the fixed resource intact.
                    statement.execute(loadScript(V8_SCRIPT).replace("${schema}", context.schema()));
                } else {
                    executeScript(statement, scripts.get(index));
                }
                if (version == 4) revokeApiRoles(connection, statement);
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to apply JDBC schema migration", e);
        }
    }

    /** Read-only startup gate for a runtime role without DDL or migration rights. */
    public void verifyLatest() {
        try (var connection = context.openConnection(); var sql = connection.createStatement();
             var rows = sql.executeQuery("SELECT version FROM " + context.table("schema_version"))) {
            var versions = new java.util.HashSet<Integer>();
            while (rows.next()) versions.add(rows.getInt(1));
            if (!versions.equals(java.util.Set.of(1, 2, 3, 4, 5, 6, 7, 8))) throw new IllegalStateException("SCHEMA_NOT_READY");
        } catch (SQLException failure) {
            throw new IllegalStateException("SCHEMA_NOT_READY", failure);
        }
    }

    /** Explicit provisioning by the schema owner, never called by runtime startup. */
    public void grantRuntimeAccess(String role) {
        if (role == null || !role.matches("[a-z_][a-z0-9_]{0,39}")
                || java.util.Set.of("public", "anon", "authenticated", "service_role").contains(role)) {
            throw new IllegalArgumentException("Dedicated runtime role required");
        }
        try (var connection = context.openConnection(); var sql = connection.createStatement()) {
            connection.setAutoCommit(false);
            try (var lookup = connection.prepareStatement("SELECT rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole"
                    + " OR oid IN (SELECT member FROM pg_auth_members)"
                    + " OR oid IN (SELECT nspowner FROM pg_namespace WHERE nspname=?) FROM pg_roles WHERE rolname=?")) {
                lookup.setString(1, context.schema());
                lookup.setString(2, role);
                try (var rows = lookup.executeQuery()) {
                    if (!rows.next() || rows.getBoolean(1)) throw new IllegalArgumentException("Unprivileged runtime role required");
                }
            }
            try (var settings = connection.prepareStatement(
                    "SELECT set_config('arena.runtime_role', ?, true), set_config('arena.runtime_schema', ?, true)")) {
                settings.setString(1, role);
                settings.setString(2, context.schema());
                settings.execute();
            }
            sql.execute(loadScript("db/runtime_access.sql"));
            connection.commit();
        } catch (SQLException failure) {
            throw new IllegalStateException("RUNTIME_ROLE_PROVISION_FAILED", failure);
        }
    }

    private void executeScript(Statement statement, String path) throws SQLException {
        for (String sql : splitStatements(loadScript(path).replace("${schema}", context.schema()))) {
            if (!sql.isBlank()) statement.execute(sql);
        }
    }

    private void revokeApiRoles(Connection connection, Statement statement) throws SQLException {
            // Supabase roles are absent on a plain PostgreSQL installation.
            // Only revoke known API roles when they exist; retain the table owner's JDBC access.
            for (String role : java.util.List.of("anon", "authenticated")) {
                try (var lookup = connection.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname=?")) {
                    lookup.setString(1, role);
                    try (var rows = lookup.executeQuery()) {
                        if (rows.next()) {
                            String revokeScript = loadScript("db/migration/api_role_revocation.sql")
                                    .replace("${schema}", context.schema()).replace("${role}", role);
                            for (String sql : splitStatements(revokeScript)) {
                                if (!sql.isBlank()) statement.execute(sql);
                            }
                        }
                    }
                }
            }
    }

    private static String loadScript(String resourcePath) {
        try (InputStream input = JdbcSchemaMigrator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing migration script: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read migration script: " + resourcePath, e);
        }
    }

    private static String[] splitStatements(String script) {
        return script.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("--"))
                .reduce(new StringBuilder(), (builder, line) -> builder.append(line).append('\n'), StringBuilder::append)
                .toString()
                .split(";");
    }
}

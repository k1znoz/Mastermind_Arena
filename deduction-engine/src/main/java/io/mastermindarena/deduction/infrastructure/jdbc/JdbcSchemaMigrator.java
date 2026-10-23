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
            if (applied.stream().anyMatch(version -> version > 5)) throw new IllegalStateException("UNSUPPORTED_SCHEMA_VERSION");
            var scripts = java.util.List.of(V1_SCRIPT, V2_SCRIPT, V3_SCRIPT, V4_SCRIPT, V5_SCRIPT);
            for (int index = 0; index < scripts.size(); index++) {
                int version = index + 1;
                if (applied.contains(version)) continue;
                if (version == 5) {
                    try (var rows = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM " + context.table("match_state") + ")")) {
                        rows.next();
                        if (rows.getBoolean(1)) throw new IllegalStateException("EMPTY_SCHEMA_REQUIRED_FOR_NEW_STORAGE");
                    }
                }
                executeScript(statement, scripts.get(index));
                if (version == 4) revokeApiRoles(connection, statement);
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to apply JDBC schema migration", e);
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

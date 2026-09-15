package io.mastermindarena.deduction.infrastructure.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class JdbcSchemaMigrator {
    private static final String V1_SCRIPT = "db/migration/V1__submit_action_init.sql";

    private final JdbcPersistenceContext context;

    public JdbcSchemaMigrator(JdbcPersistenceContext context) {
        this.context = context;
    }

    public void migrateToLatest() {
        String script = loadScript(V1_SCRIPT).replace("${schema}", context.schema());
        try (Connection connection = context.openConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            for (String sql : splitStatements(script)) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to apply JDBC schema migration", e);
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

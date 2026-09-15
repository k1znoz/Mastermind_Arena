package io.mastermindarena.deduction.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

public final class JdbcPersistenceContext {
    private static final String IDENTIFIER_REGEX = "[A-Za-z_][A-Za-z0-9_]*";

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String schema;

    public JdbcPersistenceContext(String dbUrl, String dbUser, String dbPassword, String schema) {
        this.dbUrl = Objects.requireNonNull(dbUrl, "dbUrl is required");
        this.dbUser = Objects.requireNonNull(dbUser, "dbUser is required");
        this.dbPassword = Objects.requireNonNull(dbPassword, "dbPassword is required");
        this.schema = Objects.requireNonNull(schema, "schema is required");

        if (dbUrl.isBlank()) {
            throw new IllegalArgumentException("dbUrl must not be blank");
        }
        if (dbUser.isBlank()) {
            throw new IllegalArgumentException("dbUser must not be blank");
        }
        if (!schema.matches(IDENTIFIER_REGEX)) {
            throw new IllegalArgumentException("schema must be a valid SQL identifier");
        }
    }

    public Connection openConnection() {
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to open JDBC connection", e);
        }
    }

    public String schema() {
        return schema;
    }

    public String table(String logicalName) {
        return schema + "." + logicalName;
    }

    public boolean isPostgresUrl() {
        return dbUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql:");
    }
}

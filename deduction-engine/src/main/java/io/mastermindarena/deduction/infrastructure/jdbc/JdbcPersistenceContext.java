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
    private final ThreadLocal<Connection> transaction = new ThreadLocal<>();

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
        Connection active = transaction.get();
        if (active != null) {
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if (ListHolder.TRANSACTION_METHODS.contains(method.getName())) return null;
                        try {
                            return method.invoke(active, args);
                        } catch (java.lang.reflect.InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    });
        }
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to open JDBC connection", e);
        }
    }

    public <T> T atomic(String matchId, java.util.function.Supplier<T> operation) {
        if (transaction.get() != null) return operation.get();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (isPostgresUrl()) {
                    try (var lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                        lock.setString(1, schema + ":match:" + matchId);
                        lock.execute();
                    }
                }
                transaction.set(connection);
                T result = operation.get();
                connection.commit();
                return result;
            } catch (RuntimeException | SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                transaction.remove();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("MATCH_PERSISTENCE_FAILED", failure);
        }
    }

    private static final class ListHolder {
        private static final java.util.Set<String> TRANSACTION_METHODS = java.util.Set.of("close", "commit", "rollback", "setAutoCommit");
        private ListHolder() {}
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

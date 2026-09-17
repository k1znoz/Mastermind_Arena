package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.IdempotencyStore;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class JdbcIdempotencyStore implements IdempotencyStore {
    private static final int MAX_SAVE_ATTEMPTS = 3;
    private final JdbcPersistenceContext context;

    public JdbcIdempotencyStore(JdbcPersistenceContext context) {
        this.context = context;
    }

    @Override
    public Optional<Entry> find(String key) {
        String sql = "SELECT fingerprint, result_state, result_resolution, result_events FROM "
                + context.table("idempotency")
                + " WHERE idempotency_key = ?";

        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Entry(
                        resultSet.getString(1),
                        FilePersistenceCodec.deserializeSubmitActionResult(List.of(
                                resultSet.getString(2),
                                resultSet.getString(3),
                                resultSet.getString(4)
                        ))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load idempotency entry", e);
        }
    }

    @Override
    public void save(String key, Entry entry) {
        List<String> serializedResult = FilePersistenceCodec.serializeSubmitActionResult(entry.result());
        SQLException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_SAVE_ATTEMPTS; attempt++) {
            try {
                persist(key, entry.fingerprint(), serializedResult);
                return;
            } catch (SQLException e) {
                lastFailure = e;
                if (attempt < MAX_SAVE_ATTEMPTS && isRetryable(e)) {
                    pauseBeforeRetry(attempt);
                    continue;
                }
                break;
            }
        }

        throw new IllegalStateException("Unable to persist idempotency entry", lastFailure);
    }

    private void persist(String key, String fingerprint, List<String> serializedResult) throws SQLException {
        if (context.isPostgresUrl()) {
            persistPostgres(key, fingerprint, serializedResult);
            return;
        }
        persistPortable(key, fingerprint, serializedResult);
    }

    private void persistPostgres(String key, String fingerprint, List<String> serializedResult) throws SQLException {
        String sql = "INSERT INTO " + context.table("idempotency")
                + " (idempotency_key, fingerprint, result_state, result_resolution, result_events) VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT (idempotency_key) DO UPDATE SET fingerprint = EXCLUDED.fingerprint,"
                + " result_state = EXCLUDED.result_state, result_resolution = EXCLUDED.result_resolution,"
                + " result_events = EXCLUDED.result_events";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, key, fingerprint, serializedResult);
            statement.executeUpdate();
        }
    }

    private void persistPortable(String key, String fingerprint, List<String> serializedResult) throws SQLException {
        String updateSql = "UPDATE " + context.table("idempotency")
                + " SET fingerprint = ?, result_state = ?, result_resolution = ?, result_events = ? WHERE idempotency_key = ?";
        String insertSql = "INSERT INTO " + context.table("idempotency")
                + " (idempotency_key, fingerprint, result_state, result_resolution, result_events) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = context.openConnection()) {
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, fingerprint);
                update.setString(2, serializedResult.get(0));
                update.setString(3, serializedResult.get(1));
                update.setString(4, serializedResult.get(2));
                update.setString(5, key);
                updated = update.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    bind(insert, key, fingerprint, serializedResult);
                    insert.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    private static void bind(PreparedStatement statement, String key, String fingerprint, List<String> serializedResult)
            throws SQLException {
        statement.setString(1, key);
        statement.setString(2, fingerprint);
        statement.setString(3, serializedResult.get(0));
        statement.setString(4, serializedResult.get(1));
        statement.setString(5, serializedResult.get(2));
    }

    private static boolean isRetryable(SQLException exception) {
        String sqlState = exception.getSQLState();
        return exception instanceof java.sql.SQLTransientException
                || sqlState == null
                || sqlState.startsWith("08")
                || "40001".equals(sqlState)
                || "40P01".equals(sqlState)
                || "55P03".equals(sqlState);
    }

    private static void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

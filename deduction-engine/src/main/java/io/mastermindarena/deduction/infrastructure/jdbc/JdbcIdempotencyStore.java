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
        String updateSql = "UPDATE " + context.table("idempotency")
                + " SET fingerprint = ?, result_state = ?, result_resolution = ?, result_events = ? WHERE idempotency_key = ?";
        String insertSql = "INSERT INTO " + context.table("idempotency")
                + " (idempotency_key, fingerprint, result_state, result_resolution, result_events) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = context.openConnection()) {
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, entry.fingerprint());
                update.setString(2, serializedResult.get(0));
                update.setString(3, serializedResult.get(1));
                update.setString(4, serializedResult.get(2));
                update.setString(5, key);
                updated = update.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setString(1, key);
                    insert.setString(2, entry.fingerprint());
                    insert.setString(3, serializedResult.get(0));
                    insert.setString(4, serializedResult.get(1));
                    insert.setString(5, serializedResult.get(2));
                    insert.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist idempotency entry", e);
        }
    }
}

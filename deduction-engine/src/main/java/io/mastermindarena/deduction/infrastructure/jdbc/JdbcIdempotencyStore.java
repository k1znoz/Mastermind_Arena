package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.IdempotencyStore;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcIdempotencyStore implements IdempotencyStore {
    private final JdbcPersistenceContext context;
    public JdbcIdempotencyStore(JdbcPersistenceContext context) { this.context = context; }

    @Override public Optional<Entry> find(String key) {
        String sql = "SELECT fingerprint, result_payload FROM " + context.table("idempotency") + " WHERE idempotency_key=?";
        try (var connection = context.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(new Entry(rows.getString(1),
                        BoardPersistenceCodec.decodeResult(rows.getString(2)))) : Optional.empty();
            }
        } catch (SQLException failure) { throw new IllegalStateException("IDEMPOTENCY_READ_FAILED", failure); }
    }

    @Override public void save(String key, Entry entry) {
        // Never overwrite an operation or retry a statement in an aborted transaction.
        String sql = "INSERT INTO " + context.table("idempotency") + " (idempotency_key,fingerprint,result_payload) VALUES (?,?,?)";
        try (var connection = context.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, entry.fingerprint());
            statement.setString(3, BoardPersistenceCodec.encodeResult(entry.result()));
            statement.executeUpdate();
        } catch (SQLException failure) { throw new IllegalStateException("IDEMPOTENCY_WRITE_FAILED", failure); }
    }
}

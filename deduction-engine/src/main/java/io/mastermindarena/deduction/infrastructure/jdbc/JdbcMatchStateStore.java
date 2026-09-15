package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.MatchStateStore;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcMatchStateStore implements MatchStateStore {
    private final JdbcPersistenceContext context;

    public JdbcMatchStateStore(JdbcPersistenceContext context) {
        this.context = context;
    }

    @Override
    public Optional<MatchRuntimeState> findById(String matchId) {
        String sql = "SELECT payload FROM " + context.table("match_state") + " WHERE match_id = ?";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(FilePersistenceCodec.deserializeMatchRuntimeState(resultSet.getString(1)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load match state", e);
        }
    }

    @Override
    public void save(MatchRuntimeState state) {
        String updateSql = "UPDATE " + context.table("match_state") + " SET payload = ?, updated_at = CURRENT_TIMESTAMP WHERE match_id = ?";
        String insertSql = "INSERT INTO " + context.table("match_state") + " (match_id, payload, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection connection = context.openConnection()) {
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, FilePersistenceCodec.serializeMatchRuntimeState(state));
                update.setString(2, state.matchId());
                updated = update.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setString(1, state.matchId());
                    insert.setString(2, FilePersistenceCodec.serializeMatchRuntimeState(state));
                    insert.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist match state", e);
        }
    }
}

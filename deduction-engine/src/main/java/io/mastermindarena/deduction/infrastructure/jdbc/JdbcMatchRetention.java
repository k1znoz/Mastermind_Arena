package io.mastermindarena.deduction.infrastructure.jdbc;

import java.sql.SQLException;
import java.time.OffsetDateTime;

/** Administrative operation; each match and all its dependent rows form one transaction. */
public final class JdbcMatchRetention {
    private final JdbcPersistenceContext context;

    public JdbcMatchRetention(JdbcPersistenceContext context) {
        this.context = context;
    }

    public record Preview(OffsetDateTime cutoff, long expiredMatches) {}

    public Preview preview() {
        String sql = "SELECT cutoff, (SELECT count(*) FROM " + context.table("match_state")
                + " WHERE updated_at <= cutoff) FROM (SELECT clock_timestamp() - INTERVAL '168 hours' AS cutoff) boundary";
        try (var connection = context.openConnection(); var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            rows.next();
            return new Preview(rows.getObject(1, OffsetDateTime.class), rows.getLong(2));
        } catch (SQLException failure) {
            throw new IllegalStateException("RETENTION_PREVIEW_FAILED", failure);
        }
    }

    public long purge() {
        return purgeAt(preview().cutoff());
    }

    long purgeAt(OffsetDateTime cutoff) {
        // CALL must own its transaction so the procedure can commit each match independently.
        String sql = "CALL " + context.table("purge_expired_matches") + "(?, 0)";
        try (var connection = context.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, cutoff);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("RETENTION_PURGE_FAILED", failure);
        }
    }
}

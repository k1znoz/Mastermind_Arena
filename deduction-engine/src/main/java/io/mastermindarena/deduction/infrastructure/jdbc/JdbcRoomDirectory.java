package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcRoomDirectory {
    private final JdbcPersistenceContext context;

    public JdbcRoomDirectory(JdbcPersistenceContext context) {
        this.context = context;
    }

    public record RoomInfo(
            String roomId, String name, String hostPseudo, String guestPseudo,
            boolean locked, long createdAt, boolean joined
    ) {}

    public record Admission(RoomInfo room, String actorId, String token) {}

    public Admission create(String rawName, String rawPseudo, String rawCode) {
        String name = required(rawName, "ROOM_NAME_REQUIRED", 60);
        String pseudo = required(rawPseudo, "PSEUDO_REQUIRED", 24);
        String code = optionalCode(rawCode);
        String roomId = UUID.randomUUID().toString();
        String token = RoomCredentials.newToken();
        String salt = code.isEmpty() ? null : RoomCredentials.newSalt();
        String codeHash = salt == null ? null : RoomCredentials.hashCode(code, salt);
        MatchRuntimeState state = new MatchRuntimeState(roomId, List.of("p1", "p2"));

        String roomSql = "INSERT INTO " + context.table("rooms") +
                " (room_id,name,host_pseudo,access_salt,access_hash,host_token_hash) VALUES (?,?,?,?,?,?)";
        String matchSql = "INSERT INTO " + context.table("match_state") + " (match_id,payload) VALUES (?,?)";
        try (Connection connection = context.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement match = connection.prepareStatement(matchSql);
                 PreparedStatement room = connection.prepareStatement(roomSql)) {
                match.setString(1, roomId);
                match.setString(2, FilePersistenceCodec.serializeMatchRuntimeState(state));
                match.executeUpdate();
                room.setString(1, roomId);
                room.setString(2, name);
                room.setString(3, pseudo);
                room.setString(4, salt);
                room.setString(5, codeHash);
                room.setString(6, RoomCredentials.hashToken(token));
                room.executeUpdate();
            }
            connection.commit();
            return new Admission(findInfo(roomId).orElseThrow(), "p1", token);
        } catch (SQLException e) {
            throw new IllegalStateException("ROOM_CREATE_FAILED", e);
        }
    }

    public Admission join(String roomId, String rawPseudo, String rawCode) {
        String pseudo = required(rawPseudo, "PSEUDO_REQUIRED", 24);
        String code = optionalCode(rawCode);
        String selectSql = "SELECT access_salt,access_hash,guest_pseudo FROM " +
                context.table("rooms") + " WHERE room_id=? FOR UPDATE";
        String updateSql = "UPDATE " + context.table("rooms") +
                " SET guest_pseudo=?,guest_token_hash=? WHERE room_id=?";
        String token = RoomCredentials.newToken();
        try (Connection connection = context.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setString(1, roomId);
                try (ResultSet row = select.executeQuery()) {
                    if (!row.next()) throw new IllegalStateException("ROOM_NOT_FOUND");
                    if (row.getString("guest_pseudo") != null) throw new IllegalStateException("ROOM_FULL");
                    String salt = row.getString("access_salt");
                    String codeHash = row.getString("access_hash");
                    if (codeHash != null && !RoomCredentials.matchesCode(code, salt, codeHash)) {
                        throw new IllegalStateException("ROOM_CODE_INVALID");
                    }
                }
            }
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, pseudo);
                update.setString(2, RoomCredentials.hashToken(token));
                update.setString(3, roomId);
                update.executeUpdate();
            }
            connection.commit();
            return new Admission(findInfo(roomId).orElseThrow(), "p2", token);
        } catch (SQLException e) {
            throw new IllegalStateException("ROOM_JOIN_FAILED", e);
        }
    }

    public List<RoomInfo> waitingRooms() {
        String sql = "SELECT room_id,name,host_pseudo,guest_pseudo,access_hash,created_at FROM " +
                context.table("rooms") + " WHERE guest_pseudo IS NULL ORDER BY created_at DESC LIMIT 100";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<RoomInfo> result = new ArrayList<>();
            while (rows.next()) result.add(info(rows));
            return List.copyOf(result);
        } catch (SQLException e) {
            throw new IllegalStateException("ROOM_LIST_FAILED", e);
        }
    }

    public Optional<RoomInfo> findInfo(String roomId) {
        String sql = "SELECT room_id,name,host_pseudo,guest_pseudo,access_hash,created_at FROM " +
                context.table("rooms") + " WHERE room_id=?";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(info(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("ROOM_LOOKUP_FAILED", e);
        }
    }

    public boolean authorize(String roomId, String actorId, String token) {
        String sql = "SELECT host_token_hash,guest_token_hash FROM " +
                context.table("rooms") + " WHERE room_id=?";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return true; // anciens matchs locaux sans room
                return switch (actorId == null ? "" : actorId) {
                    case "p1" -> RoomCredentials.matchesToken(token, row.getString("host_token_hash"));
                    case "p2" -> RoomCredentials.matchesToken(token, row.getString("guest_token_hash"));
                    default -> false;
                };
            }
        } catch (SQLException e) {
            throw new IllegalStateException("ROOM_AUTH_FAILED", e);
        }
    }

    public boolean waitingForGuest(String roomId) {
        return findInfo(roomId).map(info -> !info.joined()).orElse(false);
    }

    private static RoomInfo info(ResultSet row) throws SQLException {
        return new RoomInfo(row.getString("room_id"), row.getString("name"),
                row.getString("host_pseudo"), row.getString("guest_pseudo"),
                row.getString("access_hash") != null,
                row.getTimestamp("created_at").toInstant().toEpochMilli(),
                row.getString("guest_pseudo") != null);
    }

    private static String required(String raw, String code, int maxLength) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) throw new IllegalStateException(code);
        if (value.length() > maxLength) throw new IllegalStateException("FIELD_TOO_LONG");
        return value;
    }

    private static String optionalCode(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > 64) throw new IllegalStateException("FIELD_TOO_LONG");
        return value;
    }
}
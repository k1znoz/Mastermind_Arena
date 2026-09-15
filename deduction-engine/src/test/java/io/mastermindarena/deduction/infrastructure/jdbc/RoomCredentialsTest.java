package io.mastermindarena.deduction.infrastructure.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomCredentialsTest {
    @Test
    void codeIsSaltedAndOnlyMatchingCodeIsAccepted() {
        String salt = RoomCredentials.newSalt();
        String hash = RoomCredentials.hashCode("invitation-42", salt);
        assertNotEquals(hash, RoomCredentials.hashCode("invitation-42", RoomCredentials.newSalt()));
        assertTrue(RoomCredentials.matchesCode("invitation-42", salt, hash));
        assertFalse(RoomCredentials.matchesCode("wrong", salt, hash));
    }

    @Test
    void roomSummarySerializesForHttpWithoutDateModule() throws Exception {
        var room = new JdbcRoomDirectory.RoomInfo("id", "Duel", "Alex", null, true, 1000L, false);
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(room));
        assertEquals(1000L, json.path("createdAt").asLong());
        assertFalse(json.path("joined").asBoolean(true));
    }

    @Test
    void playerTokenCannotBeReusedByAnotherPlayer() {
        String host = RoomCredentials.newToken();
        String guest = RoomCredentials.newToken();
        assertNotEquals(host, guest);
        assertTrue(RoomCredentials.matchesToken(host, RoomCredentials.hashToken(host)));
        assertFalse(RoomCredentials.matchesToken(guest, RoomCredentials.hashToken(host)));
    }
}
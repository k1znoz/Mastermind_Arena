package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
import io.mastermindarena.deduction.engine.workflow.SubmitActionResult;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BoardPersistenceCodecTest {
    private static final String CODE = "[\"A\",\"B\",\"E\",\"F\"]";

    @Test void privateStateRoundTripsWithExplicitVersion() {
        var state = new MatchRuntimeState("board", List.of("p1", "p2"))
                .withVersionIncremented().withRecordedAction(new MatchActionRecord("p1", "READY_SECRET", CODE, null, 100, 1, 0));
        String encoded = BoardPersistenceCodec.encodeState(state);
        assertTrue(encoded.startsWith("{\"formatVersion\":1,"));
        assertEquals(state, BoardPersistenceCodec.decodeState(encoded));
        assertEquals(CODE, BoardPersistenceCodec.decodeState(encoded).actionLog().getFirst().guess());
    }

    @Test void idempotencyResultRoundTripsWithoutLegacyResolution() {
        var state = new MatchRuntimeState("board", List.of("p1", "p2"));
        var command = new SubmitActionCommand("board", "p1", 0, "exact-key", "READY_SECRET", CODE, null);
        var result = new SubmitActionResult(state, command, List.of("GAME_STARTED"), null);
        assertEquals(result, BoardPersistenceCodec.decodeResult(BoardPersistenceCodec.encodeResult(result)));
    }

    @Test void unknownVersionMissingFieldsAndOldFormatAreRejected() {
        String encoded = BoardPersistenceCodec.encodeState(new MatchRuntimeState("board", List.of("p1", "p2")));
        assertThrows(IllegalArgumentException.class, () -> BoardPersistenceCodec.decodeState(encoded.replace("\"formatVersion\":1", "\"formatVersion\":99")));
        assertThrows(IllegalArgumentException.class, () -> BoardPersistenceCodec.decodeState(encoded.replace("\"version\":0,", "")));
        assertThrows(IllegalArgumentException.class, () -> BoardPersistenceCodec.decodeState("old\tbase64\tformat"));
        assertThrows(IllegalArgumentException.class, () -> BoardPersistenceCodec.decodeState("null"));
        assertThrows(IllegalArgumentException.class, () -> BoardPersistenceCodec.decodeState(encoded.replace("\"gameNumber\":1", "\"gameNumber\":0")));
        assertThrows(IllegalArgumentException.class, () -> BoardPersistenceCodec.decodeState(encoded.replace("\"formatVersion\":1", "\"formatVersion\":\"1\"")));
    }
}

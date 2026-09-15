package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.api.submitaction.MatchStateHttpMapper;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RematchScoreTest {
    private final InMemoryMatchStateStore store = new InMemoryMatchStateStore();
    private final SubmitActionOrchestrator flow = new SubmitActionOrchestrator(store, new InMemoryEventSink());

    @Test
    void winnerGetsOnePointAndBothPlayersMustAcceptRematch() {
        store.save(new MatchRuntimeState("duel", List.of("p1", "p2")));
        playToFeedback();
        act("p2", "SEND_FEEDBACK", null, "{\"bienPlaces\":4,\"malPlaces\":0}");
        MatchRuntimeState finished = act("p1", "SEND_FEEDBACK", null, "{\"bienPlaces\":1,\"malPlaces\":0}");
        assertEquals(MatchRuntimeState.FINISHED, finished.status());
        assertEquals("p1", finished.winnerId());
        assertEquals(1, finished.scores().get("p1"));
        assertEquals(1, finished.gameNumber());

        MatchRuntimeState asked = act("p1", "REQUEST_REMATCH", null, null);
        assertEquals(MatchRuntimeState.FINISHED, asked.status());
        assertEquals(List.of("p1"), MatchStateHttpMapper.fromRuntimeState(asked, "p2").rematchRequestedPlayers());
        assertEquals("REMATCH_ALREADY_REQUESTED", assertThrows(IllegalStateException.class,
                () -> act("p1", "REQUEST_REMATCH", null, null)).getMessage());

        MatchRuntimeState again = act("p2", "REQUEST_REMATCH", null, null);
        assertEquals(MatchRuntimeState.PREPARATION, again.status());
        assertEquals(2, again.gameNumber());
        assertEquals(1, again.scores().get("p1"));
        assertTrue(again.actionLog().isEmpty());
        assertTrue(MatchStateHttpMapper.fromRuntimeState(again, "p1").visibleSecretCode().isEmpty());
        assertEquals(again, FilePersistenceCodec.deserializeMatchRuntimeState(
                FilePersistenceCodec.serializeMatchRuntimeState(again)));
    }

    @Test
    void simultaneousDiscoveryIsDrawAndDoesNotChangeScore() {
        store.save(new MatchRuntimeState("duel", List.of("p1", "p2")));
        playToFeedback();
        act("p1", "SEND_FEEDBACK", null, "{\"bienPlaces\":4,\"malPlaces\":0}");
        MatchRuntimeState finished = act("p2", "SEND_FEEDBACK", null, "{\"bienPlaces\":4,\"malPlaces\":0}");
        assertEquals("DRAW", finished.winnerId());
        assertTrue(finished.scores().isEmpty());
    }

    private void playToFeedback() {
        act("p1", "READY_SECRET", "[\"A\",\"B\",\"C\",\"D\"]", null);
        act("p2", "READY_SECRET", "[\"D\",\"C\",\"B\",\"A\"]", null);
        act("p1", "PLAY_GUESS", "[\"D\",\"C\",\"B\",\"A\"]", null);
        act("p2", "PLAY_GUESS", "[\"A\",\"B\",\"C\",\"D\"]", null);
    }

    private MatchRuntimeState act(String actor, String type, String payload, String feedback) {
        long version = store.findById("duel").orElseThrow().version();
        return flow.submit(new SubmitActionCommand("duel", actor, version,
                UUID.randomUUID().toString(), type, payload, feedback)).state();
    }
}
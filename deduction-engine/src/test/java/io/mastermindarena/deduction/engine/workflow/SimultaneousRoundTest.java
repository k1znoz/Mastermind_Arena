package io.mastermindarena.deduction.engine.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimultaneousRoundTest {
    private final InMemoryMatchStateStore store = new InMemoryMatchStateStore();
    private final SubmitActionOrchestrator orchestrator =
            new SubmitActionOrchestrator(store, new InMemoryEventSink());

    @Test
    void bothPlayersCompleteEachPhaseInEitherOrder() {
        store.save(new MatchRuntimeState("match", List.of("p1", "p2"), 0L));

        assertEquals(MatchRuntimeState.PREPARATION, submit("p2", "READY_SECRET", "[\"A\",\"B\",\"C\",\"D\"]", null).status());
        assertEquals(MatchRuntimeState.WAITING_GUESS, submit("p1", "READY_SECRET", "[\"D\",\"C\",\"B\",\"A\"]", null).status());

        MatchRuntimeState firstGuess = submit("p2", "PLAY_GUESS", "[\"A\",\"A\",\"A\",\"A\"]", null);
        assertEquals(MatchRuntimeState.WAITING_GUESS, firstGuess.status());
        assertEquals("GUESS_ALREADY_SUBMITTED", assertThrows(IllegalStateException.class,
                () -> submit("p2", "PLAY_GUESS", "[\"B\",\"B\",\"B\",\"B\"]", null)).getMessage());

        MatchRuntimeState bothGuessed = submit("p1", "PLAY_GUESS", "[\"B\",\"B\",\"B\",\"B\"]", null);
        assertEquals(MatchRuntimeState.WAITING_FEEDBACK, bothGuessed.status());

        MatchRuntimeState firstFeedback = submit("p1", "SEND_FEEDBACK", null, "{\"bienPlaces\":1,\"malPlaces\":0}");
        assertEquals(MatchRuntimeState.WAITING_FEEDBACK, firstFeedback.status());
        assertEquals("FEEDBACK_ALREADY_SUBMITTED", assertThrows(IllegalStateException.class,
                () -> submit("p1", "SEND_FEEDBACK", null, "{\"bienPlaces\":0,\"malPlaces\":0}")).getMessage());

        MatchRuntimeState nextRound = submit("p2", "SEND_FEEDBACK", null, "{\"bienPlaces\":0,\"malPlaces\":2}");
        assertEquals(MatchRuntimeState.WAITING_GUESS, nextRound.status());
        assertEquals(1, nextRound.turnNumber());
        assertEquals(6, nextRound.version());

        assertEquals(MatchRuntimeState.WAITING_GUESS,
                submit("p1", "PLAY_GUESS", "[\"C\",\"C\",\"C\",\"C\"]", null).status());
    }

    private MatchRuntimeState submit(String actor, String type, String payload, String feedback) {
        long version = store.findById("match").orElseThrow().version();
        return orchestrator.submit(new SubmitActionCommand(
                "match", actor, version, UUID.randomUUID().toString(), type, payload, feedback)).state();
    }
}
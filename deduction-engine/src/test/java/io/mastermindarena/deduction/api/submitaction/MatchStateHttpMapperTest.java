package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStateHttpMapperTest {
    @Test
    void exposesReadyStatusWithoutLeakingEitherSecret() {
        MatchRuntimeState state = new MatchRuntimeState("local-match", List.of("p1", "p2"), 0L)
                .withRecordedAction(new MatchActionRecord("p1", "READY_SECRET", "[\"A\",\"B\",\"C\",\"D\"]", null, 1L, 1L));

        MatchStateHttpResponse p1 = MatchStateHttpMapper.fromRuntimeState(state, "p1");
        MatchStateHttpResponse p2 = MatchStateHttpMapper.fromRuntimeState(state, "p2");

        assertEquals(List.of("p1"), p1.readyPlayers());
        assertEquals(List.of("p1"), p2.readyPlayers());
        assertEquals(List.of("A", "B", "C", "D"), p1.visibleSecretCode());
        assertTrue(p2.visibleSecretCode().isEmpty());
        assertTrue(p1.turns().isEmpty());
        assertTrue(p2.turns().isEmpty());

        MatchRuntimeState bothReady = state.withRecordedAction(
                new MatchActionRecord("p2", "READY_SECRET", "[\"D\",\"C\",\"B\",\"A\"]", null, 2L, 2L));
        assertEquals(List.of("p1", "p2"), MatchStateHttpMapper.fromRuntimeState(bothReady, "p1").readyPlayers());
        assertEquals(List.of("A", "B", "C", "D"), MatchStateHttpMapper.fromRuntimeState(bothReady, "p1").visibleSecretCode());
        assertEquals(List.of("D", "C", "B", "A"), MatchStateHttpMapper.fromRuntimeState(bothReady, "p2").visibleSecretCode());
    }
    @Test
    void publishesGuessesAndFeedbackOnlyAfterBothPlayersSubmit() {
        MatchRuntimeState firstGuess = new MatchRuntimeState("match", List.of("p1", "p2"), 0L)
                .advanceToWaitingGuess()
                .withVersionIncremented()
                .withRecordedAction(new MatchActionRecord("p1", "PLAY_GUESS", "[\"A\",\"A\",\"A\",\"A\"]", null, 1L, 2L));
        assertEquals(1, MatchStateHttpMapper.fromRuntimeState(firstGuess, "p1").turns().size());
        assertTrue(MatchStateHttpMapper.fromRuntimeState(firstGuess, "p2").turns().isEmpty());
        assertEquals(List.of("p1"), MatchStateHttpMapper.fromRuntimeState(firstGuess, "p2").submittedPlayers());

        MatchRuntimeState bothGuessed = firstGuess.advanceToWaitingFeedback()
                .withRecordedAction(new MatchActionRecord("p2", "PLAY_GUESS", "[\"B\",\"B\",\"B\",\"B\"]", null, 2L, 3L));
        assertEquals(2, MatchStateHttpMapper.fromRuntimeState(bothGuessed, "p1").turns().size());
        assertEquals(2, MatchStateHttpMapper.fromRuntimeState(bothGuessed, "p2").turns().size());

        MatchRuntimeState firstFeedback = bothGuessed.withVersionIncremented()
                .withRecordedAction(new MatchActionRecord("p1", "SEND_FEEDBACK", null, "{\"bienPlaces\":1,\"malPlaces\":0}", 3L, 4L));
        assertEquals(3, MatchStateHttpMapper.fromRuntimeState(firstFeedback, "p1").turns().size());
        assertEquals(2, MatchStateHttpMapper.fromRuntimeState(firstFeedback, "p2").turns().size());
        assertEquals(List.of("p1"), MatchStateHttpMapper.fromRuntimeState(firstFeedback, "p2").submittedPlayers());

        MatchRuntimeState bothResponded = firstFeedback.advanceToNextRound()
                .withRecordedAction(new MatchActionRecord("p2", "SEND_FEEDBACK", null, "{\"bienPlaces\":0,\"malPlaces\":1}", 4L, 5L));
        assertEquals(4, MatchStateHttpMapper.fromRuntimeState(bothResponded, "p1").turns().size());
        assertEquals(4, MatchStateHttpMapper.fromRuntimeState(bothResponded, "p2").turns().size());
    }
}
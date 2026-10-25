package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.api.submitaction.MatchStateHttpMapper;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;
import io.mastermindarena.deduction.infrastructure.jdbc.BoardPersistenceCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BoardLifecycleTest {
    private final InMemoryMatchStateStore store = new InMemoryMatchStateStore();
    private final SubmitActionOrchestrator flow = new SubmitActionOrchestrator(store, new InMemoryEventSink());
    private static final String CODE = "[\"A\",\"B\",\"E\",\"F\"]";
    private static final String ZERO = "{\"bienPlaces\":0,\"malPlaces\":0}";
    private static final String FOUND = "{\"bienPlaces\":4,\"malPlaces\":0}";

    @BeforeEach void create() { store.save(new MatchRuntimeState("board", List.of("p1", "p2"))); }

    @Test void embeddedJsonRejectsDuplicateKeysAndTrailingDocumentsWithExistingCodes() {
        for (String raw : List.of(CODE + " null", CODE + " []")) {
            assertEquals("INVALID_CODE", assertThrows(IllegalStateException.class,
                    () -> MastermindBoardRules.validateCode(raw)).getMessage());
        }
        for (String raw : List.of(ZERO + " {}", "{\"bienPlaces\":4,\"bienPlaces\":0,\"malPlaces\":0}")) {
            assertEquals("INVALID_FEEDBACK", assertThrows(IllegalStateException.class,
                    () -> MastermindBoardRules.blackPegs(raw)).getMessage());
        }
        for (String raw : List.of("{\"feedbackVersion\":1} null", "{\"feedbackVersion\":2,\"feedbackVersion\":1}")) {
            assertEquals("INVALID_FEEDBACK_TARGET", assertThrows(IllegalStateException.class,
                    () -> MastermindBoardRules.feedbackTarget(raw)).getMessage());
        }
        MastermindBoardRules.validateCode(" \n" + CODE + " \t");
        assertEquals(0, MastermindBoardRules.blackPegs(ZERO));
        assertEquals(1, MastermindBoardRules.feedbackTarget("{\"feedbackVersion\":1}"));
    }

    @Test void unlimitedRoundsRetainSecretsAndAllActions() {
        ready();
        for (int round = 0; round < 30; round++) {
            guesses();
            act("p1", "SEND_FEEDBACK", null, ZERO);
            act("p2", "SEND_FEEDBACK", null, ZERO);
        }
        MatchRuntimeState state = state();
        assertEquals(30, state.turnNumber());
        assertEquals(MatchRuntimeState.WAITING_GUESS, state.status());
        assertEquals(122, state.actionLog().size());
        assertEquals(List.of("A", "B", "E", "F"), MatchStateHttpMapper.fromRuntimeState(state, "p1").visibleSecretCode());
        assertEquals(state, FilePersistenceCodec.deserializeMatchRuntimeState(FilePersistenceCodec.serializeMatchRuntimeState(state)));
        assertEquals(state, BoardPersistenceCodec.decodeState(BoardPersistenceCodec.encodeState(state)));
    }

    @Test void loserContinuesWithoutChangingTheDuelPoint() {
        ready(); guesses();
        act("p1", "SEND_FEEDBACK", null, ZERO);
        act("p2", "SEND_FEEDBACK", null, FOUND);
        assertEquals("p1", state().winnerId());
        assertEquals(List.of("p2"), MatchStateHttpMapper.fromRuntimeState(state(), "p1").requiredPlayers());
        assertThrows(IllegalStateException.class, () -> act("p1", "PLAY_GUESS", CODE, null));
        act("p2", "PLAY_GUESS", CODE, null);
        assertEquals(MatchRuntimeState.WAITING_FEEDBACK, state().status());
        act("p1", "SEND_FEEDBACK", null, FOUND);
        assertEquals(MatchRuntimeState.FINISHED, state().status());
        assertEquals("p1", state().winnerId());
        assertEquals(1, state().scores().get("p1"));
        assertNull(state().scores().get("p2"));
        assertTrue(MatchStateHttpMapper.fromRuntimeState(state(), "p2").opponentSecretCode().isEmpty());
    }

    @Test void correctionRevokesVictoryAndReopensBothBoards() {
        ready(); guesses();
        act("p1", "SEND_FEEDBACK", null, ZERO);
        MatchRuntimeState won = act("p2", "SEND_FEEDBACK", null, FOUND);
        long target = won.actionLog().get(won.actionLog().size() - 1).version();
        act("p1", "CONTEST_FEEDBACK", target(target), null);
        MatchRuntimeState corrected = act("p2", "CORRECT_FEEDBACK", target(target), ZERO);
        assertNull(corrected.winnerId());
        assertTrue(corrected.scores().isEmpty());
        assertEquals(List.of("p1", "p2"), MatchStateHttpMapper.fromRuntimeState(corrected, "p1").requiredPlayers());
        assertEquals(0, MastermindBoardRules.blackPegs(MastermindBoardRules.editableFeedback(corrected, target).feedback()));
        guesses();
        act("p2", "SEND_FEEDBACK", null, ZERO);
        act("p1", "SEND_FEEDBACK", null, ZERO);
        assertEquals(2, state().turnNumber());
    }

    @Test void correctionHistoryPreservesInitialHintsAcrossMultipleCorrections() {
        ready(); guesses();
        MatchRuntimeState initial = act("p1", "SEND_FEEDBACK", null, ZERO);
        long target = initial.actionLog().get(initial.actionLog().size() - 1).version();
        assertTrue(MatchStateHttpMapper.fromRuntimeState(initial, "p2").turns().stream()
                .noneMatch(action -> action.version() == target));
        act("p2", "SEND_FEEDBACK", null, ZERO);
        act("p1", "CORRECT_FEEDBACK", target(target), FOUND);
        String partial = "{\"bienPlaces\":1,\"malPlaces\":1}";
        MatchRuntimeState corrected = act("p1", "CORRECT_FEEDBACK", target(target), partial);
        for (String reader : List.of("p1", "p2")) {
            var response = MatchStateHttpMapper.fromRuntimeState(corrected, reader);
            var feedback = response.turns().stream().filter(action -> action.version() == target).findFirst().orElseThrow();
            assertEquals(partial, feedback.feedback());
            assertEquals(ZERO, feedback.originalFeedback());
            var changes = response.turns().stream().filter(action -> "CORRECT_FEEDBACK".equals(action.actionType())).toList();
            assertEquals(List.of(FOUND, partial), changes.stream().map(action -> action.feedback()).toList());
            assertTrue(changes.stream().allMatch(action -> action.originalFeedback() == null));
        }
    }

    @Test void correctionOfDrawChangesScoreAndRematchNeedsNewConsent() {
        ready(); guesses();
        act("p1", "SEND_FEEDBACK", null, FOUND);
        MatchRuntimeState draw = act("p2", "SEND_FEEDBACK", null, FOUND);
        long target = draw.actionLog().get(draw.actionLog().size() - 1).version();
        act("p1", "REQUEST_REMATCH", null, null);
        act("p2", "CORRECT_FEEDBACK", target(target), ZERO);
        assertEquals("p2", state().winnerId());
        assertEquals(1, state().scores().get("p2"));
        assertEquals(MatchRuntimeState.WAITING_GUESS, state().status());
    }

    @Test void onlyOwnerCanCorrectAndOnlyLatestCompletedRoundIsEditable() {
        ready(); guesses();
        MatchRuntimeState first = act("p1", "SEND_FEEDBACK", null, ZERO);
        long target = first.actionLog().get(first.actionLog().size() - 1).version();
        assertThrows(IllegalStateException.class, () -> act("p1", "CORRECT_FEEDBACK", target(target), FOUND));
        act("p2", "SEND_FEEDBACK", null, ZERO);
        assertThrows(IllegalStateException.class, () -> act("p2", "CORRECT_FEEDBACK", target(target), FOUND));
        guesses(); act("p1", "SEND_FEEDBACK", null, ZERO); act("p2", "SEND_FEEDBACK", null, ZERO);
        assertThrows(IllegalStateException.class, () -> act("p1", "CORRECT_FEEDBACK", target(target), FOUND));
    }

    @Test void abandonmentRevealsSecretsAndKeepsWinnerPoint() {
        ready(); guesses();
        act("p1", "SEND_FEEDBACK", null, ZERO); act("p2", "SEND_FEEDBACK", null, FOUND);
        act("p2", "ABANDON", null, null);
        assertEquals("p1", state().winnerId());
        assertEquals(1, state().scores().get("p1"));
        assertEquals(List.of("A", "B", "E", "F"), MatchStateHttpMapper.fromRuntimeState(state(), "p2").opponentSecretCode());
    }

    @Test void serverRejectsMalformedCodesAndFeedbackWithoutAdvancingState() {
        for (String invalid : List.of("garbage", "[]", "[\"A\",\"B\",\"G\",\"F\"]", "[1,2,3,4]")) {
            assertThrows(IllegalStateException.class, () -> act("p1", "READY_SECRET", invalid, null));
        }
        assertEquals(0, state().version());
        ready(); guesses();
        for (String invalid : List.of("null", "{}", "{\"bienPlaces\":1.5,\"malPlaces\":0}",
                "{\"bienPlaces\":\"4\",\"malPlaces\":0}", "{\"bienPlaces\":4,\"malPlaces\":1}")) {
            assertThrows(IllegalStateException.class, () -> act("p1", "SEND_FEEDBACK", null, invalid));
        }
        assertEquals(4, state().version());
        assertThrows(IllegalArgumentException.class, () -> act("p1", "DECLARE_DISCOVERY", null, null));
        assertEquals(4, state().version());
    }

    private String target(long version) { return "{\"feedbackVersion\":" + version + "}"; }
    private void ready() { act("p1", "READY_SECRET", CODE, null); act("p2", "READY_SECRET", CODE, null); }
    private void guesses() { act("p1", "PLAY_GUESS", CODE, null); act("p2", "PLAY_GUESS", CODE, null); }
    private MatchRuntimeState state() { return store.findById("board").orElseThrow(); }
    private MatchRuntimeState act(String actor, String type, String payload, String feedback) {
        return flow.submit(new SubmitActionCommand("board", actor, state().version(), UUID.randomUUID().toString(), type, payload, feedback)).state();
    }
}

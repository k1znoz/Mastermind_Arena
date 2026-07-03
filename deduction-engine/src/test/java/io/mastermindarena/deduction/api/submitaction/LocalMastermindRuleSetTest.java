package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.RuleEvaluationContext;
import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMastermindRuleSetTest {

    @Test
    void rejectsSecondPlayerReadyForSameActor() {
        LocalMastermindRuleSet ruleSet = new LocalMastermindRuleSet();
        MatchRuntimeState state = withLog(
                record("p1", "PLAYER_READY", Map.of("type", "PLAYER_READY"), "IN_PROGRESS")
        );

        ActionResolution resolution = ruleSet.resolve(context(state, "p1", Map.of("type", "PLAYER_READY")));

        assertTrue(resolution.engineDirectives().contains(EngineDirective.REJECT_ACTION));
        assertEquals("PLAYER_ALREADY_READY", resolution.rejection().orElseThrow().code());
    }

    @Test
    void rejectsSecretResetAfterFirstValidation() {
        LocalMastermindRuleSet ruleSet = new LocalMastermindRuleSet();
        MatchRuntimeState state = withLog(
                record("p1", "PLAYER_READY", Map.of("type", "PLAYER_READY"), "IN_PROGRESS"),
                record("p2", "PLAYER_READY", Map.of("type", "PLAYER_READY"), "IN_PROGRESS"),
                record("p1", "SECRET_CODE_SET", Map.of("type", "SECRET_CODE_SET", "secretCode", List.of("A", "B", "C", "D")), "IN_PROGRESS")
        );

        ActionResolution resolution = ruleSet.resolve(context(
                state,
                "p1",
                Map.of("type", "SECRET_CODE_SET", "secretCode", List.of("D", "C", "B", "A"))
        ));

        assertTrue(resolution.engineDirectives().contains(EngineDirective.REJECT_ACTION));
        assertEquals("SECRET_ALREADY_SET", resolution.rejection().orElseThrow().code());
    }

    @Test
    void finishesOnlyWhenBothPlayersCrackOpponentSecret() {
        LocalMastermindRuleSet ruleSet = new LocalMastermindRuleSet();
        MatchRuntimeState stateAfterFirstSolver = withLog(
                record("p1", "PLAYER_READY", Map.of("type", "PLAYER_READY"), "IN_PROGRESS"),
                record("p2", "PLAYER_READY", Map.of("type", "PLAYER_READY"), "IN_PROGRESS"),
                record("p1", "SECRET_CODE_SET", Map.of("type", "SECRET_CODE_SET", "secretCode", List.of("A", "B", "C", "D")), "IN_PROGRESS"),
                record("p2", "SECRET_CODE_SET", Map.of("type", "SECRET_CODE_SET", "secretCode", List.of("E", "F", "G", "H")), "IN_PROGRESS"),
                record("p1", "SUBMIT_GUESS", Map.of("type", "SUBMIT_GUESS", "guess", List.of("E", "F", "G", "H")), "IN_PROGRESS")
        );

        ActionResolution firstResolution = ruleSet.resolve(context(
                stateAfterFirstSolver,
                "p2",
                Map.of("type", "SUBMIT_GUESS", "guess", List.of("A", "A", "A", "A"))
        ));

        assertTrue(firstResolution.engineDirectives().contains(EngineDirective.START_NEXT_TURN));
        assertTrue(firstResolution.matchOutcome().isEmpty());

        ActionResolution finishResolution = ruleSet.resolve(context(
                stateAfterFirstSolver,
                "p2",
                Map.of("type", "SUBMIT_GUESS", "guess", List.of("A", "B", "C", "D"))
        ));

        assertTrue(finishResolution.engineDirectives().contains(EngineDirective.FINISH_MATCH));
        assertEquals("BOTH_CODES_CRACKED", finishResolution.matchOutcome().orElseThrow().completionReason());
        assertEquals("p1", finishResolution.matchOutcome().orElseThrow().participantResults().get(0).participantId());
        assertEquals("WIN", finishResolution.matchOutcome().orElseThrow().participantResults().get(0).result());
    }

    private static RuleEvaluationContext context(MatchRuntimeState state, String actorId, Object payload) {
        return new RuleEvaluationContext(
                state,
                new SubmitActionCommand(state.matchId(), actorId, state.version(), "idem", payload)
        );
    }

    private static MatchRuntimeState withLog(MatchActionRecord... records) {
        return new MatchRuntimeState(
                "match-test",
                1,
                0,
                true,
                List.of("p1", "p2"),
                10,
                "IN_PROGRESS",
                null,
                null,
                List.of(records)
        );
    }

    private static MatchActionRecord record(String actorId, String actionType, Object payload, String status) {
        return MatchActionRecord.fromPayload(actorId, 1, payload, status, List.of("ActionResolved"), System.currentTimeMillis());
    }
}

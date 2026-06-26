package io.mastermindarena.deduction.engine.contract;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActionResolutionContractValidatorTest {
    private final ActionResolutionContractValidator validator = new ActionResolutionContractValidator();

    @Test
    void crP001_resolutionMustContainAtLeastOneDirective() {
        RuleSet ruleSet = input -> ActionResolution.of(Set.of());

        Rejection rejection = validator.validate(ruleSet.resolve(new Object()));

        assertNotNull(rejection);
        assertEquals(RejectionOrigin.ENGINE, rejection.origin());
        assertEquals(ActionResolutionContractValidator.RULESET_CONTRACT_VIOLATION, rejection.code());
    }

    @Test
    void crP002_finishMatchRequiresMatchOutcome() {
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(EngineDirective.FINISH_MATCH));

        Rejection rejection = validator.validate(ruleSet.resolve(new Object()));

        assertNotNull(rejection);
        assertEquals(ActionResolutionContractValidator.MATCH_OUTCOME_REQUIRED, rejection.code());
    }

    @Test
    void crP003_cancelMatchRequiresCancellationReason() {
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(EngineDirective.CANCEL_MATCH));

        Rejection rejection = validator.validate(ruleSet.resolve(new Object()));

        assertNotNull(rejection);
        assertEquals(ActionResolutionContractValidator.CANCELLATION_REASON_REQUIRED, rejection.code());
    }

    @Test
    void crP004_finishAndCancelAreMutuallyExclusive() {
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(EngineDirective.FINISH_MATCH, EngineDirective.CANCEL_MATCH));

        Rejection rejection = validator.validate(ruleSet.resolve(new Object()));

        assertNotNull(rejection);
        assertEquals(ActionResolutionContractValidator.TERMINAL_DIRECTIVES_CONFLICT, rejection.code());
    }

    @Test
    void crP005_directivesMustBeAllowedAndStructural() {
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION));

        Rejection rejection = validator.validate(ruleSet.resolve(new Object()));

        assertNull(rejection);
    }

    @Test
    void crP006_directiveCombinationsMustBeCoherent() {
        List<Set<EngineDirective>> invalidCombinations = List.of(
                Set.of(EngineDirective.REJECT_ACTION, EngineDirective.END_TURN),
                Set.of(EngineDirective.REJECT_ACTION, EngineDirective.FINISH_MATCH),
                Set.of(EngineDirective.CONTINUE_TURN, EngineDirective.END_TURN),
                Set.of(EngineDirective.START_NEXT_TURN),
                Set.of(EngineDirective.END_TURN, EngineDirective.START_NEXT_TURN, EngineDirective.FINISH_MATCH),
                Set.of(EngineDirective.END_TURN, EngineDirective.START_NEXT_TURN, EngineDirective.CANCEL_MATCH)
        );

        for (Set<EngineDirective> directives : invalidCombinations) {
            RuleSet ruleSet = input -> new ActionResolution(directives, null, null, new CancellationReason("STRUCTURAL"), null, List.of());
            Rejection rejection = validator.validate(ruleSet.resolve(new Object()));
            assertNotNull(rejection);
            assertEquals(ActionResolutionContractValidator.INVALID_ENGINE_DIRECTIVE_COMBINATION, rejection.code());
        }
    }

    @Test
    void crP007_rulesetRejectionRequiresRejectActionAndIsValidWhenConsistent() {
        Rejection ruleSetRejection = new Rejection(
                RejectionOrigin.RULESET,
                "INVALID_GUESS_LENGTH",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        MatchOutcome outcome = new MatchOutcome(
                "FINISHED",
                "GAME_ENDED",
                List.of(new ParticipantResult("p1", "LOSE", 2), new ParticipantResult("p2", "WIN", 1)),
                Instant.now()
        );

        RuleSet validRuleSet = input -> new ActionResolution(
                Set.of(EngineDirective.REJECT_ACTION),
                null,
                null,
                null,
                ruleSetRejection,
                List.of()
        );

        Rejection validRejection = validator.validate(validRuleSet.resolve(new Object()));
        assertNull(validRejection);

        RuleSet invalidRuleSet = input -> new ActionResolution(
                Set.of(EngineDirective.ACCEPT_ACTION),
                null,
                outcome,
                null,
                ruleSetRejection,
                List.of()
        );

        Rejection invalidRejection = validator.validate(invalidRuleSet.resolve(new Object()));
        assertNotNull(invalidRejection);
        assertEquals(RejectionOrigin.ENGINE, invalidRejection.origin());
        assertEquals(ActionResolutionContractValidator.RULESET_CONTRACT_VIOLATION, invalidRejection.code());
    }
}

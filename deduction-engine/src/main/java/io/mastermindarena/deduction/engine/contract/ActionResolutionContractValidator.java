package io.mastermindarena.deduction.engine.contract;

import java.util.Objects;
import java.util.Set;

public final class ActionResolutionContractValidator {
    public static final String RULESET_CONTRACT_VIOLATION = "RULESET_CONTRACT_VIOLATION";
    public static final String ENGINE_DIRECTIVE_NOT_ALLOWED = "ENGINE_DIRECTIVE_NOT_ALLOWED";
    public static final String INVALID_ENGINE_DIRECTIVE_COMBINATION = "INVALID_ENGINE_DIRECTIVE_COMBINATION";
    public static final String MATCH_OUTCOME_REQUIRED = "MATCH_OUTCOME_REQUIRED";
    public static final String CANCELLATION_REASON_REQUIRED = "CANCELLATION_REASON_REQUIRED";
    public static final String TERMINAL_DIRECTIVES_CONFLICT = "TERMINAL_DIRECTIVES_CONFLICT";

    public Rejection validate(ActionResolution resolution) {
        if (resolution == null || resolution.engineDirectives() == null || resolution.engineDirectives().isEmpty()) {
            return engineRejection(RULESET_CONTRACT_VIOLATION);
        }

        if (resolution.engineDirectives().stream().anyMatch(Objects::isNull)) {
            return engineRejection(ENGINE_DIRECTIVE_NOT_ALLOWED);
        }

        Set<EngineDirective> directives = resolution.engineDirectives();

        if (directives.contains(EngineDirective.FINISH_MATCH) && directives.contains(EngineDirective.CANCEL_MATCH)) {
            return engineRejection(TERMINAL_DIRECTIVES_CONFLICT);
        }

        if (directives.contains(EngineDirective.REJECT_ACTION) && directives.contains(EngineDirective.END_TURN)) {
            return engineRejection(INVALID_ENGINE_DIRECTIVE_COMBINATION);
        }
        if (directives.contains(EngineDirective.REJECT_ACTION) && directives.contains(EngineDirective.FINISH_MATCH)) {
            return engineRejection(INVALID_ENGINE_DIRECTIVE_COMBINATION);
        }
        if (directives.contains(EngineDirective.CONTINUE_TURN) && directives.contains(EngineDirective.END_TURN)) {
            return engineRejection(INVALID_ENGINE_DIRECTIVE_COMBINATION);
        }
        if (directives.contains(EngineDirective.START_NEXT_TURN) && !directives.contains(EngineDirective.END_TURN)) {
            return engineRejection(INVALID_ENGINE_DIRECTIVE_COMBINATION);
        }
        if (directives.contains(EngineDirective.FINISH_MATCH) && directives.contains(EngineDirective.START_NEXT_TURN)) {
            return engineRejection(INVALID_ENGINE_DIRECTIVE_COMBINATION);
        }
        if (directives.contains(EngineDirective.CANCEL_MATCH) && directives.contains(EngineDirective.START_NEXT_TURN)) {
            return engineRejection(INVALID_ENGINE_DIRECTIVE_COMBINATION);
        }

        if (directives.contains(EngineDirective.FINISH_MATCH) && resolution.matchOutcome().isEmpty()) {
            return engineRejection(MATCH_OUTCOME_REQUIRED);
        }

        if (directives.contains(EngineDirective.CANCEL_MATCH) && resolution.cancellationReason().isEmpty()) {
            return engineRejection(CANCELLATION_REASON_REQUIRED);
        }

        if (resolution.rejection().isPresent()) {
            Rejection rejection = resolution.rejection().get();
            if (rejection.origin() == RejectionOrigin.RULESET
                    && !directives.contains(EngineDirective.REJECT_ACTION)) {
                return engineRejection(RULESET_CONTRACT_VIOLATION);
            }
        }

        return null;
    }

    private Rejection engineRejection(String code) {
        return new Rejection(
                RejectionOrigin.ENGINE,
                code,
                null,
                null,
                Set.of(LogTarget.TECHNICAL_LOG)
        );
    }
}

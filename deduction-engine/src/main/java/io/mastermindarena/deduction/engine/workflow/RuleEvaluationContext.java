package io.mastermindarena.deduction.engine.workflow;

import java.util.Objects;

public record RuleEvaluationContext(
        MatchRuntimeState state,
        SubmitActionCommand command
) {
    public RuleEvaluationContext {
        Objects.requireNonNull(state, "state is required");
        Objects.requireNonNull(command, "command is required");
    }
}

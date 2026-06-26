package io.mastermindarena.deduction.engine.workflow;

import java.util.Objects;

public record SubmitActionCommand(String matchId, String actorId, Object actionPayload) {
    public SubmitActionCommand {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorId, "actorId is required");
    }
}

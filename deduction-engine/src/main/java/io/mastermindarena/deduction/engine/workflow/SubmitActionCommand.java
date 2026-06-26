package io.mastermindarena.deduction.engine.workflow;

import java.util.Objects;

public record SubmitActionCommand(
        String matchId,
        String actorId,
        long expectedVersion,
        String idempotencyKey,
        Object actionPayload
) {
    public SubmitActionCommand {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
    }
}

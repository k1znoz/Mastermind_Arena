package io.mastermindarena.deduction.application.submitaction;

import java.util.Objects;

public record SubmitActionApplicationRequest(
        String matchId,
        String actorId,
        long expectedVersion,
        String idempotencyKey,
        Object actionPayload
) {
    public SubmitActionApplicationRequest {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
    }
}

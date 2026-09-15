package io.mastermindarena.deduction.api.submitaction;

import java.util.Objects;

public record SubmitActionHttpRequest(
        String matchId,
        String actorId,
        long expectedVersion,
        String idempotencyKey,
        Object actionPayload
) {
    public SubmitActionHttpRequest {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
    }
}

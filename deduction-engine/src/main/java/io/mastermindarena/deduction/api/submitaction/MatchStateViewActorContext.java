package io.mastermindarena.deduction.api.submitaction;

import java.util.Objects;
import java.util.Optional;

public record MatchStateViewActorContext(String actorId) {
    public MatchStateViewActorContext {
        actorId = actorId == null || actorId.isBlank() ? null : actorId;
    }

    public Optional<String> actorIdOptional() {
        return Optional.ofNullable(actorId);
    }

    public boolean isOwner(String candidateActorId) {
        return Objects.equals(actorId, candidateActorId);
    }

    public static MatchStateViewActorContext anonymous() {
        return new MatchStateViewActorContext(null);
    }
}

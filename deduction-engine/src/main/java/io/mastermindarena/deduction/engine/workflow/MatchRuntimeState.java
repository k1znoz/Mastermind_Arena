package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Objects;

public record MatchRuntimeState(
        String matchId,
        int turnNumber,
        int currentActorIndex,
        List<String> actorOrder,
        long version,
        String status
) {
    public MatchRuntimeState {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorOrder, "actorOrder is required");
        Objects.requireNonNull(status, "status is required");
        actorOrder = List.copyOf(actorOrder);
        if (actorOrder.isEmpty()) {
            throw new IllegalArgumentException("actorOrder must not be empty");
        }
        if (turnNumber < 1) {
            throw new IllegalArgumentException("turnNumber must be >= 1");
        }
        if (currentActorIndex < 0 || currentActorIndex >= actorOrder.size()) {
            throw new IllegalArgumentException("currentActorIndex is out of bounds");
        }
    }

    public String currentActorId() {
        return actorOrder.get(currentActorIndex);
    }

    public MatchRuntimeState withVersionIncremented() {
        return new MatchRuntimeState(matchId, turnNumber, currentActorIndex, actorOrder, version + 1, status);
    }

    public MatchRuntimeState nextTurn() {
        int nextIndex = (currentActorIndex + 1) % actorOrder.size();
        return new MatchRuntimeState(matchId, turnNumber + 1, nextIndex, actorOrder, version + 1, status);
    }
}

package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.CancellationReason;
import io.mastermindarena.deduction.engine.contract.MatchOutcome;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MatchRuntimeState(
        String matchId,
        int turnNumber,
        int currentActorIndex,
    boolean turnActive,
        List<String> actorOrder,
        long version,
    String status,
    MatchOutcome matchOutcome,
    CancellationReason cancellationReason
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

        if ("FINISHED".equals(status) && matchOutcome == null) {
            throw new IllegalArgumentException("FINISHED state requires matchOutcome");
        }
        if ("CANCELLED".equals(status) && cancellationReason == null) {
            throw new IllegalArgumentException("CANCELLED state requires cancellationReason");
        }
    }

    public String currentActorId() {
        return actorOrder.get(currentActorIndex);
    }

    public MatchRuntimeState withVersionIncremented() {
        return new MatchRuntimeState(
                matchId,
                turnNumber,
                currentActorIndex,
            turnActive,
                actorOrder,
                version + 1,
                status,
                matchOutcome,
                cancellationReason
        );
    }

    public MatchRuntimeState nextTurn() {
        int nextIndex = (currentActorIndex + 1) % actorOrder.size();
        return new MatchRuntimeState(
                matchId,
                turnNumber + 1,
                nextIndex,
            true,
                actorOrder,
                version + 1,
                status,
                matchOutcome,
                cancellationReason
        );
    }

    public MatchRuntimeState finished(MatchOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome is required");
        return new MatchRuntimeState(
                matchId,
                turnNumber,
                currentActorIndex,
            turnActive,
                actorOrder,
                version + 1,
                "FINISHED",
                outcome,
                null
        );
    }

    public MatchRuntimeState cancelled(CancellationReason reason) {
        Objects.requireNonNull(reason, "reason is required");
        return new MatchRuntimeState(
                matchId,
                turnNumber,
                currentActorIndex,
            turnActive,
                actorOrder,
                version + 1,
                "CANCELLED",
                null,
                reason
        );
    }

    public boolean isTerminal() {
        return "FINISHED".equals(status) || "CANCELLED".equals(status);
    }

    public Optional<MatchOutcome> matchOutcomeOptional() {
        return Optional.ofNullable(matchOutcome);
    }

    public Optional<CancellationReason> cancellationReasonOptional() {
        return Optional.ofNullable(cancellationReason);
    }
}

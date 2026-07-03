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
        CancellationReason cancellationReason,
        List<MatchActionRecord> actionLog
) {
    public MatchRuntimeState {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorOrder, "actorOrder is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(actionLog, "actionLog is required");
        actorOrder = List.copyOf(actorOrder);
        actionLog = List.copyOf(actionLog);
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

    public MatchRuntimeState(
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
        this(matchId, turnNumber, currentActorIndex, turnActive, actorOrder, version, status, matchOutcome, cancellationReason, List.of());
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
                cancellationReason,
                actionLog
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
                cancellationReason,
                actionLog
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
                null,
                actionLog
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
                reason,
                actionLog
        );
    }

    public MatchRuntimeState withRecordedAction(MatchActionRecord actionRecord) {
        Objects.requireNonNull(actionRecord, "actionRecord is required");
        List<MatchActionRecord> updatedLog = new java.util.ArrayList<>(actionLog);
        updatedLog.add(actionRecord);
        if (updatedLog.size() > 50) {
            updatedLog = updatedLog.subList(updatedLog.size() - 50, updatedLog.size());
        }

        return new MatchRuntimeState(
                matchId,
                turnNumber,
                currentActorIndex,
                turnActive,
                actorOrder,
                version,
                status,
                matchOutcome,
                cancellationReason,
                updatedLog
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

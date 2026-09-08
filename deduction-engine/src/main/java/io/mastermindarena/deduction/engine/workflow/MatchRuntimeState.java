package io.mastermindarena.deduction.engine.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MatchRuntimeState(
        String matchId,
        String status,
        int turnNumber,
        int currentActorIndex,
        int currentFeedbackActorIndex,
        boolean turnActive,
        List<String> actorOrder,
        String activePlayerId,
        String feedbackPlayerId,
        long version,
        List<MatchActionRecord> actionLog,
        MatchOutcome legacyMatchOutcome
) {
    public static final String PREPARATION = "PREPARATION";
    public static final String WAITING_GUESS = "WAITING_GUESS";
    public static final String WAITING_FEEDBACK = "WAITING_FEEDBACK";
    public static final String FINISHED = "FINISHED";

    public MatchRuntimeState {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(actorOrder, "actorOrder is required");
        Objects.requireNonNull(actionLog, "actionLog is required");
        actorOrder = List.copyOf(actorOrder);
        actionLog = List.copyOf(actionLog);

        if (actorOrder.isEmpty()) {
            throw new IllegalArgumentException("actorOrder must not be empty");
        }
        if (turnNumber < 0) {
            throw new IllegalArgumentException("turnNumber must be >= 0");
        }
        if (currentActorIndex < 0 || currentActorIndex >= actorOrder.size()) {
            throw new IllegalArgumentException("currentActorIndex is out of bounds");
        }
        if (currentFeedbackActorIndex < 0 || currentFeedbackActorIndex >= actorOrder.size()) {
            throw new IllegalArgumentException("currentFeedbackActorIndex is out of bounds");
        }
        if (activePlayerId == null) {
            activePlayerId = actorOrder.get(currentActorIndex);
        }
        if (feedbackPlayerId == null) {
            feedbackPlayerId = actorOrder.get(currentFeedbackActorIndex);
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        }
        if (!List.of(PREPARATION, WAITING_GUESS, WAITING_FEEDBACK, FINISHED).contains(status)) {
            throw new IllegalArgumentException("unsupported plateau status: " + status);
        }
    }

    public MatchRuntimeState(String matchId, List<String> actorOrder, long version) {
        this(
                matchId,
                PREPARATION,
                0,
                0,
                1 % Math.max(actorOrder.size(), 1),
                true,
                actorOrder,
                actorOrder.get(0),
                actorOrder.get(1 % actorOrder.size()),
                version,
                List.of(),
                null
        );
    }

    public MatchRuntimeState(String matchId, List<String> actorOrder) {
        this(matchId, actorOrder, 0L);
    }

    public String currentActorId() {
        return actorOrder.get(currentActorIndex);
    }

    public String feedbackActorId() {
        return actorOrder.get(currentFeedbackActorIndex);
    }

    public boolean isTerminal() {
        return FINISHED.equals(status);
    }

    public MatchRuntimeState withVersionIncremented() {
        return new MatchRuntimeState(
                matchId,
                status,
                turnNumber,
                currentActorIndex,
                currentFeedbackActorIndex,
                turnActive,
                actorOrder,
                activePlayerId,
                feedbackPlayerId,
                version + 1,
                actionLog,
                legacyMatchOutcome
        );
    }

    public MatchRuntimeState withRecordedAction(MatchActionRecord actionRecord) {
        Objects.requireNonNull(actionRecord, "actionRecord is required");
        List<MatchActionRecord> updatedLog = new ArrayList<>(actionLog);
        updatedLog.add(actionRecord);
        if (updatedLog.size() > 50) {
            updatedLog = updatedLog.subList(updatedLog.size() - 50, updatedLog.size());
        }
        return new MatchRuntimeState(
                matchId,
                status,
                turnNumber,
                currentActorIndex,
                currentFeedbackActorIndex,
                turnActive,
                actorOrder,
                activePlayerId,
                feedbackPlayerId,
                version,
                updatedLog,
                legacyMatchOutcome
        );
    }

    public MatchRuntimeState advanceToWaitingGuess() {
        return new MatchRuntimeState(
                matchId,
                WAITING_GUESS,
                turnNumber,
                currentActorIndex,
                currentFeedbackActorIndex,
                true,
                actorOrder,
                activePlayerId,
                feedbackPlayerId,
                version + 1,
                actionLog,
                legacyMatchOutcome
        );
    }

    public MatchRuntimeState advanceToWaitingFeedback() {
        return new MatchRuntimeState(
                matchId,
                WAITING_FEEDBACK,
                turnNumber,
                currentActorIndex,
                currentFeedbackActorIndex,
                false,
                actorOrder,
                activePlayerId,
                feedbackPlayerId,
                version + 1,
                actionLog,
                legacyMatchOutcome
        );
    }

    public MatchRuntimeState finish() {
        return new MatchRuntimeState(
                matchId,
                FINISHED,
                turnNumber,
                currentActorIndex,
                currentFeedbackActorIndex,
                false,
                actorOrder,
                activePlayerId,
                feedbackPlayerId,
                version + 1,
                actionLog,
                legacyMatchOutcome
        );
    }

    public Optional<MatchOutcome> legacyMatchOutcomeOptional() {
        return Optional.ofNullable(legacyMatchOutcome);
    }

    @Deprecated
    public record MatchOutcome(
            String status,
            String reason
    ) {
        public MatchOutcome {
            Objects.requireNonNull(status, "status is required");
        }
    }
}

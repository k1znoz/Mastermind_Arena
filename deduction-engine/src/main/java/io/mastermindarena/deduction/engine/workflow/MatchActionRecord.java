package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Objects;

public record MatchActionRecord(
        String actorId,
        String actionType,
        String guess,
        String feedback,
        long timestamp,
        long version
) {
    public MatchActionRecord {
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(actionType, "actionType is required");
        guess = guess == null ? null : guess;
        feedback = feedback == null ? null : feedback;
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be >= 0");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        }
    }

    public boolean hasGuess() {
        return guess != null && !guess.isBlank();
    }

    public boolean hasFeedback() {
        return feedback != null && !feedback.isBlank();
    }
}

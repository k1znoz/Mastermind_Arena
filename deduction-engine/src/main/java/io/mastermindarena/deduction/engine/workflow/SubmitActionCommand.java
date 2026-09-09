package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Objects;

public record SubmitActionCommand(
        String matchId,
        String actorId,
        long expectedVersion,
        String idempotencyKey,
        String actionType,
        String payload,
        String feedback
) {
    public static final String READY_SECRET = "READY_SECRET";
    public static final String PLAY_GUESS = "PLAY_GUESS";
    public static final String SEND_FEEDBACK = "SEND_FEEDBACK";
    public static final String DECLARE_DISCOVERY = "DECLARE_DISCOVERY";

    public SubmitActionCommand {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        Objects.requireNonNull(actionType, "actionType is required");
        if (!List.of(READY_SECRET, PLAY_GUESS, SEND_FEEDBACK, DECLARE_DISCOVERY).contains(actionType)) {
            throw new IllegalArgumentException("unsupported board action type: " + actionType);
        }
        if (payload == null && (PLAY_GUESS.equals(actionType) || READY_SECRET.equals(actionType))) {
            throw new IllegalArgumentException("payload is required for " + actionType);
        }
        if (feedback == null && SEND_FEEDBACK.equals(actionType)) {
            throw new IllegalArgumentException("feedback is required for SEND_FEEDBACK");
        }
    }
}

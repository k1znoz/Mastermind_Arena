package io.mastermindarena.deduction.engine.contract;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MatchOutcome(
        String status,
        String completionReason,
        List<ParticipantResult> participantResults,
        Instant completedAt
) {
    public MatchOutcome {
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(completionReason, "completionReason is required");
        Objects.requireNonNull(participantResults, "participantResults is required");
        Objects.requireNonNull(completedAt, "completedAt is required");
        participantResults = List.copyOf(participantResults);
    }
}

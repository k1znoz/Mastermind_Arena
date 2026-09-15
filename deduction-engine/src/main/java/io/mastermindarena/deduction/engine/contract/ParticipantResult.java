package io.mastermindarena.deduction.engine.contract;

import java.util.Objects;

public record ParticipantResult(String participantId, String result, Integer rank) {
    public ParticipantResult {
        Objects.requireNonNull(participantId, "participantId is required");
        Objects.requireNonNull(result, "result is required");
    }
}

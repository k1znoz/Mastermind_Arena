package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Objects;

public record MatchActionRecord(
        String actorId,
        int turnNumber,
        String actionType,
        List<String> symbols,
        String payloadSummary,
        List<String> emittedEvents,
        String resultingStatus,
        long recordedAtEpochMs
) {
    public MatchActionRecord {
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(actionType, "actionType is required");
        Objects.requireNonNull(symbols, "symbols is required");
        Objects.requireNonNull(payloadSummary, "payloadSummary is required");
        Objects.requireNonNull(emittedEvents, "emittedEvents is required");
        Objects.requireNonNull(resultingStatus, "resultingStatus is required");
        symbols = List.copyOf(symbols);
        emittedEvents = List.copyOf(emittedEvents);
    }

    public static MatchActionRecord fromPayload(
            String actorId,
            int turnNumber,
            Object actionPayload,
            String resultingStatus,
            List<String> emittedEvents,
            long recordedAtEpochMs
    ) {
        MatchActionPayloadView payloadView = MatchActionPayloadView.from(actionPayload);

        return new MatchActionRecord(
                actorId,
                turnNumber,
                payloadView.actionType(),
                payloadView.symbols(),
                payloadView.payloadSummary(),
                emittedEvents,
                resultingStatus,
                recordedAtEpochMs
        );
    }
}

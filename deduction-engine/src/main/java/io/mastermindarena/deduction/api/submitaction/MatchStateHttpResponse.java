package io.mastermindarena.deduction.api.submitaction;

import java.util.List;

public record MatchStateHttpResponse(
        String matchId,
        long version,
        int turnNumber,
        int currentActorIndex,
        String currentActorId,
        boolean turnActive,
        List<String> actorOrder,
        String status,
        String matchOutcomeStatus,
        String matchOutcomeReason,
        String cancellationCode,
        List<String> visibleSecretCode,
        List<ActionLogEntry> actionLog
) {
    public MatchStateHttpResponse {
        actorOrder = actorOrder == null ? List.of() : List.copyOf(actorOrder);
        visibleSecretCode = visibleSecretCode == null ? List.of() : List.copyOf(visibleSecretCode);
        actionLog = actionLog == null ? List.of() : List.copyOf(actionLog);
    }

    public record ActionLogEntry(
            String actorId,
            int turnNumber,
            String actionType,
            List<String> symbols,
            String payloadSummary,
            List<String> emittedEvents,
            String resultingStatus,
            long recordedAtEpochMs
    ) {
        public ActionLogEntry {
            symbols = symbols == null ? List.of() : List.copyOf(symbols);
            emittedEvents = emittedEvents == null ? List.of() : List.copyOf(emittedEvents);
        }
    }

    public record Envelope(int statusCode, MatchStateHttpResponse body) {
    }
}

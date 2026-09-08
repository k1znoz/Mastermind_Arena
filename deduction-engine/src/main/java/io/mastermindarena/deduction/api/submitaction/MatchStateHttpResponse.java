package io.mastermindarena.deduction.api.submitaction;

import java.util.List;

public record MatchStateHttpResponse(
        String matchId,
        String state,
        String activePlayer,
        String feedbackGiver,
        List<String> players,
        List<TurnEntry> turns,
        long version,
        List<String> visibleSecretCode
) {
    public MatchStateHttpResponse {
        players = players == null ? List.of() : List.copyOf(players);
        turns = turns == null ? List.of() : List.copyOf(turns);
        visibleSecretCode = visibleSecretCode == null ? List.of() : List.copyOf(visibleSecretCode);
    }

    public record TurnEntry(
            int turnNumber,
            String actorId,
            String actionType,
            String guess,
            String feedback,
            long timestamp,
            long version
    ) {
    }

    public record Envelope(int statusCode, MatchStateHttpResponse body) {
    }
}

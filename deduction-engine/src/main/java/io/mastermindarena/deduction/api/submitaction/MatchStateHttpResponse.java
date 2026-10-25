package io.mastermindarena.deduction.api.submitaction;

import java.util.List;
import java.util.Map;

public record MatchStateHttpResponse(
        String matchId,
        String state,
        String activePlayer,
        String feedbackGiver,
        List<String> players,
        List<String> readyPlayers,
        List<TurnEntry> turns,
        long version,
        List<String> visibleSecretCode,
        Map<String, Integer> scores,
        String winnerId,
        int gameNumber,
        List<String> submittedPlayers,
        List<String> rematchRequestedPlayers,
        int roundNumber,
        List<String> solvedPlayers,
        List<String> requiredPlayers,
        List<String> opponentSecretCode
) {
    public MatchStateHttpResponse {
        players = players == null ? List.of() : List.copyOf(players);
        readyPlayers = readyPlayers == null ? List.of() : List.copyOf(readyPlayers);
        turns = turns == null ? List.of() : List.copyOf(turns);
        visibleSecretCode = visibleSecretCode == null ? List.of() : List.copyOf(visibleSecretCode);
        scores = scores == null ? Map.of() : Map.copyOf(scores);
        submittedPlayers = submittedPlayers == null ? List.of() : List.copyOf(submittedPlayers);
        rematchRequestedPlayers = rematchRequestedPlayers == null ? List.of() : List.copyOf(rematchRequestedPlayers);
        solvedPlayers = solvedPlayers == null ? List.of() : List.copyOf(solvedPlayers);
        requiredPlayers = requiredPlayers == null ? List.of() : List.copyOf(requiredPlayers);
        opponentSecretCode = opponentSecretCode == null ? List.of() : List.copyOf(opponentSecretCode);
    }

    public record TurnEntry(
            int turnNumber,
            String actorId,
            String actionType,
            String guess,
            String feedback,
            long timestamp,
            long version,
            String originalFeedback
    ) {
    }

    public record Envelope(int statusCode, MatchStateHttpResponse body) {
    }
}

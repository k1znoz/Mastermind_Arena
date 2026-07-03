package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.workflow.MatchStateStore;

import java.util.Objects;

public final class LocalMatchStateEndpoint {
    public static final String PATH = "/local/match-state";
    public static final String METHOD = "GET";

    private final MatchStateStore stateStore;

    public LocalMatchStateEndpoint(MatchStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore is required");
    }

    public MatchStateHttpResponse.Envelope getMatchState(String matchId) {
        return getMatchState(matchId, null);
    }

    public MatchStateHttpResponse.Envelope getMatchState(String matchId, String actorId) {
        if (matchId == null || matchId.isBlank()) {
            return new MatchStateHttpResponse.Envelope(400, null);
        }

        MatchStateViewActorContext actorContext = new MatchStateViewActorContext(actorId);

        return stateStore.findById(matchId)
                .map(state -> MatchStateHttpMapper.fromRuntimeState(state, actorContext))
                .map(response -> new MatchStateHttpResponse.Envelope(200, response))
                .orElseGet(() -> new MatchStateHttpResponse.Envelope(404, null));
    }
}

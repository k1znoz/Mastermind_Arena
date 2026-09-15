package io.mastermindarena.deduction.engine.workflow;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryMatchStateStore implements MatchStateStore {
    // MatchRuntimeState est immuable : stocker la reference conserve le plateau complet et sa version.
    private final Map<String, MatchRuntimeState> plateauxByMatchId = new ConcurrentHashMap<>();

    @Override
    public Optional<MatchRuntimeState> findById(String matchId) {
        Objects.requireNonNull(matchId, "matchId is required");
        return Optional.ofNullable(plateauxByMatchId.get(matchId));
    }

    @Override
    public void save(MatchRuntimeState state) {
        Objects.requireNonNull(state, "state is required");
        plateauxByMatchId.put(state.matchId(), state);
    }
}

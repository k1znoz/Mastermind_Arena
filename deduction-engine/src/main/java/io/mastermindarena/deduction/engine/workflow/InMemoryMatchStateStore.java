package io.mastermindarena.deduction.engine.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryMatchStateStore implements MatchStateStore {
    private final Map<String, MatchRuntimeState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<MatchRuntimeState> findById(String matchId) {
        return Optional.ofNullable(states.get(matchId));
    }

    @Override
    public void save(MatchRuntimeState state) {
        states.put(state.matchId(), state);
    }
}

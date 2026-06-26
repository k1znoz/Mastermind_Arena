package io.mastermindarena.deduction.engine.workflow;

import java.util.Optional;

public interface MatchStateStore {
    Optional<MatchRuntimeState> findById(String matchId);

    void save(MatchRuntimeState state);
}

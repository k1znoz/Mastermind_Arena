package io.mastermindarena.deduction.engine.workflow;

import java.util.Optional;

/** Persiste le plateau partagé d'un match, version incluse. */
public interface MatchStateStore {
    Optional<MatchRuntimeState> findById(String matchId);

    void save(MatchRuntimeState state);

    default <T> T atomic(String matchId, java.util.function.Supplier<T> operation) {
        return operation.get();
    }

    default boolean hasAtomicWrites() {
        return false;
    }
}

package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Objects;

/** Immutable acknowledgement of one accepted action, without a historical match snapshot. */
public record ActionReceipt(String matchId, long version, String status, List<String> emittedEvents) {
    public ActionReceipt {
        Objects.requireNonNull(matchId, "matchId is required");
        if (matchId.isBlank() || version < 0 || !List.of(MatchRuntimeState.PREPARATION,
                MatchRuntimeState.WAITING_GUESS, MatchRuntimeState.WAITING_FEEDBACK,
                MatchRuntimeState.FINISHED).contains(status)) throw new IllegalArgumentException("INVALID_ACTION_RECEIPT");
        emittedEvents = List.copyOf(emittedEvents);
        emittedEvents.forEach(GameEvent::valueOf);
    }
}

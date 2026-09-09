package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.Rejection;

import java.util.List;
import java.util.Objects;

public record SubmitActionResult(
        MatchRuntimeState state,
        SubmitActionCommand action,
        List<String> emittedEvents,
        Rejection rejection
) {
    public SubmitActionResult {
        Objects.requireNonNull(state, "state is required");
        Objects.requireNonNull(action, "action is required");
        emittedEvents = emittedEvents == null ? List.of() : List.copyOf(emittedEvents);
    }

    /** Reconstruit le contexte (matchId/version/payload) pour publier les emittedEvents en EventSink.PublishedEvent. */
    public List<EventSink.PublishedEvent> enrichedEmittedEvents(long timestamp) {
        return emittedEvents.stream()
                .map(name -> new EventSink.PublishedEvent(GameEvent.valueOf(name), state.matchId(), state.version(), action.payload(), timestamp))
                .toList();
    }
}

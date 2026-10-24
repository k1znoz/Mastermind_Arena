package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.Rejection;

import java.util.List;
import java.util.Objects;

/** Current match state plus the original acknowledgement; their versions may differ on replay. */
public record SubmitActionResult(
        MatchRuntimeState state,
        SubmitActionCommand action,
        List<String> emittedEvents,
        Rejection rejection,
        ActionReceipt receipt
) {
    public SubmitActionResult {
        Objects.requireNonNull(state, "state is required");
        Objects.requireNonNull(action, "action is required");
        emittedEvents = emittedEvents == null ? List.of() : List.copyOf(emittedEvents);
        Objects.requireNonNull(receipt, "receipt is required");
        if (!state.matchId().equals(receipt.matchId()) || state.version() < receipt.version()
                || !emittedEvents.equals(receipt.emittedEvents())) throw new IllegalArgumentException("INVALID_RESULT_RECEIPT");
    }

    public SubmitActionResult(MatchRuntimeState state, SubmitActionCommand action, List<String> emittedEvents, Rejection rejection) {
        this(state, action, emittedEvents, rejection, new ActionReceipt(state.matchId(), state.version(),
                state.status(), emittedEvents == null ? List.of() : emittedEvents));
    }

    /** Reconstruit le contexte (matchId/version/payload) pour publier les emittedEvents en EventSink.PublishedEvent. */
    public List<EventSink.PublishedEvent> enrichedEmittedEvents(long timestamp) {
        return emittedEvents.stream()
                .map(name -> new EventSink.PublishedEvent(GameEvent.valueOf(name), receipt.matchId(), receipt.version(), action.payload(), timestamp))
                .toList();
    }
}

package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.Rejection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SubmitActionOrchestrator {
    private final MatchStateStore stateStore;
    private final EventSink eventSink;
    private final IdempotencyStore idempotencyStore;

    public SubmitActionOrchestrator(MatchStateStore stateStore, EventSink eventSink) {
        this(stateStore, eventSink, new InMemoryIdempotencyStore());
    }

    public SubmitActionOrchestrator(MatchStateStore stateStore, EventSink eventSink, IdempotencyStore idempotencyStore) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore is required");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink is required");
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore is required");
    }

    public SubmitActionResult submit(SubmitActionCommand command) {
        MatchRuntimeState current = stateStore.findById(command.matchId())
                .orElseThrow(() -> new IllegalStateException("MATCH_NOT_FOUND"));

        String idempotencyScopeKey = command.matchId() + "|" + command.actorId() + "|" + command.idempotencyKey();
        String requestFingerprint = command.expectedVersion() + "|" + command.actionType() + "|" + command.payload() + "|" + command.feedback();
        IdempotencyStore.Entry existing = idempotencyStore.find(idempotencyScopeKey).orElse(null);
        if (existing != null) {
            if (existing.fingerprint().equals(requestFingerprint)) {
                return existing.result();
            }
            throw new IllegalStateException("IDEMPOTENCY_CONFLICT");
        }

        if (current.isTerminal()) {
            throw new IllegalStateException("MATCH_ALREADY_TERMINAL");
        }

        if (!MatchRuntimeState.WAITING_GUESS.equals(current.status())
                && !MatchRuntimeState.WAITING_FEEDBACK.equals(current.status())
                && !MatchRuntimeState.PREPARATION.equals(current.status())) {
            throw new IllegalStateException("INVALID_MATCH_STATE");
        }

        if (command.expectedVersion() != current.version()) {
            throw new IllegalStateException("VERSION_CONFLICT");
        }

        if (!current.currentActorId().equals(command.actorId())) {
            throw new IllegalStateException("ACTOR_NOT_AUTHORIZED");
        }

        List<String> emitted = new ArrayList<>();
        MatchRuntimeState updated;
        Rejection rejection = null;

        switch (command.actionType()) {
            case SubmitActionCommand.READY_SECRET -> {
                if (!MatchRuntimeState.PREPARATION.equals(current.status())) {
                    throw new IllegalStateException("INVALID_STATE_FOR_READY_SECRET");
                }
                updated = current.advanceToWaitingGuess();
                emit(emitted, GameEvent.GUESS_PLAYED);
            }
            case SubmitActionCommand.PLAY_GUESS -> {
                if (!MatchRuntimeState.WAITING_GUESS.equals(current.status())) {
                    throw new IllegalStateException("INVALID_STATE_FOR_PLAY_GUESS");
                }
                updated = current.advanceToWaitingFeedback();
                emit(emitted, GameEvent.GUESS_PLAYED);
            }
            case SubmitActionCommand.SEND_FEEDBACK -> {
                if (!MatchRuntimeState.WAITING_FEEDBACK.equals(current.status())) {
                    throw new IllegalStateException("INVALID_STATE_FOR_SEND_FEEDBACK");
                }
                updated = current.advanceToWaitingGuess();
                emit(emitted, GameEvent.TURN_CHANGED);
            }
            case SubmitActionCommand.DECLARE_DISCOVERY -> {
                updated = current.finish();
                emit(emitted, GameEvent.GAME_FINISHED);
            }
            default -> throw new IllegalStateException("UNSUPPORTED_ACTION_TYPE");
        }

        updated = updated.withRecordedAction(new MatchActionRecord(
                command.actorId(),
                command.actionType(),
                command.payload(),
                command.feedback(),
                System.currentTimeMillis(),
                updated.version()
        ));

        stateStore.save(updated);
        SubmitActionResult result = new SubmitActionResult(updated, command, List.copyOf(emitted), rejection);
        idempotencyStore.save(idempotencyScopeKey, new IdempotencyStore.Entry(requestFingerprint, result));
        return result;
    }

    private void emit(List<String> emitted, GameEvent event) {
        emitted.add(event.name());
        eventSink.publish(event.name());
    }
}

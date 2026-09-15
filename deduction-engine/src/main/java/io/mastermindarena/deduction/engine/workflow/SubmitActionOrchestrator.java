package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.Rejection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SubmitActionOrchestrator {
    private static final ObjectMapper JSON = new ObjectMapper();
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

    public synchronized SubmitActionResult submit(SubmitActionCommand command) {
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

        if (current.isTerminal() && !SubmitActionCommand.REQUEST_REMATCH.equals(command.actionType())) {
            throw new IllegalStateException("MATCH_ALREADY_TERMINAL");
        }

        if (!current.isTerminal() && !MatchRuntimeState.WAITING_GUESS.equals(current.status())
                && !MatchRuntimeState.WAITING_FEEDBACK.equals(current.status())
                && !MatchRuntimeState.PREPARATION.equals(current.status())) {
            throw new IllegalStateException("INVALID_MATCH_STATE");
        }

        if (command.expectedVersion() != current.version()) {
            throw new IllegalStateException("VERSION_CONFLICT");
        }

        if (!current.actorOrder().contains(command.actorId())) {
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
                if (hasReadySecret(current, command.actorId())) {
                    throw new IllegalStateException("SECRET_ALREADY_SET");
                }
                boolean otherPlayerReady = current.actorOrder().stream()
                        .anyMatch(actorId -> !actorId.equals(command.actorId()) && hasReadySecret(current, actorId));
                updated = otherPlayerReady ? current.advanceToWaitingGuess() : current.withVersionIncremented();
                emit(emitted, GameEvent.GUESS_PLAYED);
            }
            case SubmitActionCommand.PLAY_GUESS -> {
                if (!MatchRuntimeState.WAITING_GUESS.equals(current.status())) {
                    throw new IllegalStateException("INVALID_STATE_FOR_PLAY_GUESS");
                }
                if (hasActedThisRound(current, command.actorId(), SubmitActionCommand.PLAY_GUESS)) {
                    throw new IllegalStateException("GUESS_ALREADY_SUBMITTED");
                }
                boolean otherPlayerGuessed = current.actorOrder().stream()
                        .anyMatch(actorId -> !actorId.equals(command.actorId())
                                && hasActedThisRound(current, actorId, SubmitActionCommand.PLAY_GUESS));
                updated = otherPlayerGuessed ? current.advanceToWaitingFeedback() : current.withVersionIncremented();
                emit(emitted, GameEvent.GUESS_PLAYED);
            }
            case SubmitActionCommand.SEND_FEEDBACK -> {
                if (!MatchRuntimeState.WAITING_FEEDBACK.equals(current.status())) {
                    throw new IllegalStateException("INVALID_STATE_FOR_SEND_FEEDBACK");
                }
                if (hasActedThisRound(current, command.actorId(), SubmitActionCommand.SEND_FEEDBACK)) {
                    throw new IllegalStateException("FEEDBACK_ALREADY_SUBMITTED");
                }
                boolean otherPlayerResponded = current.actorOrder().stream()
                        .anyMatch(actorId -> !actorId.equals(command.actorId())
                                && hasActedThisRound(current, actorId, SubmitActionCommand.SEND_FEEDBACK));
                if (otherPlayerResponded) {
                    String winner = roundResult(current, command);
                    updated = winner != null || current.turnNumber() >= 9
                            ? current.finishWithResult(winner == null ? "DRAW" : winner)
                            : current.advanceToNextRound();
                    emit(emitted, updated.isTerminal() ? GameEvent.GAME_FINISHED : GameEvent.TURN_CHANGED);
                } else {
                    blackPegs(command.feedback());
                    updated = current.withVersionIncremented();
                    emit(emitted, GameEvent.TURN_CHANGED);
                }
            }
            case SubmitActionCommand.DECLARE_DISCOVERY -> {
                updated = current.finish();
                emit(emitted, GameEvent.GAME_FINISHED);
            }
            case SubmitActionCommand.REQUEST_REMATCH -> {
                if (!current.isTerminal()) throw new IllegalStateException("REMATCH_REQUIRES_FINISHED_MATCH");
                if (hasRequestedRematch(current, command.actorId())) throw new IllegalStateException("REMATCH_ALREADY_REQUESTED");
                boolean otherRequested = current.actorOrder().stream().anyMatch(id -> !id.equals(command.actorId()) && hasRequestedRematch(current, id));
                updated = otherRequested ? current.restartForRematch() : current.withVersionIncremented();
                emit(emitted, GameEvent.TURN_CHANGED);
            }
            default -> throw new IllegalStateException("UNSUPPORTED_ACTION_TYPE");
        }

        if (!(SubmitActionCommand.REQUEST_REMATCH.equals(command.actionType()) && !updated.isTerminal())) {
        updated = updated.withRecordedAction(new MatchActionRecord(
                command.actorId(),
                command.actionType(),
                command.payload(),
                command.feedback(),
                System.currentTimeMillis(),
                updated.version()
        ));
        }


        stateStore.save(updated);
        SubmitActionResult result = new SubmitActionResult(updated, command, List.copyOf(emitted), rejection);
        idempotencyStore.save(idempotencyScopeKey, new IdempotencyStore.Entry(requestFingerprint, result));
        return result;
    }

    private static boolean hasRequestedRematch(MatchRuntimeState state, String actorId) {
        return state.actionLog().stream().anyMatch(record ->
                SubmitActionCommand.REQUEST_REMATCH.equals(record.actionType()) && actorId.equals(record.actorId()));
    }

    private static String roundResult(MatchRuntimeState state, SubmitActionCommand lastFeedback) {
        List<MatchActionRecord> current = state.actionLog().stream()
                .filter(record -> SubmitActionCommand.SEND_FEEDBACK.equals(record.actionType()))
                .skip((long) state.turnNumber() * state.actorOrder().size()).toList();
        String firstWinner = null;
        boolean both = false;
        for (MatchActionRecord record : current) {
            if (blackPegs(record.feedback()) == 4) {
                String solver = state.actorOrder().stream().filter(id -> !id.equals(record.actorId())).findFirst().orElse(null);
                if (firstWinner != null && !firstWinner.equals(solver)) both = true;
                else firstWinner = solver;
            }
        }
        if (blackPegs(lastFeedback.feedback()) == 4) {
            String solver = state.actorOrder().stream().filter(id -> !id.equals(lastFeedback.actorId())).findFirst().orElse(null);
            if (firstWinner != null && !firstWinner.equals(solver)) both = true;
            else firstWinner = solver;
        }
        return both ? "DRAW" : firstWinner;
    }

    private static int blackPegs(String raw) {
        try {
            JsonNode node = JSON.readTree(raw);
            int black = node.path("bienPlaces").asInt(-1);
            int white = node.path("malPlaces").asInt(-1);
            if (black < 0 || white < 0 || black + white > 4) throw new IllegalStateException("INVALID_FEEDBACK");
            return black;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("INVALID_FEEDBACK", e);
        }
    }
    private void emit(List<String> emitted, GameEvent event) {
        emitted.add(event.name());
        eventSink.publish(event);
    }

    private static boolean hasActedThisRound(MatchRuntimeState state, String actorId, String actionType) {
        return state.actionLog().stream()
                .filter(record -> actionType.equals(record.actionType()) && actorId.equals(record.actorId()))
                .count() > state.turnNumber();
    }
    private static boolean hasReadySecret(MatchRuntimeState state, String actorId) {
        return state.actionLog().stream()
                .anyMatch(record -> SubmitActionCommand.READY_SECRET.equals(record.actionType()) && actorId.equals(record.actorId()));
    }
}

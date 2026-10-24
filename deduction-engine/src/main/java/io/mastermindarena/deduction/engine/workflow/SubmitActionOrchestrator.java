package io.mastermindarena.deduction.engine.workflow;

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
        this.stateStore = Objects.requireNonNull(stateStore);
        this.eventSink = Objects.requireNonNull(eventSink);
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore);
    }

    public synchronized SubmitActionResult submit(SubmitActionCommand command) {
        return stateStore.atomic(command.matchId(), () -> resolve(command));
    }

    private SubmitActionResult resolve(SubmitActionCommand command) {
        MatchRuntimeState current = stateStore.findById(command.matchId())
                .orElseThrow(() -> new ActionRejectedException("MATCH_NOT_FOUND"));
        if (!current.actorOrder().contains(command.actorId())) throw new ActionRejectedException("ACTOR_NOT_AUTHORIZED");
        String key = command.matchId() + "|" + command.actorId() + "|" + command.idempotencyKey();
        String fingerprint = command.expectedVersion() + "|" + command.actionType() + "|" + command.payload() + "|" + command.feedback();
        IdempotencyStore.Entry existing = idempotencyStore.find(key).orElse(null);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) throw new ActionRejectedException("IDEMPOTENCY_CONFLICT");
            var receipt = existing.receipt();
            return new SubmitActionResult(current, command, receipt.emittedEvents(), null, receipt);
        }
        // Recovery for memory stores whose idempotency journal was temporarily unavailable.
        if (command.expectedVersion() < current.version() && current.actionLog().stream().anyMatch(record ->
                record.version() == command.expectedVersion() + 1 && command.actorId().equals(record.actorId())
                        && command.actionType().equals(record.actionType()) && Objects.equals(command.payload(), record.guess())
                        && Objects.equals(command.feedback(), record.feedback()))) {
            return new SubmitActionResult(current, command, List.of(), null);
        }
        if (command.expectedVersion() != current.version()) throw new ActionRejectedException("VERSION_CONFLICT");
        String type = command.actionType();
        boolean correction = SubmitActionCommand.CORRECT_FEEDBACK.equals(type);
        boolean contest = SubmitActionCommand.CONTEST_FEEDBACK.equals(type);
        if (current.isTerminal() && !SubmitActionCommand.REQUEST_REMATCH.equals(type) && !correction && !contest) {
            throw new ActionRejectedException("MATCH_ALREADY_TERMINAL");
        }
        MatchRuntimeState updated;
        GameEvent event;
        boolean record = true;
        boolean completeRound = false;
        switch (type) {
            case SubmitActionCommand.READY_SECRET -> {
                requireState(current, MatchRuntimeState.PREPARATION, "INVALID_STATE_FOR_READY_SECRET");
                if (hasAction(current, command.actorId(), type)) throw new ActionRejectedException("SECRET_ALREADY_SET");
                MastermindBoardRules.validateCode(command.payload());
                boolean otherReady = hasAction(current, MastermindBoardRules.opponent(current, command.actorId()), type);
                updated = otherReady ? current.advanceToWaitingGuess() : current.withVersionIncremented();
                event = GameEvent.GAME_STARTED;
            }
            case SubmitActionCommand.PLAY_GUESS -> {
                requireState(current, MatchRuntimeState.WAITING_GUESS, "INVALID_STATE_FOR_PLAY_GUESS");
                if (!MastermindBoardRules.guessers(current).contains(command.actorId())) throw new ActionRejectedException("PLAYER_ALREADY_SOLVED");
                if (MastermindBoardRules.hasActed(current, command.actorId(), type)) throw new ActionRejectedException("GUESS_ALREADY_SUBMITTED");
                MastermindBoardRules.validateCode(command.payload());
                boolean all = MastermindBoardRules.guessers(current).stream().allMatch(id -> id.equals(command.actorId())
                        || MastermindBoardRules.hasActed(current, id, type));
                updated = all ? current.advanceToWaitingFeedback() : current.withVersionIncremented();
                event = GameEvent.GUESS_PLAYED;
            }
            case SubmitActionCommand.SEND_FEEDBACK -> {
                requireState(current, MatchRuntimeState.WAITING_FEEDBACK, "INVALID_STATE_FOR_SEND_FEEDBACK");
                if (!MastermindBoardRules.feedbackGivers(current).contains(command.actorId())) throw new ActionRejectedException("FEEDBACK_NOT_REQUIRED");
                if (MastermindBoardRules.hasActed(current, command.actorId(), type)) throw new ActionRejectedException("FEEDBACK_ALREADY_SUBMITTED");
                MastermindBoardRules.blackPegs(command.feedback());
                completeRound = MastermindBoardRules.feedbackGivers(current).stream().allMatch(id -> id.equals(command.actorId())
                        || MastermindBoardRules.hasActed(current, id, type));
                updated = completeRound ? current.advanceToNextRound() : current.withVersionIncremented();
                event = GameEvent.FEEDBACK_SENT;
            }
            case SubmitActionCommand.CORRECT_FEEDBACK, SubmitActionCommand.CONTEST_FEEDBACK -> {
                MatchActionRecord target = MastermindBoardRules.editableFeedback(current, MastermindBoardRules.feedbackTarget(command.payload()));
                boolean owner = target.actorId().equals(command.actorId());
                if (correction != owner) throw new ActionRejectedException("ACTOR_NOT_AUTHORIZED");
                if (correction) MastermindBoardRules.blackPegs(command.feedback());
                if (contest && current.actionLog().stream().anyMatch(action -> type.equals(action.actionType())
                        && command.actorId().equals(action.actorId()) && Objects.equals(command.payload(), action.guess()))) {
                    throw new ActionRejectedException("FEEDBACK_ALREADY_CONTESTED");
                }
                updated = correction ? current.withoutRematchRequests().withVersionIncremented() : current.withVersionIncremented();
                event = GameEvent.FEEDBACK_SENT;
            }
            case SubmitActionCommand.ABANDON -> {
                if (MatchRuntimeState.PREPARATION.equals(current.status())) {
                    throw new ActionRejectedException("ABANDON_REQUIRES_STARTED_GAME");
                }
                updated = current.finish();
                event = GameEvent.GAME_FINISHED;
            }
            case SubmitActionCommand.REQUEST_REMATCH -> {
                if (!current.isTerminal()) throw new ActionRejectedException("REMATCH_REQUIRES_FINISHED_MATCH");
                if (hasAction(current, command.actorId(), type)) throw new ActionRejectedException("REMATCH_ALREADY_REQUESTED");
                boolean both = hasAction(current, MastermindBoardRules.opponent(current, command.actorId()), type);
                updated = both ? current.restartForRematch() : current.withVersionIncremented();
                record = !both;
                event = GameEvent.GAME_STARTED;
            }
            default -> throw new IllegalStateException("UNSUPPORTED_ADMITTED_ACTION");
        }
        if (record) updated = updated.withRecordedAction(new MatchActionRecord(command.actorId(), type,
                command.payload(), command.feedback(), System.currentTimeMillis(), updated.version(), current.turnNumber()));
        if (completeRound || correction || SubmitActionCommand.ABANDON.equals(type)) updated = reconcile(updated);
        if (updated.isTerminal()) event = GameEvent.GAME_FINISHED;
        stateStore.save(updated);
        List<String> emitted = new ArrayList<>();
        emitted.add(event.name());
        eventSink.publish(event);
        SubmitActionResult result = new SubmitActionResult(updated, command, List.copyOf(emitted), null);
        try {
            idempotencyStore.save(key, new IdempotencyStore.Entry(fingerprint, result.receipt()));
        } catch (IllegalStateException failure) {
            if (stateStore.hasAtomicWrites()) throw failure;
            System.err.println("Idempotency journal unavailable; action retained in state history");
        }
        return result;
    }

    private static MatchRuntimeState reconcile(MatchRuntimeState state) {
        List<String> guessers = MastermindBoardRules.guessers(state);
        if (!guessers.isEmpty() && guessers.stream().allMatch(id -> MastermindBoardRules.hasActed(state, id, SubmitActionCommand.PLAY_GUESS))
                && MastermindBoardRules.feedbackGivers(state).stream().allMatch(id -> MastermindBoardRules.hasActed(state, id, SubmitActionCommand.SEND_FEEDBACK))) {
            return reconcile(state.completeCurrentRound());
        }
        String winner = MastermindBoardRules.result(state);
        boolean abandoned = state.actionLog().stream().anyMatch(action -> SubmitActionCommand.ABANDON.equals(action.actionType()));
        String status;
        if (abandoned || MastermindBoardRules.guessers(state).isEmpty()) status = MatchRuntimeState.FINISHED;
        else status = MastermindBoardRules.guessers(state).stream().allMatch(id ->
                MastermindBoardRules.hasActed(state, id, SubmitActionCommand.PLAY_GUESS))
                ? MatchRuntimeState.WAITING_FEEDBACK : MatchRuntimeState.WAITING_GUESS;
        return state.withBoardResult(status, winner);
    }

    private static boolean hasAction(MatchRuntimeState state, String actor, String type) {
        return state.actionLog().stream().anyMatch(action -> actor.equals(action.actorId()) && type.equals(action.actionType()));
    }

    private static void requireState(MatchRuntimeState state, String required, String code) {
        if (!required.equals(state.status())) throw new ActionRejectedException(code);
    }
}

package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.MastermindBoardRules;
import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;

import java.util.List;

public final class MatchStateHttpMapper {
    private static final ObjectMapper JSON = new ObjectMapper();
    private MatchStateHttpMapper() {}

    public static MatchStateHttpResponse fromRuntimeState(MatchRuntimeState state) {
        return fromRuntimeState(state, MatchStateViewActorContext.anonymous());
    }
    public static MatchStateHttpResponse fromRuntimeState(MatchRuntimeState state, String actorId) {
        return fromRuntimeState(state, new MatchStateViewActorContext(actorId));
    }
    public static MatchStateHttpResponse fromRuntimeState(MatchRuntimeState state, MatchStateViewActorContext actor) {
        String phase = switch (state.status()) {
            case MatchRuntimeState.PREPARATION -> SubmitActionCommand.READY_SECRET;
            case MatchRuntimeState.WAITING_GUESS -> SubmitActionCommand.PLAY_GUESS;
            case MatchRuntimeState.WAITING_FEEDBACK -> SubmitActionCommand.SEND_FEEDBACK;
            default -> "";
        };
        List<String> required = switch (state.status()) {
            case MatchRuntimeState.PREPARATION -> state.actorOrder();
            case MatchRuntimeState.WAITING_GUESS -> MastermindBoardRules.guessers(state);
            case MatchRuntimeState.WAITING_FEEDBACK -> MastermindBoardRules.feedbackGivers(state);
            default -> List.of();
        };
        List<MatchActionRecord> actions = MastermindBoardRules.effectiveActions(state);
        List<String> ready = state.actorOrder().stream().filter(id -> state.actionLog().stream().anyMatch(a ->
                SubmitActionCommand.READY_SECRET.equals(a.actionType()) && id.equals(a.actorId()))).toList();
        List<String> submitted = required.stream().filter(id -> SubmitActionCommand.READY_SECRET.equals(phase)
                ? ready.contains(id) : MastermindBoardRules.hasActed(state, id, phase)).toList();
        List<MatchStateHttpResponse.TurnEntry> turns = actions.stream()
                .filter(action -> !SubmitActionCommand.READY_SECRET.equals(action.actionType()))
                .filter(action -> !(phase.equals(action.actionType()) && action.roundNumber() == state.turnNumber()
                        && !actor.isOwner(action.actorId())))
                .map(action -> new MatchStateHttpResponse.TurnEntry(action.roundNumber() + 1, action.actorId(), action.actionType(),
                        action.guess(), action.feedback(), action.timestamp(), action.version())).toList();
        List<String> rematch = state.actorOrder().stream().filter(id -> state.actionLog().stream().anyMatch(action ->
                SubmitActionCommand.REQUEST_REMATCH.equals(action.actionType()) && id.equals(action.actorId()))).toList();
        boolean reveal = state.isTerminal() && state.actionLog().stream().anyMatch(action -> SubmitActionCommand.ABANDON.equals(action.actionType()));
        String reader = actor.actorIdOptional().orElse(null);
        List<String> opponentSecret = reveal && reader != null && state.actorOrder().contains(reader)
                ? secret(state, MastermindBoardRules.opponent(state, reader)) : List.of();
        return new MatchStateHttpResponse(state.matchId(), state.status(), state.activePlayerId(), state.feedbackPlayerId(),
                state.actorOrder(), ready, turns, state.version(), secret(state, reader), state.scores(), state.winnerId(),
                state.gameNumber(), submitted, rematch, state.turnNumber(), MastermindBoardRules.solvedPlayers(state), required, opponentSecret);
    }
    private static List<String> secret(MatchRuntimeState state, String actorId) {
        for (MatchActionRecord action : state.actionLog()) {
            if (SubmitActionCommand.READY_SECRET.equals(action.actionType()) && action.actorId().equals(actorId)) {
                try {
                    return JSON.readValue(action.guess(), new TypeReference<List<String>>() {});
                } catch (Exception failure) {
                    return List.of();
                }
            }
        }
        return List.of();
    }
}

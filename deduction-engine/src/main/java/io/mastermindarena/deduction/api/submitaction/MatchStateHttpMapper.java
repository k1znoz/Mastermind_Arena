package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;

import java.util.ArrayList;
import java.util.List;

public final class MatchStateHttpMapper {
    private static final String READY_SECRET = "READY_SECRET";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MatchStateHttpMapper() {
    }

    public static MatchStateHttpResponse fromRuntimeState(MatchRuntimeState state) {
        return fromRuntimeState(state, MatchStateViewActorContext.anonymous());
    }

    public static MatchStateHttpResponse fromRuntimeState(MatchRuntimeState state, String actorId) {
        return fromRuntimeState(state, new MatchStateViewActorContext(actorId));
    }

    public static MatchStateHttpResponse fromRuntimeState(MatchRuntimeState state, MatchStateViewActorContext actorContext) {
        List<MatchStateHttpResponse.TurnEntry> turns = new ArrayList<>();
        for (MatchActionRecord action : state.actionLog()) {
            if (shouldExpose(action, actorContext, state)) {
                turns.add(toTurnEntry(action));
            }
        }

        return new MatchStateHttpResponse(
                state.matchId(),
                state.status(),
                state.activePlayerId(),
                state.feedbackPlayerId(),
                state.actorOrder(),
                readyPlayers(state),
                turns,
                state.version(),
                visibleSecretCode(state.actionLog(), actorContext),
                state.scores(),
                state.winnerId(),
                state.gameNumber(),
                submittedPlayers(state),
                rematchRequestedPlayers(state)
        );
    }

    private static List<String> submittedPlayers(MatchRuntimeState state) {
        String actionType = switch (state.status()) {
            case MatchRuntimeState.PREPARATION -> "READY_SECRET";
            case MatchRuntimeState.WAITING_GUESS -> "PLAY_GUESS";
            case MatchRuntimeState.WAITING_FEEDBACK -> "SEND_FEEDBACK";
            default -> "";
        };
        if (actionType.isBlank()) return List.of();
        return state.actorOrder().stream().filter(actorId -> {
            long count = state.actionLog().stream().filter(entry ->
                    actionType.equals(entry.actionType()) && actorId.equals(entry.actorId())).count();
            return "READY_SECRET".equals(actionType) ? count > 0 : count > state.turnNumber();
        }).toList();
    }

    private static List<String> rematchRequestedPlayers(MatchRuntimeState state) {
        if (!state.isTerminal()) return List.of();
        return state.actorOrder().stream().filter(actorId -> state.actionLog().stream()
                .anyMatch(entry -> "REQUEST_REMATCH".equals(entry.actionType()) && actorId.equals(entry.actorId()))).toList();
    }
    private static List<String> readyPlayers(MatchRuntimeState state) {
        return state.actorOrder().stream()
                .filter(actorId -> state.actionLog().stream()
                        .anyMatch(entry -> READY_SECRET.equals(entry.actionType()) && actorId.equals(entry.actorId())))
                .toList();
    }
    private static boolean shouldExpose(MatchActionRecord entry, MatchStateViewActorContext actorContext, MatchRuntimeState state) {
        if (READY_SECRET.equals(entry.actionType())) {
            return false;
        }
        boolean pendingGuess = MatchRuntimeState.WAITING_GUESS.equals(state.status())
                && "PLAY_GUESS".equals(entry.actionType());
        boolean pendingFeedback = MatchRuntimeState.WAITING_FEEDBACK.equals(state.status())
                && "SEND_FEEDBACK".equals(entry.actionType());
        if ((pendingGuess || pendingFeedback) && !actorContext.isOwner(entry.actorId())) {
            return entry != state.actionLog().get(state.actionLog().size() - 1);
        }
        return true;
    }

    private static MatchStateHttpResponse.TurnEntry toTurnEntry(MatchActionRecord entry) {
        return new MatchStateHttpResponse.TurnEntry(
                (int) entry.version(),
                entry.actorId(),
                entry.actionType(),
                entry.guess(),
                entry.feedback(),
                entry.timestamp(),
                entry.version()
        );
    }

    private static List<String> visibleSecretCode(List<MatchActionRecord> actionLog, MatchStateViewActorContext actorContext) {
        if (actorContext.actorIdOptional().isEmpty()) {
            return List.of();
        }

        for (int index = actionLog.size() - 1; index >= 0; index--) {
            MatchActionRecord entry = actionLog.get(index);
            if (READY_SECRET.equals(entry.actionType()) && actorContext.isOwner(entry.actorId())) {
                return deserializeSecretCode(entry.guess());
            }
        }

        return List.of();
    }

    private static List<String> deserializeSecretCode(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(rawPayload, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}

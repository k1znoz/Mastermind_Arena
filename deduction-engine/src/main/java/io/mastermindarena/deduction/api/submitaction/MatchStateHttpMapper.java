package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;

import java.util.ArrayList;
import java.util.List;

public final class MatchStateHttpMapper {
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
            if (shouldExpose(action, actorContext)) {
                turns.add(toTurnEntry(action));
            }
        }

        return new MatchStateHttpResponse(
                state.matchId(),
                state.status(),
                state.activePlayerId(),
                state.feedbackPlayerId(),
                state.actorOrder(),
                turns,
                state.version(),
                visibleSecretCode(state.actionLog(), actorContext)
        );
    }

    private static boolean shouldExpose(MatchActionRecord entry, MatchStateViewActorContext actorContext) {
        return !"SECRET_CODE_SET".equals(entry.actionType()) || actorContext.isOwner(entry.actorId());
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
            if ("SECRET_CODE_SET".equals(entry.actionType()) && actorContext.isOwner(entry.actorId())) {
                return entry.guess() == null ? List.of() : List.of(entry.guess());
            }
        }

        return List.of();
    }
}

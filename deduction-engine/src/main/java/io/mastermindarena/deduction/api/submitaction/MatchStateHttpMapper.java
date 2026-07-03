package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.contract.MatchOutcome;
import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;

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
        MatchOutcome outcome = state.matchOutcome();
        List<MatchStateHttpResponse.ActionLogEntry> actionLog = state.actionLog().stream()
                .filter(entry -> shouldExpose(entry, actorContext))
                .map(entry -> toHttpEntry(entry, actorContext))
                .toList();

        return new MatchStateHttpResponse(
                state.matchId(),
                state.version(),
                state.turnNumber(),
                state.currentActorIndex(),
                state.currentActorId(),
                state.turnActive(),
                state.actorOrder(),
                state.status(),
                outcome == null ? null : outcome.status(),
                outcome == null ? null : outcome.completionReason(),
                state.cancellationReason() == null ? null : state.cancellationReason().code(),
                visibleSecretCode(state.actionLog(), actorContext),
                actionLog
        );
    }

    private static boolean shouldExpose(MatchActionRecord entry, MatchStateViewActorContext actorContext) {
        return !"SECRET_CODE_SET".equals(entry.actionType()) || actorContext.isOwner(entry.actorId());
    }

    private static MatchStateHttpResponse.ActionLogEntry toHttpEntry(MatchActionRecord entry, MatchStateViewActorContext actorContext) {
        List<String> visibleSymbols = "SECRET_CODE_SET".equals(entry.actionType())
                && !actorContext.isOwner(entry.actorId())
                ? List.of()
                : entry.symbols();

        return new MatchStateHttpResponse.ActionLogEntry(
                entry.actorId(),
                entry.turnNumber(),
                entry.actionType(),
                visibleSymbols,
                entry.payloadSummary(),
                entry.emittedEvents(),
                entry.resultingStatus(),
                entry.recordedAtEpochMs()
        );
    }

    private static List<String> visibleSecretCode(List<MatchActionRecord> actionLog, MatchStateViewActorContext actorContext) {
        if (actorContext.actorIdOptional().isEmpty()) {
            return List.of();
        }

        for (int index = actionLog.size() - 1; index >= 0; index--) {
            MatchActionRecord entry = actionLog.get(index);
            if ("SECRET_CODE_SET".equals(entry.actionType()) && actorContext.isOwner(entry.actorId())) {
                return entry.symbols();
            }
        }

        return List.of();
    }
}

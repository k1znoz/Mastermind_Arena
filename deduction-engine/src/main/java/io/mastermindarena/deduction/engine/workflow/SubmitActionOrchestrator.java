package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RuleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SubmitActionOrchestrator {
    private final MatchStateStore stateStore;
    private final EventSink eventSink;
    private final RuleSet ruleSet;
    private final ActionResolutionContractValidator validator;

    public SubmitActionOrchestrator(
            MatchStateStore stateStore,
            EventSink eventSink,
            RuleSet ruleSet,
            ActionResolutionContractValidator validator
    ) {
        this.stateStore = stateStore;
        this.eventSink = eventSink;
        this.ruleSet = ruleSet;
        this.validator = validator;
    }

    public SubmitActionResult submit(SubmitActionCommand command) {
        MatchRuntimeState current = stateStore.findById(command.matchId())
                .orElseThrow(() -> new IllegalStateException("MATCH_NOT_FOUND"));

        if (current.isTerminal()) {
            throw new IllegalStateException("MATCH_ALREADY_TERMINAL");
        }

        if (!"IN_PROGRESS".equals(current.status())) {
            throw new IllegalStateException("MATCH_NOT_IN_PROGRESS");
        }

        if (!current.currentActorId().equals(command.actorId())) {
            throw new IllegalStateException("ACTOR_NOT_AUTHORIZED");
        }

        List<String> emitted = new ArrayList<>();
        emit(emitted, "ActionSubmitted");

        ActionResolution resolution = ruleSet.resolve(command.actionPayload());
        Rejection validationRejection = validator.validate(resolution);
        if (validationRejection != null) {
            throw new IllegalStateException(validationRejection.code());
        }

        Set<EngineDirective> directives = resolution.engineDirectives();
        if (!directives.contains(EngineDirective.ACCEPT_ACTION)) {
            throw new IllegalStateException("ENGINE_DIRECTIVE_NOT_ALLOWED");
        }

        emit(emitted, "ActionAccepted");
        emit(emitted, "ActionResolved");

        MatchRuntimeState updated;
        if (directives.contains(EngineDirective.CONTINUE_TURN)) {
            updated = current.withVersionIncremented();
        } else if (directives.contains(EngineDirective.END_TURN)
                && directives.contains(EngineDirective.START_NEXT_TURN)) {
            emit(emitted, "TurnEnded");
            updated = current.nextTurn();
            emit(emitted, "TurnStarted");
        } else if (directives.contains(EngineDirective.FINISH_MATCH)) {
            emit(emitted, "MatchFinished");
            updated = current.finished(resolution.matchOutcome().orElseThrow());
        } else if (directives.contains(EngineDirective.CANCEL_MATCH)) {
            emit(emitted, "MatchCancelled");
            updated = current.cancelled(resolution.cancellationReason().orElseThrow());
        } else {
            throw new IllegalStateException("INVALID_ENGINE_DIRECTIVE_COMBINATION");
        }

        stateStore.save(updated);
        return new SubmitActionResult(updated, resolution, List.copyOf(emitted));
    }

    private void emit(List<String> emitted, String event) {
        emitted.add(event);
        eventSink.publish(event);
    }
}

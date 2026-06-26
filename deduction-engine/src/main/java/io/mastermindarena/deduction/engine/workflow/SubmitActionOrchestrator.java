package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.contract.RuleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SubmitActionOrchestrator {
    private final MatchStateStore stateStore;
    private final EventSink eventSink;
    private final RuleSet ruleSet;
    private final ActionResolutionContractValidator validator;
    private final IdempotencyStore idempotencyStore;

    public SubmitActionOrchestrator(
            MatchStateStore stateStore,
            EventSink eventSink,
            RuleSet ruleSet,
            ActionResolutionContractValidator validator
    ) {
        this(stateStore, eventSink, ruleSet, validator, new InMemoryIdempotencyStore());
    }

    public SubmitActionOrchestrator(
            MatchStateStore stateStore,
            EventSink eventSink,
            RuleSet ruleSet,
            ActionResolutionContractValidator validator,
            IdempotencyStore idempotencyStore
    ) {
        this.stateStore = stateStore;
        this.eventSink = eventSink;
        this.ruleSet = ruleSet;
        this.validator = validator;
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore is required");
    }

    public SubmitActionResult submit(SubmitActionCommand command) {
        MatchRuntimeState current = stateStore.findById(command.matchId())
                .orElseThrow(() -> new IllegalStateException("MATCH_NOT_FOUND"));

        String idempotencyScopeKey = command.matchId() + "|" + command.actorId() + "|" + command.idempotencyKey();
        String requestFingerprint = command.expectedVersion() + "|" + String.valueOf(command.actionPayload());
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

        if (!"IN_PROGRESS".equals(current.status())) {
            throw new IllegalStateException("MATCH_NOT_IN_PROGRESS");
        }

        if (!current.turnActive()) {
            throw new IllegalStateException("TURN_NOT_ACTIVE");
        }

        if (!current.currentActorId().equals(command.actorId())) {
            throw new IllegalStateException("ACTOR_NOT_AUTHORIZED");
        }

        if (command.expectedVersion() != current.version()) {
            throw new IllegalStateException("VERSION_CONFLICT");
        }

        List<String> emitted = new ArrayList<>();
        emit(emitted, "ActionSubmitted");

        ActionResolution resolution = ruleSet.resolve(command.actionPayload());
        Rejection validationRejection = validator.validate(resolution);
        if (validationRejection != null) {
            throw new IllegalStateException(validationRejection.code());
        }

        Set<EngineDirective> directives = resolution.engineDirectives();
        if (directives.contains(EngineDirective.REJECT_ACTION)) {
            Rejection rejection = resolution.rejection().orElseThrow(
                    () -> new IllegalStateException(ActionResolutionContractValidator.RULESET_CONTRACT_VIOLATION)
            );

            if (rejection.origin() != RejectionOrigin.RULESET) {
                throw new IllegalStateException("ENGINE_DIRECTIVE_NOT_ALLOWED");
            }

            if (rejection.targetLogs().contains(LogTarget.MATCH_HISTORY)
                    || rejection.targetLogs().contains(LogTarget.DOMAIN_EVENT_LOG)) {
                emit(emitted, "ActionRejected");
            }

            SubmitActionResult result = new SubmitActionResult(current, resolution, List.copyOf(emitted));
            idempotencyStore.save(idempotencyScopeKey, new IdempotencyStore.Entry(requestFingerprint, result));
            return result;
        }

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
        SubmitActionResult result = new SubmitActionResult(updated, resolution, List.copyOf(emitted));
        idempotencyStore.save(idempotencyScopeKey, new IdempotencyStore.Entry(requestFingerprint, result));
        return result;
    }

    private void emit(List<String> emitted, String event) {
        emitted.add(event);
        eventSink.publish(event);
    }
}

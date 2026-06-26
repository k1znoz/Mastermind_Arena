package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.CancellationReason;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.MatchOutcome;
import io.mastermindarena.deduction.engine.contract.ParticipantResult;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmitActionWorkflowTest {

    @Test
    void wfP001_acceptActionAndContinueTurn() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(
                EngineDirective.ACCEPT_ACTION,
                EngineDirective.CONTINUE_TURN
        ));

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-1",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-1", "p1", 0, "k1", "payload"));

        assertEquals(1, result.state().turnNumber());
        assertEquals("p1", result.state().currentActorId());
        assertEquals(1, result.state().version());
        assertEquals(List.of("ActionSubmitted", "ActionAccepted", "ActionResolved"), result.emittedEvents());
        assertEquals(result.emittedEvents(), eventSink.allEvents());
    }

    @Test
    void wfP002_acceptActionEndTurnAndStartNextTurn() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(
                EngineDirective.ACCEPT_ACTION,
                EngineDirective.END_TURN,
                EngineDirective.START_NEXT_TURN
        ));

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-2",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-2", "p1", 0, "k2", "payload"));

        assertEquals(2, result.state().turnNumber());
        assertEquals("p2", result.state().currentActorId());
        assertEquals(1, result.state().version());
        assertEquals(
                List.of("ActionSubmitted", "ActionAccepted", "ActionResolved", "TurnEnded", "TurnStarted"),
                result.emittedEvents()
        );
        assertEquals(result.emittedEvents(), eventSink.allEvents());
    }

    @Test
    void wfP003_finishMatchWithOutcome() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();

        MatchOutcome outcome = new MatchOutcome(
                "FINISHED",
                "RULESET_DECIDED",
                List.of(new ParticipantResult("p1", "WIN", 1), new ParticipantResult("p2", "LOSE", 2)),
                Instant.now()
        );

        RuleSet ruleSet = input -> new ActionResolution(
                Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.FINISH_MATCH),
                null,
                outcome,
                null,
                null,
                List.of()
        );

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-3",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-3", "p1", 0, "k3", "payload"));

        assertEquals("FINISHED", result.state().status());
        assertEquals(1, result.state().version());
        assertEquals(true, result.state().matchOutcomeOptional().isPresent());
        assertEquals(false, result.state().cancellationReasonOptional().isPresent());
        assertEquals(
                List.of("ActionSubmitted", "ActionAccepted", "ActionResolved", "MatchFinished"),
                result.emittedEvents()
        );
        assertEquals(result.emittedEvents(), eventSink.allEvents());
    }

    @Test
    void wfP004_cancelMatchWithCancellationReason() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();

        CancellationReason reason = new CancellationReason("HOST_CANCELLED");
        RuleSet ruleSet = input -> new ActionResolution(
                Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CANCEL_MATCH),
                null,
                null,
                reason,
                null,
                List.of()
        );

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-4",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-4", "p1", 0, "k4", "payload"));

        assertEquals("CANCELLED", result.state().status());
        assertEquals(1, result.state().version());
        assertEquals(false, result.state().matchOutcomeOptional().isPresent());
        assertEquals(true, result.state().cancellationReasonOptional().isPresent());
        assertEquals(
                List.of("ActionSubmitted", "ActionAccepted", "ActionResolved", "MatchCancelled"),
                result.emittedEvents()
        );
        assertEquals(result.emittedEvents(), eventSink.allEvents());
    }

    @Test
    void wfP015_terminalMatchRefusesSubmitActionAndDoesNotInvokeRuleSet() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();

        MatchOutcome outcome = new MatchOutcome(
                "FINISHED",
                "ALREADY_DONE",
                List.of(new ParticipantResult("p1", "WIN", 1), new ParticipantResult("p2", "LOSE", 2)),
                Instant.now()
        );

        MatchRuntimeState terminal = new MatchRuntimeState(
                "match-5",
                1,
                0,
                true,
                List.of("p1", "p2"),
                1,
                "FINISHED",
                outcome,
                null
        );
        stateStore.save(terminal);

        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-5", "p1", 1, "k5", "payload"))
        );

        assertEquals("MATCH_ALREADY_TERMINAL", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
    }

    @Test
    void wfP005_matchNotFound() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("missing", "p1", 0, "k-missing", "payload"))
        );

        assertEquals("MATCH_NOT_FOUND", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
    }

    @Test
    void wfP006_matchNotInProgress() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        MatchRuntimeState created = new MatchRuntimeState(
                "match-created",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "CREATED",
                null,
                null
        );
        stateStore.save(created);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-created", "p1", 0, "k-created", "payload"))
        );

        assertEquals("MATCH_NOT_IN_PROGRESS", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
        assertEquals(0, stateStore.findById("match-created").orElseThrow().version());
    }

    @Test
    void wfP007_turnNotActive() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        MatchRuntimeState state = new MatchRuntimeState(
                "match-turn-off",
                1,
                0,
                false,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(state);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-turn-off", "p1", 0, "k-turn-off", "payload"))
        );

        assertEquals("TURN_NOT_ACTIVE", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
        assertEquals(0, stateStore.findById("match-turn-off").orElseThrow().version());
    }

    @Test
    void wfP008_actorNotAuthorized() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        MatchRuntimeState state = new MatchRuntimeState(
                "match-auth",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(state);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-auth", "p2", 0, "k-auth", "payload"))
        );

        assertEquals("ACTOR_NOT_AUTHORIZED", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
        assertEquals(0, stateStore.findById("match-auth").orElseThrow().version());
    }

    @Test
    void wfP009_versionConflict() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        MatchRuntimeState state = new MatchRuntimeState(
                "match-version",
                1,
                0,
                true,
                List.of("p1", "p2"),
                2,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(state);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-version", "p1", 1, "k-version", "payload"))
        );

        assertEquals("VERSION_CONFLICT", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
        assertEquals(2, stateStore.findById("match-version").orElseThrow().version());
    }

    @Test
    void wfP010a_idempotentReplayReturnsPreviousResponseWithoutNewEffects() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        MatchRuntimeState state = new MatchRuntimeState(
                "match-idem",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(state);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionCommand first = new SubmitActionCommand("match-idem", "p1", 0, "k-idem", "payload");
        SubmitActionResult r1 = orchestrator.submit(first);
        List<String> eventsAfterFirst = eventSink.allEvents();

        SubmitActionResult r2 = orchestrator.submit(first);

        assertEquals(1, invocations.get());
        assertEquals(r1.state(), r2.state());
        assertEquals(r1.emittedEvents(), r2.emittedEvents());
        assertEquals(eventsAfterFirst, eventSink.allEvents());
    }

    @Test
    void wfP010b_idempotencyConflict() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        MatchRuntimeState state = new MatchRuntimeState(
                "match-idem-conflict",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(state);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionCommand first = new SubmitActionCommand("match-idem-conflict", "p1", 0, "k-idem", "payload-a");
        SubmitActionResult r1 = orchestrator.submit(first);
        List<String> eventsAfterFirst = eventSink.allEvents();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-idem-conflict", "p1", 0, "k-idem", "payload-b"))
        );

        assertEquals("IDEMPOTENCY_CONFLICT", ex.getMessage());
        assertEquals(1, invocations.get());
        assertEquals(eventsAfterFirst, eventSink.allEvents());
        assertEquals(r1.state(), stateStore.findById("match-idem-conflict").orElseThrow());
    }

    @Test
    void wfP011_rulesetResolutionWithoutDirectiveIsRejectedAsEngineContractViolation() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of());
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-e11",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-e11", "p1", 0, "k-e11", "payload"))
        );

        assertEquals(ActionResolutionContractValidator.RULESET_CONTRACT_VIOLATION, ex.getMessage());
        assertEquals(1, invocations.get());
        assertEquals(initial, stateStore.findById("match-e11").orElseThrow());
        assertEquals(List.of("ActionSubmitted"), eventSink.allEvents());
    }

    @Test
    void wfP012_finishMatchWithoutOutcomeRejectedWithMatchOutcomeRequired() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.FINISH_MATCH),
                    null,
                    null,
                    null,
                    null,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-e12",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-e12", "p1", 0, "k-e12", "payload"))
        );

        assertEquals(ActionResolutionContractValidator.MATCH_OUTCOME_REQUIRED, ex.getMessage());
        assertEquals(1, invocations.get());
        assertEquals(initial, stateStore.findById("match-e12").orElseThrow());
        assertEquals(List.of("ActionSubmitted"), eventSink.allEvents());
    }

    @Test
    void wfP013_cancelMatchWithoutReasonRejectedWithCancellationReasonRequired() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CANCEL_MATCH),
                    null,
                    null,
                    null,
                    null,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-e13",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-e13", "p1", 0, "k-e13", "payload"))
        );

        assertEquals(ActionResolutionContractValidator.CANCELLATION_REASON_REQUIRED, ex.getMessage());
        assertEquals(1, invocations.get());
        assertEquals(initial, stateStore.findById("match-e13").orElseThrow());
        assertEquals(List.of("ActionSubmitted"), eventSink.allEvents());
    }

    @Test
    void wfP014_finishAndCancelConflictRejectedWithTerminalDirectivesConflict() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.FINISH_MATCH, EngineDirective.CANCEL_MATCH),
                    null,
                    null,
                    null,
                    null,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-e14",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-e14", "p1", 0, "k-e14", "payload"))
        );

        assertEquals(ActionResolutionContractValidator.TERMINAL_DIRECTIVES_CONFLICT, ex.getMessage());
        assertEquals(1, invocations.get());
        assertEquals(initial, stateStore.findById("match-e14").orElseThrow());
        assertEquals(List.of("ActionSubmitted"), eventSink.allEvents());
    }

    @Test
    void wfP021_invalidDirectiveCombinationRejectedWithInvalidEngineDirectiveCombination() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN, EngineDirective.END_TURN),
                    null,
                    null,
                    null,
                    null,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-e21",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.submit(new SubmitActionCommand("match-e21", "p1", 0, "k-e21", "payload"))
        );

        assertEquals(ActionResolutionContractValidator.INVALID_ENGINE_DIRECTIVE_COMBINATION, ex.getMessage());
        assertEquals(1, invocations.get());
        assertEquals(initial, stateStore.findById("match-e21").orElseThrow());
        assertEquals(List.of("ActionSubmitted"), eventSink.allEvents());
    }

    @Test
    void wfP016_businessActionRejectedByRuleSetWithRejectAction() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        Rejection businessRejection = new Rejection(
                RejectionOrigin.RULESET,
                "INVALID_GUESS_LENGTH",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.REJECT_ACTION),
                    null,
                    null,
                    null,
                    businessRejection,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-f16",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-f16", "p1", 0, "k-f16", "payload"));

        assertEquals(1, invocations.get());
        assertEquals(RejectionOrigin.RULESET, result.resolution().rejection().orElseThrow().origin());
        assertEquals(true, result.resolution().engineDirectives().contains(EngineDirective.REJECT_ACTION));
        assertEquals(initial, result.state());
        assertEquals(List.of("ActionSubmitted", "ActionRejected"), result.emittedEvents());
        assertEquals(result.emittedEvents(), eventSink.allEvents());
    }

    @Test
    void wfP017_rulesetOpaqueCodeIsPreservedAsIs() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        String opaqueCode = "MASTERMIND_CUSTOM_OPAQUE_CODE_42";
        Rejection businessRejection = new Rejection(
                RejectionOrigin.RULESET,
                opaqueCode,
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.REJECT_ACTION),
                    null,
                    null,
                    null,
                    businessRejection,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-f17",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-f17", "p1", 0, "k-f17", "payload"));

        assertEquals(1, invocations.get());
        assertEquals(opaqueCode, result.resolution().rejection().orElseThrow().code());
    }

    @Test
    void wfP018_rulesetRejectionIsLoggedOnlyAccordingToLogTarget() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        MatchRuntimeState initial = new MatchRuntimeState(
                "match-f18",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );

        RuleSet ruleSetHistory = input -> new ActionResolution(
                Set.of(EngineDirective.REJECT_ACTION),
                null,
                null,
                null,
                new Rejection(RejectionOrigin.RULESET, "CODE_A", null, null, Set.of(LogTarget.MATCH_HISTORY)),
                List.of()
        );

        RuleSet ruleSetDomain = input -> new ActionResolution(
                Set.of(EngineDirective.REJECT_ACTION),
                null,
                null,
                null,
                new Rejection(RejectionOrigin.RULESET, "CODE_B", null, null, Set.of(LogTarget.DOMAIN_EVENT_LOG)),
                List.of()
        );

        RuleSet ruleSetNoBusinessLog = input -> new ActionResolution(
                Set.of(EngineDirective.REJECT_ACTION),
                null,
                null,
                null,
                new Rejection(RejectionOrigin.RULESET, "CODE_C", null, null, Set.of(LogTarget.TECHNICAL_LOG)),
                List.of()
        );

        stateStore.save(initial);
        InMemoryEventSink sinkHistory = new InMemoryEventSink();
        SubmitActionResult r1 = new SubmitActionOrchestrator(stateStore, sinkHistory, ruleSetHistory, new ActionResolutionContractValidator())
                .submit(new SubmitActionCommand("match-f18", "p1", 0, "k-f18-1", "payload"));
        assertEquals(List.of("ActionSubmitted", "ActionRejected"), r1.emittedEvents());

        stateStore.save(initial);
        InMemoryEventSink sinkDomain = new InMemoryEventSink();
        SubmitActionResult r2 = new SubmitActionOrchestrator(stateStore, sinkDomain, ruleSetDomain, new ActionResolutionContractValidator())
                .submit(new SubmitActionCommand("match-f18", "p1", 0, "k-f18-2", "payload"));
        assertEquals(List.of("ActionSubmitted", "ActionRejected"), r2.emittedEvents());

        stateStore.save(initial);
        InMemoryEventSink sinkNone = new InMemoryEventSink();
        SubmitActionResult r3 = new SubmitActionOrchestrator(stateStore, sinkNone, ruleSetNoBusinessLog, new ActionResolutionContractValidator())
                .submit(new SubmitActionCommand("match-f18", "p1", 0, "k-f18-3", "payload"));
        assertEquals(List.of("ActionSubmitted"), r3.emittedEvents());
    }

    @Test
    void wfP019_rulesetRejectionDoesNotMutateState() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        Rejection businessRejection = new Rejection(
                RejectionOrigin.RULESET,
                "NO_MUTATION",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.REJECT_ACTION),
                    null,
                    null,
                    null,
                    businessRejection,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-f19",
                7,
                1,
                true,
                List.of("p1", "p2"),
                4,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-f19", "p2", 4, "k-f19", "payload"));

        assertEquals(1, invocations.get());
        assertEquals(initial, result.state());
        assertEquals(initial, stateStore.findById("match-f19").orElseThrow());
        assertEquals(List.of("ActionSubmitted", "ActionRejected"), eventSink.allEvents());
    }

    @Test
    void wfP020_rulesetRejectionIsNotConfusedWithEngineRejection() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();
        AtomicInteger invocations = new AtomicInteger(0);

        Rejection businessRejection = new Rejection(
                RejectionOrigin.RULESET,
                "OPAQUE_GAME_CODE",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        RuleSet ruleSet = input -> {
            invocations.incrementAndGet();
            return new ActionResolution(
                    Set.of(EngineDirective.REJECT_ACTION),
                    null,
                    null,
                    null,
                    businessRejection,
                    List.of()
            );
        };

        MatchRuntimeState initial = new MatchRuntimeState(
                "match-f20",
                1,
                0,
                true,
                List.of("p1", "p2"),
                0,
                "IN_PROGRESS",
                null,
                null
        );
        stateStore.save(initial);

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-f20", "p1", 0, "k-f20", "payload"));

        assertEquals(1, invocations.get());
        assertEquals(RejectionOrigin.RULESET, result.resolution().rejection().orElseThrow().origin());
        assertEquals("OPAQUE_GAME_CODE", result.resolution().rejection().orElseThrow().code());
        assertEquals(false, result.emittedEvents().contains("ActionAccepted"));
        assertEquals(List.of("ActionSubmitted", "ActionRejected"), result.emittedEvents());
    }
}

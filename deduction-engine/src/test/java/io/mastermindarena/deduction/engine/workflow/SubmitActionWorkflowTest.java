package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.CancellationReason;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.MatchOutcome;
import io.mastermindarena.deduction.engine.contract.ParticipantResult;
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

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-1", "p1", "payload"));

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

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-2", "p1", "payload"));

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

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-3", "p1", "payload"));

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

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand("match-4", "p1", "payload"));

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
                () -> orchestrator.submit(new SubmitActionCommand("match-5", "p1", "payload"))
        );

        assertEquals("MATCH_ALREADY_TERMINAL", ex.getMessage());
        assertEquals(0, invocations.get());
        assertEquals(List.of(), eventSink.allEvents());
    }
}

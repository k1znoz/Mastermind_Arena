package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                "IN_PROGRESS"
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
                "IN_PROGRESS"
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
}

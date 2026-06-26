package io.mastermindarena.deduction.application.submitaction;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import io.mastermindarena.deduction.engine.workflow.InMemoryEventSink;
import io.mastermindarena.deduction.engine.workflow.InMemoryMatchStateStore;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitActionApplicationIntegrationTest {

    @Test
    void p1a_nominalAccepted() {
        SubmitActionApplicationService service = buildService(
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN)),
                true
        );

        SubmitActionApplicationResponse response = service.submit(
                new SubmitActionApplicationRequest("match-app-1", "p1", 0, "k-app-1", "payload")
        );

        assertTrue(response.accepted());
        assertNull(response.rejection());
        assertNotNull(response.resolution());
        assertTrue(response.resolution().engineDirectives().contains(EngineDirective.ACCEPT_ACTION));
        assertEquals(List.of("ActionSubmitted", "ActionAccepted", "ActionResolved"), response.emittedEvents());
    }

    @Test
    void p1a_structuralRejectionMatchNotFound() {
        SubmitActionApplicationService service = buildService(
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN)),
                false
        );

        SubmitActionApplicationResponse response = service.submit(
                new SubmitActionApplicationRequest("missing", "p1", 0, "k-app-missing", "payload")
        );

        assertFalse(response.accepted());
        assertNull(response.state());
        assertNull(response.resolution());
        assertEquals(RejectionOrigin.ENGINE, response.rejection().origin());
        assertEquals("MATCH_NOT_FOUND", response.rejection().code());
        assertNull(response.rejection().rulesetRejection());
        assertEquals(List.of(), response.emittedEvents());
    }

    @Test
    void p1a_rulesetRejectionIsPreserved() {
        Rejection rulesetRejection = new Rejection(
                RejectionOrigin.RULESET,
                "INVALID_GUESS_LENGTH",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        SubmitActionApplicationService service = buildService(
                input -> new ActionResolution(
                        Set.of(EngineDirective.REJECT_ACTION),
                        null,
                        null,
                        null,
                        rulesetRejection,
                        List.of()
                ),
                true
        );

        SubmitActionApplicationResponse response = service.submit(
                new SubmitActionApplicationRequest("match-app-1", "p1", 0, "k-app-ruleset", "payload")
        );

        assertFalse(response.accepted());
        assertNotNull(response.state());
        assertNotNull(response.resolution());
        assertEquals(RejectionOrigin.RULESET, response.rejection().origin());
        assertEquals("INVALID_GUESS_LENGTH", response.rejection().code());
        assertEquals(rulesetRejection, response.rejection().rulesetRejection());
        assertEquals(List.of("ActionSubmitted", "ActionRejected"), response.emittedEvents());
    }

    @Test
    void p1a_idempotentReplayWhenSameRequestIsResubmitted() {
        AtomicInteger invocations = new AtomicInteger(0);

        SubmitActionApplicationService service = buildService(
                input -> {
                    invocations.incrementAndGet();
                    return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
                },
                true
        );

        SubmitActionApplicationRequest request =
                new SubmitActionApplicationRequest("match-app-1", "p1", 0, "k-app-idem", "payload");

        SubmitActionApplicationResponse first = service.submit(request);
        SubmitActionApplicationResponse second = service.submit(request);

        assertEquals(1, invocations.get());
        assertTrue(first.accepted());
        assertTrue(second.accepted());
        assertEquals(first.state(), second.state());
        assertEquals(first.emittedEvents(), second.emittedEvents());
    }

    private SubmitActionApplicationService buildService(RuleSet ruleSet, boolean seedMatch) {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();

        if (seedMatch) {
            stateStore.save(new MatchRuntimeState(
                    "match-app-1",
                    1,
                    0,
                    true,
                    List.of("p1", "p2"),
                    0,
                    "IN_PROGRESS",
                    null,
                    null
            ));
        }

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator()
        );

        return new SubmitActionApplicationService(orchestrator);
    }
}

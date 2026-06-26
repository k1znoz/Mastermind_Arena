package io.mastermindarena.deduction.application.persistence;

import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationRequest;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;
import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.ActionResolutionContractValidator;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.infrastructure.file.FileIdempotencyStore;
import io.mastermindarena.deduction.infrastructure.file.FileMatchStateStore;
import io.mastermindarena.deduction.infrastructure.file.FileWorkflowEventSink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P1BFilePersistenceIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void p1b_nominalSubmissionWithStateReloadedFromFileStore() {
        PersistenceContext context = createContext(
                tempDir.resolve("nominal"),
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN))
        );

        context.stateStore.save(inProgressState("match-p1b-1", 0));

        SubmitActionApplicationResponse response = context.service.submit(
                new SubmitActionApplicationRequest("match-p1b-1", "p1", 0, "k-p1b-1", "payload")
        );

        assertTrue(response.accepted());

        FileMatchStateStore reloadedStateStore = new FileMatchStateStore(tempDir.resolve("nominal").resolve("state"));
        MatchRuntimeState reloaded = reloadedStateStore.findById("match-p1b-1").orElseThrow();
        assertEquals(1, reloaded.version());
        assertEquals("IN_PROGRESS", reloaded.status());
    }

    @Test
    void p1b_structuralRejectionDoesNotPersistMutation() {
        PersistenceContext context = createContext(
                tempDir.resolve("structural"),
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN))
        );

        context.stateStore.save(inProgressState("match-p1b-2", 2));

        SubmitActionApplicationResponse response = context.service.submit(
                new SubmitActionApplicationRequest("match-p1b-2", "p1", 1, "k-p1b-2", "payload")
        );

        assertFalse(response.accepted());
        assertEquals(RejectionOrigin.ENGINE, response.rejection().origin());
        assertEquals("VERSION_CONFLICT", response.rejection().code());

        FileMatchStateStore reloadedStateStore = new FileMatchStateStore(tempDir.resolve("structural").resolve("state"));
        MatchRuntimeState reloaded = reloadedStateStore.findById("match-p1b-2").orElseThrow();
        assertEquals(2, reloaded.version());
        assertEquals(List.of(), context.eventSink.allEvents());
    }

    @Test
    void p1b_idempotentReplayAfterStoreReconstruction() {
        AtomicInteger invocations = new AtomicInteger(0);
        RuleSet countingRuleSet = input -> {
            invocations.incrementAndGet();
            return ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));
        };

        Path root = tempDir.resolve("replay");
        PersistenceContext context1 = createContext(root, countingRuleSet);
        context1.stateStore.save(inProgressState("match-p1b-3", 0));

        SubmitActionApplicationRequest request =
                new SubmitActionApplicationRequest("match-p1b-3", "p1", 0, "k-p1b-3", "payload");

        SubmitActionApplicationResponse first = context1.service.submit(request);

        PersistenceContext context2 = createContext(root, countingRuleSet);
        SubmitActionApplicationResponse second = context2.service.submit(request);

        assertEquals(1, invocations.get());
        assertTrue(first.accepted());
        assertTrue(second.accepted());
        assertEquals(first.state(), second.state());
        assertEquals(first.emittedEvents(), second.emittedEvents());
    }

    @Test
    void p1b_idempotencyConflictIsDetectedAfterReconstruction() {
        RuleSet ruleSet = input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN));

        Path root = tempDir.resolve("idem-conflict");
        PersistenceContext context1 = createContext(root, ruleSet);
        context1.stateStore.save(inProgressState("match-p1b-4", 0));

        context1.service.submit(new SubmitActionApplicationRequest("match-p1b-4", "p1", 0, "k-p1b-4", "payload-a"));

        PersistenceContext context2 = createContext(root, ruleSet);
        SubmitActionApplicationResponse conflict = context2.service.submit(
                new SubmitActionApplicationRequest("match-p1b-4", "p1", 0, "k-p1b-4", "payload-b")
        );

        assertFalse(conflict.accepted());
        assertEquals(RejectionOrigin.ENGINE, conflict.rejection().origin());
        assertEquals("IDEMPOTENCY_CONFLICT", conflict.rejection().code());
    }

    @Test
    void p1b_workflowEventsPersistedOnlyWhenExpected() {
        PersistenceContext acceptContext = createContext(
                tempDir.resolve("events-accept"),
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN))
        );
        acceptContext.stateStore.save(inProgressState("match-p1b-5a", 0));

        SubmitActionApplicationResponse accepted = acceptContext.service.submit(
                new SubmitActionApplicationRequest("match-p1b-5a", "p1", 0, "k-p1b-5a", "payload")
        );

        assertTrue(accepted.accepted());
        assertEquals(List.of("ActionSubmitted", "ActionAccepted", "ActionResolved"), acceptContext.eventSink.allEvents());

        Rejection technicalOnlyRejection = new Rejection(
                RejectionOrigin.RULESET,
                "RULESET_TECH_ONLY",
                null,
                null,
                Set.of(LogTarget.TECHNICAL_LOG)
        );

        PersistenceContext rejectContext = createContext(
                tempDir.resolve("events-reject"),
                input -> new ActionResolution(
                        Set.of(EngineDirective.REJECT_ACTION),
                        null,
                        null,
                        null,
                        technicalOnlyRejection,
                        List.of()
                )
        );
        rejectContext.stateStore.save(inProgressState("match-p1b-5b", 0));

        SubmitActionApplicationResponse rejected = rejectContext.service.submit(
                new SubmitActionApplicationRequest("match-p1b-5b", "p1", 0, "k-p1b-5b", "payload")
        );

        assertFalse(rejected.accepted());
        assertEquals(List.of("ActionSubmitted"), rejectContext.eventSink.allEvents());
    }

    private static MatchRuntimeState inProgressState(String matchId, long version) {
        return new MatchRuntimeState(
                matchId,
                1,
                0,
                true,
                List.of("p1", "p2"),
                version,
                "IN_PROGRESS",
                null,
                null
        );
    }

    private static PersistenceContext createContext(Path root, RuleSet ruleSet) {
        FileMatchStateStore stateStore = new FileMatchStateStore(root.resolve("state"));
        FileIdempotencyStore idempotencyStore = new FileIdempotencyStore(root.resolve("idempotency"));
        FileWorkflowEventSink eventSink = new FileWorkflowEventSink(root.resolve("workflow-events.log"));

        SubmitActionOrchestrator orchestrator = new SubmitActionOrchestrator(
                stateStore,
                eventSink,
                ruleSet,
                new ActionResolutionContractValidator(),
                idempotencyStore
        );

        return new PersistenceContext(stateStore, eventSink, new SubmitActionApplicationService(orchestrator));
    }

    private record PersistenceContext(
            FileMatchStateStore stateStore,
            FileWorkflowEventSink eventSink,
            SubmitActionApplicationService service
    ) {
    }
}

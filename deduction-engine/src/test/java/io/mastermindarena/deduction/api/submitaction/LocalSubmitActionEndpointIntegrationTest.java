package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSubmitActionEndpointIntegrationTest {

    @Test
    void p3b_submitActionNominalReturns200() {
        LocalSubmitActionEndpoint endpoint = buildEndpoint(
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN)),
                true,
                0L
        );

        SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(
                new SubmitActionHttpRequest("match-api-1", "p1", 0L, "k-api-1", "payload")
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().accepted());
        assertEquals("match-api-1", response.body().matchId());
    }

    @Test
    void p3b_submitActionRulesetRejectionReturns422() {
        Rejection rulesetRejection = new Rejection(
                RejectionOrigin.RULESET,
                "INVALID_GUESS_LENGTH",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        LocalSubmitActionEndpoint endpoint = buildEndpoint(
                input -> new ActionResolution(
                        Set.of(EngineDirective.REJECT_ACTION),
                        null,
                        null,
                        null,
                        rulesetRejection,
                        List.of()
                ),
                true,
                0L
        );

        SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(
                new SubmitActionHttpRequest("match-api-2", "p1", 0L, "k-api-2", "payload")
        );

        assertEquals(422, response.statusCode());
        assertFalse(response.body().accepted());
        assertEquals("RULESET", response.body().rejectionOrigin());
        assertEquals("INVALID_GUESS_LENGTH", response.body().rejectionCode());
    }

    @Test
    void p3b_submitActionMissingMatchReturns404() {
        LocalSubmitActionEndpoint endpoint = buildEndpoint(
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN)),
                false,
                0L
        );

        SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(
                new SubmitActionHttpRequest("missing", "p1", 0L, "k-api-3", "payload")
        );

        assertEquals(404, response.statusCode());
        assertFalse(response.body().accepted());
        assertEquals("ENGINE", response.body().rejectionOrigin());
        assertEquals("MATCH_NOT_FOUND", response.body().rejectionCode());
    }

    @Test
    void p3b_submitActionVersionConflictReturns409() {
        LocalSubmitActionEndpoint endpoint = buildEndpoint(
                input -> ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN)),
                true,
                3L
        );

        SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(
                new SubmitActionHttpRequest("match-api-4", "p1", 2L, "k-api-4", "payload")
        );

        assertEquals(409, response.statusCode());
        assertFalse(response.body().accepted());
        assertEquals("ENGINE", response.body().rejectionOrigin());
        assertEquals("VERSION_CONFLICT", response.body().rejectionCode());
    }

    private static LocalSubmitActionEndpoint buildEndpoint(RuleSet ruleSet, boolean seedMatch, long version) {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        InMemoryEventSink eventSink = new InMemoryEventSink();

        if (seedMatch) {
            stateStore.save(new MatchRuntimeState(
                    "match-api-1",
                    1,
                    0,
                    true,
                    List.of("p1", "p2"),
                    version,
                    "IN_PROGRESS",
                    null,
                    null
            ));

            stateStore.save(new MatchRuntimeState(
                    "match-api-2",
                    1,
                    0,
                    true,
                    List.of("p1", "p2"),
                    version,
                    "IN_PROGRESS",
                    null,
                    null
            ));

            stateStore.save(new MatchRuntimeState(
                    "match-api-4",
                    1,
                    0,
                    true,
                    List.of("p1", "p2"),
                    version,
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

        return new LocalSubmitActionEndpoint(new SubmitActionApplicationService(orchestrator));
    }
}

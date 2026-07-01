package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationRejection;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationRequest;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse;
import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitActionHttpMapperContractTest {

    @Test
    void p3a_mapsHttpRequestToApplicationRequest() {
        SubmitActionHttpRequest http = new SubmitActionHttpRequest("match-1", "p1", 3L, "idem-1", "payload");

        SubmitActionApplicationRequest app = SubmitActionHttpMapper.toApplicationRequest(http);

        assertEquals("match-1", app.matchId());
        assertEquals("p1", app.actorId());
        assertEquals(3L, app.expectedVersion());
        assertEquals("idem-1", app.idempotencyKey());
        assertEquals("payload", app.actionPayload());
    }

    @Test
    void p3a_mapsNominalSuccessTo200() {
        MatchRuntimeState state = inProgressState("match-2", 4L);
        SubmitActionApplicationResponse appResponse = new SubmitActionApplicationResponse(
                true,
                state,
                ActionResolution.of(Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.CONTINUE_TURN)),
                null,
                List.of("ActionSubmitted", "ActionAccepted", "ActionResolved")
        );

        SubmitActionHttpResponse.Envelope http = SubmitActionHttpMapper.fromApplicationResponse(appResponse);

        assertEquals(200, http.statusCode());
        assertTrue(http.body().accepted());
        assertNull(http.body().rejectionOrigin());
        assertNull(http.body().rejectionCode());
        assertEquals("match-2", http.body().matchId());
    }

    @Test
    void p3a_mapsRulesetRejectionTo422WithoutCodeTransformation() {
        MatchRuntimeState state = inProgressState("match-3", 1L);
        Rejection rejection = new Rejection(
                RejectionOrigin.RULESET,
                "INVALID_GUESS_LENGTH",
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );

        SubmitActionApplicationResponse appResponse = new SubmitActionApplicationResponse(
                false,
                state,
                new ActionResolution(Set.of(EngineDirective.REJECT_ACTION), null, null, null, rejection, List.of()),
                new SubmitActionApplicationRejection(RejectionOrigin.RULESET, "INVALID_GUESS_LENGTH", rejection),
                List.of("ActionSubmitted", "ActionRejected")
        );

        SubmitActionHttpResponse.Envelope http = SubmitActionHttpMapper.fromApplicationResponse(appResponse);

        assertEquals(422, http.statusCode());
        assertFalse(http.body().accepted());
        assertEquals("RULESET", http.body().rejectionOrigin());
        assertEquals("INVALID_GUESS_LENGTH", http.body().rejectionCode());
    }

    @Test
    void p3a_mapsEngineMatchNotFoundTo404() {
        SubmitActionApplicationResponse appResponse = SubmitActionApplicationResponse.fromEngineError("MATCH_NOT_FOUND");

        SubmitActionHttpResponse.Envelope http = SubmitActionHttpMapper.fromApplicationResponse(appResponse);

        assertEquals(404, http.statusCode());
        assertFalse(http.body().accepted());
        assertEquals("ENGINE", http.body().rejectionOrigin());
        assertEquals("MATCH_NOT_FOUND", http.body().rejectionCode());
    }

    @Test
    void p3a_mapsEngineVersionConflictTo409() {
        SubmitActionApplicationResponse appResponse = SubmitActionApplicationResponse.fromEngineError("VERSION_CONFLICT");

        SubmitActionHttpResponse.Envelope http = SubmitActionHttpMapper.fromApplicationResponse(appResponse);

        assertEquals(409, http.statusCode());
        assertFalse(http.body().accepted());
        assertEquals("ENGINE", http.body().rejectionOrigin());
        assertEquals("VERSION_CONFLICT", http.body().rejectionCode());
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
}

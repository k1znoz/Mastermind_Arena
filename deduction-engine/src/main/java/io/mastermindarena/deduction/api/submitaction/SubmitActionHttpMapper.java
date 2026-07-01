package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationRequest;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;

public final class SubmitActionHttpMapper {
    private SubmitActionHttpMapper() {
    }

    public static SubmitActionApplicationRequest toApplicationRequest(SubmitActionHttpRequest request) {
        return new SubmitActionApplicationRequest(
                request.matchId(),
                request.actorId(),
                request.expectedVersion(),
                request.idempotencyKey(),
                request.actionPayload()
        );
    }

    public static SubmitActionHttpResponse.Envelope fromApplicationResponse(SubmitActionApplicationResponse response) {
        MatchRuntimeState state = response.state();

        if (response.accepted()) {
            return new SubmitActionHttpResponse.Envelope(
                    200,
                    new SubmitActionHttpResponse(
                            true,
                            null,
                            null,
                            state == null ? null : state.matchId(),
                            state == null ? null : state.version(),
                            state == null ? null : state.status(),
                            response.emittedEvents()
                    )
            );
        }

        RejectionOrigin origin = response.rejection().origin();
        int statusCode = origin == RejectionOrigin.RULESET
                ? 422
                : mapEngineStatus(response.rejection().code());

        return new SubmitActionHttpResponse.Envelope(
                statusCode,
                new SubmitActionHttpResponse(
                        false,
                        origin.name(),
                        response.rejection().code(),
                        state == null ? null : state.matchId(),
                        state == null ? null : state.version(),
                        state == null ? null : state.status(),
                        response.emittedEvents()
                )
        );
    }

    private static int mapEngineStatus(String code) {
        return switch (code) {
            case "MATCH_NOT_FOUND" -> 404;
            case "ACTOR_NOT_AUTHORIZED" -> 403;
            case "MATCH_NOT_IN_PROGRESS", "TURN_NOT_ACTIVE", "VERSION_CONFLICT", "IDEMPOTENCY_CONFLICT", "MATCH_ALREADY_TERMINAL" -> 409;
            default -> 400;
        };
    }
}

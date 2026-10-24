package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationRequest;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.workflow.ActionReceipt;

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
        ActionReceipt receipt = response.receipt();

        if (response.accepted()) {
            return new SubmitActionHttpResponse.Envelope(
                    200,
                    new SubmitActionHttpResponse(
                            true,
                            null,
                            null,
                            receipt == null ? null : receipt.matchId(),
                            receipt == null ? null : receipt.version(),
                            receipt == null ? null : receipt.status(),
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
                        receipt == null ? null : receipt.matchId(),
                        receipt == null ? null : receipt.version(),
                        receipt == null ? null : receipt.status(),
                        response.emittedEvents()
                )
        );
    }

    private static int mapEngineStatus(String code) {
        return switch (code) {
            case "MATCH_NOT_FOUND" -> 404;
            case "ACTOR_NOT_AUTHORIZED" -> 403;
            case "MATCH_NOT_IN_PROGRESS", "TURN_NOT_ACTIVE", "VERSION_CONFLICT", "IDEMPOTENCY_CONFLICT", "MATCH_ALREADY_TERMINAL", "GUESS_ALREADY_SUBMITTED", "FEEDBACK_ALREADY_SUBMITTED" -> 409;
            default -> 400;
        };
    }
}

package io.mastermindarena.deduction.application.submitaction;

import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.engine.workflow.SubmitActionResult;
import io.mastermindarena.deduction.engine.workflow.ActionRejectedException;

import java.util.Map;
import java.util.Objects;

public final class SubmitActionApplicationService {
    private final SubmitActionOrchestrator orchestrator;

    public SubmitActionApplicationService(SubmitActionOrchestrator orchestrator) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator is required");
    }

    public SubmitActionApplicationResponse submit(SubmitActionApplicationRequest request) {
        // actionPayload est un objet opaque (JSON) ; le plateau attend actionType/payload/feedback distincts
        Map<?, ?> payloadMap = request.actionPayload() instanceof Map<?, ?> map ? map : Map.of();
        Object payloadValue = payloadMap.get("payload");
        Object feedbackValue = payloadMap.get("feedback");
        SubmitActionCommand command = new SubmitActionCommand(
                request.matchId(),
                request.actorId(),
                request.expectedVersion(),
                request.idempotencyKey(),
                String.valueOf(payloadMap.get("actionType")),
                payloadValue == null ? null : String.valueOf(payloadValue),
                feedbackValue == null ? null : String.valueOf(feedbackValue)
        );

        try {
            SubmitActionResult result = orchestrator.submit(command);
            return SubmitActionApplicationResponse.fromEngineResult(result);
        } catch (ActionRejectedException ex) {
            return SubmitActionApplicationResponse.fromEngineError(ex.getMessage());
        } catch (RuntimeException failure) {
            // Request validation above is distinct from faults while reading or applying persisted state.
            throw new IllegalStateException("SUBMIT_ACTION_FAILED", failure);
        }
    }
}

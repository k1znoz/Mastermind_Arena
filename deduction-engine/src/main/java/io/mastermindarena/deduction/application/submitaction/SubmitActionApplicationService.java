package io.mastermindarena.deduction.application.submitaction;

import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.engine.workflow.SubmitActionResult;

import java.util.Objects;

public final class SubmitActionApplicationService {
    private final SubmitActionOrchestrator orchestrator;

    public SubmitActionApplicationService(SubmitActionOrchestrator orchestrator) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator is required");
    }

    public SubmitActionApplicationResponse submit(SubmitActionApplicationRequest request) {
        SubmitActionCommand command = new SubmitActionCommand(
                request.matchId(),
                request.actorId(),
                request.expectedVersion(),
                request.idempotencyKey(),
                request.actionPayload()
        );

        try {
            SubmitActionResult result = orchestrator.submit(command);
            return SubmitActionApplicationResponse.fromEngineResult(result);
        } catch (IllegalStateException ex) {
            return SubmitActionApplicationResponse.fromEngineError(ex.getMessage());
        }
    }
}

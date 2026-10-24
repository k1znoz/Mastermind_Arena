package io.mastermindarena.deduction.application.submitaction;

import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.workflow.ActionReceipt;
import io.mastermindarena.deduction.engine.workflow.SubmitActionResult;

import java.util.List;
import java.util.Objects;

public record SubmitActionApplicationResponse(
        boolean accepted,
        ActionReceipt receipt,
        SubmitActionApplicationRejection rejection,
        List<String> emittedEvents
) {
    public SubmitActionApplicationResponse {
        emittedEvents = emittedEvents == null ? List.of() : List.copyOf(emittedEvents);
        if (accepted && rejection != null) {
            throw new IllegalArgumentException("accepted response must not contain rejection");
        }
        if (!accepted && rejection == null) {
            throw new IllegalArgumentException("rejected response must contain rejection");
        }
    }

    public static SubmitActionApplicationResponse fromEngineResult(SubmitActionResult result) {
        Objects.requireNonNull(result, "result is required");
        Rejection rulesetRejection = result.rejection();
        if (rulesetRejection != null && rulesetRejection.origin() == RejectionOrigin.RULESET) {
            return new SubmitActionApplicationResponse(
                    false,
                    result.receipt(),
                    new SubmitActionApplicationRejection(
                            RejectionOrigin.RULESET,
                            rulesetRejection.code(),
                            rulesetRejection
                    ),
                    result.emittedEvents()
            );
        }

        return new SubmitActionApplicationResponse(
                true,
                result.receipt(),
                null,
                result.emittedEvents()
        );
    }

    public static SubmitActionApplicationResponse fromEngineError(String code) {
        return new SubmitActionApplicationResponse(
                false,
                null,
                new SubmitActionApplicationRejection(RejectionOrigin.ENGINE, code, null),
                List.of()
        );
    }
}

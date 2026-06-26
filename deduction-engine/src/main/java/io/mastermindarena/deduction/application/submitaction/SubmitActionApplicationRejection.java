package io.mastermindarena.deduction.application.submitaction;

import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;

import java.util.Objects;

public record SubmitActionApplicationRejection(
        RejectionOrigin origin,
        String code,
        Rejection rulesetRejection
) {
    public SubmitActionApplicationRejection {
        Objects.requireNonNull(origin, "origin is required");
        Objects.requireNonNull(code, "code is required");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }

        if (origin == RejectionOrigin.RULESET && rulesetRejection == null) {
            throw new IllegalArgumentException("rulesetRejection is required for RULESET origin");
        }
        if (origin == RejectionOrigin.ENGINE && rulesetRejection != null) {
            throw new IllegalArgumentException("rulesetRejection must be null for ENGINE origin");
        }
    }
}

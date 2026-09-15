package io.mastermindarena.deduction.engine.contract;

import java.util.Objects;

public record CancellationReason(String code) {
    public CancellationReason {
        Objects.requireNonNull(code, "code is required");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
    }
}

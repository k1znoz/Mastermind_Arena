package io.mastermindarena.deduction.engine.contract;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record Rejection(
        RejectionOrigin origin,
        String code,
        String messageKey,
        Map<String, Object> details,
        Set<LogTarget> targetLogs
) {
    public Rejection {
        Objects.requireNonNull(origin, "origin is required");
        Objects.requireNonNull(code, "code is required");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }

        details = details == null ? Map.of() : Map.copyOf(details);
        targetLogs = targetLogs == null ? Set.of() : Set.copyOf(targetLogs);
        if (targetLogs.isEmpty()) {
            throw new IllegalArgumentException("targetLogs must not be empty");
        }
    }
}

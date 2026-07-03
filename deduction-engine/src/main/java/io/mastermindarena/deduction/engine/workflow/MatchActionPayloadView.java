package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record MatchActionPayloadView(
        String actionType,
        List<String> symbols,
        String payloadSummary
) {
    public MatchActionPayloadView {
        Objects.requireNonNull(actionType, "actionType is required");
        Objects.requireNonNull(symbols, "symbols is required");
        Objects.requireNonNull(payloadSummary, "payloadSummary is required");
        symbols = List.copyOf(symbols);
    }

    public static MatchActionPayloadView from(Object actionPayload) {
        if (actionPayload instanceof Map<?, ?> payloadMap) {
            String actionType = stringValue(payloadMap.get("type"), "UNKNOWN_ACTION");
            List<String> symbols = extractSymbols(payloadMap);
            String payloadSummary = payloadMap.entrySet().stream()
                    .map(entry -> String.valueOf(entry.getKey()) + "=" + String.valueOf(entry.getValue()))
                    .collect(Collectors.joining(", "));
            return new MatchActionPayloadView(actionType, symbols, payloadSummary);
        }

        return new MatchActionPayloadView("OPAQUE_ACTION", List.of(), String.valueOf(actionPayload));
    }

    private static List<String> extractSymbols(Map<?, ?> payloadMap) {
        Object symbols = payloadMap.get("secretCode");
        if (!(symbols instanceof List<?>)) {
            symbols = payloadMap.get("guess");
        }
        if (!(symbols instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    private static String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}

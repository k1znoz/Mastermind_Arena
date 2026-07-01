package io.mastermindarena.deduction.api.submitaction;

import java.util.List;

public record SubmitActionHttpResponse(
        boolean accepted,
        String rejectionOrigin,
        String rejectionCode,
        String matchId,
        Long version,
        String status,
        List<String> emittedEvents
) {
    public SubmitActionHttpResponse {
        emittedEvents = emittedEvents == null ? List.of() : List.copyOf(emittedEvents);
    }

    public record Envelope(int statusCode, SubmitActionHttpResponse body) {
    }
}

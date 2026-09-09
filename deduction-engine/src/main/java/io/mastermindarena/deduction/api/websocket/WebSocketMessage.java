package io.mastermindarena.deduction.api.websocket;

import java.util.Objects;

public record WebSocketMessage(
        WebSocketMessageType type,
        String matchId,
        long version,
        Object payload,
        long timestamp
) {
    public WebSocketMessage {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(matchId, "matchId is required");
    }
}

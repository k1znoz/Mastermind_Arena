package io.mastermindarena.deduction.api.websocket.runtime;

import io.mastermindarena.deduction.api.websocket.WebSocketMessage;
import io.mastermindarena.deduction.api.websocket.WebSocketMessageMapper;
import io.mastermindarena.deduction.engine.workflow.EventSink;
import io.mastermindarena.deduction.engine.workflow.GameEvent;

import java.util.Objects;

/** S'abonne aux GameEvent et prepare leur diffusion aux sessions enregistrees. */
public final class WebSocketBroadcastService {
    private final WebSocketSessionRegistry sessionRegistry;

    public WebSocketBroadcastService(EventSink eventSink, WebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry is required");
        Objects.requireNonNull(eventSink, "eventSink is required").subscribe(this::onGameEvent);
    }

    private void onGameEvent(GameEvent event) {
        // EventSink.subscribe ne porte pas matchId/version/payload reels : placeholders en attendant leur propagation
        WebSocketMessage message = WebSocketMessageMapper.toMessage(event, "unknown", 0L, event.name(), System.currentTimeMillis());
        broadcast(message);
    }

    private void broadcast(WebSocketMessage message) {
        String serialized = message.toString();
        for (WebSocketSessionRegistry.WebSocketSession session : sessionRegistry.activeSessions()) {
            session.send(serialized);
        }
    }
}

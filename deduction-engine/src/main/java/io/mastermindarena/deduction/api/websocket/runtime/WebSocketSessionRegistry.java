package io.mastermindarena.deduction.api.websocket.runtime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registre thread-safe des sessions WebSocket connectees. */
public final class WebSocketSessionRegistry {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public void register(WebSocketSession session) {
        sessions.add(Objects.requireNonNull(session, "session is required"));
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public Collection<WebSocketSession> activeSessions() {
        return List.copyOf(sessions);
    }

    /** Abstraction minimale d'une session, en attendant une implementation reseau concrete. */
    public interface WebSocketSession {
        void send(String payload);
    }
}

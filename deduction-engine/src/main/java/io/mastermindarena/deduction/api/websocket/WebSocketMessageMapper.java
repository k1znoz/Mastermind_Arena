package io.mastermindarena.deduction.api.websocket;

import io.mastermindarena.deduction.engine.workflow.GameEvent;

public final class WebSocketMessageMapper {

    private WebSocketMessageMapper() {
    }

    public static WebSocketMessage toMessage(GameEvent event, String matchId, long version, Object payload, long timestamp) {
        return new WebSocketMessage(toType(event), matchId, version, payload, timestamp);
    }

    private static WebSocketMessageType toType(GameEvent event) {
        return switch (event) {
            case PLAYER_JOINED -> WebSocketMessageType.PLAYER_JOINED;
            case GAME_STARTED -> WebSocketMessageType.GAME_STARTED;
            case GUESS_PLAYED -> WebSocketMessageType.GUESS_PLAYED;
            case FEEDBACK_SENT -> WebSocketMessageType.FEEDBACK_SENT;
            case TURN_CHANGED -> WebSocketMessageType.TURN_CHANGED;
            case GAME_FINISHED -> WebSocketMessageType.GAME_FINISHED;
        };
    }
}

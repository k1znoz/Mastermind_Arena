package io.mastermindarena.deduction.api.websocket;

/** Miroir reseau des six evenements plateau publies par le moteur. */
public enum WebSocketMessageType {
    PLAYER_JOINED,
    GAME_STARTED,
    GUESS_PLAYED,
    FEEDBACK_SENT,
    TURN_CHANGED,
    GAME_FINISHED
}

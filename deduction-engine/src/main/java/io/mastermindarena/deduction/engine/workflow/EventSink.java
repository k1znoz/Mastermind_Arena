package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.function.Consumer;

public interface EventSink {
    void publish(GameEvent event);

    List<GameEvent> allEvents();

    /** Point d'extension pour un futur transport temps reel (ex. WebSocket) sans y dependre ici. */
    default void subscribe(Consumer<GameEvent> listener) {
        // no-op par defaut : les implementations existantes ne sont pas concernees
    }
}

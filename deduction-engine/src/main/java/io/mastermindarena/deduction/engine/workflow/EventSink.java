package io.mastermindarena.deduction.engine.workflow;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public interface EventSink {
    void publish(GameEvent event);

    List<GameEvent> allEvents();

    /** Evenement enrichi du contexte match, pour un futur transport temps reel. */
    record PublishedEvent(GameEvent event, String matchId, long version, Object payload, long timestamp) {
        public PublishedEvent {
            Objects.requireNonNull(event, "event is required");
            Objects.requireNonNull(matchId, "matchId is required");
        }
    }

    /** Publie un evenement enrichi ; retombe par defaut sur publish(GameEvent) pour les sinks existants. */
    default void publish(PublishedEvent event) {
        publish(Objects.requireNonNull(event, "event is required").event());
    }

    /** Point d'extension pour un futur transport temps reel (ex. WebSocket) sans y dependre ici. */
    default void subscribe(Consumer<GameEvent> listener) {
        // no-op par defaut : les implementations existantes ne sont pas concernees
    }

    /** Variante enrichie de subscribe pour les sinks capables de fournir le contexte complet. */
    default void subscribeEnriched(Consumer<PublishedEvent> listener) {
        // no-op par defaut : les implementations existantes ne sont pas concernees
    }
}

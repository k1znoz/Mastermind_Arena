package io.mastermindarena.deduction.engine.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryEventSink implements EventSink {
    private final List<PublishedEvent> enrichedEvents = new ArrayList<>();
    private final List<Consumer<GameEvent>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PublishedEvent>> enrichedListeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(GameEvent event) {
        // contexte minimal pour les appelants qui ne fournissent pas encore matchId/version/payload
        publish(new PublishedEvent(event, "unknown", 0L, null, System.currentTimeMillis()));
    }

    @Override
    public void publish(PublishedEvent event) {
        Objects.requireNonNull(event, "event is required");
        enrichedEvents.add(event);
        // notifie dans l'ordre d'emission pour un futur transport temps reel
        for (Consumer<GameEvent> listener : listeners) {
            listener.accept(event.event());
        }
        for (Consumer<PublishedEvent> listener : enrichedListeners) {
            listener.accept(event);
        }
    }

    @Override
    public List<GameEvent> allEvents() {
        return enrichedEvents.stream().map(PublishedEvent::event).toList();
    }

    public List<PublishedEvent> allEnrichedEvents() {
        return List.copyOf(enrichedEvents);
    }

    @Override
    public void subscribe(Consumer<GameEvent> listener) {
        listeners.add(listener);
    }

    @Override
    public void subscribeEnriched(Consumer<PublishedEvent> listener) {
        enrichedListeners.add(listener);
    }
}

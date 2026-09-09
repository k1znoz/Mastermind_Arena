package io.mastermindarena.deduction.engine.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryEventSink implements EventSink {
    private final List<GameEvent> events = new ArrayList<>();
    private final List<Consumer<GameEvent>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(GameEvent event) {
        events.add(event);
        // notifie dans l'ordre d'emission pour un futur transport temps reel
        for (Consumer<GameEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    @Override
    public List<GameEvent> allEvents() {
        return Collections.unmodifiableList(events);
    }

    @Override
    public void subscribe(Consumer<GameEvent> listener) {
        listeners.add(listener);
    }
}

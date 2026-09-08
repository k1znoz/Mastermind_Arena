package io.mastermindarena.deduction.engine.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryEventSink implements EventSink {
    private final List<GameEvent> events = new ArrayList<>();

    @Override
    public void publish(GameEvent event) {
        events.add(event);
    }

    @Override
    public List<GameEvent> allEvents() {
        return Collections.unmodifiableList(events);
    }
}

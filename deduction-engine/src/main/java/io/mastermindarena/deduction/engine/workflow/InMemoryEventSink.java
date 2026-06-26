package io.mastermindarena.deduction.engine.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryEventSink implements EventSink {
    private final List<String> events = new ArrayList<>();

    @Override
    public void publish(String event) {
        events.add(event);
    }

    @Override
    public List<String> allEvents() {
        return Collections.unmodifiableList(events);
    }
}

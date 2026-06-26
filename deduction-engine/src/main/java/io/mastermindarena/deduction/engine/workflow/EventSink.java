package io.mastermindarena.deduction.engine.workflow;

import java.util.List;

public interface EventSink {
    void publish(String event);

    List<String> allEvents();
}

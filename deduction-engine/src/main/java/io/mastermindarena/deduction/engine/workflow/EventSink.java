package io.mastermindarena.deduction.engine.workflow;

import java.util.List;

public interface EventSink {
    void publish(GameEvent event);

    List<GameEvent> allEvents();
}

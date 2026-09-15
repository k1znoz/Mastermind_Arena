package io.mastermindarena.deduction.infrastructure.preferences;

import io.mastermindarena.deduction.engine.workflow.EventSink;
import io.mastermindarena.deduction.engine.workflow.GameEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class PreferencesWorkflowEventSink implements EventSink {
    private final Preferences node;

    public PreferencesWorkflowEventSink(Preferences rootNode) {
        this.node = rootNode.node("workflow-events");
    }

    @Override
    public void publish(GameEvent event) {
        int nextIndex = node.getInt("count", 0);
        node.put(Integer.toString(nextIndex), event.name());
        node.putInt("count", nextIndex + 1);
        flush(node);
    }

    @Override
    public List<GameEvent> allEvents() {
        try {
            List<GameEvent> result = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            for (String key : node.keys()) {
                if (!"count".equals(key)) {
                    keys.add(key);
                }
            }
            keys.sort(Comparator.comparingInt(Integer::parseInt));
            for (String key : keys) {
                result.add(GameEvent.valueOf(node.get(key, "")));
            }
            return List.copyOf(result);
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Unable to read workflow event preferences", e);
        }
    }

    private static void flush(Preferences preferences) {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Unable to flush workflow event preferences", e);
        }
    }
}

package io.mastermindarena.deduction.engine.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdempotencyStore implements IdempotencyStore {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<Entry> find(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void save(String key, Entry entry) {
        entries.put(key, entry);
    }
}

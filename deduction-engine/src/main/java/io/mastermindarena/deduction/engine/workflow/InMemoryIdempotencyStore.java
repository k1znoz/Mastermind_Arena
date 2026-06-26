package io.mastermindarena.deduction.engine.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryIdempotencyStore {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    Optional<Entry> find(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    void save(String key, Entry entry) {
        entries.put(key, entry);
    }

    record Entry(String fingerprint, SubmitActionResult result) {
    }
}

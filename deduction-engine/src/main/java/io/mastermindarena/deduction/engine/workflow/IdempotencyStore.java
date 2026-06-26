package io.mastermindarena.deduction.engine.workflow;

import java.util.Optional;

public interface IdempotencyStore {
    Optional<Entry> find(String key);

    void save(String key, Entry entry);

    record Entry(String fingerprint, SubmitActionResult result) {
    }
}
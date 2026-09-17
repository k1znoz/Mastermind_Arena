package io.mastermindarena.deduction.engine.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdempotencyPersistenceFailureTest {
    @Test
    void keepsAcceptedActionWhenIdempotencyJournalIsTemporarilyUnavailable() {
        InMemoryMatchStateStore stateStore = new InMemoryMatchStateStore();
        stateStore.save(new MatchRuntimeState("match", List.of("p1", "p2"), 0L));
        IdempotencyStore failingStore = new IdempotencyStore() {
            @Override
            public Optional<Entry> find(String key) {
                return Optional.empty();
            }

            @Override
            public void save(String key, Entry entry) {
                throw new IllegalStateException("Unable to persist idempotency entry");
            }
        };
        SubmitActionOrchestrator orchestrator =
                new SubmitActionOrchestrator(stateStore, new InMemoryEventSink(), failingStore);

        SubmitActionResult result = orchestrator.submit(new SubmitActionCommand(
                "match", "p1", 0L, "request-1", SubmitActionCommand.READY_SECRET,
                "[\"A\",\"B\",\"C\",\"D\"]", null));

        assertEquals(1L, result.state().version());
        assertEquals(1L, stateStore.findById("match").orElseThrow().version());

        SubmitActionResult replay = orchestrator.submit(new SubmitActionCommand(
                "match", "p1", 0L, "request-2", SubmitActionCommand.READY_SECRET,
                "[\"A\",\"B\",\"C\",\"D\"]", null));

        assertEquals(1L, replay.state().version());
        assertEquals(1L, stateStore.findById("match").orElseThrow().version());
    }
}

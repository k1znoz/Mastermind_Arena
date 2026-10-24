package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.api.submitaction.SubmitActionHttpMapper;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse;
import io.mastermindarena.deduction.infrastructure.file.FileIdempotencyStore;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompactReceiptReplayTest {
    @TempDir Path directory;

    @Test void memoryAndReopenedFileStoresReplayTheReceiptWithTheCurrentState() {
        for (IdempotencyStore journal : List.of(new InMemoryIdempotencyStore(), new FileIdempotencyStore(directory))) {
            var states = new InMemoryMatchStateStore();
            states.save(new MatchRuntimeState("compact", List.of("p1", "p2")));
            var events = new InMemoryEventSink();
            var flow = new SubmitActionOrchestrator(states, events, journal);
            var first = new SubmitActionCommand("compact", "p1", 0, "first", "READY_SECRET", "[\"A\",\"B\",\"E\",\"F\"]", null);
            var original = flow.submit(first);
            flow.submit(new SubmitActionCommand("compact", "p2", 1, "second", "READY_SECRET", "[\"A\",\"B\",\"E\",\"F\"]", null));
            var reopened = journal instanceof FileIdempotencyStore ? new FileIdempotencyStore(directory) : journal;
            var replay = new SubmitActionOrchestrator(states, events, reopened).submit(first);
            assertEquals(original.receipt(), replay.receipt());
            assertEquals(1, replay.receipt().version());
            assertEquals("PREPARATION", replay.receipt().status());
            assertEquals(2, replay.state().version()); // A real current state, never a fabricated historical snapshot.
            assertEquals("WAITING_GUESS", replay.state().status());
            assertEquals(SubmitActionHttpMapper.fromApplicationResponse(SubmitActionApplicationResponse.fromEngineResult(original)),
                    SubmitActionHttpMapper.fromApplicationResponse(SubmitActionApplicationResponse.fromEngineResult(replay)));
            assertEquals(2, events.allEvents().size());
            assertEquals(original.receipt(), FilePersistenceCodec.deserializeReceipt(FilePersistenceCodec.serializeReceipt(original.receipt())));
        }
    }
}

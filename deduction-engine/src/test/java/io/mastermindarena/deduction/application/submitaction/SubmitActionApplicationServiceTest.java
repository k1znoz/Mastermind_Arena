package io.mastermindarena.deduction.application.submitaction;

import io.mastermindarena.deduction.engine.workflow.InMemoryEventSink;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.MatchStateStore;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class SubmitActionApplicationServiceTest {
    @Test void unexpectedStoreFailuresAreNotPublishedAsPlayerRejections() {
        for (RuntimeException failure : List.of(new IllegalStateException("private persistence diagnostic"),
                new IllegalArgumentException("invalid stored document"))) {
            MatchStateStore unavailable = new MatchStateStore() {
                @Override public Optional<MatchRuntimeState> findById(String matchId) { throw failure; }
                @Override public void save(MatchRuntimeState state) { fail("Failure must happen before writes"); }
            };
            var service = new SubmitActionApplicationService(new SubmitActionOrchestrator(unavailable, new InMemoryEventSink()));
            assertThrows(IllegalStateException.class, () -> service.submit(new SubmitActionApplicationRequest(
                    "board", "p1", 0, "operation", Map.of("actionType", "READY_SECRET", "payload", "[\"A\",\"B\",\"E\",\"F\"]"))));
        }
    }
}

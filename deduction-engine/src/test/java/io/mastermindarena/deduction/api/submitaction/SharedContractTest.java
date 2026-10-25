package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SharedContractTest {
    @Test void correctionAndConflictMatchTheFrontendFixtures() throws Exception {
        var json = new ObjectMapper();
        var action = new MatchStateHttpResponse.TurnEntry(2, "p1", "CORRECT_FEEDBACK",
                "{\"feedbackVersion\":5}", "{\"bienPlaces\":2,\"malPlaces\":1}", 1000L, 8L, null);
        var rejection = SubmitActionHttpMapper.fromApplicationResponse(
                io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse.fromEngineError("VERSION_CONFLICT"));
        assertEquals(409, rejection.statusCode());
        for (var fixture : java.util.Map.of("turn-correction.json", action, "action-version-conflict.json", rejection.body()).entrySet()) {
            try (var input = getClass().getResourceAsStream("/contracts/" + fixture.getKey())) {
                assertNotNull(input);
                assertEquals(json.readTree(input), json.readTree(json.writeValueAsString(fixture.getValue())));
            }
        }
    }
    @Test void admissionAndSubmissionMatchTheFixturesConsumedByTheFrontend() throws Exception {
        var json = new ObjectMapper();
        var room = new io.mastermindarena.deduction.infrastructure.jdbc.JdbcRoomDirectory.RoomInfo(
                "contract-room", "Contract duel", "Alice", null, false, 1000, false);
        var admission = new io.mastermindarena.deduction.infrastructure.jdbc.JdbcRoomDirectory.Admission(
                room, "p1", "example-test-token");
        var response = new SubmitActionHttpResponse(true, null, null, "contract-room", 1L,
                "PREPARATION", List.of("GAME_STARTED"));
        for (var fixture : java.util.Map.of("room-admission.json", admission, "action-accepted.json", response).entrySet()) {
            try (var input = getClass().getResourceAsStream("/contracts/" + fixture.getKey())) {
                assertNotNull(input);
                assertEquals(json.readTree(input), json.readTree(json.writeValueAsString(fixture.getValue())));
            }
        }
    }
    @Test void preparationMatchesTheFixtureConsumedByTheFrontend() throws Exception {
        var json = new ObjectMapper();
        var state = new MatchRuntimeState("contract-room", List.of("p1", "p2"));
        var actual = json.readTree(json.writeValueAsString(MatchStateHttpMapper.fromRuntimeState(state, "p1")));
        try (var input = getClass().getResourceAsStream("/contracts/match-preparation.json")) {
            assertNotNull(input);
            assertEquals(json.readTree(input), actual);
        }
    }
}

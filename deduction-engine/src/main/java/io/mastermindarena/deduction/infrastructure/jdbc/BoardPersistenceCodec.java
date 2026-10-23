package io.mastermindarena.deduction.infrastructure.jdbc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionResult;

/** Explicitly versioned JSON for new JDBC installations. No legacy game imports. */
public final class BoardPersistenceCodec {
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();
    private BoardPersistenceCodec() {}
    public record StateDocument(int formatVersion, MatchRuntimeState state) {}
    public record ResultDocument(int formatVersion, SubmitActionResult result) {}

    public static String encodeState(MatchRuntimeState state) {
        validate(state);
        return write(new StateDocument(1, state));
    }
    public static MatchRuntimeState decodeState(String payload) {
        StateDocument document = read(payload, StateDocument.class);
        requireVersion(document.formatVersion());
        validate(document.state());
        return document.state();
    }
    public static String encodeResult(SubmitActionResult result) {
        validate(result.state());
        return write(new ResultDocument(1, result));
    }
    public static SubmitActionResult decodeResult(String payload) {
        ResultDocument document = read(payload, ResultDocument.class);
        requireVersion(document.formatVersion());
        if (document.result() == null) throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT");
        validate(document.result().state());
        return document.result();
    }
    private static void requireVersion(int version) {
        if (version != 1) throw new IllegalArgumentException("UNSUPPORTED_STORAGE_VERSION");
    }
    private static void validate(MatchRuntimeState state) {
        if (state == null || !state.actorOrder().equals(java.util.List.of("p1", "p2"))
                || state.legacyMatchOutcome() != null || state.gameNumber() < 1
                || state.actionLog().stream().anyMatch(action -> action.roundNumber() < 0
                    || action.version() < 1 || action.version() > state.version()
                    || !state.actorOrder().contains(action.actorId()))) {
            throw new IllegalArgumentException("INVALID_STORED_MATCH");
        }
    }
    private static String write(Object document) {
        try { return JSON.writeValueAsString(document); }
        catch (java.io.IOException failure) { throw new IllegalStateException("MATCH_ENCODING_FAILED", failure); }
    }
    private static <T> T read(String payload, Class<T> type) {
        try {
            var tree = JSON.readTree(payload);
            if (tree == null || !tree.isObject() || !tree.path("formatVersion").isInt()) {
                throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT");
            }
            var state = type == StateDocument.class ? tree.path("state") : tree.path("result").path("state");
            if (!state.path("gameNumber").isInt() || state.path("gameNumber").asInt() < 1) {
                throw new IllegalArgumentException("INVALID_STORED_MATCH");
            }
            T document = JSON.treeToValue(tree, type);
            if (document == null) throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT");
            return document;
        }
        catch (java.io.IOException failure) { throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT", failure); }
    }
}

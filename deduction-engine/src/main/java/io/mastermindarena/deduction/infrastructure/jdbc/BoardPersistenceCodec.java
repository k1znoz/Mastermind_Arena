package io.mastermindarena.deduction.infrastructure.jdbc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.ActionReceipt;

/** Explicitly versioned JSON for new JDBC installations. No legacy game imports. */
public final class BoardPersistenceCodec {
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();
    private BoardPersistenceCodec() {}
    public record StateDocument(int formatVersion, MatchRuntimeState state) {}
    public record ReceiptDocument(int formatVersion, ActionReceipt receipt) {}

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
    public static String encodeReceipt(ActionReceipt receipt) {
        java.util.Objects.requireNonNull(receipt, "receipt is required");
        return write(new ReceiptDocument(2, receipt));
    }
    public static ActionReceipt decodeReceipt(String payload) {
        ReceiptDocument document = read(payload, ReceiptDocument.class);
        if (document.formatVersion() != 2) throw new IllegalArgumentException("UNSUPPORTED_STORAGE_VERSION");
        if (document.receipt() == null) throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT");
        return document.receipt();
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
            if (type == StateDocument.class && (!tree.path("state").path("gameNumber").isInt()
                    || tree.path("state").path("gameNumber").asInt() < 1)) {
                throw new IllegalArgumentException("INVALID_STORED_MATCH");
            }
            T document = JSON.treeToValue(tree, type);
            if (document == null) throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT");
            return document;
        }
        catch (java.io.IOException failure) { throw new IllegalArgumentException("INVALID_STORAGE_DOCUMENT", failure); }
    }
}

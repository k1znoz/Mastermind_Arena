package io.mastermindarena.deduction.infrastructure.preferences;

import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.MatchStateStore;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class PreferencesMatchStateStore implements MatchStateStore {
    private final Preferences node;

    public PreferencesMatchStateStore(Preferences rootNode) {
        this.node = rootNode.node("match-state");
    }

    @Override
    public Optional<MatchRuntimeState> findById(String matchId) {
        String value = node.get(hashKey(matchId), null);
        return value == null ? Optional.empty() : Optional.of(FilePersistenceCodec.deserializeMatchRuntimeState(value));
    }

    @Override
    public void save(MatchRuntimeState state) {
        node.put(hashKey(state.matchId()), FilePersistenceCodec.serializeMatchRuntimeState(state));
        flush(node);
    }

    private static void flush(Preferences preferences) {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Unable to flush match state preferences", e);
        }
    }

    private static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}

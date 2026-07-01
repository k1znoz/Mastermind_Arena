package io.mastermindarena.deduction.infrastructure.preferences;

import io.mastermindarena.deduction.engine.workflow.IdempotencyStore;
import io.mastermindarena.deduction.infrastructure.file.FilePersistenceCodec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class PreferencesIdempotencyStore implements IdempotencyStore {
    private final Preferences node;

    public PreferencesIdempotencyStore(Preferences rootNode) {
        this.node = rootNode.node("idempotency");
    }

    @Override
    public Optional<Entry> find(String key) {
        Preferences entryNode = node.node(hashKey(key));
        String fingerprint = entryNode.get("fingerprint", null);
        if (fingerprint == null) {
            return Optional.empty();
        }

        return Optional.of(new Entry(
                fingerprint,
                FilePersistenceCodec.deserializeSubmitActionResult(List.of(
                        entryNode.get("result.state", ""),
                        entryNode.get("result.resolution", ""),
                        entryNode.get("result.events", "")
                ))
        ));
    }

    @Override
    public void save(String key, Entry entry) {
        Preferences entryNode = node.node(hashKey(key));
        List<String> serialized = FilePersistenceCodec.serializeSubmitActionResult(entry.result());
        entryNode.put("fingerprint", entry.fingerprint());
        entryNode.put("result.state", serialized.get(0));
        entryNode.put("result.resolution", serialized.get(1));
        entryNode.put("result.events", serialized.get(2));
        flush(entryNode);
    }

    private static void flush(Preferences preferences) {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Unable to flush idempotency preferences", e);
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

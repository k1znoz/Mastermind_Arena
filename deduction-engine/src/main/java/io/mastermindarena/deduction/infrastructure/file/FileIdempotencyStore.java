package io.mastermindarena.deduction.infrastructure.file;

import io.mastermindarena.deduction.engine.workflow.IdempotencyStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class FileIdempotencyStore implements IdempotencyStore {
    private final Path rootDirectory;

    public FileIdempotencyStore(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize FileIdempotencyStore", e);
        }
    }

    @Override
    public Optional<Entry> find(String key) {
        Path file = resolvePath(key);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.size() != 5) {
                throw new IllegalStateException("Invalid idempotency entry payload");
            }

            String storedKey = FilePersistenceCodec.decode(lines.get(0));
            if (!storedKey.equals(key)) {
                throw new IllegalStateException("Idempotency key mismatch");
            }

            String fingerprint = FilePersistenceCodec.decode(lines.get(1));
            return Optional.of(new Entry(fingerprint, FilePersistenceCodec.deserializeSubmitActionResult(lines.subList(2, 5))));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read idempotency entry", e);
        }
    }

    @Override
    public void save(String key, Entry entry) {
        Path file = resolvePath(key);
        List<String> lines = new java.util.ArrayList<>();
        lines.add(FilePersistenceCodec.encode(key));
        lines.add(FilePersistenceCodec.encode(entry.fingerprint()));
        lines.addAll(FilePersistenceCodec.serializeSubmitActionResult(entry.result()));

        try {
            Files.createDirectories(file.getParent());
            Files.write(
                    file,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist idempotency entry", e);
        }
    }

    private Path resolvePath(String key) {
        return rootDirectory.resolve(hashKey(key) + ".idem");
    }

    private static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}

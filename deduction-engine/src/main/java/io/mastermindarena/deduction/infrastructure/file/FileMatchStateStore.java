package io.mastermindarena.deduction.infrastructure.file;

import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.MatchStateStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

public final class FileMatchStateStore implements MatchStateStore {
    private final Path rootDirectory;

    public FileMatchStateStore(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize FileMatchStateStore", e);
        }
    }

    @Override
    public Optional<MatchRuntimeState> findById(String matchId) {
        Path file = resolvePath(matchId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            String line = Files.readString(file, StandardCharsets.UTF_8);
            if (line.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(FilePersistenceCodec.deserializeMatchRuntimeState(line));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read match state", e);
        }
    }

    @Override
    public void save(MatchRuntimeState state) {
        Path file = resolvePath(state.matchId());
        String payload = FilePersistenceCodec.serializeMatchRuntimeState(state);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(
                    file,
                    payload,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist match state", e);
        }
    }

    private Path resolvePath(String key) {
        return rootDirectory.resolve(hashKey(key) + ".state");
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

package io.mastermindarena.deduction.infrastructure.file;

import io.mastermindarena.deduction.engine.workflow.EventSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class FileWorkflowEventSink implements EventSink {
    private final Path filePath;

    public FileWorkflowEventSink(Path filePath) {
        this.filePath = filePath;
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize FileWorkflowEventSink", e);
        }
    }

    @Override
    public void publish(String event) {
        String encoded = FilePersistenceCodec.encode(event) + System.lineSeparator();
        try {
            Files.writeString(filePath, encoded, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist workflow event", e);
        }
    }

    @Override
    public List<String> allEvents() {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<String> decoded = new ArrayList<>();
            for (String line : lines) {
                if (!line.isBlank()) {
                    decoded.add(FilePersistenceCodec.decode(line));
                }
            }
            return List.copyOf(decoded);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read workflow events", e);
        }
    }
}

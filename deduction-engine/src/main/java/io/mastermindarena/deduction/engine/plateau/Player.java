package io.mastermindarena.deduction.engine.plateau;

import java.util.Objects;

public record Player(String id) {
    public Player {
        Objects.requireNonNull(id, "id is required");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
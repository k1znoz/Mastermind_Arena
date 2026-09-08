package io.mastermindarena.deduction.engine.plateau;

import java.util.List;
import java.util.Objects;

public record Guess(Player author, List<String> symbols) {
    public Guess {
        Objects.requireNonNull(author, "author is required");
        Objects.requireNonNull(symbols, "symbols is required");
        symbols = List.copyOf(symbols);
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("symbols must not be empty");
        }
        if (symbols.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("symbols must not contain null");
        }
    }
}
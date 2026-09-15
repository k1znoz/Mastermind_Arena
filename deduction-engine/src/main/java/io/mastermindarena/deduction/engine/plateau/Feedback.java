package io.mastermindarena.deduction.engine.plateau;

public record Feedback(int bienPlaces, int malPlaces) {
    public Feedback {
        if (bienPlaces < 0) {
            throw new IllegalArgumentException("bienPlaces must be >= 0");
        }
        if (malPlaces < 0) {
            throw new IllegalArgumentException("malPlaces must be >= 0");
        }
    }
}
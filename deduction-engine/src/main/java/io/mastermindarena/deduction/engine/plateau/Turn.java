package io.mastermindarena.deduction.engine.plateau;

import java.util.Objects;
import java.util.Optional;

public record Turn(int number, Player guesser, Player feedbackGiver, Guess guess, Feedback feedback) {
    public Turn {
        if (number < 1) {
            throw new IllegalArgumentException("number must be >= 1");
        }
        Objects.requireNonNull(guesser, "guesser is required");
        Objects.requireNonNull(feedbackGiver, "feedbackGiver is required");
        if (guesser.equals(feedbackGiver)) {
            throw new IllegalArgumentException("guesser and feedbackGiver must be different");
        }
        if (guess != null && !guess.author().equals(guesser)) {
            throw new IllegalArgumentException("guess author must be the guesser");
        }
        if (feedback != null && guess == null) {
            throw new IllegalArgumentException("feedback requires a guess");
        }
    }

    public Optional<Guess> guessOptional() {
        return Optional.ofNullable(guess);
    }

    public Optional<Feedback> feedbackOptional() {
        return Optional.ofNullable(feedback);
    }
}
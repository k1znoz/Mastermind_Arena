package io.mastermindarena.deduction.engine.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ActionResolution {
    private final Set<EngineDirective> engineDirectives;
    private final Optional<EvaluationResult> evaluationResult;
    private final Optional<MatchOutcome> matchOutcome;
    private final Optional<CancellationReason> cancellationReason;
    private final Optional<Rejection> rejection;
    private final List<Object> domainEvents;

    public ActionResolution(
            Set<EngineDirective> engineDirectives,
            EvaluationResult evaluationResult,
            MatchOutcome matchOutcome,
            CancellationReason cancellationReason,
            Rejection rejection,
            List<Object> domainEvents
    ) {
        Objects.requireNonNull(engineDirectives, "engineDirectives is required");
        this.engineDirectives = Set.copyOf(engineDirectives);
        this.evaluationResult = Optional.ofNullable(evaluationResult);
        this.matchOutcome = Optional.ofNullable(matchOutcome);
        this.cancellationReason = Optional.ofNullable(cancellationReason);
        this.rejection = Optional.ofNullable(rejection);
        this.domainEvents = domainEvents == null ? List.of() : List.copyOf(domainEvents);
    }

    public Set<EngineDirective> engineDirectives() {
        return engineDirectives;
    }

    public Optional<EvaluationResult> evaluationResult() {
        return evaluationResult;
    }

    public Optional<MatchOutcome> matchOutcome() {
        return matchOutcome;
    }

    public Optional<CancellationReason> cancellationReason() {
        return cancellationReason;
    }

    public Optional<Rejection> rejection() {
        return rejection;
    }

    public List<Object> domainEvents() {
        return domainEvents;
    }

    public static ActionResolution of(Set<EngineDirective> directives) {
        return new ActionResolution(directives, null, null, null, null, List.of());
    }
}

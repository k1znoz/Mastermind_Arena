# ADR 0002: ActionResolution Contract

## Status

Accepted

## Context

RuleSet outputs must be validated consistently to protect engine invariants.

## Decision

Use a single contract validation responsibility:

- ActionResolutionContractValidator

It validates P0 constraints including directive coherence, terminal exclusivity,
and mandatory MatchOutcome or CancellationReason for terminal directives.

## Consequences

- One source of truth for contract checks.
- Reduced risk of divergent validation behavior.
- Contract-first test strategy for Sprint A.

# ADR 0001: Deduction Engine Agnostic of Game Semantics

## Status

Accepted

## Context

The engine must support multiple deduction games without being modified for each game.

## Decision

The engine will never interpret game-specific semantics.
EvaluationResult and RULESET rejection codes are opaque payloads.
Engine transitions are driven only by EngineDirective.

## Consequences

- Strong decoupling between engine and game modules.
- Better extensibility for future games.
- Strict contract validation becomes mandatory.

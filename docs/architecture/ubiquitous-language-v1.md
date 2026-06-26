# Ubiquitous Language v1

This document defines the shared vocabulary for the deduction engine core.

## Core Terms

- RuleSet: game-provided contract that resolves an action into an ActionResolution.
- ActionResolution: contract output used by the engine.
- EngineDirective: structural directive interpreted by the engine.
- EvaluationResult: opaque game payload transported by the engine without interpretation.
- MatchOutcome: generic terminal result for FINISH_MATCH.
- ParticipantResult: per participant terminal result.
- CancellationReason: explicit structural reason required for CANCEL_MATCH.
- Rejection: structured rejection metadata with origin and code.
- RejectionOrigin: ENGINE or RULESET.
- LogTarget: MATCH_HISTORY, DOMAIN_EVENT_LOG, AUDIT_SECURITY_LOG, TECHNICAL_LOG.

## Design Guardrails

- The engine never interprets game semantics.
- The engine transitions are driven only by EngineDirective.
- RULESET rejection codes are opaque to the engine.

# Rejection v1.1

## RejectionOrigin

- ENGINE
- RULESET

## Rejection Structure

- origin: ENGINE or RULESET
- code: uppercase snake case
- messageKey: optional
- details: opaque optional map
- targetLogs: non-empty set when rejection is logged or persisted

## Engine Codes (v1)

- MATCH_NOT_FOUND
- MATCH_NOT_IN_PROGRESS
- MATCH_ALREADY_TERMINAL
- TURN_NOT_ACTIVE
- ACTOR_NOT_AUTHORIZED
- INVALID_ACTION_FORMAT
- DUPLICATE_ACTION
- VERSION_CONFLICT
- IDEMPOTENCY_CONFLICT
- RULESET_NOT_FOUND
- RULESET_UNAVAILABLE
- RULESET_CONTRACT_VIOLATION
- ENGINE_DIRECTIVE_NOT_ALLOWED
- INVALID_ENGINE_DIRECTIVE_COMBINATION
- MATCH_OUTCOME_REQUIRED
- CANCELLATION_REASON_REQUIRED
- TERMINAL_DIRECTIVES_CONFLICT

## Rules

- ENGINE rejection is generally pre-RuleSet.
- Contract violations detected after RuleSet invocation remain ENGINE rejections.
- RULESET codes are defined only in games modules and are opaque to the engine.

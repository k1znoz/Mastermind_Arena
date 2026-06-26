# ActionResolution Contract v1.1

This is the Sprint A contract source of truth.

## Single Validation Responsibility

Validation is centralized in one conceptual responsibility:

- ActionResolutionContractValidator

No duplicated or conflicting validation logic is allowed.

## Contract Rules (P0)

1. At least one EngineDirective is required.
2. Engine directives must be structural and allowed.
3. Directive combinations must be coherent.
4. FINISH_MATCH and CANCEL_MATCH are mutually exclusive.
5. FINISH_MATCH requires MatchOutcome.
6. CANCEL_MATCH requires CancellationReason.
7. RULESET rejection requires REJECT_ACTION.

## Notes

- CR-P0-04 is intentionally separate from CR-P0-06 because terminal exclusivity is lifecycle-critical.
- CancellationReason taxonomy is intentionally minimal in Sprint A.

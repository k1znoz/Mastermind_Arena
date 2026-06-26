# Test Plan v1.1 (Sprint A Scope)

Scope is strictly limited to:

- CR-P0-01
- CR-P0-02
- CR-P0-03
- CR-P0-04
- CR-P0-05
- CR-P0-06
- CR-P0-07

## Exclusions

- No SubmitAction workflow tests.
- No REST, persistence, websocket, security, or frontend.
- No game-specific business logic in engine.

## Goal

Validate ActionResolution contract with one source of truth:

- ActionResolutionContractValidator

## Traceability Table (Sprint A)

| Test ID | Invariant Covered | Validator Rule | Expected Result |
|---|---|---|---|
| CR-P0-01 | ActionResolution contains at least one EngineDirective | Non-empty directives required | Invalid resolution returns ENGINE rejection with RULESET_CONTRACT_VIOLATION |
| CR-P0-02 | FINISH_MATCH requires MatchOutcome | MatchOutcome mandatory when FINISH_MATCH present | Invalid resolution returns ENGINE rejection with MATCH_OUTCOME_REQUIRED |
| CR-P0-03 | CANCEL_MATCH requires CancellationReason | CancellationReason mandatory when CANCEL_MATCH present | Invalid resolution returns ENGINE rejection with CANCELLATION_REASON_REQUIRED |
| CR-P0-04 | FINISH_MATCH and CANCEL_MATCH are mutually exclusive | Terminal directives conflict is forbidden | Invalid resolution returns ENGINE rejection with TERMINAL_DIRECTIVES_CONFLICT |
| CR-P0-05 | Engine directives are structural and allowed | Only allowed EngineDirective values accepted | Valid structural directive set passes contract validation |
| CR-P0-06 | Directive combinations must be coherent | Incoherent combinations are forbidden | Invalid resolution returns ENGINE rejection with INVALID_ENGINE_DIRECTIVE_COMBINATION |
| CR-P0-07 | RULESET rejection must be coherent | RULESET origin requires REJECT_ACTION | Valid coherent rejection passes; otherwise ENGINE rejection with RULESET_CONTRACT_VIOLATION |

## Rejection v1.1 Assertions in Sprint A

- ENGINE rejections in Sprint A contract tests are technical contract protections.
- RULESET rejection codes are treated as opaque payloads by the engine.
- targetLogs must be non-empty for any persisted or logged rejection.

## Documentation Cross-References

- Contract source: docs/architecture/action-resolution-contract-v1.1.md
- Directive list and incoherent combinations: docs/architecture/engine-directives-v1.1.md
- Rejection dictionary: docs/architecture/rejection-v1.1.md
- ADR on contract centralization: docs/adr/0002-action-resolution-contract.md

## Review Checklist Sprint A Exit

Use this checklist before closing Sprint A.

### Scope Control

- [ ] Only CR-P0-01 to CR-P0-07 were implemented and executed.
- [ ] No SubmitAction workflow tests were added.
- [ ] No REST, persistence, websocket, security, or frontend artifacts were introduced.
- [ ] No game-specific business logic was added to deduction-engine.

### Contract Integrity

- [ ] ActionResolutionContractValidator is the single source of truth for contract validation.
- [ ] CR-P0-01 enforces non-empty directives.
- [ ] CR-P0-05 enforces structural directives only.
- [ ] CR-P0-06 enforces incoherent-combination rejection.
- [ ] CR-P0-04 explicitly enforces FINISH_MATCH and CANCEL_MATCH exclusivity.
- [ ] CR-P0-02 enforces MatchOutcome requirement with FINISH_MATCH.
- [ ] CR-P0-03 enforces CancellationReason requirement with CANCEL_MATCH.
- [ ] CR-P0-07 enforces RULESET rejection coherence with REJECT_ACTION.

### Rejection v1.1 Compliance (Sprint A)

- [ ] ENGINE and RULESET are the only allowed rejection origins.
- [ ] RULESET codes are treated as opaque by the engine contract layer.
- [ ] targetLogs are non-empty for any logged or persisted rejection.
- [ ] Contract violations are represented as ENGINE rejections.

### Test Execution and Quality Gate

- [ ] mvn -q test is green on the Sprint A scope.
- [ ] No flaky behavior observed in CR-P0 tests.
- [ ] Failures produce deterministic rejection codes for contract violations.

### Documentation Alignment

- [ ] test-plan-v1.1.md traceability table matches implemented tests.
- [ ] action-resolution-contract-v1.1.md matches validator behavior.
- [ ] engine-directives-v1.1.md matches accepted and rejected combinations.
- [ ] rejection-v1.1.md matches rejection structure and usage.
- [ ] ADR 0001 and ADR 0002 remain consistent with implemented contract decisions.

### Exit Decision

- [ ] All checklist items above are satisfied.
- [ ] Sprint A is formally closed.
- [ ] Sprint B has not started.

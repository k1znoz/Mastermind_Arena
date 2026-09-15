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

## Sprint G - P0 Stabilization Scope

Sprint G is a stabilization sprint. No new feature is introduced.

### Included

- Full review of CR-P0 and WF-P0 coverage.
- Final contract/workflow traceability table.
- P0 Exit checklist.
- Minimal documentation alignment with implemented behavior.
- Full test execution using mvn -q test.
- Final decision: P0 DONE or P0 NOT DONE.

### Excluded

- Any new workflow.
- Any new business feature.
- REST, JPA, WebSocket, Security, frontend.
- Real outbox, full read model, or full game logic.
- Large refactor unrelated to P0 stabilization.

## Final P0 Traceability (Contract + Workflow)

| Test ID | Expected Behavior | Status |
|---|---|---|
| CR-P0-01 | Empty ActionResolution directives are rejected with RULESET_CONTRACT_VIOLATION | Covered |
| CR-P0-02 | FINISH_MATCH requires MatchOutcome | Covered |
| CR-P0-03 | CANCEL_MATCH requires CancellationReason | Covered |
| CR-P0-04 | FINISH_MATCH and CANCEL_MATCH are mutually exclusive | Covered |
| CR-P0-05 | Engine directives are structural and allowed | Covered |
| CR-P0-06 | Incoherent directive combinations are rejected | Covered |
| CR-P0-07 | RULESET rejection requires REJECT_ACTION to be valid | Covered |
| WF-P0-01 | ACCEPT_ACTION + CONTINUE_TURN | Covered |
| WF-P0-02 | ACCEPT_ACTION + END_TURN + START_NEXT_TURN | Covered |
| WF-P0-03 | FINISH_MATCH with MatchOutcome terminalizes match | Covered |
| WF-P0-04 | CANCEL_MATCH with CancellationReason terminalizes match | Covered |
| WF-P0-05 | MATCH_NOT_FOUND is rejected pre-RuleSet | Covered |
| WF-P0-06 | MATCH_NOT_IN_PROGRESS is rejected pre-RuleSet | Covered |
| WF-P0-07 | TURN_NOT_ACTIVE is rejected pre-RuleSet | Covered |
| WF-P0-08 | ACTOR_NOT_AUTHORIZED is rejected pre-RuleSet | Covered |
| WF-P0-09 | VERSION_CONFLICT is rejected pre-RuleSet | Covered |
| WF-P0-10A | Legitimate idempotent replay returns previous response | Covered |
| WF-P0-10B | Idempotency conflict is rejected with IDEMPOTENCY_CONFLICT | Covered |
| WF-P0-11 | No directive post-RuleSet -> RULESET_CONTRACT_VIOLATION | Covered |
| WF-P0-12 | FINISH_MATCH without MatchOutcome -> MATCH_OUTCOME_REQUIRED | Covered |
| WF-P0-13 | CANCEL_MATCH without CancellationReason -> CANCELLATION_REASON_REQUIRED | Covered |
| WF-P0-14 | FINISH_MATCH + CANCEL_MATCH -> TERMINAL_DIRECTIVES_CONFLICT | Covered |
| WF-P0-15 | Terminal match rejects SubmitAction with MATCH_ALREADY_TERMINAL | Covered |
| WF-P0-16 | RuleSet business rejection with REJECT_ACTION is handled as valid result | Covered |
| WF-P0-17 | RuleSet rejection code remains opaque and unchanged | Covered |
| WF-P0-18 | RuleSet rejection logging follows requested LogTarget only | Covered |
| WF-P0-19 | RuleSet rejection does not mutate state | Covered |
| WF-P0-20 | RuleSet rejection is not confused with ENGINE rejection | Covered |
| WF-P0-21 | Invalid directive combination post-RuleSet -> INVALID_ENGINE_DIRECTIVE_COMBINATION | Covered |

## P0 Exit Checklist

- [ ] CR-P0 and WF-P0 suites are fully covered and aligned with implemented behavior.
- [ ] RuleSet/ActionResolution contract remains stable and centrally validated.
- [ ] ENGINE and RULESET rejection paths are clearly separated.
- [ ] Terminal states are opposable and block further submit actions.
- [ ] Minimal idempotency behavior (10A/10B) is covered and deterministic.
- [ ] RuleSet business rejection path (WF-P0-16..20) is covered with opaque code handling.
- [ ] Documentation reflects actual behavior without scope drift.
- [ ] Full test suite passes with mvn -q test.

## Final P0 Decision

Mark one outcome after final test run:

- [ ] P0 DONE
- [ ] P0 NOT DONE

Decision basis:

- P0 DONE requires all checklist items above and green test execution.
- P0 NOT DONE applies if any P0 invariant, test, or alignment item is missing.

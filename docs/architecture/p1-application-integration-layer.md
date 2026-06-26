# P1 Architecture Decision - Application Integration Layer

## Decision
P1 introduces a minimal application layer around the P0 engine to execute SubmitAction end-to-end while strictly preserving:
- engine agnosticism regarding Mastermind business logic,
- unchanged ActionResolution contract,
- a fully in-memory P1-A phase.

Real persistence is explicitly deferred to P1-B.

## Context
P0 is closed and validated (green tests, stable contract and workflow invariants). The next step is controlled application integration without responsibility drift:
- engine remains responsible for contract execution,
- application layer orchestrates exposure, mappings, and external dependencies.

## Scope
- Minimal application exposure for SubmitAction.
- Explicit mapping:
- application input -> engine command,
- engine output -> application response.
- Clear application orchestration around existing ports/adapters.
- Application integration tests focused on nominal flow and structural rejections.
- P1-A remains fully in-memory, with no real database.

## Out of Scope
- Any Mastermind business logic in the engine.
- Refactor or semantic extension of ActionResolution contract.
- Real persistence (deferred to P1-B).
- Rich API concerns (full auth, realtime websocket, full matchmaking, UI).
- Advanced performance and scalability optimization.

## Sprint Breakdown
1. P1-A - Application Integration (In-Memory)
- minimal SubmitAction application facade,
- input/output mappings and application error handling,
- in-memory integration tests,
- mandatory P0 non-regression.

2. P1-B - Controlled Persistence Introduction
- minimal technical persistence (state/version/idempotency/events),
- dedicated adapters with no engine pollution,
- contract invariant validation after persistence.

3. P1-C - Hardening
- robustness hardening (errors/recovery),
- minimal observability,
- integration test consolidation and initial run-readiness criteria.

## Risks
- Business logic leaking from application into engine.
- Contract alteration through incorrect application mappings.
- Premature persistence complexity.
- Undetected P0 regressions on idempotency/versioning/terminalization.
- Weak port/adapter boundary discipline.

## Exit Criteria
- End-to-end SubmitAction works through application layer with unchanged engine.
- No regression on critical P0 invariants.
- Engine/application separation validated in architecture review.
- P1-A integration tests green on nominal flow + structural rejections + idempotency replay.
- P1 decisions documented (responsibilities, limits, deferred persistence strategy).

## Immediate Next Step
Start detailed P1-A framing (no coding yet):
- define minimal application input/output contract,
- freeze mapping and error propagation rules,
- define P1-A integration test matrix aligned with P0 invariants.

## P1-B Completion Note

### Decision
P1-B is completed with local file-based persistence adapters.

### Implemented
- `IdempotencyStore` port.
- `FileMatchStateStore`.
- `FileIdempotencyStore`.
- `FileWorkflowEventSink`.
- Persistent integration tests for nominal submit, structural rejection without mutation, idempotent replay, idempotency conflict, and expected event persistence.

### Constraints Confirmed
- No Spring.
- No JPA.
- No H2.
- No database.
- No REST.
- No Security.
- No WebSocket.
- No UI.
- `ActionResolution` unchanged.
- Engine remains game-agnostic.

### Known Debt
- Minimal file codec, not a long-term exchange format.
- No advanced inter-process concurrency strategy.
- Append-only event journal without rotation or production outbox semantics.
- Idempotent result restoration covers the subset needed for P1-B.

### Status
`P1-B DONE`

## File Persistence Operational Limits

The P1-B file persistence adapters are intended as minimal local persistence for integration hardening, not as production-grade storage.

### Format
- UTF-8 local files.
- Deterministic line-based records.
- URL-safe Base64 encoded fields.
- Tab-separated technical fields.
- SHA-256 derived file names for persisted keys.

### Guarantees
- Local best-effort durability.
- Deterministic serialization/deserialization for supported workflow state.
- Suitable for single-process integration scenarios.

### Limits
- No inter-process locking.
- No distributed consistency.
- No transaction manager.
- No event journal rotation.
- No production outbox semantics.
- No long-term exchange-format commitment.

### Corruption Handling
Invalid or unsupported persisted data should fail fast rather than be silently repaired or partially interpreted.

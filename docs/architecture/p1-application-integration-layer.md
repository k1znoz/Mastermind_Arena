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

## P2-B Boundary Guardrails (Checklist)

- [x] Engine package contains no Mastermind-specific business logic.
- [x] `ActionResolution` contract remains unchanged.
- [x] Application layer maps to/from engine contracts without semantic transformation of rejection origin/code.
- [x] File persistence remains an infrastructure adapter concern only.
- [x] No REST, no database, no Spring/JPA/H2 introduced in P2.

## P2-C Local Runbook (File Persistence)

### Prerequisites
- JDK 25 active in shell (`mvn -version` should report Java 25.x).
- Maven 3.9.x available on PATH.

### Validation Command
- Run full verification with: `mvn -q test`.

### Operational Limits
- File persistence is local and adapter-based only.
- Single-process integration target; no inter-process locking guarantees.
- Append-only workflow journal; no rotation and no production outbox semantics.

## P2 Known Debt (Consolidated)

- File codec is intentionally minimal and not a long-term exchange format.
- No advanced inter-process concurrency strategy for local file persistence.
- Workflow event journal remains append-only with no rotation lifecycle.
- Idempotent result restoration is limited to the subset required by current P1/P2 flows.

## P2 Closure Status

- P2-A: DONE
- P2-B: DONE
- P2-C: DONE
- P2: DONE

## P3-A Minimal SubmitAction HTTP Contract

### Request Payload
- `matchId` (string, required)
- `actorId` (string, required)
- `expectedVersion` (number, required)
- `idempotencyKey` (string, required)
- `actionPayload` (opaque object, required)

### Response Payload
- `accepted` (boolean)
- `rejectionOrigin` (nullable string: `ENGINE` or `RULESET`)
- `rejectionCode` (nullable string)
- `matchId` (nullable string)
- `version` (nullable number)
- `status` (nullable string)
- `emittedEvents` (array of strings)

### Transport Mapping Table
- Nominal success (`accepted=true`) -> HTTP `200`
- RULESET rejection (`rejectionOrigin=RULESET`) -> HTTP `422`
- ENGINE `MATCH_NOT_FOUND` -> HTTP `404`
- ENGINE `ACTOR_NOT_AUTHORIZED` -> HTTP `403`
- ENGINE `MATCH_NOT_IN_PROGRESS` / `TURN_NOT_ACTIVE` / `VERSION_CONFLICT` / `IDEMPOTENCY_CONFLICT` / `MATCH_ALREADY_TERMINAL` -> HTTP `409`
- Other ENGINE structural errors -> HTTP `400`

## P3-C Local API SubmitAction Note

### Endpoint
- Method: `POST`
- Path: `/local/submit-action`

### Minimal Payload
- Request: `matchId`, `actorId`, `expectedVersion`, `idempotencyKey`, `actionPayload`.
- Response: `accepted`, `rejectionOrigin`, `rejectionCode`, `matchId`, `version`, `status`, `emittedEvents`.

### Status Mapping
- `200`: nominal accepted submit.
- `422`: RULESET rejection.
- `404`: ENGINE `MATCH_NOT_FOUND`.
- `409`: ENGINE structural conflicts (`VERSION_CONFLICT`, `IDEMPOTENCY_CONFLICT`, `MATCH_NOT_IN_PROGRESS`, `TURN_NOT_ACTIVE`, `MATCH_ALREADY_TERMINAL`).
- `403`: ENGINE `ACTOR_NOT_AUTHORIZED`.
- `400`: other ENGINE structural errors.

### Operational Limits (Local/Dev Only)
- Local endpoint for development and integration hardening only.
- No authentication/authorization layer in P3 scope.
- No production transport hardening (rate limiting, API versioning, distributed concerns).

## P3 Known Debt (Consolidated)

- Local endpoint is intentionally minimal and not production-ready.
- Transport contract covers current SubmitAction flow only; no broader API surface in P3.
- HTTP status mapping is intentionally structural and may require refinement when cross-cutting concerns are introduced.
- File persistence constraints from P1/P2 still apply unchanged.

## P3 Closure Status

- P3-A: DONE
- P3-B: DONE
- P3-C: DONE
- P3: DONE

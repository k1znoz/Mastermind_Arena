import test from 'node:test'
import assert from 'node:assert/strict'
import { createGameConnection } from '../src/lib/state/gameConnection.js'
import { createSessionRepository } from '../src/lib/state/session.js'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}

function fixture(overrides = {}) {
  let context = { matchId: 'board', actorId: 'p1', roomToken: 'private-test' }
  let state = { matchId: 'board', version: 2, gameNumber: 1, roundNumber: 0, state: 'WAITING_GUESS' }
  const changes = []
  const errors = []
  const sync = []
  const connection = createGameConnection({
    getContext: () => context, getState: () => state,
    fetchState: async () => state,
    sendAction: async () => ({ accepted: true }),
    onState: value => { state = value; changes.push(value) },
    onSync: (status, error) => sync.push([status, error]),
    onSubmit: (status, action, error) => errors.push([status, action, error]),
    ...overrides
  })
  return { connection, changes, errors, sync, state: () => state, setContext: value => { context = value } }
}

test('simultaneous readers await the same refresh', async () => {
  const pending = deferred()
  let calls = 0
  const f = fixture({ fetchState: () => { calls++; return pending.promise } })
  const first = f.connection.refresh()
  const second = f.connection.refresh()
  assert.equal(first, second)
  pending.resolve({ ...f.state(), version: 3 })
  assert.equal((await second).version, 3)
  assert.equal(calls, 1)
})

test('late state or error cannot modify a different place, including token rotation', async () => {
  for (const fail of [false, true]) {
    const pending = deferred()
    const f = fixture({ fetchState: () => pending.promise })
    const result = f.connection.refresh()
    f.setContext({ matchId: 'board', actorId: 'p1', roomToken: 'rotated' })
    if (fail) pending.reject(new Error('offline'))
    else pending.resolve({ ...f.state(), version: 4 })
    await result
    assert.equal(f.changes.length, 0)
    assert.equal(f.sync.length, 1)
  }
})

test('old snapshot cannot roll back the current version', async () => {
  const f = fixture({ fetchState: async () => ({ matchId: 'board', version: 1 }) })
  await f.connection.refresh()
  assert.equal(f.state().version, 2)
  assert.equal(f.changes.length, 0)
})

test('wrong-match response is a contract failure', async () => {
  const f = fixture({ fetchState: async () => ({ matchId: 'other', version: 10 }) })
  await f.connection.refresh()
  assert.equal(f.sync.at(-1)[0], 'error')
  assert.equal(f.changes.length, 0)
})

test('failed refresh preserves the last board and retry distinguishes loading from error', async () => {
  const retry = deferred()
  let calls = 0
  const failure = new Error('HTTP:INTERNAL_ERROR')
  const f = fixture({ fetchState: () => ++calls === 1 ? Promise.reject(failure) : retry.promise })
  const previous = f.state()
  assert.equal(await f.connection.refresh(), null)
  assert.equal(f.state(), previous)
  assert.deepEqual(f.sync.at(-1), ['error', failure])
  const loading = f.connection.refresh()
  assert.deepEqual(f.sync.at(-1), ['syncing', undefined])
  assert.equal(f.state(), previous)
  retry.resolve({ ...previous, version: previous.version + 1 })
  await loading
  assert.equal(f.state().version, previous.version + 1)
  assert.deepEqual(f.sync.at(-1), ['ok', undefined])
})

test('lost response replays the exact operation and its idempotency key', async () => {
  const requests = []
  const f = fixture({ sendAction: async (_context, request) => {
    requests.push(request)
    if (requests.length === 1) throw new TypeError('Network response lost')
    return { accepted: true }
  } })
  assert.equal(await f.connection.submit({ actionType: 'PLAY_GUESS', payload: '["A","B","C","D"]' }), true)
  assert.deepEqual(requests[0], requests[1])
  assert.equal(f.errors.at(-1)[0], 'success')
})

test('confirmed version conflict creates a new request only in the same phase and round', async () => {
  for (const nextRound of [0, 1]) {
    const requests = []
    const f = fixture({ fetchState: async () => ({ ...f.state(), version: 3, roundNumber: nextRound }),
      sendAction: async (_context, request) => {
        requests.push(request)
        if (requests.length === 1) throw Object.assign(new Error('conflict'), { code: 'VERSION_CONFLICT' })
        return { accepted: true }
      } })
    assert.equal(await f.connection.submit({ actionType: 'PLAY_GUESS' }), nextRound === 0)
    assert.equal(requests.length, nextRound === 0 ? 2 : 1)
    if (nextRound === 0) {
      assert.equal(requests[1].expectedVersion, 3)
      assert.notEqual(requests[0].idempotencyKey, requests[1].idempotencyKey)
    }
  }
})

test('invalidation cancels work and suppresses old submission results', async () => {
  const pending = deferred()
  let signal
  const f = fixture({ sendAction: (_context, _request, cancellation) => { signal = cancellation; return pending.promise } })
  const result = f.connection.submit({ actionType: 'SEND_FEEDBACK' })
  f.connection.invalidate()
  assert.equal(signal.aborted, true)
  pending.resolve({ accepted: true })
  assert.equal(await result, false)
  assert.equal(f.errors.length, 1)
})

test('duplicate clicks do not create a second operation', async () => {
  const pending = deferred()
  const f = fixture({ sendAction: () => pending.promise })
  const first = f.connection.submit({ actionType: 'ABANDON' })
  assert.equal(await f.connection.submit({ actionType: 'ABANDON' }), false)
  pending.resolve({ accepted: true })
  assert.equal(await first, true)
})

function storage() {
  const values = new Map()
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
}

test('private seat survives a new tab; corrupt or partial stored values are rejected', () => {
  const session = storage()
  const local = storage()
  const value = { matchId: 'board', actorId: 'p2', roomName: 'Duel', roomToken: 'secret', playerName: 'Bob' }
  assert.equal(createSessionRepository(session, local).save(value), true)
  const reopened = createSessionRepository(storage(), local)
  assert.deepEqual(reopened.load(), value)
  reopened.forget()
  assert.equal(reopened.load(), null)
  local.setItem('arenaSavedRoom', '{')
  assert.equal(reopened.load(), null)
  local.setItem('arenaSavedRoom', JSON.stringify({ ...value, actorId: 'p3' }))
  assert.equal(reopened.load(), null)
})

test('disabled storage does not prevent playing and cannot pretend a private code was saved', () => {
  const unavailable = { getItem() { throw new Error('blocked') }, setItem() { throw new Error('blocked') }, removeItem() { throw new Error('blocked') } }
  const repository = createSessionRepository(unavailable, unavailable)
  assert.equal(repository.load(), null)
  assert.equal(repository.playerName(), '')
  assert.equal(repository.savePlayerName('Alice'), false)
  assert.equal(repository.save({ matchId: 'board', actorId: 'p1', roomToken: 'private', roomName: 'Duel', playerName: 'Alice' }), false)
  assert.equal(repository.forget(), false)
})

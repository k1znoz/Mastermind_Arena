import test from 'node:test'
import assert from 'node:assert/strict'
import { createRoomConnection, roomErrorText } from '../src/lib/state/roomConnection.js'

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function harness(client) {
  const events = { rooms: [], statuses: [], admissions: [], busy: [], statusBusy: [], errors: [] }
  let scope = 'lobby:first-seat'
  const connection = createRoomConnection({
    getScope: () => scope, getContext: () => ({ matchId: 'board', roomToken: 'test-seat' }),
    createClient: () => client, onRooms: value => events.rooms.push(value),
    onStatus: value => events.statuses.push(value),
    onAdmission: (value, recovered) => events.admissions.push({ value, recovered }),
    onBusy: value => events.busy.push(value), onStatusBusy: value => events.statusBusy.push(value),
    onError: (message, recovery) => events.errors.push({ message, recovery })
  })
  return { connection, events, scope: value => { scope = value } }
}

test('double admission clicks share one operation and recovery keeps its identity', async () => {
  const pending = deferred()
  const calls = []
  const { connection, events } = harness({ resume: (...args) => { calls.push(args); return pending.promise } })
  const first = connection.recover('test-recovery')
  assert.equal(connection.recover('test-recovery'), first)
  assert.equal(calls.length, 1)
  assert.equal(calls[0][0], 'test-recovery')
  pending.resolve({ actorId: 'p2' })
  await first
  assert.deepEqual(events.admissions, [{ value: { actorId: 'p2' }, recovered: true }])
  assert.deepEqual(events.busy, [true, false])
})

test('late admission success or failure cannot alter a different screen or seat', async () => {
  for (const failure of [false, true]) {
    const pending = deferred()
    const h = harness({ create: () => pending.promise })
    const operation = h.connection.create('Duel', 'Alice', '')
    h.scope('game:other-seat')
    if (failure) pending.reject(new Error('ROOM_FULL'))
    else pending.resolve({ actorId: 'p1' })
    await operation
    assert.deepEqual(h.events.admissions, [])
    assert.equal(h.events.errors.filter(event => event.message).length, 0)
  }
})

test('leaving recovery aborts its request and ignores late success or failure after reopening', async () => {
  for (const failure of [false, true]) {
    const old = deferred(), next = deferred()
    const signals = []
    const h = harness({ resume: (_code, options) => {
      signals.push(options.signal)
      return signals.length === 1 ? old.promise : next.promise
    } })
    h.scope('recovery:first-seat')
    const previous = h.connection.recover('old-private-test-code')
    h.connection.invalidate()
    h.scope('welcome:first-seat')
    assert.equal(signals[0].aborted, true)
    h.scope('recovery:first-seat')
    const current = h.connection.recover('new-private-test-code')
    if (failure) old.reject(new Error('ROOM_RESUME_INVALID'))
    else old.resolve({ actorId: 'p1', room: 'old' })
    await previous
    assert.deepEqual(h.events.admissions, [])
    assert.equal(h.events.errors.filter(event => event.message).length, 0)
    assert.equal(h.events.busy.at(-1), true)
    next.resolve({ actorId: 'p2', room: 'current' })
    await current
    assert.deepEqual(h.events.admissions, [{ value: { actorId: 'p2', room: 'current' }, recovered: true }])
    assert.equal(h.events.busy.at(-1), false)
  }
})

test('invalidation aborts reads and prevents an old finally from unlocking a newer admission', async () => {
  const old = deferred(), next = deferred()
  const signals = []
  let count = 0
  const h = harness({ join: (_id, _name, _code, options) => {
    signals.push(options.signal)
    return count++ === 0 ? old.promise : next.promise
  } })
  const first = h.connection.join('one', 'Alice', '')
  h.connection.invalidate()
  const second = h.connection.join('two', 'Alice', '')
  assert.equal(signals[0].aborted, true)
  old.resolve({ room: 'one' })
  await first
  assert.equal(h.events.busy.at(-1), true)
  next.resolve({ room: 'two' })
  await second
  assert.deepEqual(h.events.admissions.map(event => event.value.room), ['two'])
  assert.equal(h.events.busy.at(-1), false)
})

test('list reads are shared and a stale waiting status cannot reopen the game', async () => {
  const list = deferred(), status = deferred()
  const h = harness({ list: () => list.promise, status: () => status.promise })
  const first = h.connection.refreshRooms()
  assert.equal(h.connection.refreshRooms(), first)
  list.resolve([{ roomId: 'one' }])
  await first
  assert.equal(h.events.rooms.length, 1)
  const waiting = h.connection.refreshStatus()
  h.connection.invalidate()
  status.resolve({ joined: true })
  await waiting
  assert.deepEqual(h.events.statuses, [])
})

test('admission errors leave the operation retryable and never disclose unknown payloads', async () => {
  let attempts = 0
  const h = harness({ join: async () => {
    if (attempts++ === 0) throw new Error('ROOM_CODE_INVALID')
    return { actorId: 'p2' }
  } })
  await h.connection.join('board', 'Bob', 'wrong')
  assert.equal(h.events.errors.at(-1).message, 'Code d’accès incorrect.')
  await h.connection.join('board', 'Bob', 'correct')
  assert.equal(h.events.admissions.length, 1)
  assert.ok(!roomErrorText(new Error('private payload')).includes('private payload'))
  assert.equal(roomErrorText(new Error('constructor')), 'Impossible de charger cette partie. Réessayez.')
})

test('failed shared waiting reads preserve the last status and allow retry', async () => {
  const pending = deferred()
  let attempts = 0
  const h = harness({ status: () => ++attempts === 1 ? pending.promise : Promise.resolve({ joined: true }) })
  const first = h.connection.refreshStatus()
  assert.equal(h.connection.refreshStatus(), first)
  assert.equal(attempts, 1)
  assert.deepEqual(h.events.statusBusy, [true])
  pending.reject(new Error('private database diagnostic'))
  await first
  assert.deepEqual(h.events.statuses, [])
  assert.equal(h.events.errors.at(-1).message, 'Impossible de charger cette partie. Réessayez.')
  await h.connection.refreshStatus()
  assert.deepEqual(h.events.statuses, [{ joined: true }])
  assert.equal(attempts, 2)
  assert.equal(h.events.errors.at(-1).message, '')
  assert.deepEqual(h.events.statusBusy, [true, false, true, false])
})

test('late waiting failures cannot replace the error of a newer screen', async () => {
  const pending = deferred()
  const h = harness({ status: () => pending.promise, list: () => Promise.reject(new Error('HTTP:NETWORK_ERROR')) })
  h.scope('waiting:first-seat')
  const waiting = h.connection.refreshStatus()
  h.connection.invalidate()
  h.scope('lobby:first-seat')
  await h.connection.refreshRooms()
  const errors = [...h.events.errors]
  pending.reject(new Error('ROOM_NOT_FOUND'))
  await waiting
  assert.deepEqual(h.events.errors, errors)
  assert.deepEqual(h.events.statuses, [])
})

test('an old waiting finally cannot clear a new waiting loading indicator', async () => {
  const old = deferred(), next = deferred()
  let calls = 0
  const h = harness({ status: () => calls++ === 0 ? old.promise : next.promise })
  const previous = h.connection.refreshStatus()
  h.connection.invalidate()
  const current = h.connection.refreshStatus()
  old.resolve({ joined: false })
  await previous
  assert.deepEqual(h.events.statusBusy, [true, false, true])
  next.resolve({ joined: true })
  await current
  assert.deepEqual(h.events.statusBusy, [true, false, true, false])
  assert.deepEqual(h.events.statuses, [{ joined: true }])
})

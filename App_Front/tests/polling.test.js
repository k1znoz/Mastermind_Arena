import test from 'node:test'
import assert from 'node:assert/strict'
import { createPolling } from '../src/lib/state/polling.js'

function clock() {
  let next = 0
  const pending = new Map()
  return { pending, schedule: callback => { pending.set(++next, callback); return next }, cancel: id => pending.delete(id),
    tick() { const [id, callback] = pending.entries().next().value; pending.delete(id); return callback() } }
}

test('start is idempotent and polling follows navigation without touching the welcome screen', async () => {
  const timer = clock()
  let view = 'welcome'
  const calls = []
  const poll = createPolling({ ...timer, getView: () => view, refreshGame: async () => { calls.push('game') },
    refreshWaiting: async () => { calls.push('waiting') }, refreshLobby: async () => { calls.push('lobby') } })
  poll.start(); poll.start()
  assert.equal(timer.pending.size, 1)
  for (const next of ['welcome', 'lobby', 'waiting', 'game']) { view = next; timer.tick(); await Promise.resolve(); await Promise.resolve() }
  assert.deepEqual(calls, ['lobby', 'waiting', 'game'])
  poll.stop()
  assert.equal(timer.pending.size, 0)
})

test('slow reads cannot accumulate ticks and stopped work cannot restart its timer', async () => {
  const timer = clock()
  let release
  const pending = new Promise(resolve => { release = resolve })
  const poll = createPolling({ ...timer, getView: () => 'game', refreshGame: () => pending,
    refreshWaiting: async () => {}, refreshLobby: async () => {} })
  poll.start()
  timer.tick()
  assert.equal(timer.pending.size, 0)
  poll.stop()
  release()
  await pending; await Promise.resolve()
  assert.equal(timer.pending.size, 0)
})

test('a rejected refresh is handled and the next tick remains scheduled', async () => {
  const timer = clock()
  const errors = []
  const poll = createPolling({ ...timer, getView: () => 'game', refreshGame: async () => { throw new Error('offline') },
    refreshWaiting: async () => {}, refreshLobby: async () => {}, onError: error => errors.push(error) })
  poll.start(); timer.tick()
  await Promise.resolve(); await Promise.resolve()
  assert.equal(errors.length, 1)
  assert.equal(timer.pending.size, 1)
  poll.stop()
})

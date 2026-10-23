import test from 'node:test'
import assert from 'node:assert/strict'
import { createRoomClient } from '../src/lib/api/roomClient.js'

test('every room operation forwards caller cancellation to the transport', async t => {
  t.mock.method(globalThis, 'fetch', async (_url, { signal }) => {
    assert.equal(signal.aborted, true)
    throw new Error('cancelled')
  })
  const client = createRoomClient({ baseUrl: 'http://test', apiKeyValue: 'test-only' })
  const controller = new AbortController()
  controller.abort()
  const options = { signal: controller.signal }
  for (const request of [() => client.list(options), () => client.status('board', options),
    () => client.create('Duel', 'Alice', '', options), () => client.join('board', 'Bob', '', options),
    () => client.resume('test-recovery', options)]) {
    await assert.rejects(request(), /REQUEST_ABORTED/)
  }
})

test('admission keeps routes and credentials in POST bodies, and status remains a public read', async t => {
  const requests = []
  const admission = { actorId: 'p1', token: 'test-private-seat', room: {
    roomId: 'board', name: 'Duel', hostPseudo: 'Alice', guestPseudo: null, joined: false, locked: false, createdAt: 1
  } }
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    requests.push({ url, ...options })
    const joined = { ...admission, actorId:'p2', room:{ ...admission.room, guestPseudo:'Bob', joined:true } }
    return Response.json(url.endsWith('/join') ? joined : options.method === 'POST' ? admission : url.includes('?') ? admission.room : { rooms: [admission.room] })
  })
  const client = createRoomClient({ baseUrl: 'http://test', apiKeyValue: 'test-only' })
  assert.deepEqual(await client.list(), [admission.room])
  assert.deepEqual(await client.status('board'), admission.room)
  await client.create('Duel', 'Alice', 'test-access')
  await client.join('board', 'Bob', 'test-access')
  await client.resume('board.p1.test-private-seat')
  assert.equal(requests[2].url, 'http://test/local/rooms')
  assert.deepEqual(JSON.parse(requests[2].body), { name: 'Duel', pseudo: 'Alice', accessCode: 'test-access' })
  assert.equal(requests[3].url, 'http://test/local/rooms/join')
  assert.deepEqual(JSON.parse(requests[3].body), { roomId: 'board', pseudo: 'Bob', accessCode: 'test-access' })
  assert.equal(requests[4].url, 'http://test/local/rooms/resume')
  assert.deepEqual(JSON.parse(requests[4].body), { recoveryCode: 'board.p1.test-private-seat' })
  assert.ok(requests.every(request => !request.url.includes('test-access') && !request.url.includes('test-recovery')))
})

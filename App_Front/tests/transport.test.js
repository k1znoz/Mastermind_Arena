import test from 'node:test'
import assert from 'node:assert/strict'
import { fetchWithTimeout } from '../src/lib/api/httpClientUtils.js'
import { createSubmitActionClient } from '../src/lib/api/submitActionClient.js'
import { createMatchStateClient } from '../src/lib/api/matchStateClient.js'
import { createRoomClient } from '../src/lib/api/roomClient.js'

test('read clients preserve their error codes and do not expose invalid response text', async t => {
  let response
  t.mock.method(globalThis, 'fetch', async () => response.clone())
  const room = createRoomClient({ baseUrl: 'http://test', apiKeyValue: 'test-only-strong-api-key' })
  const match = createMatchStateClient({ baseUrl: 'http://test', apiKeyValue: 'test-only-strong-api-key' })
  for (const text of ['', 'private upstream failure']) {
    response = new Response(text, { status: 503 })
    await assert.rejects(room.list(), { message: 'ROOM_HTTP_ERROR' })
    await assert.rejects(match.getMatchState('board', 'p1'), { message: 'HTTP_ERROR' })
  }
  response = Response.json({ code: 'ROOM_ACCESS_DENIED' }, { status: 403 })
  await assert.rejects(room.list(), { message: 'ROOM_ACCESS_DENIED' })
  await assert.rejects(match.getMatchState('board', 'p1'), { message: 'ROOM_ACCESS_DENIED' })
})

test('game clients reject the same missing or placeholder public API configuration', () => {
  for (const create of [createMatchStateClient, createSubmitActionClient]) {
    assert.throws(() => create({ apiKeyValue: '' }), /CLIENT:API_KEY_MISSING/)
    assert.throws(() => create({ apiKeyValue: 'change-me-api-key' }), /CLIENT:API_KEY_WEAK_DEFAULT/)
    assert.throws(() => create({ apiKeyHeaderName: '', apiKeyValue: 'test-only-strong-api-key' }), /CLIENT:API_KEY_HEADER_MISSING/)
  }
})

test('deadline covers a response body stalled after headers', async t => {
  t.mock.method(globalThis, 'fetch', async (_url, { signal }) => ({
    arrayBuffer: () => new Promise((_resolve, reject) => signal.addEventListener('abort', () => reject(new Error('aborted')), { once: true }))
  }))
  await assert.rejects(fetchWithTimeout('http://test/', {}, 20), /REQUEST_TIMEOUT/)
})

test('caller cancellation is distinct from a timeout', async t => {
  const controller = new AbortController()
  t.mock.method(globalThis, 'fetch', async (_url, { signal }) => {
    if (signal.aborted) throw new Error('cancelled')
  })
  controller.abort()
  await assert.rejects(fetchWithTimeout('http://test/', { signal: controller.signal }), /REQUEST_ABORTED/)
})

test('submit transport preserves explicit identity and rejects malformed responses', async t => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (_url, options) => {
    requests.push(JSON.parse(options.body))
    return new Response('not json', { status: 200 })
  })
  const client = createSubmitActionClient({ baseUrl: 'http://test', apiKeyValue: 'test-only-strong-api-key' })
  const operation = { matchId: 'board', actorId: 'p1', expectedVersion: 0,
    idempotencyKey: 'exact-test-operation', actionPayload: { actionType: 'READY_SECRET' } }
  await assert.rejects(client.submitAction(operation), /INVALID_RESPONSE_BODY/)
  assert.equal(requests[0].idempotencyKey, operation.idempotencyKey)
})

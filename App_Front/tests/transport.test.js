import test from 'node:test'
import assert from 'node:assert/strict'
import { fetchWithTimeout, requestJson } from '../src/lib/api/httpClientUtils.js'
import { createSubmitActionClient } from '../src/lib/api/submitActionClient.js'
import { createMatchStateClient } from '../src/lib/api/matchStateClient.js'
import { createRoomClient } from '../src/lib/api/roomClient.js'

test('room admissions recognize HTTP 429 without depending on a firewall response body', async t => {
  let body = ''
  let calls = 0
  t.mock.method(globalThis, 'fetch', async () => { calls++; return new Response(body, { status: 429 }) })
  const client = createRoomClient({ baseUrl: 'http://test', apiKeyValue: 'test-only-strong-api-key' })
  const operations = [() => client.create('Duel', 'Alice', ''),
    () => client.join('board', 'Bob', ''), () => client.resume('board.p1.test-token')]
  for (const raw of ['', '<html>Private upstream information</html>', '{"code":"ROOM_CODE_INVALID"}']) {
    body = raw
    for (const operation of operations) {
      const before = calls
      await assert.rejects(operation(), error =>
        error.message === 'ROOM_RATE_LIMITED' && !('body' in error) && !('responseBodyState' in error))
      assert.equal(calls, before + 1, 'a refused admission must not retry automatically')
    }
  }
})

test('successful empty and unreadable bodies remain replayable but are distinct from valid JSON', async t => {
  let text = ''
  t.mock.method(globalThis, 'fetch', async () => new Response(text))
  for (const [raw, state] of [['', 'empty'], [' \n ', 'empty'], ['private upstream payload', 'unreadable']]) {
    text = raw
    await assert.rejects(requestJson('http://test'), error =>
      error.message === 'HTTP:INVALID_RESPONSE_BODY' && error.responseBodyState === state && !('body' in error))
  }
  text = 'null'
  assert.equal((await requestJson('http://test')).body, null)
  text = '{"rooms":[]}'
  assert.deepEqual((await requestJson('http://test')).body, { rooms: [] })
})

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

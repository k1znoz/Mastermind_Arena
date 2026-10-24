import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { performance } from 'node:perf_hooks'
import { createRoomClient } from '../App_Front/src/lib/api/roomClient.js'
import { createSubmitActionClient } from '../App_Front/src/lib/api/submitActionClient.js'
import { validateMatchResponse } from '../App_Front/src/lib/api/responseContract.js'

// Synthetic load on a separately launched API backed by disposable PostgreSQL only.
const baseUrl = process.env.ARENA_INTEGRATION_HTTP_URL
const apiKeyValue = process.env.ARENA_INTEGRATION_API_KEY
assert.ok(baseUrl && apiKeyValue, 'Explicit integration URL and test API key required')
const url = new URL(baseUrl)
assert.ok(url.protocol === 'http:' && url.hostname === '127.0.0.1' && !url.username && !url.password
  && !url.search && !url.hash, 'Explicit loopback API required')
const config = { baseUrl, apiKeyValue }
const rooms = createRoomClient(config)
const host = await rooms.create('Long game measurement', 'Alice test', '')
const guest = await rooms.join(host.room.roomId, 'Bob test', '')
const id = host.room.roomId
const players = [host, guest].map(seat => ({ ...seat, client: createSubmitActionClient({ ...config, roomToken: seat.token }) }))
let version = 0
const submitTimes = []
async function send(player, actionType, payload = null, feedback = null) {
  const start = performance.now()
  const result = await player.client.submitAction({ matchId: id, actorId: player.actorId, expectedVersion: version,
    idempotencyKey: randomUUID(), actionPayload: { actionType, payload, feedback } })
  assert.equal(result.version, version + 1)
  version = result.version
  submitTimes.push(performance.now() - start)
}
const code = '["A","B","E","F"]', zero = '{"bienPlaces":0,"malPlaces":0}'
for (const player of players) await send(player, 'READY_SECRET', code)
const percentile = (values, fraction) => +[...values].sort((a, b) => a - b)[Math.ceil(values.length * fraction) - 1].toFixed(2)
let rounds = 0
for (const target of [0, 10, 30, 100]) {
  while (rounds < target) {
    for (const player of players) await send(player, 'PLAY_GUESS', code)
    for (const player of players) await send(player, 'SEND_FEEDBACK', null, zero)
    rounds++
  }
  const times = [], sizes = []
  for (let sample = 0; sample < 20; sample++) {
    for (const player of players) {
      const start = performance.now()
      const response = await fetch(`${baseUrl}/local/match-state?` + new URLSearchParams({matchId:id, actorId:player.actorId}),
        {headers:{'X-API-Key':apiKeyValue, 'X-Room-Token':player.token}, signal:AbortSignal.timeout(15000)})
      assert.equal(response.status, 200)
      const text = await response.text()
      const state = validateMatchResponse(JSON.parse(text), id)
      assert.ok(state.version === version && state.roundNumber === target && state.turns.length === 4 * target,
        'Long game state lost history or round/version identity')
      assert.ok(state.state === 'WAITING_GUESS' && state.opponentSecretCode.length === 0,
        'Long game changed rules or confidentiality')
      times.push(performance.now() - start)
      sizes.push(Buffer.byteLength(text))
    }
  }
  process.stdout.write(JSON.stringify({rounds, visibleActions:4 * rounds, samples:times.length,
    readMedianMs:percentile(times, .5), readP95Ms:percentile(times, .95), responseBytes:Math.max(...sizes),
    twoPlayersPayloadBytesPerMinute:20 * Math.max(...sizes)}) + '\n')
}
process.stdout.write(JSON.stringify({acceptedActions:submitTimes.length, submitMedianMs:percentile(submitTimes, .5),
  submitP95Ms:percentile(submitTimes, .95), note:'Local sequential measurements; not a capacity test; no game data printed'}) + '\n')

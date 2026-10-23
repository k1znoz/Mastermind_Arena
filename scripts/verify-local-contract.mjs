import assert from 'node:assert/strict'
import process from 'node:process'
import { randomUUID } from 'node:crypto'
import { createRoomClient } from '../App_Front/src/lib/api/roomClient.js'
import { createMatchStateClient } from '../App_Front/src/lib/api/matchStateClient.js'
import { createSubmitActionClient } from '../App_Front/src/lib/api/submitActionClient.js'

// Opt-in: point only at a separately launched API backed by a disposable database.
const baseUrl = process.env.ARENA_INTEGRATION_HTTP_URL
const apiKeyValue = process.env.ARENA_INTEGRATION_API_KEY
assert.ok(baseUrl && apiKeyValue, 'Explicit local integration URL and test API key are required')
const url = new URL(baseUrl)
assert.ok(url.protocol === 'http:' && ['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname)
  && !url.username && !url.password && !url.search && !url.hash, 'Integration requires a loopback HTTP API')
const config = { baseUrl, apiKeyValue }
const rooms = createRoomClient(config)
const host = await rooms.create(`Contract check ${randomUUID()}`, 'Alice test', '')
const id = host.room.roomId
assert.ok((await rooms.list()).some(room => room.roomId === id), 'Created room must be discoverable')
const guest = await rooms.join(id, 'Bob test', '')
assert.ok((await rooms.status(id)).joined, 'The room must have both players')
const actor = admission => ({
  id: admission.actorId,
  state: createMatchStateClient({ ...config, roomToken: admission.token }),
  actions: createSubmitActionClient({ ...config, roomToken: admission.token })
})
const p1 = actor(host), p2 = actor(guest)
const read = player => player.state.getMatchState(id, player.id)
const code = JSON.stringify(['A', 'B', 'E', 'F'])
const otherCode = JSON.stringify(['D', 'D', 'C', 'C'])
async function send(player, actionType, payload = null, feedback = null) {
  const state = await read(player)
  const request = { matchId:id, actorId:player.id, expectedVersion:state.version,
    idempotencyKey:randomUUID(), actionPayload:{actionType, payload, feedback} }
  await player.actions.submitAction(request)
  return { state:await read(player), request }
}
const ready = await send(p1, 'READY_SECRET', code)
await send(p2, 'READY_SECRET', otherCode)
await p1.actions.submitAction(ready.request)
assert.equal((await read(p1)).version, 2, 'Replaying an old accepted operation must not write again')
await assert.rejects(p1.actions.submitAction({ ...ready.request, idempotencyKey:randomUUID(),
  actionPayload:{actionType:'PLAY_GUESS',payload:code,feedback:null} }),
  error => error.code === 'VERSION_CONFLICT' && error.rejectionOrigin === 'ENGINE')
assert.ok((await read(p1)).opponentSecretCode.length === 0, 'Opponent secret stays hidden')
await send(p1, 'PLAY_GUESS', code)
assert.equal((await read(p2)).turns.length, 0, 'Pending guess stays hidden')
await send(p2, 'PLAY_GUESS', otherCode)
await send(p1, 'SEND_FEEDBACK', null, JSON.stringify({bienPlaces:0, malPlaces:0}))
assert.equal((await read(p2)).turns.length, 2, 'Pending feedback stays hidden')
const won = (await send(p2, 'SEND_FEEDBACK', null, JSON.stringify({bienPlaces:4, malPlaces:0}))).state
assert.equal(won.winnerId, 'p1')
assert.equal(won.scores.p1, 1)
assert.ok(won.requiredPlayers.includes('p2') && !won.requiredPlayers.includes('p1'), 'Only the loser keeps guessing')
const target = won.turns.find(action => action.actorId === 'p2' && action.actionType === 'SEND_FEEDBACK').version
const corrected = (await send(p2, 'CORRECT_FEEDBACK', JSON.stringify({feedbackVersion:target}),
  JSON.stringify({bienPlaces:3, malPlaces:0}))).state
assert.ok(!corrected.winnerId && (corrected.scores.p1 ?? 0) === 0, 'Correction must withdraw the point')
assert.equal(corrected.requiredPlayers.length, 2)
await send(p2, 'PLAY_GUESS', otherCode)
await send(p1, 'CONTEST_FEEDBACK', JSON.stringify({feedbackVersion:target}))
assert.ok(!(await read(p1)).turns.some(action => action.actorId === 'p2'
  && action.actionType === 'PLAY_GUESS' && action.turnNumber === 2), 'Notification must not reveal a pending guess')
await send(p2, 'ABANDON')
assert.ok(JSON.stringify((await read(p1)).opponentSecretCode) === otherCode, 'Abandonment reveals the opponent code')
await send(p1, 'REQUEST_REMATCH')
const rematch = (await send(p2, 'REQUEST_REMATCH')).state
assert.equal(rematch.gameNumber, 2)
assert.equal(rematch.scores.p1, 1)
assert.equal(rematch.state, 'PREPARATION')
const resumed = await rooms.resume(`${id}.p2.${guest.token}`)
assert.ok(resumed.actorId === 'p2' && resumed.token === guest.token, 'Recovery restores the same private seat')
assert.equal((await read(actor(resumed))).gameNumber, 2)
process.stdout.write('PASS: real frontend clients + local HTTP/JDBC; two players, replay, conflict, hidden actions, correction, contest, abandon, rematch, recovery.\n')

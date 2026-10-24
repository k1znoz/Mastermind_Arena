import assert from 'node:assert/strict'
import process from 'node:process'
import { randomUUID } from 'node:crypto'
import { createRoomClient } from '../App_Front/src/lib/api/roomClient.js'
import { createMatchStateClient } from '../App_Front/src/lib/api/matchStateClient.js'
import { createSubmitActionClient } from '../App_Front/src/lib/api/submitActionClient.js'
import { validateSubmitError } from '../App_Front/src/lib/api/responseContract.js'
import { apiErrorText } from '../App_Front/src/lib/utils/apiErrorText.js'

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
const actor = admission => ({
  id: admission.actorId,
  matchId: admission.room.roomId,
  token: admission.token,
  state: createMatchStateClient({ ...config, roomToken: admission.token }),
  actions: createSubmitActionClient({ ...config, roomToken: admission.token })
})
const p1 = actor(host)
const read = player => player.state.getMatchState(player.matchId, player.id)
const code = JSON.stringify(['A', 'B', 'E', 'F'])
const otherCode = JSON.stringify(['D', 'D', 'C', 'C'])
async function send(player, actionType, payload = null, feedback = null) {
  const state = await read(player)
  const request = { matchId:player.matchId, actorId:player.id, expectedVersion:state.version,
    idempotencyKey:randomUUID(), actionPayload:{actionType, payload, feedback} }
  await player.actions.submitAction(request)
  return { state:await read(player), request }
}
const rejections = new Set()
async function reject(player, actionType, expectedCode, status, payload = null, feedback = null) {
  const before = await read(player)
  const response = await fetch(player.actions.endpoint, {
    signal: AbortSignal.timeout(15000),
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-API-Key': apiKeyValue, 'X-Room-Token': player.token },
    body: JSON.stringify({ matchId: player.matchId, actorId: player.id, expectedVersion: before.version,
      idempotencyKey: randomUUID(), actionPayload: { actionType, payload, feedback } })
  })
  assert.ok(response.status === status, 'Unexpected rejection HTTP status')
  const metadata = validateSubmitError(await response.json(), player.matchId)
  assert.ok(metadata.code === expectedCode, 'Unexpected rejection code')
  assert.ok(metadata.rejectionOrigin === (expectedCode === 'ROOM_WAITING_FOR_PLAYER' ? 'HTTP' : 'ENGINE'), 'Wrong rejection origin')
  const error = Object.assign(new Error(`${metadata.rejectionOrigin}:${metadata.code}`), metadata)
  const beforeError = JSON.stringify({ message: error.message, ...error })
  Object.freeze(error)
  const message = apiErrorText(error, 'submit')
  assert.ok(message && message !== apiErrorText(null, 'submit'), 'Known rejection needs a dedicated message')
  assert.ok(JSON.stringify({ message: error.message, ...error }) === beforeError, 'Translation changed retry metadata')
  assert.ok(JSON.stringify(await read(player)) === JSON.stringify(before), 'Rejected action changed the player view')
  rejections.add(expectedCode)
}
await reject(p1, 'READY_SECRET', 'ROOM_WAITING_FOR_PLAYER', 409, code)
const guest = await rooms.join(id, 'Bob test', '')
const p2 = actor(guest)
assert.ok((await rooms.status(id)).joined, 'The room must have both players')
await reject(p1, 'ABANDON', 'ABANDON_REQUIRES_STARTED_GAME', 400)
await reject(p1, 'REQUEST_REMATCH', 'REMATCH_REQUIRES_FINISHED_MATCH', 400)
await reject(p1, 'PLAY_GUESS', 'INVALID_STATE_FOR_PLAY_GUESS', 400, code)
await reject(p1, 'SEND_FEEDBACK', 'INVALID_STATE_FOR_SEND_FEEDBACK', 400, null, '{"bienPlaces":0,"malPlaces":0}')
const ready = await send(p1, 'READY_SECRET', code)
await reject(p1, 'READY_SECRET', 'SECRET_ALREADY_SET', 400, otherCode)
await send(p2, 'READY_SECRET', otherCode)
await reject(p1, 'READY_SECRET', 'INVALID_STATE_FOR_READY_SECRET', 400, code)
await p1.actions.submitAction(ready.request)
assert.equal((await read(p1)).version, 2, 'Replaying an old accepted operation must not write again')
await assert.rejects(p1.actions.submitAction({ ...ready.request, idempotencyKey:randomUUID(),
  actionPayload:{actionType:'PLAY_GUESS',payload:code,feedback:null} }),
  error => error.code === 'VERSION_CONFLICT' && error.rejectionOrigin === 'ENGINE')
assert.ok((await read(p1)).opponentSecretCode.length === 0, 'Opponent secret stays hidden')
await send(p1, 'PLAY_GUESS', code)
await reject(p1, 'PLAY_GUESS', 'GUESS_ALREADY_SUBMITTED', 409, otherCode)
assert.equal((await read(p2)).turns.length, 0, 'Pending guess stays hidden')
await send(p2, 'PLAY_GUESS', otherCode)
await send(p1, 'SEND_FEEDBACK', null, JSON.stringify({bienPlaces:0, malPlaces:0}))
await reject(p1, 'SEND_FEEDBACK', 'FEEDBACK_ALREADY_SUBMITTED', 409, null, '{"bienPlaces":0,"malPlaces":0}')
assert.equal((await read(p2)).turns.length, 2, 'Pending feedback stays hidden')
const won = (await send(p2, 'SEND_FEEDBACK', null, JSON.stringify({bienPlaces:4, malPlaces:0}))).state
assert.equal(won.winnerId, 'p1')
assert.equal(won.scores.p1, 1)
assert.ok(won.requiredPlayers.includes('p2') && !won.requiredPlayers.includes('p1'), 'Only the loser keeps guessing')
const target = won.turns.find(action => action.actorId === 'p2' && action.actionType === 'SEND_FEEDBACK').version
await reject(p1, 'PLAY_GUESS', 'PLAYER_ALREADY_SOLVED', 400, code)
const corrected = (await send(p2, 'CORRECT_FEEDBACK', JSON.stringify({feedbackVersion:target}),
  JSON.stringify({bienPlaces:3, malPlaces:0}))).state
assert.ok(!corrected.winnerId && (corrected.scores.p1 ?? 0) === 0, 'Correction must withdraw the point')
assert.equal(corrected.requiredPlayers.length, 2)
await send(p2, 'PLAY_GUESS', otherCode)
await send(p1, 'CONTEST_FEEDBACK', JSON.stringify({feedbackVersion:target}))
await reject(p1, 'CONTEST_FEEDBACK', 'FEEDBACK_ALREADY_CONTESTED', 400, JSON.stringify({feedbackVersion:target}))
assert.ok(!(await read(p1)).turns.some(action => action.actorId === 'p2'
  && action.actionType === 'PLAY_GUESS' && action.turnNumber === 2), 'Notification must not reveal a pending guess')
await send(p2, 'ABANDON')
await reject(p1, 'PLAY_GUESS', 'MATCH_ALREADY_TERMINAL', 409, code)
assert.ok(JSON.stringify((await read(p1)).opponentSecretCode) === otherCode, 'Abandonment reveals the opponent code')
await send(p1, 'REQUEST_REMATCH')
await reject(p1, 'REQUEST_REMATCH', 'REMATCH_ALREADY_REQUESTED', 400)
const rematch = (await send(p2, 'REQUEST_REMATCH')).state
assert.equal(rematch.gameNumber, 2)
assert.equal(rematch.scores.p1, 1)
assert.equal(rematch.state, 'PREPARATION')
await reject(p1, 'CONTEST_FEEDBACK', 'FEEDBACK_NOT_EDITABLE', 400, JSON.stringify({feedbackVersion:target}))
await reject(p2, 'CORRECT_FEEDBACK', 'FEEDBACK_NOT_EDITABLE', 400, JSON.stringify({feedbackVersion:target}), '{"bienPlaces":0,"malPlaces":0}')
const resumed = await rooms.resume(`${id}.p2.${guest.token}`)
assert.ok(resumed.actorId === 'p2' && resumed.token === guest.token, 'Recovery restores the same private seat')
assert.equal((await read(actor(resumed))).gameNumber, 2)
// Separate continuation: completing another round must not invalidate the correction target above.
const extraHost = await rooms.create('Continuation rejection check', 'Alice test', '')
const extraGuest = await rooms.join(extraHost.room.roomId, 'Bob test', '')
const winner = actor(extraHost), searching = actor(extraGuest)
await send(winner, 'READY_SECRET', code)
await send(searching, 'READY_SECRET', otherCode)
await send(winner, 'PLAY_GUESS', code)
await send(searching, 'PLAY_GUESS', otherCode)
await send(winner, 'SEND_FEEDBACK', null, '{"bienPlaces":0,"malPlaces":0}')
await send(searching, 'SEND_FEEDBACK', null, '{"bienPlaces":4,"malPlaces":0}')
await send(searching, 'PLAY_GUESS', otherCode)
await reject(searching, 'SEND_FEEDBACK', 'FEEDBACK_NOT_REQUIRED', 400, null, '{"bienPlaces":0,"malPlaces":0}')
assert.equal(rejections.size, 15, 'All approved rejection messages must be exercised')
process.stdout.write('PASS: real HTTP/JDBC + frontend clients; 15 rejection codes/statuses/messages, unchanged player views, replay, privacy, correction, contest, abandon, rematch, recovery.\n')

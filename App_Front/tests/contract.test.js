import test from 'node:test'
import assert from 'node:assert/strict'
import { validateMatchResponse, validateAdmission } from '../src/lib/api/responseContract.js'
import { readFile } from 'node:fs/promises'
import { createMatchStateClient } from '../src/lib/api/matchStateClient.js'

const state = () => ({ matchId: 'board', state: 'WAITING_GUESS', version: 2, roundNumber: 0, gameNumber: 1,
  players: ['p1', 'p2'], readyPlayers: ['p1', 'p2'], submittedPlayers: [], requiredPlayers: ['p1', 'p2'],
  solvedPlayers: [], rematchRequestedPlayers: [], visibleSecretCode: ['A', 'B', 'E', 'F'], opponentSecretCode: [],
  turns: [], scores: {}, winnerId: null })

test('frontend consumes the exact preparation fixture asserted by the Java mapper', async t => {
  const raw = await readFile(new URL('../../deduction-engine/src/test/resources/contracts/match-preparation.json', import.meta.url), 'utf8')
  t.mock.method(globalThis, 'fetch', async () => new Response(raw, { status: 200 }))
  const client = createMatchStateClient({ baseUrl: 'http://test', apiKeyValue: 'test-only-strong-api-key' })
  const result = await client.getMatchState('contract-room', 'p1')
  assert.equal(result.version, 0)
  assert.equal(result.roundNumber, 0)
  assert.equal(result.gameNumber, 1)
  assert.deepEqual(result.requiredPlayers, ['p1', 'p2'])
  assert.deepEqual(result.visibleSecretCode, [])
})

test('current state contract distinguishes version, round and game', () => {
  const value = state()
  value.turns = [{ actorId: 'p1', actionType: 'PLAY_GUESS', version: 2, turnNumber: 1,
    timestamp: 100, guess: '["A","B","C","D"]', feedback: null }]
  assert.equal(validateMatchResponse(value, 'board'), value)
})

test('malformed, wrong-match and incomplete states cannot masquerade as an empty board', () => {
  for (const patch of [{ matchId: 'other' }, { version: '2' }, { version: -1 }, { state: 'UP' },
    { requiredPlayers: ['p3'] }, { scores: { p1: -1 } }, { visibleSecretCode: ['G', 'A', 'B', 'C'] },
    { turns: [{}] }, { gameNumber: 0 }, { winnerId: 'p3' }]) {
    assert.throws(() => validateMatchResponse({ ...state(), ...patch }, 'board'), /INVALID_MATCH_RESPONSE/)
  }
  assert.throws(() => validateMatchResponse({}, 'board'), /INVALID_MATCH_RESPONSE/)
})

test('admission validates private identity rather than accepting arbitrary successful JSON', () => {
  const value = { actorId: 'p2', token: 'private', room: { roomId: 'board', name: 'Duel', hostPseudo: 'Alice', guestPseudo: 'Bob', joined: true, locked: false, createdAt: 1 } }
  assert.equal(validateAdmission(value), value)
  assert.throws(() => validateAdmission({ ...value, actorId: 'p3' }), /INVALID_ADMISSION/)
  assert.throws(() => validateAdmission({ ...value, token: '' }), /INVALID_ADMISSION/)
})

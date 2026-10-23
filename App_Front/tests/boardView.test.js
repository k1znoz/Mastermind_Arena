import test from 'node:test'
import assert from 'node:assert/strict'
import { createBoardDraft, selectDraftSlot, placeDraftColor, completeDraft } from '../src/lib/state/boardDraft.js'
import { deriveGameView } from '../src/lib/state/gameView.js'

const snapshot = (overrides = {}) => ({ matchId: 'board', state: 'PREPARATION', players: ['p1', 'p2'], version: 0,
  gameNumber: 1, roundNumber: 0, turns: [], readyPlayers: [], requiredPlayers: ['p1', 'p2'],
  visibleSecretCode: [], submittedPlayers: [], solvedPlayers: [], rematchRequestedPlayers: [], winnerId: null, ...overrides })
const ready = () => snapshot({ state: 'WAITING_GUESS', readyPlayers: ['p1', 'p2'], visibleSecretCode: ['A', 'B', 'C', 'D'] })
const fullDraft = () => ({ slots: ['A', 'B', 'E', 'F'], selected: 3 })

test('filling the last case first retains its position and requires the other three', () => {
  const original = createBoardDraft()
  let draft = placeDraftColor(selectDraftSlot(original, 3), 'F')
  assert.deepEqual(draft.slots, ['', '', '', 'F'])
  assert.equal(completeDraft(draft.slots), null)
  assert.deepEqual(original.slots, ['', '', '', ''])
  draft = selectDraftSlot(draft, 0)
  for (const color of ['A', 'A', 'E']) draft = placeDraftColor(draft, color)
  assert.deepEqual(completeDraft(draft.slots), ['A', 'A', 'E', 'F'])
  assert.equal(selectDraftSlot(draft, -1), draft)
  assert.equal(placeDraftColor(draft, 'G'), draft)
})

test('confirmation needs a complete draft and an available server', () => {
  assert.equal(deriveGameView({ state: snapshot(), syncStatus: 'ok' }).canSubmitSecret, false)
  assert.equal(deriveGameView({ state: snapshot(), setupDraft: fullDraft(), syncStatus: 'ok' }).canSubmitSecret, true)
  assert.equal(deriveGameView({ state: snapshot(), setupDraft: fullDraft(), syncStatus: 'error' }).canSubmitSecret, false)
  assert.equal(deriveGameView({ state: snapshot({ readyPlayers: ['p1'] }), setupDraft: fullDraft() }).canSubmitSecret, false)
})

test('a submitted guess stops its owner while the other player can still propose', () => {
  const state = { ...ready(), turns: [{ actionType: 'PLAY_GUESS', actorId: 'p1', turnNumber: 1 }] }
  assert.equal(deriveGameView({ state, guessDraft: fullDraft() }).canSubmitGuess, false)
  assert.equal(deriveGameView({ state, actorId: 'p2', guessDraft: fullDraft() }).canSubmitGuess, true)
  assert.equal(deriveGameView({ state, actorId: 'p2' }).canSubmitGuess, false)
})

test('feedback phase and already-submitted flags authorize only the required giver', () => {
  const state = { ...ready(), state: 'WAITING_FEEDBACK', requiredPlayers: ['p1'], submittedPlayers: [] }
  assert.equal(deriveGameView({ state }).canRespond, true)
  assert.equal(deriveGameView({ state, actorId: 'p2' }).canRespond, false)
  assert.equal(deriveGameView({ state: { ...state, submittedPlayers: ['p1'] } }).canRespond, false)
})

test('winner cannot guess again while loser continues, and correction reopens both views', () => {
  const won = { ...ready(), roundNumber: 1, winnerId: 'p1', solvedPlayers: ['p1'], requiredPlayers: ['p2'] }
  assert.equal(deriveGameView({ state: won, guessDraft: fullDraft() }).canSubmitGuess, false)
  assert.equal(deriveGameView({ state: won, actorId: 'p2', guessDraft: fullDraft() }).canSubmitGuess, true)
  const corrected = { ...won, winnerId: null, solvedPlayers: [], requiredPlayers: ['p1', 'p2'] }
  assert.equal(deriveGameView({ state: corrected, guessDraft: fullDraft() }).canSubmitGuess, true)
  assert.equal(deriveGameView({ state: corrected }).result, '')
})

test('draw, abandonment round and rematch use server facts without calculating an outcome', () => {
  const finished = { ...ready(), state: 'FINISHED', roundNumber: 1, winnerId: 'DRAW', requiredPlayers: [], rematchRequestedPlayers: ['p2'] }
  const view = deriveGameView({ state: finished })
  assert.equal(view.result, 'Égalité dans le duel')
  assert.equal(view.displayRound, 1)
  assert.equal(view.theirRematchAsked, true)
  assert.equal(deriveGameView({ state: { ...finished, turns: [{ actionType: 'ABANDON', actorId: 'p2' }] } }).displayRound, 2)
  const restarted = deriveGameView({ state: snapshot({ gameNumber: 2 }) })
  assert.equal(restarted.result, '')
  assert.deepEqual(restarted.setupSlots, ['_', '_', '_', '_'])
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { createBoardDraft, selectDraftSlot, placeDraftColor, completeDraft } from '../src/lib/state/boardDraft.js'
import { deriveGameView } from '../src/lib/state/gameView.js'

const snapshot = (overrides = {}) => ({ matchId: 'board', state: 'PREPARATION', players: ['p1', 'p2'], version: 0,
  gameNumber: 1, roundNumber: 0, turns: [], readyPlayers: [], requiredPlayers: ['p1', 'p2'],
  visibleSecretCode: [], submittedPlayers: [], solvedPlayers: [], rematchRequestedPlayers: [], winnerId: null, ...overrides })
const ready = () => snapshot({ state: 'WAITING_GUESS', readyPlayers: ['p1', 'p2'], visibleSecretCode: ['A', 'B', 'C', 'D'] })
const fullDraft = () => ({ slots: ['A', 'B', 'E', 'F'], selected: 3 })

test('submission status distinguishes saving, sending and waiting for the opponent', () => {
  const pending = action => deriveGameView({ state: snapshot(), submitStatus: 'loading', pendingActionType: action }).submissionMessage
  assert.equal(pending('READY_SECRET'), 'Envoi du code…')
  assert.equal(pending('PLAY_GUESS'), 'Envoi de l’essai…')
  assert.equal(pending('SEND_FEEDBACK'), 'Envoi des indices…')
  assert.equal(pending('CORRECT_FEEDBACK'), 'Enregistrement de la correction…')
  assert.equal(pending('REQUEST_REMATCH'), 'Envoi de la demande de revanche…')
  assert.equal(deriveGameView({ state: snapshot(), submitStatus: 'success', pendingActionType: 'REQUEST_REMATCH' }).submissionMessage, '')
})

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

test('research summaries trust solved flags and only display an authorized published guess', () => {
  const turns = [
    { actionType: 'PLAY_GUESS', actorId: 'p1', turnNumber: 1, guess: ['A','A','C','B'] },
    { actionType: 'SEND_FEEDBACK', actorId: 'p2', turnNumber: 1, feedback: ['BLACK','BLACK','BLACK','BLACK'] },
    { actionType: 'PLAY_GUESS', actorId: 'p2', turnNumber: 2, guess: ['F','F','F','F'] },
    { actionType: 'SEND_FEEDBACK', actorId: 'p1', turnNumber: 2, feedback: ['BLACK','BLACK','BLACK','BLACK'] }
  ]
  const state = { ...ready(), turns, roundNumber: 1, winnerId: 'p1', solvedPlayers: ['p1'] }
  const summaries = deriveGameView({state, playerName:'Alice', roomInfo:{guestPseudo:'Bob'}}).researchSummaries
  assert.equal(summaries[0].name, 'Alice')
  assert.equal(summaries[0].turnNumber, 1)
  assert.deepEqual(summaries[0].guess, ['A','A','C','B'])
  assert.equal(summaries[1].solved, false)
  assert.deepEqual(summaries[1].guess, [], 'private hints must not anticipate a server discovery')
  const reopened = deriveGameView({state:{...state, winnerId:null,solvedPlayers:[]}}).researchSummaries
  assert.ok(reopened.every(research => !research.solved && research.guess.length === 0))
})

test('abandonment and missing discovery records never fabricate a result combination', () => {
  const state = snapshot({state:'FINISHED',solvedPlayers:['p2'],winnerId:'p2',turns:[{actionType:'ABANDON',actorId:'p1'}]})
  const summaries = deriveGameView({state,actorId:'p2',playerName:'Bob',roomInfo:{hostPseudo:'Alice'}}).researchSummaries
  assert.equal(summaries[0].name,'Alice')
  assert.equal(summaries[0].abandoned,true)
  assert.deepEqual(summaries[0].guess,[])
  assert.equal(summaries[1].winner,true)
  assert.equal(summaries[1].turnNumber,null)
  assert.deepEqual(summaries[1].guess,[])
})

test('persistent notices are not silently limited to the last three events', () => {
  const turns = Array.from({length:5},(_,index)=>({actionType:'CONTEST_FEEDBACK',version:index+1}))
  assert.deepEqual(deriveGameView({state:snapshot({turns})}).notices.map(entry=>entry.version),[5,4,3,2,1])
})

import { ALLOWED_SYMBOLS, CODE_LENGTH } from '../utils/symbolSequence.js'
import { completeDraft, createBoardDraft } from './boardDraft.js'

/** Pure projection of an authorized server snapshot. Never calculates feedback or awards points.
 * @param {*} [options]
 */
export function deriveGameView(options = {}) {
  const { state = null, actorId = 'p1', syncStatus = 'idle', submitStatus = 'idle', pendingActionType = '',
    setupDraft = createBoardDraft(), guessDraft = createBoardDraft(), playerName = '', roomInfo = null } = options
  const turns = state?.turns ?? []
  const round = state?.roundNumber ?? 0
  const opponentActorId = (state?.players ?? []).find(actor => actor !== actorId) ?? null
  const opponentName = (actorId === 'p1' ? roomInfo?.guestPseudo : roomInfo?.hostPseudo) || 'Adversaire'
  const hasSecretSet = (state?.readyPlayers ?? []).includes(actorId) || state?.visibleSecretCode?.length === CODE_LENGTH
  const hasOpponentReady = Boolean(opponentActorId && (state?.readyPlayers ?? []).includes(opponentActorId))
  const isBackendReady = syncStatus !== 'error' && Boolean(state)
  const required = (state?.requiredPlayers ?? []).includes(actorId)
  const acted = type => turns.some(entry => entry.actionType === type && entry.actorId === actorId && entry.turnNumber === round + 1)
  const ownGuessSubmitted = acted('PLAY_GUESS')
  const ownFeedbackSubmitted = acted('SEND_FEEDBACK')
  const isMyTurn = isBackendReady && state?.state === 'WAITING_GUESS' && required && !ownGuessSubmitted
  const canRespond = isBackendReady && state?.state === 'WAITING_FEEDBACK' && required
    && !ownFeedbackSubmitted && !(state?.submittedPlayers ?? []).includes(actorId)
  const isSecretSubmitting = submitStatus === 'loading' && pendingActionType === 'READY_SECRET'
  const isGuessSubmitting = submitStatus === 'loading' && pendingActionType === 'PLAY_GUESS'
  const byActor = (type, actor) => turns.filter(entry => entry.actionType === type && entry.actorId === actor)
  const ownGuesses = byActor('PLAY_GUESS', actorId)
  const opponentGuesses = byActor('PLAY_GUESS', opponentActorId)
  const finished = state?.state === 'FINISHED'
  const displayPlayerName = playerName.trim() || (actorId === 'p2' ? 'Joueur 2' : 'Joueur 1')
  const researchSummaries = (state?.players ?? []).map(player => {
    const solved = (state?.solvedPlayers ?? []).includes(player)
    // Solved status comes from the server. Effective hints only locate its published attempt.
    const discovery = solved ? turns.filter(entry => entry.actionType === 'SEND_FEEDBACK'
      && entry.actorId !== player && entry.feedback?.length === CODE_LENGTH
      && entry.feedback.every(peg => peg === 'BLACK')).sort((a, b) => a.turnNumber - b.turnNumber)[0] : null
    const foundGuess = discovery ? turns.find(entry => entry.actionType === 'PLAY_GUESS'
      && entry.actorId === player && entry.turnNumber === discovery.turnNumber) : null
    return { actorId: player, name: player === actorId ? displayPlayerName : opponentName, solved,
      winner: state?.winnerId === player, turnNumber: discovery?.turnNumber ?? null,
      guess: foundGuess?.guess ?? [], abandoned: turns.some(entry => entry.actionType === 'ABANDON' && entry.actorId === player) }
  })

  return {
    actorId, opponentActorId, opponentName, turns, round, finished,
    researchSummaries,
    matchState: state,
    phaseKey: `${state?.matchId}:${state?.gameNumber}:${state?.state}`,
    feedbackKey: `${state?.matchId}:${state?.gameNumber}:${round}`,
    preparation: state?.state === 'PREPARATION',
    ownGuesses, opponentGuesses,
    given: byActor('SEND_FEEDBACK', actorId), received: byActor('SEND_FEEDBACK', opponentActorId),
    currentOpponentGuess: opponentGuesses.find(entry => entry.turnNumber === round + 1),
    mySolved: (state?.solvedPlayers ?? []).includes(actorId),
    theirSolved: (state?.solvedPlayers ?? []).includes(opponentActorId),
    myRematchAsked: (state?.rematchRequestedPlayers ?? []).includes(actorId),
    theirRematchAsked: (state?.rematchRequestedPlayers ?? []).includes(opponentActorId),
    notices: turns.filter(entry => ['CORRECT_FEEDBACK', 'CONTEST_FEEDBACK'].includes(entry.actionType)).reverse(),
    result: state?.winnerId === 'DRAW' ? 'Égalité dans le duel' : state?.winnerId === actorId
      ? 'Vous avez remporté le duel' : state?.winnerId ? `${opponentName} a remporté le duel` : '',
    isPrototype: !isBackendReady, isBackendReady, isMyTurn, canRespond, hasSecretSet, hasOpponentReady,
    ownGuessSubmitted, ownFeedbackSubmitted,
    opponentStepDone: Boolean(opponentActorId && (state?.submittedPlayers ?? []).includes(opponentActorId)),
    busy: submitStatus === 'loading', isSecretSubmitting, isGuessSubmitting, pendingActionType,
    submissionMessage: submitStatus === 'loading' ? {
      READY_SECRET: 'Envoi du code…', PLAY_GUESS: 'Envoi de l’essai…', SEND_FEEDBACK: 'Envoi des indices…',
      CORRECT_FEEDBACK: 'Enregistrement de la correction…', REQUEST_REMATCH: 'Envoi de la demande de revanche…',
      CONTEST_FEEDBACK: 'Envoi de la demande de vérification…', ABANDON: 'Envoi de l’abandon…'
    }[pendingActionType] ?? 'Envoi…' : '',
    setupSequence: setupDraft.slots,
    setupSlots: Array.from({ length: CODE_LENGTH }, (_, index) =>
      (hasSecretSet && state?.visibleSecretCode?.length === CODE_LENGTH ? state.visibleSecretCode : setupDraft.slots)[index] || '_'),
    selectedSetupSlot: setupDraft.selected, draftGuess: guessDraft.slots, guessSequence: guessDraft.slots,
    selectedGuessSlot: guessDraft.selected, symbolPalette: [...ALLOWED_SYMBOLS],
    canSubmitSecret: isBackendReady && state?.state === 'PREPARATION' && !hasSecretSet && !isSecretSubmitting && Boolean(completeDraft(setupDraft.slots)),
    canSubmitGuess: isMyTurn && hasSecretSet && hasOpponentReady && !isGuessSubmitting && Boolean(completeDraft(guessDraft.slots)),
    secretButtonLabel: isSecretSubmitting ? 'TRANSMISSION...' : hasSecretSet ? 'CODE DEJA VALIDE'
      : state?.state !== 'PREPARATION' ? 'PREPARATION TERMINEE' : 'VALIDER CODE SECRET',
    sendButtonLabel: !isBackendReady ? 'BACKEND HORS LIGNE' : !hasSecretSet || !hasOpponentReady ? 'VALIDEZ LES DEUX CODES'
      : state?.state === 'WAITING_FEEDBACK' ? 'REPONDEZ A L ADVERSAIRE' : ownGuessSubmitted ? 'TENTATIVE ENVOYEE'
        : !completeDraft(guessDraft.slots) ? 'SEQUENCE INCOMPLETE' : 'ENVOYER TENTATIVE',
    historyRows: turns.slice().reverse(),
    displayPlayerName,
    displayRound: finished && !turns.some(entry => entry.actionType === 'ABANDON') ? Math.max(1, round) : round + 1
  }
}

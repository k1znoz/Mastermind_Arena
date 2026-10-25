const STATES = ['PREPARATION', 'WAITING_GUESS', 'WAITING_FEEDBACK', 'FINISHED']
const ACTORS = ['p1', 'p2']
const ACTIONS = ['PLAY_GUESS', 'SEND_FEEDBACK', 'CORRECT_FEEDBACK', 'CONTEST_FEEDBACK', 'ABANDON', 'REQUEST_REMATCH']
const integer = value => Number.isSafeInteger(value) && value >= 0
const actors = value => Array.isArray(value) && value.every(actor => ACTORS.includes(actor)) && new Set(value).size === value.length
const colors = value => Array.isArray(value) && [0, 4].includes(value.length) && value.every(color => typeof color === 'string' && /^[A-F]$/.test(color))

/** Decode only the documented nested JSON; invalid data must never become an empty guess or zero hints.
 * @param {*} action
 */
export function decodeTurnContent(action) {
  const invalid = () => { throw new Error('HTTP:INVALID_MATCH_RESPONSE') }
  const parse = raw => {
    if (typeof raw !== 'string') return invalid()
    try { return JSON.parse(raw) } catch { return invalid() }
  }
  const decodeFeedback = raw => {
    const counts = parse(raw)
    if (!counts || !integer(counts.bienPlaces) || !integer(counts.malPlaces)
        || counts.bienPlaces + counts.malPlaces > 4) invalid()
    return [...Array(counts.bienPlaces).fill('BLACK'), ...Array(counts.malPlaces).fill('WHITE')]
  }
  let guess = [], feedback = [], feedbackVersion = 0
  if (action.actionType === 'PLAY_GUESS') {
    guess = parse(action.guess)
    if (!colors(guess) || guess.length !== 4 || action.feedback !== null) invalid()
  } else if (['SEND_FEEDBACK', 'CORRECT_FEEDBACK', 'CONTEST_FEEDBACK'].includes(action.actionType)) {
    if (action.actionType !== 'SEND_FEEDBACK') {
      const target = parse(action.guess)
      if (!target || !integer(target.feedbackVersion) || target.feedbackVersion < 1 || target.feedbackVersion >= action.version) invalid()
      feedbackVersion = target.feedbackVersion
    } else if (action.guess !== null) invalid()
    if (action.actionType !== 'CONTEST_FEEDBACK') {
      feedback = decodeFeedback(action.feedback)
    } else if (action.feedback !== null) invalid()
  } else if (!['ABANDON', 'REQUEST_REMATCH'].includes(action.actionType) || action.guess !== null || action.feedback !== null) invalid()
  if (action.originalFeedback != null) {
    if (action.actionType !== 'SEND_FEEDBACK') invalid()
    return { guess, feedback, feedbackVersion, originalFeedback: decodeFeedback(action.originalFeedback) }
  }
  return { guess, feedback, feedbackVersion }
}

/** @param {*} value @param {string} matchId */
export function validateMatchResponse(value, matchId) {
  if (!value || value.matchId !== matchId || !STATES.includes(value.state) || !integer(value.version)
      || !integer(value.roundNumber) || !integer(value.gameNumber) || value.gameNumber < 1
      || !actors(value.players) || value.players.length !== 2
      || !['readyPlayers', 'submittedPlayers', 'requiredPlayers', 'solvedPlayers', 'rematchRequestedPlayers'].every(key => actors(value[key]))
      || !colors(value.visibleSecretCode) || !colors(value.opponentSecretCode)
      || !Array.isArray(value.turns) || !value.turns.every(action => action && ACTORS.includes(action.actorId)
        && ACTIONS.includes(action.actionType) && integer(action.version) && action.version <= value.version
        && integer(action.turnNumber) && action.turnNumber >= 1 && integer(action.timestamp)
        && (action.guess === null || typeof action.guess === 'string')
        && (action.feedback === null || typeof action.feedback === 'string'))
      || !value.scores || typeof value.scores !== 'object' || Array.isArray(value.scores)
      || !Object.entries(value.scores).every(([actor, score]) => ACTORS.includes(actor) && integer(score))
      || !(value.winnerId === null || [...ACTORS, 'DRAW'].includes(value.winnerId))) {
    throw new Error('HTTP:INVALID_MATCH_RESPONSE')
  }
  value.turns.forEach(decodeTurnContent)
  return value
}

const errorCode = value => typeof value === 'string' && /^[A-Z][A-Z0-9_]*$/.test(value)

/** Validate error metadata before it can trigger a version-conflict retry.
 * @param {*} body @param {string} matchId
 */
export function validateSubmitError(body, matchId) {
  const invalid = () => { throw new Error('HTTP:INVALID_ERROR_RESPONSE') }
  if (!body || typeof body !== 'object' || Array.isArray(body)) return invalid()
  if (!Object.hasOwn(body, 'accepted')) {
    if (!errorCode(body.code)) return invalid()
    return { code: body.code, rejectionOrigin: 'HTTP' }
  }
  if (body.accepted !== false || !['ENGINE', 'RULESET'].includes(body.rejectionOrigin) || !errorCode(body.rejectionCode)
      || !Array.isArray(body.emittedEvents) || !body.emittedEvents.every(event => typeof event === 'string')) return invalid()
  const result = { code: body.rejectionCode, rejectionOrigin: body.rejectionOrigin }
  if (body.matchId === null && body.version === null && body.status === null) return result
  if (body.matchId !== matchId || !integer(body.version) || !STATES.includes(body.status)) return invalid()
  return { ...result, version: body.version, matchStatus: body.status }
}

/** @param {*} value @param {{roomId?: string, actorId?: string, token?: string}} [expected] */
export function validateAdmission(value, expected = {}) {
  if (!value || !ACTORS.includes(value.actorId) || typeof value.token !== 'string' || !value.token
      || !value.room || typeof value.room.roomId !== 'string' || !value.room.roomId
      || typeof value.room.name !== 'string' || typeof value.room.hostPseudo !== 'string'
      || !(value.room.guestPseudo === null || typeof value.room.guestPseudo === 'string')
      || typeof value.room.joined !== 'boolean'
      || (expected.roomId !== undefined && value.room.roomId !== expected.roomId)
      || (expected.actorId !== undefined && value.actorId !== expected.actorId)
      || (expected.token !== undefined && value.token !== expected.token)
      || (value.actorId === 'p2' && !value.room.joined)) throw new Error('HTTP:INVALID_ADMISSION_RESPONSE')
  validateRoomInfo(value.room)
  return value
}

/** @param {*} value @param {string} [roomId] */
export function validateRoomInfo(value, roomId) {
  if (!value || typeof value.roomId !== 'string' || !value.roomId.trim()
      || (roomId !== undefined && value.roomId !== roomId)
      || typeof value.name !== 'string' || !value.name.trim()
      || typeof value.hostPseudo !== 'string' || !value.hostPseudo.trim()
      || !(value.guestPseudo === null || (typeof value.guestPseudo === 'string' && !!value.guestPseudo.trim()))
      || typeof value.joined !== 'boolean' || value.joined !== (value.guestPseudo !== null)
      || typeof value.locked !== 'boolean' || !integer(value.createdAt)) {
    throw new Error('HTTP:INVALID_ROOM_RESPONSE')
  }
  return value
}

/** @param {*} body */
export function validateRoomList(body) {
  if (!body || !Array.isArray(body.rooms)) throw new Error('HTTP:INVALID_ROOM_RESPONSE')
  const ids = new Set()
  for (const room of body.rooms) {
    validateRoomInfo(room)
    if (room.joined || ids.has(room.roomId)) throw new Error('HTTP:INVALID_ROOM_RESPONSE')
    ids.add(room.roomId)
  }
  return body.rooms
}

/** @param {*} body @param {string} matchId @param {number} expectedVersion */
export function validateSubmitResponse(body, matchId, expectedVersion) {
  if (!body || body.accepted !== true || body.matchId !== matchId
      || !integer(body.version) || body.version <= expectedVersion || !STATES.includes(body.status)
      || body.rejectionCode !== null || body.rejectionOrigin !== null
      || !Array.isArray(body.emittedEvents) || !body.emittedEvents.every(event => typeof event === 'string')) {
    throw new Error('HTTP:INVALID_RESPONSE_BODY')
  }
  return body
}

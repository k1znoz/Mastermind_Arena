import { buildUrl, requestJson, assertAuthConfig, readErrorCode } from './httpClientUtils.js'
import { validateMatchResponse, decodeTurnContent } from './responseContract.js'

/**
 * Selects a path default depending on access mode.
 * If baseUrl is set, frontend calls backend directly and must not use '/api' prefix.
 * If baseUrl is empty, Vite local proxy path is expected.
 * @param {string} baseUrl
 * @param {string | undefined} explicitPath
 * @returns {string}
 */
function resolveMatchStatePath(baseUrl, explicitPath) {
  if (explicitPath) {
    return explicitPath
  }

  return baseUrl ? '/local/match-state' : '/api/local/match-state'
}

/** @param {string} endpoint @param {string} matchId @param {string} actorId */
function buildMatchStateUrl(endpoint, matchId, actorId) {
  const params = new URLSearchParams({ matchId })
  if (actorId) {
    params.set('actorId', actorId)
  }
  return `${endpoint}?${params.toString()}`
}

/**
 * @typedef {{
 *  baseUrl?: string,
 *  path?: string,
 *  apiKeyHeaderName?: string,
 *  apiKeyValue?: string,
 *  roomToken?: string
 * }} MatchStateClientConfig
 */

/**
 * @typedef {{
 *  turnNumber: number,
 *  actorId: string,
 *  actionType: string,
 *  guess: string[],
 *  feedback: string[],
 *  timestamp: number,
 *  version: number
 * }} MatchTurnEntry
 */

/** Validated wire actions are decoded without clamps or fabricated defaults.
 * @param {any[]} rawTurns @returns {MatchTurnEntry[]}
 */
function normalizeTurns(rawTurns) {
  return rawTurns.map(turn => ({ ...turn, ...decodeTurnContent(turn) }))
}

/**
 * @typedef {{
 *  matchId?: string,
 *  state?: string,
 *  activePlayer?: string,
 *  feedbackGiver?: string,
 *  players?: string[],
 *  readyPlayers?: string[],
 *  turns?: MatchTurnEntry[],
 *  version?: number,
 *  visibleSecretCode?: string[]
 * }} MatchStateResponse
 */

/**
 * @param {MatchStateClientConfig} [config]
 */
// Client one-shot : chaque appel renvoie un instantane MatchStateHttpResponse.
// Le rafraichissement (polling ou futur push WebSocket) reste a la charge de l'appelant.
export function createMatchStateClient(config = {}) {
  const {
    baseUrl = import.meta.env?.VITE_API_BASE_URL ?? '',
    path,
    apiKeyHeaderName = import.meta.env?.VITE_API_KEY_HEADER ?? 'X-API-Key',
    apiKeyValue = import.meta.env?.VITE_API_KEY ?? '',
    roomToken = ''
  } = config

  assertAuthConfig(apiKeyHeaderName, apiKeyValue)

  const resolvedPath = resolveMatchStatePath(baseUrl, path ?? import.meta.env?.VITE_MATCH_STATE_PATH)

  const endpoint = buildUrl(baseUrl, resolvedPath)

  /**
   * @param {string} matchId
   * @param {string} [actorId]
   * @param {{signal?: AbortSignal}} [options]
   * @returns {Promise<MatchStateResponse>}
   */
  async function getMatchState(matchId, actorId = '', options = {}) {
    if (!matchId) {
      throw new Error('CLIENT:INVALID_MATCH_ID')
    }

    const url = buildMatchStateUrl(endpoint, matchId, actorId)
    const { response, body } = await requestJson(url, {
      method: 'GET',
      signal: options.signal,
      headers: {
        [apiKeyHeaderName]: apiKeyValue,
        ...(roomToken ? { 'X-Room-Token': roomToken } : {})
      }
    })


    if (!response.ok) {
      throw new Error(readErrorCode(body, 'HTTP_ERROR'))
    }

    if (!body || typeof body !== 'object') {
      throw new Error('HTTP:INVALID_RESPONSE_BODY')
    }

    validateMatchResponse(body, matchId)
    const payload = /** @type {{
     *  matchId?: unknown,
     *  state?: unknown,
     *  activePlayer?: unknown,
     *  feedbackGiver?: unknown,
     *  players?: unknown,
     *  readyPlayers?: unknown,
     *  turns?: unknown,
     *  version?: unknown,
     *  visibleSecretCode?: unknown
     * }} */ (body)

    return {
      matchId: typeof payload.matchId === 'string' ? payload.matchId : matchId,
      state: typeof payload.state === 'string' ? payload.state : 'IN_PROGRESS',
      activePlayer: typeof payload.activePlayer === 'string' ? payload.activePlayer : undefined,
      feedbackGiver: typeof payload.feedbackGiver === 'string' ? payload.feedbackGiver : undefined,
      players: Array.isArray(payload.players) ? payload.players.map((entry) => String(entry)) : [],
      readyPlayers: Array.isArray(payload.readyPlayers) ? payload.readyPlayers.map((entry) => String(entry)) : [],
      turns: normalizeTurns(payload.turns),
      version: typeof payload.version === 'number' && Number.isFinite(payload.version) ? payload.version : undefined,
      visibleSecretCode: Array.isArray(payload.visibleSecretCode) ? payload.visibleSecretCode.map((entry) => String(entry)) : [],
      scores: payload.scores && typeof payload.scores === "object" ? /** @type {Record<string, number>} */ (payload.scores) : {},
      winnerId: typeof payload.winnerId === "string" ? payload.winnerId : undefined,
      gameNumber: typeof payload.gameNumber === "number" ? payload.gameNumber : 1,
      submittedPlayers: Array.isArray(payload.submittedPlayers) ? payload.submittedPlayers.map(String) : [],
      rematchRequestedPlayers: Array.isArray(payload.rematchRequestedPlayers) ? payload.rematchRequestedPlayers.map(String) : [],
      roundNumber: typeof payload.roundNumber === 'number' ? payload.roundNumber : 0,
      solvedPlayers: Array.isArray(payload.solvedPlayers) ? payload.solvedPlayers.map(String) : [],
      requiredPlayers: Array.isArray(payload.requiredPlayers) ? payload.requiredPlayers.map(String) : [],
      opponentSecretCode: Array.isArray(payload.opponentSecretCode) ? payload.opponentSecretCode.map(String) : []
    }
  }

  return {
    getMatchState,
    endpoint
  }
}

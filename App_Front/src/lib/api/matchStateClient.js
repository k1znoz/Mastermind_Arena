import { buildUrl, readErrorCode, safeJsonParse } from './httpClientUtils'

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

  return baseUrl ? '/health' : '/api/health'
}

/** @param {string} path */
function isHealthPath(path) {
  const normalized = String(path ?? '').toLowerCase()
  return normalized.endsWith('/health')
}

/** @param {string} endpoint @param {string} matchId @param {string} actorId */
function buildMatchStateUrl(endpoint, matchId, actorId) {
  const params = new URLSearchParams({ matchId })
  if (actorId) {
    params.set('actorId', actorId)
  }
  return `${endpoint}?${params.toString()}`
}

/** @param {string} headerName @param {string} apiKeyValue */
function assertAuthConfig(headerName, apiKeyValue) {
  if (!headerName || !String(headerName).trim()) {
    throw new Error('CLIENT:API_KEY_HEADER_MISSING')
  }

  const safeApiKey = String(apiKeyValue ?? '').trim()
  if (!safeApiKey) {
    throw new Error('CLIENT:API_KEY_MISSING')
  }

  const weakKeys = new Set([
    'dev-submit-action-key',
    'dev-local-key-123',
    'change-me-dev-key',
    'change-me-api-key'
  ])

  if (weakKeys.has(safeApiKey)) {
    throw new Error('CLIENT:API_KEY_WEAK_DEFAULT')
  }
}

/**
 * @typedef {{
 *  baseUrl?: string,
 *  path?: string,
 *  apiKeyHeaderName?: string,
 *  apiKeyValue?: string
 * }} MatchStateClientConfig
 */

/**
 * @typedef {{
 *  turnNumber: number,
 *  actorId: string,
 *  actionType: string,
 *  guess?: string,
 *  feedback?: string,
 *  timestamp: number,
 *  version: number
 * }} MatchTurnEntry
 */

/**
 * @typedef {{
 *  matchId?: string,
 *  state?: string,
 *  activePlayer?: string,
 *  feedbackGiver?: string,
 *  players?: string[],
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
    baseUrl = import.meta.env.VITE_API_BASE_URL ?? '',
    path,
    apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key',
    apiKeyValue = import.meta.env.VITE_API_KEY ?? ''
  } = config

  assertAuthConfig(apiKeyHeaderName, apiKeyValue)

  const resolvedPath = resolveMatchStatePath(baseUrl, path ?? import.meta.env.VITE_MATCH_STATE_PATH)
  const healthMode = isHealthPath(resolvedPath)

  const endpoint = buildUrl(baseUrl, resolvedPath)

  /**
   * @param {string} matchId
   * @param {string} [actorId]
   * @returns {Promise<MatchStateResponse>}
   */
  async function getMatchState(matchId, actorId = '') {
    if (!matchId) {
      throw new Error('CLIENT:INVALID_MATCH_ID')
    }

    const url = healthMode ? endpoint : buildMatchStateUrl(endpoint, matchId, actorId)
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        [apiKeyHeaderName]: apiKeyValue
      }
    })

    const rawText = await response.text()
    const body = safeJsonParse(rawText)

    if (!response.ok) {
      throw new Error(readErrorCode(body, 'HTTP_ERROR'))
    }

    if (!body || typeof body !== 'object') {
      throw new Error('HTTP:INVALID_RESPONSE_BODY')
    }

    if (healthMode) {
      const payload = /** @type {{ status?: unknown }} */ (body)
      return {
        matchId,
        state: typeof payload.status === 'string' ? payload.status : 'UP',
        players: [],
        turns: [],
        visibleSecretCode: []
      }
    }

    const payload = /** @type {{
     *  matchId?: unknown,
     *  state?: unknown,
     *  activePlayer?: unknown,
     *  feedbackGiver?: unknown,
     *  players?: unknown,
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
      turns: Array.isArray(payload.turns) ? /** @type {MatchTurnEntry[]} */ (payload.turns) : [],
      version: typeof payload.version === 'number' && Number.isFinite(payload.version) ? payload.version : undefined,
      visibleSecretCode: Array.isArray(payload.visibleSecretCode) ? payload.visibleSecretCode.map((entry) => String(entry)) : []
    }
  }

  return {
    getMatchState,
    endpoint
  }
}

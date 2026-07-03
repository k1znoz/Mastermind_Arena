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
 *  matchId?: string,
 *  version?: number,
 *  turnNumber?: number,
 *  turnActive?: boolean,
 *  status?: string,
 *  actionLog?: Array<Record<string, unknown>>
 * }} MatchStateResponse
 */

/**
 * @param {MatchStateClientConfig} [config]
 */
export function createMatchStateClient(config = {}) {
  const {
    baseUrl = import.meta.env.VITE_API_BASE_URL ?? '',
    path,
    apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key',
    apiKeyValue = import.meta.env.VITE_API_KEY ?? 'dev-submit-action-key'
  } = config

  const resolvedPath = resolveMatchStatePath(baseUrl, path ?? import.meta.env.VITE_MATCH_STATE_PATH)

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

    const url = endpoint
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

    const payload = /** @type {{ status?: unknown }} */ (body)
    return {
      matchId,
      version: 0,
      turnNumber: 0,
      turnActive: true,
      status: typeof payload.status === 'string' ? payload.status : 'UP',
      actionLog: []
    }
  }

  return {
    getMatchState,
    endpoint
  }
}

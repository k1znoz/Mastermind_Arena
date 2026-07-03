import { buildUrl, readErrorCode, safeJsonParse } from './httpClientUtils'

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
    path = import.meta.env.VITE_MATCH_STATE_PATH ?? '/api/local/match-state',
    apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key',
    apiKeyValue = import.meta.env.VITE_API_KEY ?? 'dev-submit-action-key'
  } = config

  const endpoint = buildUrl(baseUrl, path)

  /**
   * @param {string} matchId
   * @param {string} [actorId]
   * @returns {Promise<MatchStateResponse>}
   */
  async function getMatchState(matchId, actorId = '') {
    if (!matchId) {
      throw new Error('CLIENT:INVALID_MATCH_ID')
    }

    const params = new URLSearchParams({ matchId })
    if (actorId) {
      params.set('actorId', actorId)
    }

    const url = `${endpoint}?${params.toString()}`
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

    return /** @type {MatchStateResponse} */ (body)
  }

  return {
    getMatchState,
    endpoint
  }
}

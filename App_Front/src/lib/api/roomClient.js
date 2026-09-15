import { buildUrl, readErrorCode, safeJsonParse } from './httpClientUtils'

/**
 * @param {{ baseUrl?: string, apiKeyHeaderName?: string, apiKeyValue?: string }} config
 */
export function createRoomClient(config = {}) {
  const baseUrl = config.baseUrl ?? import.meta.env.VITE_API_BASE_URL ?? ''
  const header = config.apiKeyHeaderName ?? import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key'
  const key = config.apiKeyValue ?? import.meta.env.VITE_API_KEY ?? ''
  const path = baseUrl ? '/local/rooms' : '/api/local/rooms'
  const endpoint = buildUrl(baseUrl, path)

  /** @param {string} url @param {RequestInit} [options] */
  async function request(url, options = {}) {
    const response = await fetch(url, {
      ...options,
      headers: {
        [header]: key,
        ...(options.body ? { 'Content-Type': 'application/json' } : {})
      }
    })
    const body = safeJsonParse(await response.text())
    if (!response.ok) throw new Error(readErrorCode(body, 'ROOM_HTTP_ERROR'))
    return /** @type {any} */ (body)
  }

  return {
    endpoint,
    async list() {
      const body = await request(endpoint)
      return Array.isArray(body?.rooms) ? body.rooms : []
    },
    /** @param {string} roomId */
    async status(roomId) {
      return request(endpoint + '?' + new URLSearchParams({ roomId }))
    },
    /** @param {string} name @param {string} pseudo @param {string} accessCode */
    async create(name, pseudo, accessCode) {
      return request(endpoint, { method: 'POST', body: JSON.stringify({ name, pseudo, accessCode }) })
    },
    /** @param {string} roomId @param {string} pseudo @param {string} accessCode */
    async join(roomId, pseudo, accessCode) {
      return request(endpoint + '/join', { method: 'POST', body: JSON.stringify({ roomId, pseudo, accessCode }) })
    }
  }
}
import { buildUrl, readErrorCode, requestJson } from './httpClientUtils.js'
import { validateAdmission, validateRoomInfo, validateRoomList } from './responseContract.js'

/**
 * @param {{ baseUrl?: string, apiKeyHeaderName?: string, apiKeyValue?: string }} config
 */
export function createRoomClient(config = {}) {
  const baseUrl = config.baseUrl ?? import.meta.env?.VITE_API_BASE_URL ?? ''
  const header = config.apiKeyHeaderName ?? import.meta.env?.VITE_API_KEY_HEADER ?? 'X-API-Key'
  const key = config.apiKeyValue ?? import.meta.env?.VITE_API_KEY ?? ''
  const path = baseUrl ? '/local/rooms' : '/api/local/rooms'
  const endpoint = buildUrl(baseUrl, path)

  /** @param {string} url @param {RequestInit} [options] */
  async function request(url, options = {}) {
    const { response, body } = await requestJson(url, {
      ...options,
      headers: {
        [header]: key,
        ...(options.body ? { 'Content-Type': 'application/json' } : {})
      }
    })
    if (!response.ok) throw new Error(readErrorCode(body, 'ROOM_HTTP_ERROR'))
    return /** @type {any} */ (body)
  }

  return {
    endpoint,
    async resume(recoveryCode, { signal } = {}) {
      const [roomId, actorId, token] = recoveryCode.trim().split('.')
      return validateAdmission(await request(endpoint + '/resume', { signal, method: 'POST', body: JSON.stringify({ recoveryCode }) }), { roomId, actorId, token })
    },
    async list({ signal } = {}) {
      return validateRoomList(await request(endpoint, { signal }))
    },
    /** @param {string} roomId */
    async status(roomId, { signal } = {}) {
      return validateRoomInfo(await request(endpoint + '?' + new URLSearchParams({ roomId }), { signal }), roomId)
    },
    /** @param {string} name @param {string} pseudo @param {string} accessCode */
    async create(name, pseudo, accessCode, { signal } = {}) {
      return validateAdmission(await request(endpoint, { signal, method: 'POST', body: JSON.stringify({ name, pseudo, accessCode }) }), { actorId: 'p1' })
    },
    /** @param {string} roomId @param {string} pseudo @param {string} accessCode */
    async join(roomId, pseudo, accessCode, { signal } = {}) {
      return validateAdmission(await request(endpoint + '/join', { signal, method: 'POST', body: JSON.stringify({ roomId, pseudo, accessCode }) }), { roomId, actorId: 'p2' })
    }
  }
}

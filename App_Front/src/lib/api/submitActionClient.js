import { buildUrl, requestJson, assertAuthConfig } from './httpClientUtils.js'
import { validateSubmitResponse, validateSubmitError } from './responseContract.js'

/**
 * @param {string} baseUrl
 * @param {string | undefined} explicitPath
 * @returns {string}
 */
function resolveSubmitPath(baseUrl, explicitPath) {
  if (explicitPath) {
    return explicitPath
  }

  return baseUrl ? '/local/submit-action' : '/api/local/submit-action'
}

function createIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  const random = Math.random().toString(36).slice(2, 10)
  return `idem-${Date.now()}-${random}`
}

/** @param {*} [config] */
export function createSubmitActionClient(config = {}) {
  const safeConfig = /** @type {Record<string, any>} */ (config ?? {})
  const baseUrl = safeConfig.baseUrl ?? import.meta.env?.VITE_API_BASE_URL ?? ''
  const path = resolveSubmitPath(baseUrl, safeConfig.path ?? import.meta.env?.VITE_SUBMIT_ACTION_PATH)
  const apiKeyHeaderName = safeConfig.apiKeyHeaderName ?? import.meta.env?.VITE_API_KEY_HEADER ?? 'X-API-Key'
  const apiKeyValue = safeConfig.apiKeyValue ?? import.meta.env?.VITE_API_KEY ?? ''
  const requestIdHeaderName = safeConfig.requestIdHeaderName ?? import.meta.env?.VITE_REQUEST_ID_HEADER ?? 'X-Request-Id'
  const roomToken = safeConfig.roomToken ?? ''

  assertAuthConfig(apiKeyHeaderName, apiKeyValue)

  const endpoint = buildUrl(baseUrl, path)

  /** @param {*} request */
  async function submitAction({
    matchId,
    actorId,
    expectedVersion,
    actionPayload,
    idempotencyKey = createIdempotencyKey(),
    signal
  }) {
    if (!matchId) {
      throw new Error('CLIENT:INVALID_MATCH_ID')
    }
    if (!actorId) {
      throw new Error('CLIENT:INVALID_ACTOR_ID')
    }
    if (typeof expectedVersion !== 'number' || !Number.isFinite(expectedVersion)) {
      throw new Error('CLIENT:INVALID_EXPECTED_VERSION')
    }
    if (!actionPayload || typeof actionPayload !== 'object') {
      throw new Error('CLIENT:INVALID_ACTION_PAYLOAD')
    }

    const requestId = createIdempotencyKey()

    const { response, body } = await requestJson(endpoint, {
      method: 'POST',
      signal,
      headers: {
        'Content-Type': 'application/json',
        [apiKeyHeaderName]: apiKeyValue,
        [requestIdHeaderName]: requestId,
        ...(roomToken ? { 'X-Room-Token': roomToken } : {})
      },
      body: JSON.stringify({
        matchId,
        actorId,
        expectedVersion,
        idempotencyKey,
        actionPayload
      })
    })


    if (!response.ok) {
      const metadata = validateSubmitError(body, matchId)
      if (metadata.code === 'VERSION_CONFLICT' && (response.status !== 409 || metadata.rejectionOrigin !== 'ENGINE')) {
        throw new Error('HTTP:INVALID_ERROR_RESPONSE')
      }
      throw Object.assign(new Error(`${metadata.rejectionOrigin}:${metadata.code}`), metadata)
    }

    return validateSubmitResponse(body, matchId, expectedVersion)
  }

  return {
    submitAction,
    endpoint
  }
}

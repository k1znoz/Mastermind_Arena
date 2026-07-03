/** @param {*} baseUrl */
function normalizeBaseUrl(baseUrl) {
  if (!baseUrl) {
    return ''
  }

  return String(baseUrl).endsWith('/') ? String(baseUrl).slice(0, -1) : String(baseUrl)
}

/** @param {*} baseUrl @param {*} path */
function buildUrl(baseUrl, path) {
  const textPath = String(path ?? '')
  const normalizedPath = textPath.startsWith('/') ? textPath : `/${textPath}`
  return `${normalizeBaseUrl(baseUrl)}${normalizedPath}`
}

/** @param {*} text */
function safeJsonParse(text) {
  try {
    return text ? JSON.parse(String(text)) : null
  } catch {
    return null
  }
}

/** @param {*} body @param {string} fallbackCode */
function readErrorCode(body, fallbackCode) {
  if (body && typeof body === 'object') {
    if (typeof body.rejectionCode === 'string' && body.rejectionCode) {
      return body.rejectionCode
    }
    if (typeof body.code === 'string' && body.code) {
      return body.code
    }
  }
  return fallbackCode
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
  const baseUrl = safeConfig.baseUrl ?? import.meta.env.VITE_API_BASE_URL ?? ''
  const path = safeConfig.path ?? import.meta.env.VITE_SUBMIT_ACTION_PATH ?? '/api/local/submit-action'
  const apiKeyHeaderName = safeConfig.apiKeyHeaderName ?? import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key'
  const apiKeyValue = safeConfig.apiKeyValue ?? import.meta.env.VITE_API_KEY ?? 'dev-submit-action-key'
  const requestIdHeaderName = safeConfig.requestIdHeaderName ?? import.meta.env.VITE_REQUEST_ID_HEADER ?? 'X-Request-Id'

  const endpoint = buildUrl(baseUrl, path)

  /** @param {*} request */
  async function submitAction({
    matchId,
    actorId,
    expectedVersion,
    actionPayload
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
    const idempotencyKey = createIdempotencyKey()

    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        [apiKeyHeaderName]: apiKeyValue,
        [requestIdHeaderName]: requestId
      },
      body: JSON.stringify({
        matchId,
        actorId,
        expectedVersion,
        idempotencyKey,
        actionPayload
      })
    })

    const rawText = await response.text()
    const body = safeJsonParse(rawText)

    if (!response.ok) {
      const rejectionCode = readErrorCode(body, 'HTTP_ERROR')
      const rejectionOrigin = body && typeof body === 'object' && typeof body.rejectionOrigin === 'string'
        ? body.rejectionOrigin
        : 'HTTP'
      throw new Error(`${rejectionOrigin}:${rejectionCode}`)
    }

    if (!body || typeof body !== 'object') {
      throw new Error('HTTP:INVALID_RESPONSE_BODY')
    }

    return /** @type {any} */ (body)
  }

  return {
    submitAction,
    endpoint
  }
}

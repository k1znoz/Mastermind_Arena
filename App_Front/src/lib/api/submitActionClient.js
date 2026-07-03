function normalizeBaseUrl(baseUrl) {
  if (!baseUrl) {
    return ''
  }

  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
}

function buildUrl(baseUrl, path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizeBaseUrl(baseUrl)}${normalizedPath}`
}

function safeJsonParse(text) {
  try {
    return text ? JSON.parse(text) : null
  } catch {
    return null
  }
}

function createIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  const random = Math.random().toString(36).slice(2, 10)
  return `idem-${Date.now()}-${random}`
}

export function createSubmitActionClient(config = {}) {
  const {
    baseUrl = import.meta.env.VITE_API_BASE_URL ?? '',
    path = import.meta.env.VITE_SUBMIT_ACTION_PATH ?? '/api/local/submit-action',
    apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key',
    apiKeyValue = import.meta.env.VITE_API_KEY ?? 'dev-submit-action-key',
    requestIdHeaderName = import.meta.env.VITE_REQUEST_ID_HEADER ?? 'X-Request-Id'
  } = config

  const endpoint = buildUrl(baseUrl, path)

  async function submitAction({
    matchId,
    actorId,
    expectedVersion,
    actionPayload
  }) {
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
      const rejectionCode = body?.rejectionCode ?? 'HTTP_ERROR'
      const rejectionOrigin = body?.rejectionOrigin ?? 'HTTP'
      throw new Error(`${rejectionOrigin}:${rejectionCode}`)
    }

    return body
  }

  return {
    submitAction,
    endpoint
  }
}

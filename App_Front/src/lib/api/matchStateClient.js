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

export function createMatchStateClient(config = {}) {
  const {
    baseUrl = import.meta.env.VITE_API_BASE_URL ?? '',
    path = import.meta.env.VITE_MATCH_STATE_PATH ?? '/api/local/match-state',
    apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key',
    apiKeyValue = import.meta.env.VITE_API_KEY ?? 'dev-submit-action-key'
  } = config

  const endpoint = buildUrl(baseUrl, path)

  async function getMatchState(matchId, actorId = '') {
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
      const code = body?.code ?? 'HTTP_ERROR'
      throw new Error(code)
    }

    return body
  }

  return {
    getMatchState,
    endpoint
  }
}

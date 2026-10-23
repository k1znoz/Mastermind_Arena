/**
 * Shared low-level helpers for local HTTP clients.
 */

/**
 * @param {string | undefined | null} baseUrl
 * @returns {string}
 */
function normalizeBaseUrl(baseUrl) {
  if (!baseUrl) {
    return ''
  }

  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
}

/**
 * @param {string} baseUrl
 * @param {string} path
 * @returns {string}
 */
export function buildUrl(baseUrl, path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizeBaseUrl(baseUrl)}${normalizedPath}`
}

/**
 * @param {string} text
 * @returns {unknown}
 */
export function safeJsonParse(text) {
  try {
    return text ? JSON.parse(text) : null
  } catch {
    return null
  }
}

/**
 * @param {unknown} body
 * @param {string} fallbackCode
 * @returns {string}
 */
export function readErrorCode(body, fallbackCode) {
  if (body && typeof body === 'object') {
    const candidate = /** @type {{ code?: unknown, rejectionCode?: unknown }} */ (body)
    if (typeof candidate.rejectionCode === 'string' && candidate.rejectionCode) {
      return candidate.rejectionCode
    }
    if (typeof candidate.code === 'string' && candidate.code) {
      return candidate.code
    }
  }

  return fallbackCode
}

/** @param {string} url @param {RequestInit} [options] @param {number} [timeoutMs] */
export async function fetchWithTimeout(url, options = {}, timeoutMs = 15000) {
  const controller = new AbortController()
  let timedOut = false
  const cancel = () => controller.abort()
  options.signal?.addEventListener('abort', cancel, { once: true })
  if (options.signal?.aborted) cancel()
  const timer = setTimeout(() => { timedOut = true; controller.abort() }, timeoutMs)
  try {
    const response = await fetch(url, { ...options, signal: controller.signal })
    const body = await response.arrayBuffer()
    if (controller.signal.aborted) throw new Error('REQUEST_ABORTED')
    return new Response([204, 205, 304].includes(response.status) ? null : body,
      { status: response.status, statusText: response.statusText, headers: response.headers })
  } catch (error) {
    if (timedOut) throw new Error('REQUEST_TIMEOUT', { cause: error })
    if (controller.signal.aborted) throw new Error('REQUEST_ABORTED', { cause: error })
    throw error
  } finally {
    clearTimeout(timer)
    options.signal?.removeEventListener('abort', cancel)
  }
}

/** @param {string} headerName @param {string} apiKeyValue */
export function assertAuthConfig(headerName, apiKeyValue) {
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

/** Fetch and read once. Endpoint clients retain their own response/error contracts.
 * @param {string} url @param {RequestInit} [options] @param {number} [timeoutMs]
 */
export async function requestJson(url, options = {}, timeoutMs = 15000) {
  const response = await fetchWithTimeout(url, options, timeoutMs)
  const body = safeJsonParse(await response.text())
  return { response, body }
}

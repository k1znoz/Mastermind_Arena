/**
 * Shared low-level helpers for local HTTP clients.
 */

/**
 * @param {string | undefined | null} baseUrl
 * @returns {string}
 */
export function normalizeBaseUrl(baseUrl) {
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

const PRIVATE_FIELD = /authorization|token|api[_-]?key|password|secret|keyvalue|access[_-]?code|recovery[_-]?code/i

/** Redact before retaining a diagnostic snapshot, including structured private fields.
 * @param {unknown} value @param {string} [key] @returns {unknown}
 */
export function sanitizeDebug(value, key = '') {
  if (value == null) return value
  if (PRIVATE_FIELD.test(key)) return '********'
  if (Array.isArray(value)) return value.map(entry => sanitizeDebug(entry))
  if (typeof value !== 'object') return value
  return Object.fromEntries(Object.entries(value).map(([name, entry]) => [name, sanitizeDebug(entry, name)]))
}

/** @param {unknown} value */
export function debugJson(value) {
  return JSON.stringify(sanitizeDebug(value) ?? {}, null, 2)
}

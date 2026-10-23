/** @param {unknown} value @returns {string[]} */
export function asSymbols(value) {
  return Array.isArray(value) ? value.map(String) : []
}

/** @param {unknown} value @returns {number | null} */
export function asTimestamp(value) {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

/** @param {number | null | undefined} value */
export function formatTimestamp(value) {
  return value ? new Date(value).toLocaleTimeString('fr-FR') : '--:--:--'
}

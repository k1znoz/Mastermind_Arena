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

/** Notifications refer to an earlier feedback action, not the request's current round. */
export function referencedTurnNumber(action, actions) {
  return actions.find(entry => entry.version === action.feedbackVersion)?.turnNumber ?? action.turnNumber
}

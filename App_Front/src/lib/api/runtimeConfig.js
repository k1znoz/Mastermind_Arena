const DEFAULT_MATCH_ID = 'local-match'
const DEFAULT_ACTOR_ID = 'p1'

/**
 * @param {unknown} value
 * @param {string} fallback
 * @returns {string}
 */
function asNonEmptyString(value, fallback) {
  const normalized = String(value ?? '').trim()
  return normalized || fallback
}

export const runtimeConfig = {
  matchId: asNonEmptyString(import.meta.env.VITE_MATCH_ID, DEFAULT_MATCH_ID),
  actorId: asNonEmptyString(import.meta.env.VITE_ACTOR_ID, DEFAULT_ACTOR_ID)
}

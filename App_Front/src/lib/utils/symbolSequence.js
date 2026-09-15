export const CODE_LENGTH = 4
export const ALLOWED_SYMBOLS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

/** @param {*} token */
export function normalizeSymbolToken(token) {
  return String(token ?? '').trim().toUpperCase()
}

/**
 * @param {Array<*>} sequence
 * @param {number} [maxLength]
 * @param {string[]} [allowedSymbols]
 * @returns {string[]}
 */
export function normalizeSymbolSequence(sequence, maxLength = CODE_LENGTH, allowedSymbols = [...ALLOWED_SYMBOLS]) {
  if (!Array.isArray(sequence)) {
    return []
  }

  return sequence
    .map(normalizeSymbolToken)
    .filter((token) => allowedSymbols.includes(token))
    .slice(0, maxLength)
}

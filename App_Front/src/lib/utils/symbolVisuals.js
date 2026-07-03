/** @typedef {{ token: string, icon: string, toneClass: string }} SymbolVisual */

/** @type {Record<string, SymbolVisual>} */
const VISUALS = {
  A: { token: 'A', icon: '●', toneClass: 'symbol-tone-red' },
  B: { token: 'B', icon: '■', toneClass: 'symbol-tone-cyan' },
  C: { token: 'C', icon: '◆', toneClass: 'symbol-tone-amber' },
  D: { token: 'D', icon: '▲', toneClass: 'symbol-tone-lime' },
  E: { token: 'E', icon: '⬢', toneClass: 'symbol-tone-violet' },
  F: { token: 'F', icon: '✦', toneClass: 'symbol-tone-ice' },
  G: { token: 'G', icon: '✚', toneClass: 'symbol-tone-rose' },
  H: { token: 'H', icon: '◉', toneClass: 'symbol-tone-emerald' },
  L: { token: 'L', icon: 'ϟ', toneClass: 'symbol-tone-amber' },
  Q: { token: 'Q', icon: '□', toneClass: 'symbol-tone-violet' },
  T: { token: 'T', icon: '△', toneClass: 'symbol-tone-emerald' }
}

const FALLBACKS = /** @type {SymbolVisual[]} */ ([
  { token: '?', icon: '◆', toneClass: 'symbol-tone-cyan' },
  { token: '?', icon: '●', toneClass: 'symbol-tone-rose' },
  { token: '?', icon: '▲', toneClass: 'symbol-tone-emerald' },
  { token: '?', icon: '■', toneClass: 'symbol-tone-amber' },
  { token: '?', icon: '✦', toneClass: 'symbol-tone-violet' },
  { token: '?', icon: '✚', toneClass: 'symbol-tone-red' },
  { token: '?', icon: '⬢', toneClass: 'symbol-tone-lime' },
  { token: '?', icon: '◉', toneClass: 'symbol-tone-ice' }
])

/**
 * Returns a stable icon+color visual descriptor for a symbol token.
 * @param {string | null | undefined} raw
 * @returns {SymbolVisual}
 */
export function getSymbolVisual(raw) {
  const token = String(raw ?? '').trim().toUpperCase()

  if (!token) {
    return { token: '_', icon: '·', toneClass: 'symbol-tone-muted' }
  }

  const direct = VISUALS[token]
  if (direct) {
    return direct
  }

  const code = token.charCodeAt(0)
  const fallback = FALLBACKS[code % FALLBACKS.length]
  return {
    token,
    icon: fallback.icon,
    toneClass: fallback.toneClass
  }
}

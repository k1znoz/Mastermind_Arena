/** @typedef {{ token: string, icon: string, toneClass: string }} SymbolVisual */
const VISUALS = {
  A: { token: 'Bleu', icon: '●', toneClass: 'color-blue' },
  B: { token: 'Vert', icon: '●', toneClass: 'color-green' },
  C: { token: 'Jaune', icon: '●', toneClass: 'color-yellow' },
  D: { token: 'Rouge', icon: '●', toneClass: 'color-red' },
  E: { token: 'Noir', icon: '●', toneClass: 'color-black' },
  F: { token: 'Blanc', icon: '●', toneClass: 'color-white' },
  G: { token: 'Ancienne couleur G', icon: '●', toneClass: 'symbol-tone-rose' },
  H: { token: 'Ancienne couleur H', icon: '●', toneClass: 'symbol-tone-emerald' }
}
/** @param {string | null | undefined} raw @returns {SymbolVisual} */
export function getSymbolVisual(raw) {
  const token = String(raw ?? '').trim().toUpperCase()
  return VISUALS[token] ?? { token: 'Vide', icon: '·', toneClass: 'symbol-tone-muted' }
}

export const CODE_LENGTH = 4
export const MAX_ATTEMPTS = 10

export const SYMBOLS = [
  { id: 'CYAN', glyph: 'O', label: 'Cyan', color: '#26c8ec' },
  { id: 'EMERALD', glyph: 'S', label: 'Emerald', color: '#37d297' },
  { id: 'AMBER', glyph: 'H', label: 'Amber', color: '#f0a50f' },
  { id: 'ROSE', glyph: 'T', label: 'Rose', color: '#f458a8' },
  { id: 'VIOLET', glyph: 'P', label: 'Violet', color: '#7d79ff' }
]

export const SYMBOL_MAP = new Map(SYMBOLS.map((item) => [item.id, item]))

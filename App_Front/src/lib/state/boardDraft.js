import { ALLOWED_SYMBOLS, CODE_LENGTH } from '../utils/symbolSequence.js'

export function createBoardDraft() {
  return { slots: Array(CODE_LENGTH).fill(''), selected: 0 }
}

/** @param {{slots: string[], selected: number}} draft @param {number} index */
export function selectDraftSlot(draft, index) {
  return Number.isInteger(index) && index >= 0 && index < CODE_LENGTH ? { ...draft, selected: index } : draft
}

/** @param {{slots: string[], selected: number}} draft @param {string} color */
export function placeDraftColor(draft, color) {
  if (!ALLOWED_SYMBOLS.includes(color)) return draft
  const slots = [...draft.slots]
  slots[draft.selected] = color
  return { slots, selected: Math.min(CODE_LENGTH - 1, draft.selected + 1) }
}

/** @param {string[]} slots @returns {string[] | null} */
export function completeDraft(slots) {
  return slots.length === CODE_LENGTH && slots.every(color => ALLOWED_SYMBOLS.includes(color)) ? [...slots] : null
}

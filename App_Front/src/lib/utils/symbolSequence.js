export function parseSymbolSequence(text, maxLength = 4) {
  return String(text ?? '')
    .toUpperCase()
    .split(/[^A-Z0-9]+/)
    .map((value) => value.trim())
    .filter(Boolean)
    .slice(0, maxLength)
}

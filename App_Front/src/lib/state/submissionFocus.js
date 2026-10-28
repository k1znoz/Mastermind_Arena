/** Keep an action's focus only while the user stays in its context and has not moved it.
 * @param {{getContext: () => string, afterRender: () => Promise<unknown>, getSurface: () => any}} options
 */
export function createSubmissionFocus(options) {
  let pending = null
  function cancel() { pending?.unwatch(); pending = null }
  function begin() {
    cancel()
    const surface = options.getSurface()
    const origin = surface?.active()
    const entry = { moved: false, unwatch: () => {}, context: options.getContext() }
    entry.unwatch = surface?.watch(target => { if (target !== origin && !origin?.contains?.(target)) entry.moved = true }) ?? (() => {})
    pending = entry
    return async (selector = '', expectedContext = entry.context) => {
      await options.afterRender()
      if (pending !== entry) return
      cancel()
      if (!surface || !origin || entry.moved || options.getContext() !== expectedContext || !surface.hasFocus()) return
      if (surface.active() !== origin && (origin.isConnected || !surface.isBodyActive())) return
      if (selector) surface.find(selector)?.focus()
    }
  }
  return { begin, cancel }
}

/** DOM access stays here, not in the network coordinators. */
export function browserSubmissionSurface() {
  if (typeof document === 'undefined') return null
  return {
    active: () => document.activeElement,
    hasFocus: () => document.hasFocus(),
    isBodyActive: () => document.activeElement === document.body,
    find: selector => document.querySelector(selector),
    watch(callback) {
      const listener = event => callback(event.target)
      document.addEventListener('focusin', listener)
      document.addEventListener('pointerdown', listener)
      return () => {
        document.removeEventListener('focusin', listener)
        document.removeEventListener('pointerdown', listener)
      }
    }
  }
}

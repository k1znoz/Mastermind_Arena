/** Scroll to the active phase after rendering, only for the context that requested it.
 * @param {{getContext: () => any, afterRender: () => Promise<unknown>, getSurface: () => any}} options
 */
export function createPhaseFocus(options) {
  let generation = 0
  let lastKey = ''
  let disposed = false

  function cancel() { generation += 1 }
  function reset() { cancel(); lastKey = '' }

  async function focus() {
    const context = { ...options.getContext() }
    if (disposed || context.view !== 'game' || context.activeTab !== 'partie') return
    const request = ++generation
    await options.afterRender()
    if (disposed || request !== generation || JSON.stringify(context) !== JSON.stringify(options.getContext())) return
    const surface = options.getSurface()
    if (!surface || surface.scrollHeight <= surface.height + 16) return
    const phase = context.state === 'FINISHED' ? ['result', '.match-overview']
      : surface.find('.secretActionActive') ? ['secret', '.secretActionActive .secret-panel']
        : surface.find('.feedbackActionActive') ? ['feedback', '.feedbackActionActive .response-panel']
          : surface.find('.guessActionActive') ? ['guess', '.guessActionActive .own-board'] : null
    if (!phase) return
    const key = `${context.matchId}:${context.gameNumber}:${phase[0]}`
    if (key === lastKey) return
    const target = surface.find(phase[1])
    if (!target) return
    const topbarHeight = surface.find('.topbar')?.getBoundingClientRect().height ?? 0
    const top = Math.max(0, surface.scrollY + target.getBoundingClientRect().top - topbarHeight - 8)
    lastKey = key
    surface.scrollTo({ top, behavior: surface.reducedMotion ? 'auto' : 'smooth' })
  }

  return { focus, cancel, reset, dispose() { reset(); disposed = true } }
}

/** Read browser geometry only when needed, so tests do not require a DOM. */
export function browserFocusSurface() {
  if (typeof window === 'undefined') return null
  return {
    height: window.innerHeight,
    scrollHeight: document.documentElement.scrollHeight,
    scrollY: window.scrollY,
    reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    find: selector => document.querySelector(selector),
    scrollTo: options => window.scrollTo(options)
  }
}

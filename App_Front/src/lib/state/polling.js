/** One polling lifecycle, with an injected scheduler for deterministic tests.
 * @param {{getView: () => string, refreshGame: () => Promise<unknown>, refreshWaiting: () => Promise<unknown>,
 * refreshLobby: () => Promise<unknown>, intervalMs?: number, onError?: (error: unknown) => void,
 * schedule?: (callback: () => void, delay: number) => any, cancel?: (timer: any) => void}} options
 */
export function createPolling(options) {
  const schedule = options.schedule ?? setTimeout
  const cancel = options.cancel ?? clearTimeout
  let timer = null
  let running = false
  let epoch = 0

  async function tick(generation) {
    timer = null
    if (!running || generation !== epoch) return
    try {
      const view = options.getView()
      if (view === 'game') await options.refreshGame()
      else if (view === 'waiting') await options.refreshWaiting()
      else if (view === 'lobby') await options.refreshLobby()
    } catch (error) { options.onError?.(error) }
    finally {
      if (running && generation === epoch) timer = schedule(() => { void tick(generation) }, options.intervalMs ?? 6000)
    }
  }
  return {
    start() {
      if (running) return
      running = true
      const generation = ++epoch
      timer = schedule(() => { void tick(generation) }, options.intervalMs ?? 6000)
    },
    stop() {
      running = false
      epoch += 1
      if (timer !== null) cancel(timer)
      timer = null
    }
  }
}

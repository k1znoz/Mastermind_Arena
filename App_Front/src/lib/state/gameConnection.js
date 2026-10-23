/** @typedef {{matchId: string, actorId: string, roomToken: string}} GameContext */
/** @param {GameContext} a @param {GameContext} b */
export function sameContext(a, b) {
  return a.matchId === b.matchId && a.actorId === b.actorId && a.roomToken === b.roomToken
}

/**
 * Coordinates network work, independently of Svelte and browser storage.
 * @param {{getContext: () => GameContext, getState: () => any,
 * fetchState: (context: GameContext, signal: AbortSignal) => Promise<any>,
 * sendAction: (context: GameContext, request: any, signal: AbortSignal) => Promise<any>,
 * onState: (state: any) => void, onSync: (status: string, error?: unknown) => void,
 * onSubmit: (status: string, action: string, error?: unknown) => void}} options
 */
export function createGameConnection(options) {
  let epoch = 0
  let reading = null
  let writing = null
  const controllers = new Set()
  const current = (context, generation) => generation === epoch && sameContext(context, options.getContext())

  async function read(context, generation, controller) {
    options.onSync('syncing')
    try {
      const result = await options.fetchState(context, controller.signal)
      if (!current(context, generation)) return null
      if (result.matchId !== context.matchId) throw new Error('HTTP:WRONG_MATCH_RESPONSE')
      const previous = options.getState()
      if (previous && result.version < previous.version) {
        options.onSync('ok')
        return previous
      }
      options.onState(result)
      options.onSync('ok')
      return result
    } catch (error) {
      if (current(context, generation)) options.onSync('error', error)
      return null
    } finally { controllers.delete(controller) }
  }

  function refresh() {
    const context = { ...options.getContext() }
    if (reading && reading.epoch === epoch && sameContext(reading.context, context)) return reading.promise
    const controller = new AbortController()
    controllers.add(controller)
    const entry = { context, epoch, promise: null }
    entry.promise = read(context, epoch, controller).finally(() => { if (reading === entry) reading = null })
    reading = entry
    return entry.promise
  }

  async function submit(actionPayload) {
    if (writing) return false
    const context = { ...options.getContext() }
    const generation = epoch
    const state = options.getState()
    if (!state || !Number.isSafeInteger(state.version)) return false
    const controller = new AbortController()
    controllers.add(controller)
    const operation = { context }
    writing = operation
    const action = String(actionPayload.actionType)
    const request = { matchId: context.matchId, actorId: context.actorId, expectedVersion: state.version,
      actionPayload: { ...actionPayload }, idempotencyKey: crypto.randomUUID() }
    options.onSubmit('loading', action)
    try {
      let response
      try {
        response = await options.sendAction(context, request, controller.signal)
      } catch (error) {
        if (!current(context, generation)) return false
        if (error.code === 'VERSION_CONFLICT') {
          const updated = await refresh()
          if (!current(context, generation) || !updated || updated.gameNumber !== state.gameNumber
              || updated.roundNumber !== state.roundNumber || updated.state !== state.state) throw error
          // A confirmed version rejection did not apply the original request.
          response = await options.sendAction(context, { ...request, expectedVersion: updated.version,
            idempotencyKey: crypto.randomUUID() }, controller.signal)
        } else if (error instanceof TypeError || error.message === 'REQUEST_TIMEOUT'
            || error.message === 'HTTP:INVALID_RESPONSE_BODY') {
          // A lost response is ambiguous: replay the exact operation, never a new intention.
          response = await options.sendAction(context, request, controller.signal)
        } else { throw error }
      }
      if (!current(context, generation)) return false
      if (response.accepted !== true) throw new Error('HTTP:INVALID_RESPONSE_BODY')
      await refresh()
      if (!current(context, generation)) return false
      options.onSubmit('success', action)
      return true
    } catch (error) {
      if (current(context, generation)) {
        await refresh()
        if (current(context, generation)) options.onSubmit('error', action, error)
      }
      return false
    } finally {
      controllers.delete(controller)
      if (writing === operation) writing = null
    }
  }

  function invalidate() {
    epoch += 1
    for (const controller of controllers) controller.abort()
    controllers.clear()
    reading = null
    writing = null
  }
  return { refresh, submit, invalidate }
}

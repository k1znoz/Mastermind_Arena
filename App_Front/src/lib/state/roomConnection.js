import { apiErrorText } from '../utils/apiErrorText.js'

/** User-facing messages never include server payloads or credentials. @param {unknown} error */
export function roomErrorText(error) {
  return apiErrorText(error, 'room')
}

/** Admission and lobby reads, scoped to the screen and private seat that started them.
 * @param {{getScope: () => string, getContext: () => {matchId: string, roomToken: string},
 * createClient: () => any, onRooms: (rooms: any[]) => void, onStatus: (room: any) => unknown,
 * onAdmission: (admission: any, recovered: boolean) => void, onBusy: (busy: boolean) => void,
 * onError: (message: string, recovery: boolean) => void}} options
 */
export function createRoomConnection(options) {
  let epoch = 0
  const flights = new Map()
  const current = entry => entry.epoch === epoch && entry.scope === options.getScope()

  function invalidate() {
    epoch += 1
    for (const entry of flights.values()) entry.controller.abort()
    flights.clear()
    options.onBusy(false)
  }

  function run(lane, request, apply, recovery = false) {
    const scope = options.getScope()
    const previous = flights.get(lane)
    if (previous && current(previous)) return previous.promise
    previous?.controller.abort()
    const entry = { scope, epoch, controller: new AbortController(), promise: null }
    flights.set(lane, entry)
    if (lane === 'admission') options.onBusy(true)
    options.onError('', recovery)
    entry.promise = (async () => {
      try {
        const result = await request(options.createClient(), entry.controller.signal)
        if (!current(entry)) return null
        await apply(result)
        return result
      } catch (error) {
        if (current(entry)) options.onError(roomErrorText(error), recovery)
        return null
      } finally {
        if (flights.get(lane) === entry) {
          flights.delete(lane)
          if (lane === 'admission') options.onBusy(false)
        }
      }
    })()
    return entry.promise
  }

  return {
    invalidate,
    refreshRooms: () => run('list', (client, signal) => client.list({ signal }), options.onRooms),
    refreshStatus() {
      const { matchId, roomToken } = options.getContext()
      if (!roomToken) return Promise.resolve(null)
      return run('status', (client, signal) => client.status(matchId, { signal }), options.onStatus)
    },
    create: (name, pseudo, accessCode) => run('admission', (client, signal) => client.create(name, pseudo, accessCode, { signal }), result => options.onAdmission(result, false)),
    join: (roomId, pseudo, accessCode) => run('admission', (client, signal) => client.join(roomId, pseudo, accessCode, { signal }), result => options.onAdmission(result, false)),
    recover: code => run('admission', (client, signal) => client.resume(code, { signal }), result => options.onAdmission(result, true), true)
  }
}

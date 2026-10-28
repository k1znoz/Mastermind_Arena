import { apiErrorText } from '../utils/apiErrorText.js'

/** User-facing messages never include server payloads or credentials. @param {unknown} error */
export function roomErrorText(error, operation = 'status') {
  const message = apiErrorText(error, 'room')
  if (!message) return ''
  const uncertain = message === 'Impossible de charger cette partie. Réessayez.' || error instanceof TypeError
    || (error instanceof Error && ['REQUEST_TIMEOUT', 'ROOM_SERVER_ERROR', 'SERVER_ERROR'].includes(error.message))
  if (!uncertain) return message
  return {
    list: 'La liste des parties n’a pas pu être chargée. Réessayez.',
    create: 'La création n’a pas pu être confirmée. Réessayez.',
    join: 'La connexion à la partie n’a pas pu être confirmée. Réessayez.',
    recover: 'La reprise n’a pas pu être confirmée. Réessayez.'
  }[operation] ?? message
}

/** Admission and lobby reads, scoped to the screen and private seat that started them.
 * @param {{getScope: () => string, getContext: () => {matchId: string, roomToken: string},
 * createClient: () => any, onRooms: (rooms: any[]) => void, onStatus: (room: any) => unknown,
 * onAdmission: (admission: any, recovered: boolean) => void,
 * onBusy: (busy: boolean, operation?: {type: string, roomId?: string} | null) => void,
 * onListBusy?: (busy: boolean) => void,
 * onStatusBusy?: (busy: boolean) => void,
 * onError: (message: string, source: string) => void}} options
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
    options.onListBusy?.(false)
    options.onStatusBusy?.(false)
  }

  function run(lane, request, apply, operation = null) {
    const scope = options.getScope()
    const previous = flights.get(lane)
    if (previous && current(previous)) return previous.promise
    previous?.controller.abort()
    const entry = { scope, epoch, controller: new AbortController(), promise: null }
    flights.set(lane, entry)
    if (lane === 'admission') options.onBusy(true, operation)
    if (lane === 'list') options.onListBusy?.(true)
    if (lane === 'status') options.onStatusBusy?.(true)
    const source = operation?.type ?? lane
    options.onError('', source)
    entry.promise = (async () => {
      try {
        const result = await request(options.createClient(), entry.controller.signal)
        if (!current(entry)) return null
        await apply(result)
        return result
      } catch (error) {
        if (current(entry)) options.onError(roomErrorText(error, source), source)
        return null
      } finally {
        if (flights.get(lane) === entry) {
          flights.delete(lane)
          if (lane === 'admission') options.onBusy(false)
          if (lane === 'list') options.onListBusy?.(false)
          if (lane === 'status') options.onStatusBusy?.(false)
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
    create: (name, pseudo, accessCode) => run('admission', (client, signal) => client.create(name, pseudo, accessCode, { signal }), result => options.onAdmission(result, false), { type: 'create' }),
    join: (roomId, pseudo, accessCode) => run('admission', (client, signal) => client.join(roomId, pseudo, accessCode, { signal }), result => options.onAdmission(result, false), { type: 'join', roomId }),
    recover: code => run('admission', (client, signal) => client.resume(code, { signal }), result => options.onAdmission(result, true), { type: 'recover' })
  }
}

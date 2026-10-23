/** User-facing messages never include server payloads or credentials. @param {unknown} error */
export function roomErrorText(error) {
  const code = error instanceof Error ? error.message : ''
  const messages = {
    ROOM_CODE_INVALID: 'Code d’accès incorrect.',
    ROOM_FULL: 'Cette partie a déjà deux joueurs.',
    ROOM_NOT_FOUND: 'Cette partie n’existe plus.',
    ROOM_ACCESS_DENIED: 'Ce code de reprise n’est plus valide.',
    ROOM_RESUME_INVALID: 'Ce code de reprise est incorrect ou n’est plus valide.',
    'HTTP:INVALID_ROOM_RESPONSE': 'La réponse du serveur est incompatible. Réessayez ou rechargez la page.',
    'HTTP:INVALID_ADMISSION_RESPONSE': 'La réponse du serveur est incompatible. Votre place n’a pas été remplacée.',
    ROOM_NAME_REQUIRED: 'Choisissez un nom de partie.',
    PSEUDO_REQUIRED: 'Choisissez un pseudo.',
    FIELD_TOO_LONG: 'Le nom ou le code est trop long.',
    ROOM_SERVER_ERROR: 'Le serveur n’a pas pu ouvrir cette partie.',
    REQUEST_TIMEOUT: 'Le serveur met trop de temps à répondre. Réessayez.',
    'Failed to fetch': 'Le serveur est inaccessible.'
  }
  return Object.hasOwn(messages, code) ? messages[code] : 'Impossible de charger cette partie. Réessayez.'
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

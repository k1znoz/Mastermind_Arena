const KEYS = {
  matchId: 'arenaMatchId', actorId: 'arenaActorId', roomName: 'arenaRoomName',
  roomToken: 'arenaRoomToken', playerName: 'arenaPlayerName'
}

/** @param {*} value */
export function validSession(value) {
  return value && typeof value.matchId === 'string' && value.matchId.length > 0
    && ['p1', 'p2'].includes(value.actorId) && typeof value.roomToken === 'string' && value.roomToken.length > 0
    && typeof value.roomName === 'string' && typeof value.playerName === 'string'
}

/** Storage is injected so session behavior can be tested without a browser. */
export function createSessionRepository(session, local) {
  return {
    playerName() {
      try { return session.getItem('arenaPlayerName') || '' } catch { return '' }
    },
    savePlayerName(value) {
      try { session.setItem('arenaPlayerName', value); return true } catch { return false }
    },
    save(value) {
      if (!validSession(value)) throw new Error('INVALID_SESSION')
      try {
        for (const [field, key] of Object.entries(KEYS)) session.setItem(key, value[field])
        local.setItem('arenaSavedRoom', JSON.stringify(value))
        return true
      } catch { return false }
    },
    load() {
      try {
        const current = Object.fromEntries(Object.entries(KEYS).map(([field, key]) => [field, session.getItem(key)]))
        if (validSession(current)) return current
        const saved = JSON.parse(local.getItem('arenaSavedRoom') || 'null')
        if (validSession(saved)) return saved
        local.removeItem('arenaSavedRoom')
      } catch {
        try { local.removeItem('arenaSavedRoom') } catch { /* Storage may be disabled. */ }
      }
      return null
    },
    forget() {
      try {
        for (const key of Object.values(KEYS)) session.removeItem(key)
        local.removeItem('arenaSavedRoom')
        return true
      } catch { return false }
    }
  }
}

export function createBrowserSessionRepository() {
  // Access storage lazily: restricted browsers can throw even when reading window.sessionStorage.
  const storage = kind => ({
    getItem: key => window[kind].getItem(key),
    setItem: (key, value) => window[kind].setItem(key, value),
    removeItem: key => window[kind].removeItem(key)
  })
  return createSessionRepository(storage('sessionStorage'), storage('localStorage'))
}

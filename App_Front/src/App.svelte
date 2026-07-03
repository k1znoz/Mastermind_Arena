<script>
  import { onMount } from 'svelte'
  import { fade, fly } from 'svelte/transition'
  import { createMatchStateClient } from './lib/api/matchStateClient'
  import { createSubmitActionClient } from './lib/api/submitActionClient'
  import { runtimeConfig } from './lib/api/runtimeConfig'
  import { parseSymbolSequence } from './lib/utils/symbolSequence'
  import TopBar from './lib/components/TopBar.svelte'
  import StatusBar from './lib/components/StatusBar.svelte'
  import MatchSnapshotCard from './lib/components/MatchSnapshotCard.svelte'
  import ScreenNav from './lib/components/ScreenNav.svelte'
  import LobbyScreen from './lib/components/LobbyScreen.svelte'
  import SetupScreen from './lib/components/SetupScreen.svelte'
  import ArenaScreen from './lib/components/ArenaScreen.svelte'
  import HistoryScreen from './lib/components/HistoryScreen.svelte'
  import ResultsScreen from './lib/components/ResultsScreen.svelte'

  const matchStateClient = createMatchStateClient()
  const submitActionClient = createSubmitActionClient()

  /**
   * @typedef {'home'|'lobby'|'setup'|'arena'|'correction'|'transition'|'results'|'history'|'recovery'} Screen
   */

  /**
   * @typedef {Object} MatchState
   * @property {string} matchId
   * @property {number} version
   * @property {number} turnNumber
   * @property {number} currentActorIndex
   * @property {string} currentActorId
   * @property {boolean} turnActive
   * @property {string[]} actorOrder
   * @property {string} status
   * @property {string | null} matchOutcomeStatus
   * @property {string | null} matchOutcomeReason
   * @property {string | null} cancellationCode
  * @property {string[]} visibleSecretCode
  * @property {Array<{actorId: string, turnNumber: number, actionType: string, symbols: string[], payloadSummary: string, emittedEvents: string[], resultingStatus: string, recordedAtEpochMs: number}>} actionLog
   */

  const screens = /** @type {const} */ ({
    HOME: 'home',
    LOBBY: 'lobby',
    SETUP: 'setup',
    ARENA: 'arena',
    CORRECTION: 'correction',
    TRANSITION: 'transition',
    RESULTS: 'results',
    HISTORY: 'history',
    RECOVERY: 'recovery'
  })

  const tacticalNav = /** @type {Array<{label: string, subLabel: string, screen: Screen, icon: string}>} */ ([
    { label: 'Match History', subLabel: '', screen: screens.HISTORY, icon: '⌁' },
    { label: 'Active Match', subLabel: '', screen: screens.ARENA, icon: '◎' },
    { label: 'Leaderboard', subLabel: '', screen: screens.RESULTS, icon: '▥' },
    { label: 'Training Ground', subLabel: '', screen: screens.SETUP, icon: '◈' }
  ])

  let currentScreen = /** @type {Screen} */ (screens.LOBBY)
  let matchState = /** @type {MatchState | null} */ (null)
  let syncStatus = 'idle'
  let syncMessage = ''
  let submitStatus = 'idle'
  let submitMessage = ''
  let setupCodeText = ''
  let arenaGuessText = ''
  let autoNavigationEnabled = true

  function backendActionLog() {
    return Array.isArray(matchState?.actionLog) ? matchState.actionLog : []
  }

  function derivedDefenseCode() {
    return Array.isArray(matchState?.visibleSecretCode) ? matchState.visibleSecretCode : []
  }

  function derivedAttemptHistory() {
    return backendActionLog()
      .filter((entry) => entry.actionType === 'SUBMIT_GUESS' && entry.actorId === runtimeConfig.actorId)
      .slice()
      .reverse()
      .map((entry, index) => ({
        id: index + 1,
        guess: entry.symbols,
        accepted: true,
        status: entry.resultingStatus,
        version: matchState?.version ?? null,
        emittedEvents: entry.emittedEvents,
        createdAt: new Date(entry.recordedAtEpochMs).toLocaleTimeString('fr-FR', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      }))
  }

  function derivedOpponentAttempts() {
    return backendActionLog()
      .filter((entry) => entry.actionType === 'SUBMIT_GUESS' && entry.actorId !== runtimeConfig.actorId)
      .slice()
      .reverse()
      .map((entry, index) => ({
        id: index + 1,
        actorId: entry.actorId,
        guess: entry.symbols,
        turnNumber: entry.turnNumber,
        createdAt: new Date(entry.recordedAtEpochMs).toLocaleTimeString('fr-FR', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      }))
  }

  function derivedTacticalLog() {
    return backendActionLog()
      .slice()
      .reverse()
      .map((entry, index) => ({
        id: index + 1,
        label: entry.actionType,
        detail: `${entry.actorId} | turn ${entry.turnNumber} | ${(entry.emittedEvents ?? []).join(', ') || entry.payloadSummary || '-'}`,
        emphasis: entry.actionType === 'SECRET_CODE_SET' ? 'primary' : entry.actionType === 'SUBMIT_GUESS' ? 'success' : 'muted',
        createdAt: new Date(entry.recordedAtEpochMs).toLocaleTimeString('fr-FR', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      }))
  }

  function inferScreenFromState(/** @type {MatchState | null} */ state) {
    const status = String(state?.status ?? '').toUpperCase()

    if (!status) {
      return currentScreen
    }

    if (status.includes('CREATED')) {
      return screens.LOBBY
    }

    if (status.includes('IN_PROGRESS')) {
      return screens.ARENA
    }

    if (status.includes('FINISHED') || status.includes('CANCELLED')) {
      return screens.RESULTS
    }

    return currentScreen
  }

  async function refreshMatchState() {
    syncStatus = 'syncing'
    syncMessage = 'loading match state'

    try {
      const state = await matchStateClient.getMatchState(runtimeConfig.matchId, runtimeConfig.actorId)
      matchState = state
      if (autoNavigationEnabled) {
        currentScreen = inferScreenFromState(state)
      }
      syncStatus = 'ok'
      syncMessage = `loaded v${state?.version ?? 0}`
    } catch (error) {
      syncStatus = 'error'
      syncMessage = `state load failed: ${error instanceof Error ? error.message : 'unknown error'}`
    }
  }

  onMount(async () => {
    await refreshMatchState()
  })

  function openScreen(/** @type {Screen} */ screen) {
    autoNavigationEnabled = false
    currentScreen = screen
  }

  function onSetupCodeTextChange(/** @type {Event | string} */ event) {
    if (typeof event === 'string') {
      setupCodeText = event
      return
    }

    const target = event.currentTarget
    setupCodeText = target instanceof HTMLInputElement ? target.value : ''
  }

  function onArenaGuessTextChange(/** @type {Event} */ event) {
    const target = event.currentTarget
    arenaGuessText = target instanceof HTMLInputElement ? target.value : ''
  }

  function parseSecretCode(/** @type {string} */ text) {
    return parseSymbolSequence(text, 4)
  }

  function parseGuess(/** @type {string} */ text) {
    return parseSymbolSequence(text, 4)
  }

  async function submitPayload(
    /** @type {Record<string, unknown>} */ actionPayload,
    /** @type {string} */ actionLabel
  ) {
    if (!matchState) {
      submitStatus = 'error'
      submitMessage = 'cannot submit before match state is loaded'
      return
    }

    submitStatus = 'syncing'
    submitMessage = `${actionLabel} in progress`

    try {
      const response = await submitActionClient.submitAction({
        matchId: runtimeConfig.matchId,
        actorId: runtimeConfig.actorId,
        expectedVersion: matchState.version,
        actionPayload
      })

      submitStatus = response.accepted ? 'ok' : 'error'
      submitMessage = response.accepted
        ? `${actionLabel} accepted (v${response.version ?? 'n/a'})`
        : `${actionLabel} rejected: ${response.rejectionOrigin ?? 'UNKNOWN'}:${response.rejectionCode ?? 'UNKNOWN'}`

      return response
    } catch (error) {
      submitStatus = 'error'
      submitMessage = `${actionLabel} failed: ${error instanceof Error ? error.message : 'unknown error'}`
      return null
    }

    await refreshMatchState()
  }

  async function submitPlayerReady() {
    const response = await submitPayload(
      {
        type: 'PLAYER_READY'
      },
      'PLAYER_READY'
    )

    await refreshMatchState()
    return response
  }

  async function submitSecretCodeSet() {
    const secretCode = parseSecretCode(setupCodeText)

    if (secretCode.length !== 4) {
      submitStatus = 'error'
      submitMessage = 'setup requires exactly 4 symbols (example: A B C D)'
      return
    }

    const response = await submitPayload(
      {
        type: 'SECRET_CODE_SET',
        secretCode
      },
      'SECRET_CODE_SET'
    )

    await refreshMatchState()
  }

  async function submitGuess() {
    const guess = parseGuess(arenaGuessText)

    if (guess.length !== 4) {
      submitStatus = 'error'
      submitMessage = 'arena requires exactly 4 symbols (example: A B C D)'
      return
    }

    const response = await submitPayload(
      {
        type: 'SUBMIT_GUESS',
        guess
      },
      'SUBMIT_GUESS'
    )

    if (response?.accepted) {
      arenaGuessText = ''
    }

    await refreshMatchState()
  }
</script>

<main class="app-shell">
  <div class="command-layout">
    <aside class="command-sidebar">
      <div class="sidebar-identity">
        <div class="sidebar-avatar">OP</div>
        <div>
          <p class="sidebar-name">{runtimeConfig.actorId}</p>
          <p class="sidebar-rank">match {runtimeConfig.matchId}</p>
        </div>
      </div>

      <nav class="sidebar-nav" aria-label="Navigation tactique">
        {#each tacticalNav as item}
          <button
            type="button"
            class={`sidebar-link ${currentScreen === item.screen ? 'active' : ''}`}
            on:click={() => openScreen(item.screen)}
          >
            <span class="sidebar-icon">{item.icon}</span>
            <span>{item.label}</span>
          </button>
        {/each}
      </nav>

      <div class="sidebar-foot">
        <p>logout</p>
      </div>
    </aside>

    <div class="command-main">
      <TopBar
        {matchState}
        {syncStatus}
        {autoNavigationEnabled}
        onRefresh={refreshMatchState}
        onToggleAutoNavigation={() => (autoNavigationEnabled = !autoNavigationEnabled)}
      />

      <StatusBar
        {syncStatus}
        {syncMessage}
        {submitStatus}
        {submitMessage}
        matchId={runtimeConfig.matchId}
        actorId={runtimeConfig.actorId}
        currentActorId={matchState?.currentActorId ?? ''}
      />

      <section class={`screen-container screen-${currentScreen}`}>
        {#key currentScreen}
          <div class="screen-stage" in:fly={{ y: 10, duration: 220, opacity: 0.25 }} out:fade={{ duration: 140 }}>
            <h2>{currentScreen.toUpperCase()}</h2>

            {#if currentScreen === screens.LOBBY}
              <LobbyScreen
                {matchState}
                {submitStatus}
                onSubmitPlayerReady={submitPlayerReady}
              />
            {:else if currentScreen === screens.SETUP}
              <SetupScreen
                {matchState}
                {submitStatus}
                actorId={runtimeConfig.actorId}
                {setupCodeText}
                parsedSymbols={parseSecretCode(setupCodeText)}
                onSetupCodeTextChange={onSetupCodeTextChange}
                onSubmitSecretCodeSet={submitSecretCodeSet}
              />
            {:else if currentScreen === screens.ARENA}
              <ArenaScreen
                {matchState}
                {submitStatus}
                actorId={runtimeConfig.actorId}
                guessText={arenaGuessText}
                parsedGuess={parseGuess(arenaGuessText)}
                defenseCode={derivedDefenseCode()}
                attemptHistory={derivedAttemptHistory()}
                opponentAttempts={derivedOpponentAttempts()}
                tacticalLog={derivedTacticalLog()}
                onGuessTextChange={onArenaGuessTextChange}
                onSubmitGuess={submitGuess}
              />
            {:else if currentScreen === screens.HISTORY}
              <HistoryScreen {matchState} logEntries={derivedTacticalLog()} />
            {:else if currentScreen === screens.RESULTS}
              <ResultsScreen {matchState} logEntries={derivedTacticalLog()} />
            {:else}
              <p>Base clean front. Screen implementation starts here.</p>
              <MatchSnapshotCard {matchState} />
            {/if}
          </div>
        {/key}
      </section>
    </div>
  </div>

  <div class="mobile-nav-wrap">
    <ScreenNav
      {screens}
      {currentScreen}
      onOpenScreen={openScreen}
    />
  </div>
</main>

<script>
	import { onDestroy, onMount, tick } from 'svelte'
	import { createGameConnection, sameContext } from './lib/state/gameConnection.js'
	import { createBrowserSessionRepository } from './lib/state/session.js'
	import { createMatchStateClient } from './lib/api/matchStateClient'
	import { createRoomClient } from './lib/api/roomClient'
	import { createSubmitActionClient } from './lib/api/submitActionClient'
	import { runtimeConfig } from './lib/api/runtimeConfig'
	import { createBoardDraft, selectDraftSlot, placeDraftColor, completeDraft } from './lib/state/boardDraft.js'
	import { deriveGameView } from './lib/state/gameView.js'
	import { createPolling } from './lib/state/polling.js'
	import { createRoomConnection } from './lib/state/roomConnection.js'
	import { apiErrorText } from './lib/utils/apiErrorText.js'
	import { sanitizeDebug } from './lib/utils/diagnostics.js'
	import { createPhaseFocus, browserFocusSurface } from './lib/state/phaseFocus.js'
    import { createSubmissionFocus, browserSubmissionSurface } from './lib/state/submissionFocus.js'
	import TopBar from './lib/components/TopBar.svelte'
	import DuelNav from './lib/components/DuelNav.svelte'
	import PrivateRecoveryPanel from './lib/components/PrivateRecoveryPanel.svelte'
	import Icon from './lib/components/Icon.svelte'
	import userCircle from './lib/assets/tabler/user-circle.svg'

	import PartieScreen from './lib/components/PartieScreen.svelte'
	import WelcomeScreen from './lib/components/WelcomeScreen.svelte'
	import LobbyScreen from './lib/components/LobbyScreen.svelte'
	import WaitingScreen from './lib/components/WaitingScreen.svelte'
	import RecoveryScreen from './lib/components/RecoveryScreen.svelte'
	import HistoryScreen from './lib/components/HistoryScreen.svelte'

	import DebugDrawer from './lib/components/DebugDrawer.svelte'
	import ToastMessage from './lib/components/ToastMessage.svelte'

	const POLL_MS = 6000

	/** @typedef {{ kind: 'success' | 'error', text: string } | null} ToastState */
	/** @typedef {{ matchId?: string, state?: string, activePlayer?: string, feedbackGiver?: string, players?: string[], readyPlayers?: string[], turns?: Array<Record<string, unknown>>, version?: number, visibleSecretCode?: string[] }} MatchStateLike */

	let activeTab = 'partie'
	let noticeDismissals = { context: '', versions: [] }
	let view = 'welcome'
	let rooms = []
	let roomBusy = false
    let roomOperation = /** @type {{type: string, roomId?: string} | null} */ (null)
    let roomsRefreshing = false
    let listError = ''
    let admissionError = ''
    let waitingError = ''
	let roomToken = ''
	let roomName = ''
	let roomInfo = null
	let hasSavedRoom = false
	let showDebugDrawer = false

	let playerName = ''
	let theme = 'dark'
	let lastNotifiedVersion = null
	let recoveryInput = ''
	let recoveryError = ''
	let recoveryReturnView = 'welcome'
	let lobbyDraftName = ''
	let lobbyDraftCode = ''

	const debugEnabled = typeof window !== 'undefined' && new URLSearchParams(window.location.search).has('debug')

	let setupDraft = createBoardDraft()
	let guessDraft = createBoardDraft()

	let matchState = /** @type {MatchStateLike | null} */ (null)
	let syncStatus = 'idle'
	let syncMessage = ''
	let submitStatus = 'idle'
	let pendingActionType = ''
	let submitError = ''

	let toast = /** @type {ToastState} */ (null)
	let waitingRefreshing = false
	let toastTimer = /** @type {ReturnType<typeof setTimeout> | null} */ (null)
    let toastReturnFocus = /** @type {HTMLElement | null} */ (null)

	let debugLastRequest = /** @type {unknown} */ (null)
	let debugLastResponse = /** @type {unknown} */ (null)

	let apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
	let apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key'
	let apiKeyValue = import.meta.env.VITE_API_KEY ?? ''
	let currentMatchId = runtimeConfig.matchId
	let currentActorId = runtimeConfig.actorId

	function resolveGameStatePath() {
		return apiBaseUrl ? '/local/match-state' : '/api/local/match-state'
	}

    function lobbyClient() {
        return createRoomClient({ baseUrl: apiBaseUrl, apiKeyHeaderName, apiKeyValue })
    }
    function refreshRooms() { return roomConnection.refreshRooms() }
    function refreshRoomStatus() { return roomConnection.refreshStatus() }

	function toggleTheme() {
        theme = theme === 'dark' ? 'light' : 'dark'
        document.documentElement.dataset.theme = theme
        try { localStorage.setItem('arenaTheme', theme) } catch { /* Theme still works without persistence. */ }
    }

    function recoverRoom(event) { return admit(() => roomConnection.recover(event.detail.code)) }

    function clearLobbyDraft() {
        lobbyDraftName = ''
        lobbyDraftCode = ''
    }

    function openRecovery() {
        recoveryReturnView = view
        phaseFocus.cancel()
        roomConnection.invalidate()
        recoveryError = ''
        view = 'recovery'
    }

    async function closeRecovery() {
        roomConnection.invalidate()
        const destination = recoveryReturnView
        view = destination
        if (view === 'lobby') refreshRooms()
        await tick()
        if (view === destination) document.getElementById('open-recovery')?.focus()
    }

    function continueWelcome() {
		if (!playerName.trim()) return
		playerName = playerName.trim()
		sessionRepository?.savePlayerName(playerName)
		roomConnection.invalidate()
		view = 'lobby'
		refreshRooms()
		void focusAfterNavigation('lobby-title')
	}

	async function enterRoom(admission) {
		clearLobbyDraft()
        waitingError = ''
        admissionError = ''
		roomConnection.invalidate()
		gameConnection.invalidate()
		submitStatus = 'idle'
		pendingActionType = ''
		submitError = ''
		currentMatchId = admission.room.roomId
		currentActorId = admission.actorId
		roomName = admission.room.name
		roomInfo = admission.room
		roomToken = admission.token
		matchState = null
		debugLastRequest = null
		debugLastResponse = null
		phaseFocus.reset()
		lastNotifiedVersion = null
		setupDraft = createBoardDraft()
		guessDraft = createBoardDraft()
		hasSavedRoom = true
		if (!sessionRepository?.save({ matchId: currentMatchId, actorId: currentActorId, roomName, roomToken, playerName })) openToast('error', 'Stockage indisponible : conservez votre code de reprise.')
		view = admission.room.joined ? 'game' : 'waiting'
		activeTab = 'partie'
		if (view === 'game') await refreshMatchState()
	}

    function createRoom(event) {
        return admit(() => roomConnection.create(event.detail.name, playerName, event.detail.accessCode))
    }
    function joinRoom(event) {
        return admit(() => roomConnection.join(event.detail.roomId, playerName, event.detail.accessCode))
    }

    async function admit(request) {
        if (roomBusy) return null
        const completeFocus = submissionFocus.begin()
        const admission = await request()
        await completeFocus(admission ? view === 'waiting' ? '#waiting-title' : '[data-phase-heading]' : '',
            admission ? submissionContext() : undefined)
        return admission
    }

	function resumeRoom() {
		if (!hasSavedRoom) return
		clearLobbyDraft()
		phaseFocus.cancel()
		roomConnection.invalidate()
		view = roomToken && currentActorId === 'p1' && !roomInfo?.joined ? 'waiting' : 'game'
		activeTab = 'partie'
		if (view === 'waiting') refreshRoomStatus()
		else refreshMatchState()
	}

	function returnToLobby() {
		clearLobbyDraft()
		phaseFocus.cancel()
		roomConnection.invalidate()
		view = 'lobby'
		activeTab = 'partie'
		refreshRooms()
		void focusAfterNavigation('lobby-title')
	}

    async function focusAfterNavigation(targetId) {
        const context = getGameContext()
        const destination = view
        const tab = activeTab
        await tick()
        if (view === destination && activeTab === tab && sameContext(context, getGameContext())) document.getElementById(targetId)?.focus()
    }

    function forgetRoom() {
        if (!sessionRepository?.forget()) {
            openToast('error', 'Impossible de supprimer la reprise sur cet appareil.')
            return
        }
        roomConnection.invalidate()
        gameConnection.invalidate()
        phaseFocus.reset()
        currentMatchId = runtimeConfig.matchId
        currentActorId = runtimeConfig.actorId
        roomToken = ''
        roomName = ''
        roomInfo = null
        hasSavedRoom = false
        matchState = null
        debugLastRequest = null
        debugLastResponse = null
        lastNotifiedVersion = null
        setupDraft = createBoardDraft()
        guessDraft = createBoardDraft()
        syncStatus = 'idle'
        submitStatus = 'idle'
        pendingActionType = ''
        submitError = ''
        returnToLobby()
    }
	function currentClients(context = getGameContext()) {
		const config = {
			baseUrl: apiBaseUrl,
			apiKeyHeaderName,
			apiKeyValue,
			roomToken: context.roomToken
		}

		return {
			matchClient: createMatchStateClient({
				...config,
				path: resolveGameStatePath()
			}),
			submitClient: createSubmitActionClient(config)
		}
	}

	/** @param {'success' | 'error'} kind @param {string} text */
	function openToast(kind, text) {
        if (kind === 'success' && toast?.kind === 'error') return
		if (toastTimer) {
			clearTimeout(toastTimer)
		}
        toastTimer = null
        toastReturnFocus = kind === 'error' && typeof document !== 'undefined' && document.activeElement instanceof HTMLElement ? document.activeElement : null
        toast = { kind, text }
		if (kind === 'success') toastTimer = setTimeout(() => {
			toast = null
			toastTimer = null
		}, 2400)
	}

    function dismissToast() {
        const closingFocused = typeof document !== 'undefined' && document.activeElement?.closest('.toast')
        toast = null
        if (closingFocused && toastReturnFocus?.isConnected) toastReturnFocus.focus()
        toastReturnFocus = null
    }

	/** @param {unknown} request @param {unknown} response */
	function logDebug(request, response) {
		debugLastRequest = sanitizeDebug(request)
		debugLastResponse = sanitizeDebug(response)
	}

    const phaseFocus = createPhaseFocus({
        getContext: () => ({ view, activeTab, matchId: currentMatchId, actorId: currentActorId,
            roomToken, gameNumber: matchState?.gameNumber ?? 1, state: matchState?.state }),
        afterRender: tick,
        getSurface: browserFocusSurface
    })

    const getGameContext = () => ({ matchId: currentMatchId, actorId: currentActorId, roomToken })
    const submissionContext = () => JSON.stringify({ view, activeTab, ...getGameContext() })
    const submissionFocus = createSubmissionFocus({ getContext: submissionContext, afterRender: tick, getSurface: browserSubmissionSurface })
    const sessionRepository = typeof window === 'undefined' ? null : createBrowserSessionRepository()
    const roomConnection = createRoomConnection({
        getScope: () => JSON.stringify({ view, ...getGameContext() }),
        getContext: getGameContext,
        createClient: lobbyClient,
        onRooms: result => { rooms = result },
        onStatus: async result => {
            roomInfo = result
            if (roomInfo.joined) {
                view = 'game'
                activeTab = 'partie'
                await refreshMatchState()
            }
        },
        onAdmission: (admission, recovered) => {
            if (recovered) {
                playerName = admission.actorId === 'p1' ? admission.room.hostPseudo : admission.room.guestPseudo
                recoveryInput = ''
            }
            return enterRoom(admission)
        },
        onBusy: (busy, operation) => { roomBusy = busy; roomOperation = busy ? operation ?? null : null },
        onListBusy: busy => { roomsRefreshing = busy },
        onStatusBusy: busy => { waitingRefreshing = busy },
        onError: (message, source) => {
            if (source === 'recover') recoveryError = message
            else if (source === 'list') listError = message
            else if (source === 'status') waitingError = message
            else admissionError = message
        }
    })
    function returnToWelcome() {
        clearLobbyDraft()
        phaseFocus.cancel()
        roomConnection.invalidate()
        view = 'welcome'
        void focusAfterNavigation('welcome-pseudo')
    }

    const gameConnection = createGameConnection({
        getContext: getGameContext,
        getState: () => matchState,
        fetchState: (context, signal) => currentClients(context).matchClient.getMatchState(context.matchId, context.actorId, { signal }),
        sendAction: (context, request, signal) => currentClients(context).submitClient.submitAction({ ...request, signal }),
        onState: (result) => {
            const notices = result.turns.filter(action => action.actorId !== currentActorId && action.version > (lastNotifiedVersion ?? result.version) && ['CORRECT_FEEDBACK', 'CONTEST_FEEDBACK'].includes(action.actionType))
            if (notices.length) openToast('success', notices.at(-1).actionType === 'CORRECT_FEEDBACK' ? 'Votre adversaire a corrigé ses indices. Le résultat a été actualisé.' : 'Votre adversaire demande de vérifier les derniers indices.')
            lastNotifiedVersion = result.version
            if (matchState && result.gameNumber !== matchState.gameNumber) {
                setupDraft = createBoardDraft()
                guessDraft = createBoardDraft()
            }
            matchState = result
            logDebug({ matchId: currentMatchId, actorId: currentActorId }, result)
            void phaseFocus.focus()
        },
        onSync: (status, error) => {
            syncStatus = status
            syncMessage = status === 'error' ? apiErrorText(error, 'sync') : ''
        },
        onSubmit: (status, action, error) => {
            submitStatus = status
            pendingActionType = status === 'loading' ? action : ''
            submitError = status === 'error' ? apiErrorText(error, 'submit') : ''
            if (status === 'success') openToast('success', 'Action envoyée')
            if (status === 'error' && submitError) openToast('error', submitError)
        }
    })
    function refreshMatchState() { return gameConnection.refresh() }
    async function submitPayload(actionPayload, onConfirmed = () => {}) {
        if (submitStatus === 'loading') return false
        const completeFocus = submissionFocus.begin()
        const accepted = await gameConnection.submit(actionPayload)
        if (accepted) onConfirmed()
        const action = actionPayload.actionType
        const selector = action === 'REQUEST_REMATCH' && matchState?.state === 'FINISHED' ? '.rematch-panel h3' : '[data-phase-heading]'
        await completeFocus(accepted && action !== 'CONTEST_FEEDBACK' ? selector : '')
        return accepted
    }

	/** @param {string} actionType @param {unknown} [payload] @param {unknown} [feedback] */
	function buildActionPayload(actionType, payload, feedback) {
		return {
			actionType,
			payload: encodeActionField(payload),
			feedback: encodeActionField(feedback)
		}
	}

	/** @param {unknown} value */
	function encodeActionField(value) {
		if (value === undefined || value === null) {
			return null
		}
		return typeof value === 'string' ? value : JSON.stringify(value)
	}

	function submitSecretCode() {
		if (!gameView.canSubmitSecret) {
			openToast('error', 'Action non autorisee a ce stade')
			return
		}

		const normalizedCode = completeDraft(setupDraft.slots)
		if (!normalizedCode) {
			openToast('error', 'Code secret invalide (4 symboles)')
			return
		}

		submitPayload(buildActionPayload('READY_SECRET', normalizedCode))
	}


    function chooseSetupSymbol(color) { setupDraft = placeDraftColor(setupDraft, color) }
    function clearSetupSequence() { setupDraft = createBoardDraft() }
    function setSelectedSetupSlot(index) { setupDraft = selectDraftSlot(setupDraft, index) }
    function chooseGuessSymbol(color) { guessDraft = placeDraftColor(guessDraft, color) }
    function clearGuess() { guessDraft = createBoardDraft() }
    function setSelectedGuessSlot(index) { guessDraft = selectDraftSlot(guessDraft, index) }

	async function submitGuess() {
		if (!gameView.canSubmitGuess) {
			openToast('error', 'Tentative non autorisee a ce stade')
			return
		}

		const normalizedGuess = completeDraft(guessDraft.slots)
		if (!normalizedGuess) {
			openToast('error', 'Séquence de tentative invalide')
			return
		}

		const context = getGameContext()
		if (await submitPayload(buildActionPayload('PLAY_GUESS', normalizedGuess)) && sameContext(context, getGameContext())) clearGuess()
	}

	/** @param {{ bienPlaces: number, malPlaces: number }} detail */
	function submitFeedbackAction(detail) {
		if (!gameView.canRespond) {
			openToast('error', 'Action non autorisee a ce stade')
			return
		}

		submitPayload(buildActionPayload('SEND_FEEDBACK', null, detail))
	}

	/** @param {CustomEvent<{ bienPlaces: number, malPlaces: number }>} event */
	function onSubmitFeedback(event) {
		submitFeedbackAction(event.detail)
	}

	function abandon() {
        submitPayload(buildActionPayload('ABANDON'))
    }

    function correctFeedback(event) {
        submitPayload(buildActionPayload('CORRECT_FEEDBACK', { feedbackVersion: event.detail.version }, event.detail.feedback), event.detail.onConfirmed)
    }

    function contestFeedback(event) {
        submitPayload(buildActionPayload('CONTEST_FEEDBACK', { feedbackVersion: event.detail.version }))
    }

    function requestRematch() {
		if (matchState?.state === "FINISHED") submitPayload(buildActionPayload("REQUEST_REMATCH"))
	}

	/** @param {CustomEvent<{ tab: string }>} event */
	function onNavChange(event) {
		phaseFocus.cancel()
		activeTab = event.detail.tab
	}

    function openHistory(event) {
        onNavChange(event)
        void focusAfterNavigation('history-title')
    }

	/** @param {CustomEvent<{ index: number }>} event */
	function onSelectSetupSlot(event) {
		setSelectedSetupSlot(event.detail.index)
	}

	/** @param {CustomEvent<{ index: number }>} event */
	function onSelectGuessSlot(event) {
		setSelectedGuessSlot(event.detail.index)
	}

	/** @param {CustomEvent<{ symbol: string }>} event */
	function onChooseSetupSymbol(event) {
		chooseSetupSymbol(event.detail.symbol)
	}

	/** @param {CustomEvent<{ symbol: string }>} event */
	function onChooseGuessSymbol(event) {
		chooseGuessSymbol(event.detail.symbol)
	}

    $: gameView = deriveGameView({ state: matchState, actorId: currentActorId, syncStatus,
        submitStatus, pendingActionType, setupDraft, guessDraft, playerName, roomInfo })
	$: recoveryCode = roomToken ? `${currentMatchId}.${currentActorId}.${roomToken}` : ''

    const polling = createPolling({ getView: () => view, refreshGame: refreshMatchState,
        refreshWaiting: refreshRoomStatus, refreshLobby: refreshRooms, intervalMs: POLL_MS,
        onError: () => openToast('error', 'Actualisation impossible. Réessayez.') })

	onMount(() => {
        const preferredTheme = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
        try { theme = localStorage.getItem('arenaTheme') || preferredTheme } catch { theme = preferredTheme }
        if (!['dark', 'light'].includes(theme)) theme = preferredTheme
        document.documentElement.dataset.theme = theme

        const saved = sessionRepository?.load()
        playerName = saved?.playerName || sessionRepository?.playerName() || ''
        if (saved) {
            currentMatchId = saved.matchId
            currentActorId = saved.actorId
            roomName = saved.roomName
            roomToken = saved.roomToken
            hasSavedRoom = true
            view = currentActorId === 'p1' ? 'waiting' : 'game'
            if (view === 'waiting') refreshRoomStatus()
            else { refreshRoomStatus(); refreshMatchState() }
        }
        polling.start()
        return () => polling.stop()
	})
	onDestroy(() => {
        submissionFocus.cancel()
        phaseFocus.dispose()
        roomConnection.invalidate()
		gameConnection.invalidate()
		if (toastTimer) {
			clearTimeout(toastTimer)
		}
	})
</script>

<div class="app-root" class:compact-board={view === 'game' && activeTab === 'partie'} class:arrival-view={view !== 'game'} class:duel-view={view === 'game'}>

	<TopBar arrival={view !== 'game'} duel={view === 'game'} {debugEnabled} {theme} on:theme={toggleTheme} homeAvailable={view === 'game' || view === 'waiting'} on:home={returnToLobby} on:opendebug={() => showDebugDrawer = true} />

	<main class="screen">
        {#if toast?.kind === 'error'}<ToastMessage {toast} inline on:close={dismissToast} />{/if}
		{#if view === 'welcome'}
			<WelcomeScreen bind:playerName canResume={hasSavedRoom} on:openrecovery={openRecovery} on:continue={continueWelcome} on:resume={resumeRoom} />
		{/if}
		{#if view === 'lobby'}
			<LobbyScreen {playerName} bind:newName={lobbyDraftName} bind:newCode={lobbyDraftCode} on:openrecovery={openRecovery} {rooms} busy={roomBusy} operation={roomOperation} refreshing={roomsRefreshing} {listError} error={admissionError} canResume={hasSavedRoom} ownRoomId={currentActorId === "p1" && roomToken ? currentMatchId : ""} on:back={returnToWelcome} on:refresh={refreshRooms} on:createroom={createRoom} on:joinroom={joinRoom} on:resume={resumeRoom} />
		{/if}
		{#if view === 'recovery'}
			<RecoveryScreen bind:value={recoveryInput} busy={roomBusy} error={recoveryError} on:recover={recoverRoom} on:back={closeRecovery} />
		{/if}
		{#if view === 'waiting'}
			<WaitingScreen {roomName} hostPseudo={roomInfo?.hostPseudo || playerName} locked={roomInfo?.locked ?? false} error={waitingError} refreshing={waitingRefreshing} on:refresh={refreshRoomStatus} on:lobby={returnToLobby} on:forget={forgetRoom} />
		{/if}
        {#if view === 'game'}
        <section class="match-overview">
            <h1 class="sr-only">{roomName || 'Votre duel'}</h1>
            <p class="match-round">Manche {matchState?.gameNumber ?? 1} · Tour {gameView.displayRound}</p>
            <div class="match-heading">
                <div class="match-player"><Icon src={userCircle} size={32} /><span>{gameView.displayPlayerName}</span></div>
                <span class="match-score" aria-label={`Score : vous ${matchState?.scores?.[currentActorId] ?? 0}, adversaire ${matchState?.scores?.[gameView.opponentActorId] ?? 0}`}>{matchState?.scores?.[currentActorId] ?? 0} – {matchState?.scores?.[gameView.opponentActorId] ?? 0}</span>
                <div class="match-player"><Icon src={userCircle} size={32} /><span>{gameView.opponentName}</span></div>
            </div>
            {#if syncStatus === 'error' && syncMessage}<div class="connection-error" role="alert"><p>{syncMessage}</p><button type="button" on:click={refreshMatchState}>Réessayer</button></div>{/if}
            <PrivateRecoveryPanel code={recoveryCode} playerName={gameView.displayPlayerName} opponentName={gameView.opponentName} on:forget={forgetRoom} />
        </section>
        {/if}

		{#if view === 'game' && activeTab === 'partie'}
			<PartieScreen view={gameView} bind:noticeDismissals on:history={openHistory}
				on:selectsetupslot={onSelectSetupSlot}
				on:selectguessslot={onSelectGuessSlot}
				on:choosesetupsymbol={onChooseSetupSymbol}
				on:chooseguesssymbol={onChooseGuessSymbol}
				on:clearsetup={clearSetupSequence}
				on:submitsecret={submitSecretCode}
				on:clearguess={clearGuess}
				on:submitguess={submitGuess}
				on:submitfeedback={onSubmitFeedback}
				on:requestrematch={requestRematch}
				on:abandon={abandon}
				on:correctfeedback={correctFeedback}
				on:contestfeedback={contestFeedback}
			/>
		{/if}

		{#if view === 'game' && activeTab === 'historique'}
			<HistoryScreen
				syncing={syncStatus === 'syncing'}
				historyRows={gameView.historyRows}
				actorId={currentActorId} playerName={gameView.displayPlayerName} opponentName={gameView.opponentName}
				onRefresh={refreshMatchState}
			/>
		{/if}
	</main>

	{#if view === "game"}<DuelNav {activeTab} on:change={onNavChange} on:home={returnToLobby} />{/if}

	<DebugDrawer
		open={showDebugDrawer}
		{syncStatus}
		{debugLastRequest}
		{debugLastResponse}
		{syncMessage}
		{submitError}
		on:close={() => showDebugDrawer = false}
	/>

    {#if toast?.kind === 'success'}<ToastMessage {toast} />{/if}
</div>

<style>
  .app-root { min-height:100dvh; position:relative; padding-top:70px; padding-bottom:72px; }
  .screen { position:relative; z-index:1; display:grid; gap:14px; width:100%; max-width:1000px; margin:0 auto; padding:20px 16px; }
  .compact-board .screen { grid-template-columns:minmax(0,1fr); }
  @media(min-width:651px) { .compact-board .screen { max-width:1080px; } }
  @media(max-width:600px) { .screen { padding:16px 12px; } }
  .arrival-view { padding-top:104px; padding-bottom:32px; background:var(--bg); }
  .arrival-view .screen { max-width:1360px; padding:32px 48px; }
  :global(:root:not([data-theme="light"])) .arrival-view,
  :global(:root:not([data-theme="light"])) .duel-view { --bg:#10181f; --panel:#162129; --panel-strong:#202c36; --line:#2d3c48; --line-soft:#435461; --primary:#63d0d4; --primary-soft:#83dfdf; --on-primary:#10262d; --text:#f0f3f7; --muted:#b9c4d0; }
  @media(max-width:600px) { .arrival-view { padding-top:80px; padding-bottom:24px; } .arrival-view .screen { padding:20px 24px; } }
  @media(max-width:650px) { .compact-board .screen { padding:6px; gap:5px; } }
  .duel-view { background:var(--bg); padding-bottom:calc(88px + env(safe-area-inset-bottom)); }
  @media(min-width:851px) { .duel-view { padding-left:216px; padding-bottom:32px; } }
.match-overview { position:relative; display:grid; gap:12px; width:100%; max-width:1048px; margin:0 auto 10px; padding:8px 0; }
.match-round { margin:0; text-align:center; color:var(--muted); font-size:13px; }
.match-heading { display:grid; grid-template-columns:minmax(0,1fr) auto minmax(0,1fr); gap:24px; align-items:center; max-width:600px; width:100%; margin:auto; }
.match-player { display:flex; align-items:center; gap:12px; min-width:0; padding:12px 16px; border:1px solid var(--line); border-radius:14px; background:var(--panel); }
.match-player span { min-width:0; overflow-wrap:anywhere; font-size:15px; }
.match-score { white-space:nowrap; font:600 30px Inter,sans-serif; }
.connection-error { margin-top:12px; color:var(--error); font-size:14px; }
.connection-error button { min-height:44px; border:1px solid var(--error); background:transparent; color:var(--error); padding:8px 12px; }
@media(max-width:650px) {
  .match-overview { padding:12px 10px 0; gap:10px; margin-bottom:0; }
  .match-heading { gap:12px; }
  .match-player { gap:6px; padding:10px 8px; border-radius:12px; }
  .match-player span { font-size:13px; }
  .match-score { font-size:24px; }
}

</style>

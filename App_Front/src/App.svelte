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
	import { sanitizeDebug } from './lib/utils/diagnostics.js'
	import { createPhaseFocus, browserFocusSurface } from './lib/state/phaseFocus.js'
	import TopBar from './lib/components/TopBar.svelte'
	import BottomNav from './lib/components/BottomNav.svelte'

	import PartieScreen from './lib/components/PartieScreen.svelte'
	import WelcomeScreen from './lib/components/WelcomeScreen.svelte'
	import LobbyScreen from './lib/components/LobbyScreen.svelte'
	import HistoryScreen from './lib/components/HistoryScreen.svelte'

	import DebugDrawer from './lib/components/DebugDrawer.svelte'
	import ToastMessage from './lib/components/ToastMessage.svelte'

	const POLL_MS = 6000

	/** @typedef {{ kind: 'success' | 'error', text: string } | null} ToastState */
	/** @typedef {{ matchId?: string, state?: string, activePlayer?: string, feedbackGiver?: string, players?: string[], readyPlayers?: string[], turns?: Array<Record<string, unknown>>, version?: number, visibleSecretCode?: string[] }} MatchStateLike */

	let activeTab = 'partie'
	let view = 'welcome'
	let rooms = []
	let roomBusy = false
	let lobbyError = ''
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
	let toastTimer = /** @type {ReturnType<typeof setTimeout> | null} */ (null)

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

    function recoverRoom(event) { return roomConnection.recover(event.detail.code) }

    function continueWelcome() {
		if (!playerName.trim()) return
		playerName = playerName.trim()
		sessionRepository?.savePlayerName(playerName)
		roomConnection.invalidate()
		view = 'lobby'
		refreshRooms()
	}

	function enterRoom(admission) {
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
		if (view === 'game') refreshMatchState()
	}

    function createRoom(event) {
        return roomConnection.create(event.detail.name, playerName, event.detail.accessCode)
    }
    function joinRoom(event) {
        return roomConnection.join(event.detail.roomId, playerName, event.detail.accessCode)
    }

	function resumeRoom() {
		if (!hasSavedRoom) return
		phaseFocus.cancel()
		roomConnection.invalidate()
		view = roomToken && currentActorId === 'p1' && !roomInfo?.joined ? 'waiting' : 'game'
		activeTab = 'partie'
		if (view === 'waiting') refreshRoomStatus()
		else refreshMatchState()
	}

	function returnToLobby() {
		phaseFocus.cancel()
		roomConnection.invalidate()
		view = 'lobby'
		activeTab = 'partie'
		refreshRooms()
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
		toast = { kind, text }
		if (toastTimer) {
			clearTimeout(toastTimer)
		}
		toastTimer = setTimeout(() => {
			toast = null
			toastTimer = null
		}, 2400)
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
            enterRoom(admission)
        },
        onBusy: busy => { roomBusy = busy },
        onError: (message, recovery) => {
            if (recovery) recoveryError = message
            else lobbyError = message
        }
    })
    function returnToWelcome() {
        phaseFocus.cancel()
        roomConnection.invalidate()
        view = 'welcome'
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
            syncMessage = error instanceof Error ? error.message : ''
        },
        onSubmit: (status, action, error) => {
            submitStatus = status
            pendingActionType = status === 'loading' ? action : ''
            submitError = error instanceof Error ? error.message : ''
            if (status === 'success') openToast('success', 'Action envoyée')
            if (status === 'error') openToast('error', submitError)
        }
    })
    function refreshMatchState() { return gameConnection.refresh() }
    function submitPayload(actionPayload) { return gameConnection.submit(actionPayload) }

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
        submitPayload(buildActionPayload('CORRECT_FEEDBACK', { feedbackVersion: event.detail.version }, event.detail.feedback))
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
        phaseFocus.dispose()
        roomConnection.invalidate()
		gameConnection.invalidate()
		if (toastTimer) {
			clearTimeout(toastTimer)
		}
	})
</script>

<div class="app-root" class:compact-board={view === 'game' && activeTab === 'partie'}>

	<TopBar compact={view === 'game' && activeTab === 'partie'} {debugEnabled} {theme} on:theme={toggleTheme} homeAvailable={view === 'game' || view === 'waiting'} on:home={returnToLobby} on:opendebug={() => showDebugDrawer = true} />

	<main class="screen">
		{#if view === 'welcome'}
			<WelcomeScreen bind:playerName bind:recoveryInput recoveryError={recoveryError} busy={roomBusy} on:recover={recoverRoom} canResume={hasSavedRoom} on:continue={continueWelcome} on:resume={resumeRoom} />
		{/if}
		{#if view === 'lobby'}
			<LobbyScreen {playerName} bind:recoveryInput recoveryError={recoveryError} on:recover={recoverRoom} {rooms} busy={roomBusy} error={lobbyError} canResume={hasSavedRoom} ownRoomId={currentActorId === "p1" && roomToken ? currentMatchId : ""} on:back={returnToWelcome} on:refresh={refreshRooms} on:createroom={createRoom} on:joinroom={joinRoom} on:resume={resumeRoom} />
		{/if}
		{#if view === 'waiting'}
			<section class="waiting-room">
				<span>PARTIE CRÉÉE</span><h1>{roomName}</h1>
				<p>En attente d’un deuxième joueur. Votre partie apparaît dans la liste des parties en attente.</p>
				<p>{roomInfo?.locked ? 'Un code d’accès est requis pour la rejoindre.' : 'Tout joueur peut la rejoindre.'}</p>
				{#if lobbyError}<p role="alert">{lobbyError}</p><button type="button" on:click={forgetRoom}>Oublier cette reprise sur cet appareil</button>{/if}
				<div><button type="button" on:click={refreshRoomStatus}>ACTUALISER</button><button type="button" on:click={returnToLobby}>VOIR LES PARTIES</button></div>
			</section>
		{/if}
        {#if view === 'game'}
        <section class="match-overview">
            <div class="match-heading"><div><small>Manche {matchState?.gameNumber ?? 1} · Tour {gameView.displayRound}</small><h1>{roomName || 'Votre duel'}</h1></div><span class="match-score" aria-label={`Score : vous ${matchState?.scores?.[currentActorId] ?? 0}, adversaire ${matchState?.scores?.[gameView.opponentActorId] ?? 0}`}>{matchState?.scores?.[currentActorId] ?? 0} — {matchState?.scores?.[gameView.opponentActorId] ?? 0}</span></div>
            <p class="match-players">{gameView.displayPlayerName} <span>contre</span> {gameView.opponentName}</p>
            {#if syncStatus === 'error'}<div class="connection-error" role="alert"><p>{syncMessage === 'ROOM_ACCESS_DENIED' || syncMessage === 'MATCH_NOT_FOUND' ? 'Cette reprise n’est plus valide ou vous n’avez plus accès à cette partie.' : 'La connexion est interrompue. Le dernier plateau reçu reste affiché.'}</p><button type="button" on:click={refreshMatchState}>Réessayer</button></div>{/if}
            <details class="room-tools"><summary>Reprendre sur un autre appareil</summary><p>Ce code privé donne accès à votre place dans la partie. Conservez-le et ne le partagez pas avec votre adversaire.</p><label>Votre code de reprise<input readonly value={recoveryCode} on:focus={(event) => event.currentTarget.select()} /></label><p>Sur un appareil partagé, copiez votre code avant d’oublier cette place. La partie ne sera pas supprimée.</p><button type="button" on:click={forgetRoom}>Oublier ma place sur cet appareil</button></details>
        </section>
        {/if}

		{#if view === 'game' && activeTab === 'partie'}
			<PartieScreen view={gameView}
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
				isPrototype={gameView.isPrototype}
				historyRows={gameView.historyRows}
				onRefresh={refreshMatchState}
			/>
		{/if}
	</main>

	{#if view === "game"}<BottomNav compact={activeTab === 'partie'} {activeTab} on:change={onNavChange} />{/if}

	<DebugDrawer
		open={showDebugDrawer}
		{syncStatus}
		{debugLastRequest}
		{debugLastResponse}
		{syncMessage}
		{submitError}
		on:close={() => showDebugDrawer = false}
	/>

	<ToastMessage {toast} />
</div>

<style>
  .app-root { min-height:100dvh; position:relative; padding-top:70px; padding-bottom:72px; }
  .screen { position:relative; z-index:1; display:grid; gap:14px; width:100%; max-width:1000px; margin:0 auto; padding:20px 16px; }
  .compact-board .screen { grid-template-columns:minmax(0,1fr); }
  @media(min-width:651px) { .compact-board .screen { max-width:1080px; } }
  @media(max-width:600px) { .screen { padding:16px 12px; } }
  @media(max-width:650px) { .app-root.compact-board { padding-top:48px; padding-bottom:54px; } .compact-board .screen { padding:6px; gap:5px; } }
.match-overview { display:grid; gap:5px; width:100%; max-width:760px; margin:0 auto 18px; padding:18px; background:var(--panel); border:1px solid var(--line); border-radius:16px; }
@media(max-width:650px) { .match-overview { gap:4px; } }
.match-heading { display:flex; gap:12px; justify-content:space-between; align-items:center; }
.match-heading h1 { margin:6px 0 0; font:600 22px 'Space Grotesk',sans-serif; overflow-wrap:anywhere; }
.match-heading small { font:12px Inter,sans-serif; color:var(--muted); }
.match-score { flex:none; font:600 22px 'Space Grotesk',sans-serif; }
.match-players { font-size:13px; margin:10px 0 0; color:var(--text); overflow-wrap:anywhere; }
.match-players span { color:var(--muted); margin:0 6px; }
.room-tools { margin-top:12px; }
.room-tools summary { min-height:44px; padding:12px 0; cursor:pointer; font-size:13px; color:var(--muted); }
.room-tools p { color:var(--muted); font-size:13px; line-height:1.6; }
.room-tools label { display:grid; gap:8px; font-size:13px; }
.room-tools input { min-width:0; width:100%; min-height:44px; padding:10px; color:var(--text); border:1px solid var(--line); background:var(--panel-strong); font-size:14px; }
.connection-error { margin-top:12px; color:var(--error); font-size:14px; }
.connection-error button { min-height:44px; border:1px solid var(--error); background:transparent; color:var(--error); padding:8px 12px; }
.waiting-room { display:grid; gap:14px; padding:clamp(20px,5vw,42px); border:1px solid var(--line); background:var(--panel); border-radius:16px; }
.waiting-room > span { color:var(--primary); font:11px "JetBrains Mono",monospace; letter-spacing:.1em; }
.waiting-room h1 { margin:0; font:700 clamp(28px,5vw,42px) "Space Grotesk",sans-serif; }
.waiting-room p { margin:0; color:var(--muted); font-size:14px; line-height:1.45; }
.waiting-room div { display:flex; gap:8px; flex-wrap:wrap; }
.waiting-room button { min-height:44px; padding:8px 12px; border:1px solid var(--primary); background:rgba(0,219,233,.1); color:var(--primary-soft); cursor:pointer; font:500 13px Inter,sans-serif; }

</style>

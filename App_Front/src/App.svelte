<script>
	import { onDestroy, onMount, tick } from 'svelte'
	import { createMatchStateClient } from './lib/api/matchStateClient'
	import { createRoomClient } from './lib/api/roomClient'
	import { createSubmitActionClient } from './lib/api/submitActionClient'
	import { runtimeConfig } from './lib/api/runtimeConfig'
	import { ALLOWED_SYMBOLS, CODE_LENGTH, normalizeSymbolSequence } from './lib/utils/symbolSequence'
	import { getSymbolVisual } from './lib/utils/symbolVisuals'
	import TopBar from './lib/components/TopBar.svelte'
	import BottomNav from './lib/components/BottomNav.svelte'

	import PartieScreen from './lib/components/PartieScreen.svelte'
	import WelcomeScreen from './lib/components/WelcomeScreen.svelte'
	import LobbyScreen from './lib/components/LobbyScreen.svelte'
	import HistoryScreen from './lib/components/HistoryScreen.svelte'

	import DebugDrawer from './lib/components/DebugDrawer.svelte'
	import ToastMessage from './lib/components/ToastMessage.svelte'

	const symbolPalette = [...ALLOWED_SYMBOLS]
	const POLL_MS = 6000

	/** @typedef {{ kind: 'success' | 'error', text: string } | null} ToastState */
	/** @typedef {{ matchId?: string, state?: string, activePlayer?: string, feedbackGiver?: string, players?: string[], readyPlayers?: string[], turns?: Array<Record<string, unknown>>, version?: number, visibleSecretCode?: string[] }} MatchStateLike */
	/** @typedef {Error & { code?: string, rejectionOrigin?: string, version?: number, matchStatus?: string }} SubmitClientError */

	let activeTab = 'partie'
	let view = 'welcome'
	let rooms = []
	let roomBusy = false
	let lobbyError = ''
	let roomToken = ''
	let roomName = ''
	let roomInfo = null
	let hasSavedRoom = false
	let lastScrollFocusKey = ''
	let showDebugDrawer = false
	let maskSecrets = true

	let playerName = ''

	const debugEnabled = typeof window !== 'undefined' && new URLSearchParams(window.location.search).has('debug')

	let setupSequence = /** @type {string[]} */ ([])
	let selectedSetupSlot = 0

	let draftGuess = ['', '', '', '']
	let selectedGuessSlot = 0

	let matchState = /** @type {MatchStateLike | null} */ (null)
	let lastKnownVersion = 0
	let syncStatus = 'idle'
	let syncMessage = ''
	let submitStatus = 'idle'
	let pendingActionType = ''
	let submitError = ''
	let submitInFlight = false

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

	function roomErrorText(error) {
		const code = error instanceof Error ? error.message : String(error)
		const messages = {
			ROOM_CODE_INVALID: 'Code d’accès incorrect.',
			ROOM_FULL: 'Cette partie a déjà deux joueurs.',
			ROOM_NOT_FOUND: 'Cette partie n’existe plus.',
			ROOM_NAME_REQUIRED: 'Choisissez un nom de partie.',
			PSEUDO_REQUIRED: 'Choisissez un pseudo.',
			FIELD_TOO_LONG: 'Le nom ou le code est trop long.',
			ROOM_SERVER_ERROR: 'Le serveur n’a pas pu ouvrir cette partie.'
		}
		return messages[code] || (code === 'Failed to fetch' ? 'Le serveur est inaccessible.' : code)
	}
	function lobbyClient() {
		return createRoomClient({ baseUrl: apiBaseUrl, apiKeyHeaderName, apiKeyValue })
	}

	async function refreshRooms() {
		try {
			rooms = await lobbyClient().list()
			lobbyError = ''
		} catch (error) {
			lobbyError = roomErrorText(error)
		}
	}

	async function refreshRoomStatus() {
		if (!roomToken || currentActorId !== 'p1') return
		try {
			roomInfo = await lobbyClient().status(currentMatchId)
			if (roomInfo.joined) {
				view = 'game'
				activeTab = 'partie'
				await refreshMatchState()
			}
		} catch (error) {
			lobbyError = roomErrorText(error)
		}
	}

	function continueWelcome() {
		if (!playerName.trim()) return
		playerName = playerName.trim()
		sessionStorage.setItem('arenaPlayerName', playerName)
		view = 'lobby'
		refreshRooms()
	}

	function enterRoom(admission) {
		currentMatchId = admission.room.roomId
		currentActorId = admission.actorId
		roomName = admission.room.name
		roomInfo = admission.room
		roomToken = admission.token
		matchState = null
		lastScrollFocusKey = ''
		lastKnownVersion = 0
		setupSequence = []
		draftGuess = ['', '', '', '']
		hasSavedRoom = true
		sessionStorage.setItem('arenaMatchId', currentMatchId)
		sessionStorage.setItem('arenaActorId', currentActorId)
		sessionStorage.setItem('arenaRoomName', roomName)
		sessionStorage.setItem('arenaRoomToken', roomToken)
		sessionStorage.setItem('arenaPlayerName', playerName)
		view = currentActorId === 'p1' ? 'waiting' : 'game'
		activeTab = 'partie'
		if (view === 'game') refreshMatchState()
	}

	async function createRoom(event) {
		roomBusy = true
		lobbyError = ''
		try {
			const result = await lobbyClient().create(event.detail.name, playerName, event.detail.accessCode)
			enterRoom(result)
		} catch (error) {
			lobbyError = roomErrorText(error)
		} finally {
			roomBusy = false
		}
	}

	async function joinRoom(event) {
		roomBusy = true
		lobbyError = ''
		try {
			const result = await lobbyClient().join(event.detail.roomId, playerName, event.detail.accessCode)
			enterRoom(result)
		} catch (error) {
			lobbyError = roomErrorText(error)
		} finally {
			roomBusy = false
		}
	}

	function resumeRoom() {
		if (!hasSavedRoom) return
		view = roomToken && currentActorId === 'p1' && !roomInfo?.joined ? 'waiting' : 'game'
		activeTab = 'partie'
		if (view === 'waiting') refreshRoomStatus()
		else refreshMatchState()
	}

	function returnToLobby() {
		view = 'lobby'
		activeTab = 'partie'
		refreshRooms()
	}
	function currentClients() {
		const config = {
			baseUrl: apiBaseUrl,
			apiKeyHeaderName,
			apiKeyValue,
			roomToken
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
		debugLastRequest = request
		debugLastResponse = response
	}

	/** @param {unknown} value @param {string} [keyPath] @returns {unknown} */
	function sanitizeDebug(value, keyPath = '') {
		const secretKey = /authorization|token|api[_-]?key|password|secret|keyvalue/i.test(keyPath)

		if (value === null || value === undefined) {
			return value
		}

		if (typeof value === 'string') {
			if (secretKey) {
				return '********'
			}
			return value
		}

		if (typeof value !== 'object') {
			return value
		}

		if (Array.isArray(value)) {
			return value.map((entry, index) => sanitizeDebug(entry, `${keyPath}[${index}]`))
		}

		const out = /** @type {Record<string, unknown>} */ ({})
		for (const [key, inner] of Object.entries(value)) {
			out[key] = sanitizeDebug(inner, key)
		}
		return out
	}

	/** @param {unknown} value */
	function debugJson(value) {
		const data = maskSecrets ? sanitizeDebug(value) : value
		return JSON.stringify(data ?? {}, null, 2)
	}

	async function focusActivePhase() {
		if (view !== 'game' || activeTab !== 'partie' || typeof window === 'undefined') return
		await tick()
		if (document.documentElement.scrollHeight <= window.innerHeight + 16) return
		const focus = matchState?.state === 'FINISHED'
			? { key: 'result', selector: '.match-overview' }
			: document.querySelector('.secretActionActive')
				? { key: 'secret', selector: '.secretActionActive .secret-panel' }
				: document.querySelector('.feedbackActionActive')
					? { key: 'feedback', selector: '.feedbackActionActive .response-panel' }
					: document.querySelector('.guessActionActive')
						? { key: 'guess', selector: '.guessActionActive .own-board' }
						: null
		if (!focus) return
		const key = `${currentMatchId}:${matchState?.gameNumber ?? 1}:${focus.key}`
		if (key === lastScrollFocusKey) return
		const target = document.querySelector(focus.selector)
		if (!target) return
		lastScrollFocusKey = key
		const topbarHeight = document.querySelector('.topbar')?.getBoundingClientRect().height ?? 0
		const top = window.scrollY + target.getBoundingClientRect().top - topbarHeight - 8
		window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
	}

	async function refreshMatchState() {
		if (!matchState) {
			syncStatus = 'syncing'
			syncMessage = 'Synchronisation en cours'
		} else {
			syncMessage = 'Actualisation en cours'
		}
		const { matchClient } = currentClients()

		const request = {
			endpoint: `GET ${matchClient.endpoint}`,
			matchId: currentMatchId,
			actorId: currentActorId,
			apiKeyHeaderName,
			apiKeyValue,
			roomToken
		}

		try {
			const result = await matchClient.getMatchState(currentMatchId, currentActorId)
			if (typeof result.version === 'number' && Number.isFinite(result.version)) {
				lastKnownVersion = result.version
			}
			if (matchState && result.gameNumber && result.gameNumber !== matchState.gameNumber) {
				setupSequence = []
				draftGuess = ["", "", "", ""]
				selectedSetupSlot = 0
				selectedGuessSlot = 0
			}
			matchState = result
			syncStatus = 'ok'
			focusActivePhase()
			syncMessage = 'Données backend reçues'
			logDebug(request, { accepted: true, body: result })
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error)
			syncStatus = 'error'
			syncMessage = `Erreur backend: ${message}`
			logDebug(request, { accepted: false, error: message })
		}
	}

	async function resolveExpectedVersion() {
		const currentVersion = matchState?.version ?? lastKnownVersion
		if (typeof currentVersion === 'number' && Number.isFinite(currentVersion) && currentVersion > 0) {
			return currentVersion
		}

		const precisePath = apiBaseUrl ? '/local/match-state' : '/api/local/match-state'
		const preciseClient = createMatchStateClient({
			baseUrl: apiBaseUrl,
			path: precisePath,
			apiKeyHeaderName,
			apiKeyValue,
			roomToken
		})

		const preciseState = await preciseClient.getMatchState(currentMatchId, currentActorId)
		if (typeof preciseState.version !== 'number' || !Number.isFinite(preciseState.version)) {
			throw new Error('CLIENT:VERSION_UNAVAILABLE')
		}

		lastKnownVersion = preciseState.version
		matchState = {
			...(matchState ?? {}),
			...preciseState
		}

		return preciseState.version
	}

	/** @param {Record<string, unknown>} actionPayload */
	async function submitPayload(actionPayload) {
		if (submitInFlight) {
			return false
		}

		submitInFlight = true
		try {
			return await performSubmitPayload(actionPayload)
		} finally {
			submitInFlight = false
		}
	}

	/** @param {Record<string, unknown>} actionPayload */
	async function performSubmitPayload(actionPayload) {
		if (syncStatus === 'error' || !matchState) {
			openToast('error', 'Backend non synchronisé')
			await refreshMatchState()
			return false
		}

		pendingActionType = String(actionPayload.actionType ?? '')
		submitStatus = 'loading'
		submitError = ''

		let expectedVersion
		try {
			expectedVersion = await resolveExpectedVersion()
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error)
			submitStatus = 'error'
			pendingActionType = ''
			submitError = message
			openToast('error', `Version backend indisponible: ${message}`)
			await refreshMatchState()
			return false
		}

		const actionType = String(actionPayload.actionType ?? '')
		const actionCountBefore = (matchState?.turns ?? []).filter((entry) =>
			entry?.actorId === currentActorId && entry?.actionType === actionType
		).length
		const gameNumberBefore = Number(matchState?.gameNumber ?? 1)
		const { submitClient } = currentClients()
		const request = {
			endpoint: `POST ${submitClient.endpoint}`,
			matchId: currentMatchId,
			actorId: currentActorId,
			expectedVersion,
			actionPayload,
			apiKeyHeaderName,
			apiKeyValue,
			roomToken
		}

		const syncVersionState = (version, status) => {
			if (typeof version === 'number' && Number.isFinite(version)) {
				lastKnownVersion = version
				if (matchState) {
					matchState = {
						...matchState,
						version,
						state: typeof status === 'string' && status ? status : matchState.state
					}
				}
			}
		}

		const submitOnce = (expectedVersion) => submitClient.submitAction({
			matchId: currentMatchId,
			actorId: currentActorId,
			expectedVersion,
			actionPayload
		})

		try {
			const response = await submitOnce(expectedVersion)

			if (typeof response.version === 'number' && Number.isFinite(response.version)) {
				syncVersionState(response.version, response.status)
			}

			submitStatus = 'success'
			pendingActionType = ''
			logDebug(request, response)
			openToast('success', 'Action envoyée')
			await refreshMatchState()
			return true
		} catch (caughtError) {
			let submissionError = caughtError
			const submitErrorDetails = /** @type {SubmitClientError} */ (submissionError instanceof Error ? submissionError : new Error(String(submissionError)))
			if (submitErrorDetails.code === 'VERSION_CONFLICT') {
				await refreshMatchState()
				try {
					const retryResponse = await submitOnce(matchState?.version ?? lastKnownVersion)
					syncVersionState(retryResponse.version, retryResponse.status)
					submitStatus = 'success'
					pendingActionType = ''
					logDebug(request, { retriedAfterVersionConflict: true, body: retryResponse })
					openToast('success', 'Action renvoyee apres resynchronisation')
					await refreshMatchState()
					return true
				} catch (retryError) {
					submissionError = retryError
				}
			}

			await refreshMatchState()
			const actionCountAfter = (matchState?.turns ?? []).filter((entry) =>
				entry?.actorId === currentActorId && entry?.actionType === actionType
			).length
			const rematchApplied = actionType === 'REQUEST_REMATCH'
				&& Number(matchState?.gameNumber ?? 1) > gameNumberBefore
			if (actionCountAfter > actionCountBefore || rematchApplied) {
				submitStatus = 'success'
				pendingActionType = ''
				submitError = ''
				logDebug(request, { accepted: true, recoveredFromResponseError: true })
				openToast('success', 'Action confirmée après resynchronisation')
				return true
			}

			const message = submissionError instanceof Error ? submissionError.message : String(submissionError)
			submitStatus = 'error'
			pendingActionType = ''
			submitError = message
			logDebug(request, { accepted: false, error: submitError })
			openToast('error', `Échec envoi: ${submitError}`)
			return false
		}
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
		if (!canSubmitSecret) {
			openToast('error', 'Action non autorisee a ce stade')
			return
		}

		const normalizedCode = normalizeSymbolSequence(setupSequence, CODE_LENGTH, symbolPalette)
		if (normalizedCode.length !== CODE_LENGTH) {
			openToast('error', 'Code secret invalide (4 symboles)')
			return
		}

		submitPayload(buildActionPayload('READY_SECRET', normalizedCode))
	}

	/** @param {string} symbol */
	function chooseSetupSymbol(symbol) {
		setupSequence[selectedSetupSlot] = symbol
		if (selectedSetupSlot < 3) {
			selectedSetupSlot += 1
		}
		setupSequence = setupSequence.filter(Boolean)
	}

	function clearSetupSequence() {
		setupSequence = []
		selectedSetupSlot = 0
	}

	/** @param {number} index */
	function setSelectedSetupSlot(index) {
		selectedSetupSlot = index
	}

	/** @param {string} symbol */
	function chooseGuessSymbol(symbol) {
		draftGuess[selectedGuessSlot] = symbol
		if (selectedGuessSlot < 3) {
			selectedGuessSlot += 1
		}
		draftGuess = [...draftGuess]
	}

	function clearGuess() {
		draftGuess = ['', '', '', '']
		selectedGuessSlot = 0
	}

	/** @param {number} index */
	function setSelectedGuessSlot(index) {
		selectedGuessSlot = index
	}

	async function submitGuess() {
		if (!canSubmitGuess) {
			openToast('error', 'Tentative non autorisee a ce stade')
			return
		}

		const normalizedGuess = normalizeSymbolSequence(guessSequence, CODE_LENGTH, symbolPalette)
		if (normalizedGuess.length !== CODE_LENGTH) {
			openToast('error', 'Séquence de tentative invalide')
			return
		}

		if (await submitPayload(buildActionPayload('PLAY_GUESS', normalizedGuess))) clearGuess()
	}

	/** @param {{ bienPlaces: number, malPlaces: number }} detail */
	function submitFeedbackAction(detail) {
		if (!isBackendReady || matchState?.state !== 'WAITING_FEEDBACK' || ownFeedbackSubmitted || !opponentGuessSubmitted) {
			openToast('error', 'Action non autorisee a ce stade')
			return
		}

		submitPayload(buildActionPayload('SEND_FEEDBACK', null, detail))
	}

	/** @param {CustomEvent<{ bienPlaces: number, malPlaces: number }>} event */
	function onSubmitFeedback(event) {
		submitFeedbackAction(event.detail)
	}

	function requestRematch() {
		if (matchState?.state === "FINISHED") submitPayload(buildActionPayload("REQUEST_REMATCH"))
	}/** @param {number | null | undefined} value */
	function formatTimestamp(value) {
		if (!value) {
			return '--:--:--'
		}
		return new Date(value).toLocaleTimeString('fr-FR')
	}/** @param {unknown} value @returns {string[]} */
	function asSymbols(value) {
		if (!Array.isArray(value)) {
			return []
		}
		return value.map((entry) => String(entry))
	}

	/** @param {unknown} value @returns {number | null} */
	function asTimestamp(value) {
		if (typeof value === 'number' && Number.isFinite(value)) {
			return value
		}
		return null
	}

	/** @param {CustomEvent<{ tab: string }>} event */
	function onNavChange(event) {
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

	$: isPrototype = syncStatus === 'error' || !matchState
	$: isBackendReady = syncStatus !== 'error' && Boolean(matchState)
	$: guessSequence = draftGuess.filter(Boolean)
	$: setupSlots = Array.from({ length: CODE_LENGTH }, (_, index) => (hasSecretSet && matchState?.visibleSecretCode?.length === CODE_LENGTH ? matchState.visibleSecretCode : setupSequence)[index] ?? '_')
	$: actorId = currentActorId
	$: actorOrder = matchState?.players ?? []
	$: opponentActorId = actorOrder.find((entry) => entry !== actorId) ?? null
	$: matchActions = matchState?.turns ?? []
	$: roundNumber = Math.floor(matchActions.filter((entry) => entry?.actionType === 'SEND_FEEDBACK').length / Math.max(actorOrder.length, 1))
	$: ownGuessSubmitted = matchActions.filter((entry) => entry?.actionType === 'PLAY_GUESS' && entry?.actorId === actorId).length > roundNumber
	$: ownFeedbackSubmitted = matchActions.filter((entry) => entry?.actionType === 'SEND_FEEDBACK' && entry?.actorId === actorId).length > roundNumber
	$: opponentGuessSubmitted = matchActions.filter((entry) => entry?.actionType === 'PLAY_GUESS' && entry?.actorId === opponentActorId).length > roundNumber
	$: isMyTurn = isBackendReady && matchState?.state === 'WAITING_GUESS' && !ownGuessSubmitted
	$: hasSecretSet = (matchState?.readyPlayers ?? []).includes(actorId) || matchState?.visibleSecretCode?.length === CODE_LENGTH
	$: hasOpponentSecretSet = opponentActorId ? (matchState?.readyPlayers ?? []).includes(opponentActorId) : false
	$: hasPlayerReady = hasSecretSet
	$: hasOpponentReady = hasOpponentSecretSet
	$: isSecretSubmitting = submitStatus === 'loading' && pendingActionType === 'READY_SECRET'
	$: isGuessSubmitting = submitStatus === 'loading' && pendingActionType === 'PLAY_GUESS'
	$: canSubmitSecret = isBackendReady && matchState?.state === 'PREPARATION' && !hasSecretSet && !isSecretSubmitting
	$: canSubmitGuess = isMyTurn && hasSecretSet && hasOpponentSecretSet && guessSequence.length === CODE_LENGTH && !isGuessSubmitting
	$: secretButtonLabel = isSecretSubmitting
		? 'TRANSMISSION...'
		: (hasSecretSet ? 'CODE DEJA VALIDE' : (matchState?.state !== 'PREPARATION' ? 'PREPARATION TERMINEE' : 'VALIDER CODE SECRET'))
	$: sendButtonLabel = !isBackendReady
		? 'BACKEND HORS LIGNE'
		: (!hasSecretSet || !hasOpponentSecretSet
			? 'VALIDEZ LES DEUX CODES'
			: (matchState?.state === 'WAITING_FEEDBACK'
				? 'REPONDEZ A L ADVERSAIRE'
				: (ownGuessSubmitted ? 'TENTATIVE ENVOYEE' : (guessSequence.length !== CODE_LENGTH ? 'SEQUENCE INCOMPLETE' : 'ENVOYER TENTATIVE'))))
	$: historyRows = (matchState?.turns ?? []).slice().reverse()
	$: latestAction = historyRows[0] ?? null
	$: latestOwnAction = historyRows.find((entry) => entry?.actorId === actorId) ?? null
	$: latestOpponentAction = opponentActorId ? (historyRows.find((entry) => entry?.actorId === opponentActorId) ?? null) : null
	$: opponentDisplayName = currentActorId === "p1" ? (roomInfo?.guestPseudo || "Adversaire") : (roomInfo?.hostPseudo || "Adversaire")
	$: displayPlayerName = playerName.trim() || (currentActorId === 'p2' ? 'Joueur 2' : 'Joueur 1')
	$: phaseStepDone = (matchState?.submittedPlayers ?? []).includes(currentActorId)
	$: opponentStepDone = Boolean(opponentActorId && (matchState?.submittedPlayers ?? []).includes(opponentActorId))
	$: phaseTitle = matchState?.state === 'PREPARATION' ? 'Choisissez votre code secret' :
		matchState?.state === 'WAITING_GUESS' ? 'Trouvez le code adverse' :
		matchState?.state === 'WAITING_FEEDBACK' ? 'Donnez les indices à votre adversaire' :
		matchState?.state === 'FINISHED' ? 'Partie terminée' : 'Connexion à la partie'
	$: phaseDetail = matchState?.state === 'PREPARATION'
		? (phaseStepDone ? 'Votre code est validé. Attendez celui de votre adversaire.' : 'Composez quatre symboles et validez votre code.')
		: matchState?.state === 'WAITING_GUESS'
			? (phaseStepDone ? 'Votre tentative est envoyée. Attendez votre adversaire.' : 'Préparez puis envoyez votre tentative.')
			: matchState?.state === 'WAITING_FEEDBACK'
				? (phaseStepDone ? 'Vos indices sont envoyés. Attendez votre adversaire.' : 'Comparez sa tentative à votre code et donnez les indices.')
				: matchState?.state === 'FINISHED' ? 'Consultez le résultat et proposez une revanche.' : syncMessage
	$: feedbackCount = (matchState?.turns ?? []).filter((entry) => entry?.actionType === 'SEND_FEEDBACK').length
	$: displayRound = matchState?.state === 'FINISHED' ? Math.max(1, Math.ceil(feedbackCount / 2)) : Math.floor(feedbackCount / 2) + 1
	$: displayedRuntimeConfig = { ...runtimeConfig, matchId: currentMatchId, actorId: currentActorId }

	onMount(() => {
		playerName = sessionStorage.getItem('arenaPlayerName') || ''
		const savedId = sessionStorage.getItem('arenaMatchId')
		if (savedId) {
			currentMatchId = savedId
			currentActorId = sessionStorage.getItem('arenaActorId') || 'p1'
			roomName = sessionStorage.getItem('arenaRoomName') || savedId
			roomToken = sessionStorage.getItem('arenaRoomToken') || ''
			hasSavedRoom = true
			view = roomToken && currentActorId === 'p1' ? 'waiting' : 'game'
			if (view === 'waiting') refreshRoomStatus()
			else {
				if (roomToken) lobbyClient().status(currentMatchId).then((status) => roomInfo = status).catch(() => {})
				refreshMatchState()
			}
		}
		const timer = setInterval(() => {
			if (view === 'game') refreshMatchState()
			else if (view === 'waiting') refreshRoomStatus()
			else if (view === 'lobby') refreshRooms()
		}, POLL_MS)
		return () => clearInterval(timer)
	})
	onDestroy(() => {
		if (toastTimer) {
			clearTimeout(toastTimer)
		}
	})
</script>

<div class="app-root">
	<div class="bg-grid"></div>

	<TopBar {debugEnabled} homeAvailable={view === 'game' || view === 'waiting'} on:home={returnToLobby} on:opendebug={() => showDebugDrawer = true} />

	<main class="screen">
		{#if view === 'welcome'}
			<WelcomeScreen bind:playerName canResume={hasSavedRoom} on:continue={continueWelcome} on:resume={resumeRoom} />
		{/if}
		{#if view === 'lobby'}
			<LobbyScreen {playerName} {rooms} busy={roomBusy} error={lobbyError} canResume={hasSavedRoom} ownRoomId={currentActorId === "p1" && roomToken ? currentMatchId : ""} on:back={() => view = 'welcome'} on:refresh={refreshRooms} on:createroom={createRoom} on:joinroom={joinRoom} on:resume={resumeRoom} />
		{/if}
		{#if view === 'waiting'}
			<section class="waiting-room">
				<span>PARTIE CRÉÉE</span><h1>{roomName}</h1>
				<p>En attente d’un deuxième joueur. Votre partie apparaît dans la liste des parties en attente.</p>
				<p>{roomInfo?.locked ? 'Un code d’accès est requis pour la rejoindre.' : 'Tout joueur peut la rejoindre.'}</p>
				<div><button type="button" on:click={refreshRoomStatus}>ACTUALISER</button><button type="button" on:click={returnToLobby}>VOIR LES PARTIES</button></div>
			</section>
		{/if}
		{#if view === 'game'}
		<section class="match-overview" aria-live="polite">
			<div class="overview-top">
				<div class="overview-identity"><span class="player-avatar">{currentActorId === 'p2' ? 'P2' : 'P1'}</span><div><small>VOUS JOUEZ EN TANT QUE</small><strong>{displayPlayerName}</strong></div></div>
			</div>
			<div class="overview-match"><span>PARTIE</span><strong>{roomName || currentMatchId}</strong><small>Partie {matchState?.gameNumber ?? 1} · Manche {displayRound}</small></div>
			{#if matchState?.state === 'FINISHED'}
				<div class="overview-final-score" aria-label={'Score final : ' + displayPlayerName + ' ' + (matchState?.scores?.[currentActorId] ?? 0) + ', ' + opponentDisplayName + ' ' + (matchState?.scores?.[opponentActorId] ?? 0)}>
					<span class="score-player" class:winner={matchState?.winnerId === currentActorId}><small>VOUS · {displayPlayerName}</small><strong>{matchState?.scores?.[currentActorId] ?? 0}</strong></span>
					<span class="score-separator" aria-hidden="true">—</span>
					<span class="score-player" class:winner={matchState?.winnerId === opponentActorId}><strong>{matchState?.scores?.[opponentActorId] ?? 0}</strong><small>{opponentDisplayName} · ADVERSAIRE</small></span>
				</div>
			{:else}
				<div class="overview-phase"><span>À FAIRE</span><strong>{phaseTitle}</strong><p>{phaseDetail}</p></div>
				<div class="overview-foot"><span>SCORE · {matchState?.scores?.[currentActorId] ?? 0} — {matchState?.scores?.[opponentActorId] ?? 0}</span></div>
			{/if}
			{#if syncStatus === 'error'}<div class="overview-foot"><button type="button" on:click={refreshMatchState}>RÉESSAYER LA CONNEXION</button></div>{/if}
		</section>
		{/if}

		{#if view === 'game' && activeTab === 'partie'}
			<PartieScreen
				{matchState}
				{isMyTurn}
				{actorId}

				{hasPlayerReady}
				{hasOpponentReady}
				{hasSecretSet}
				opponentName={opponentDisplayName}
				{opponentStepDone}

				{setupSequence}
				{setupSlots}
				{selectedSetupSlot}
				{draftGuess}
				{selectedGuessSlot}
				{guessSequence}
				{symbolPalette}
				{isSecretSubmitting}
				{isGuessSubmitting}
				{isBackendReady}
				{canSubmitSecret}
				{secretButtonLabel}
				{canSubmitGuess}
				{sendButtonLabel}
				{latestAction}
				{latestOwnAction}
				{latestOpponentAction}
				{opponentActorId}
				{asSymbols}
				{getSymbolVisual}
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
			/>
		{/if}

		{#if view === 'game' && activeTab === 'historique'}
			<HistoryScreen
				runtimeConfig={displayedRuntimeConfig}
				{isPrototype}
				{historyRows}
				{asSymbols}
				{getSymbolVisual}
				{formatTimestamp}
				{asTimestamp}
				onRefresh={refreshMatchState}
			/>
		{/if}
	</main>

	{#if view === "game"}<BottomNav {activeTab} on:change={onNavChange} />{/if}

	<DebugDrawer
		open={showDebugDrawer}
		{syncStatus}
		bind:maskSecrets
		{debugLastRequest}
		{debugLastResponse}
		{debugJson}
		{syncMessage}
		{submitError}
		on:close={() => showDebugDrawer = false}
	/>

	<ToastMessage {toast} />
</div>

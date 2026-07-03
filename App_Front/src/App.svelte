<script>
	import { onDestroy, onMount } from 'svelte'
	import { createMatchStateClient } from './lib/api/matchStateClient'
	import { createSubmitActionClient } from './lib/api/submitActionClient'
	import { runtimeConfig } from './lib/api/runtimeConfig'
	import { ALLOWED_SYMBOLS, CODE_LENGTH, normalizeSymbolSequence } from './lib/utils/symbolSequence'
	import { getSymbolVisual } from './lib/utils/symbolVisuals'
	import TopBar from './lib/components/TopBar.svelte'
	import BottomNav from './lib/components/BottomNav.svelte'
	import SessionScreen from './lib/components/SessionScreen.svelte'
	import PartieScreen from './lib/components/PartieScreen.svelte'
	import HistoryScreen from './lib/components/HistoryScreen.svelte'
	import SettingsScreen from './lib/components/SettingsScreen.svelte'
	import DebugDrawer from './lib/components/DebugDrawer.svelte'
	import ToastMessage from './lib/components/ToastMessage.svelte'

	const symbolPalette = [...ALLOWED_SYMBOLS]
	const POLL_MS = 6000

	/** @typedef {{ kind: 'success' | 'error', text: string } | null} ToastState */
	/** @typedef {{ turnNumber?: number, version?: number, turnActive?: boolean, status?: string, matchOutcomeStatus?: string, actionLog?: Array<Record<string, unknown>> }} MatchStateLike */

	let activeTab = 'session'
	let showDebugDrawer = false
	let maskSecrets = true

	let playerName = ''
	let roomCode = ''

	let setupSequence = /** @type {string[]} */ ([])
	let selectedSetupSlot = 0

	let draftGuess = ['', '', '', '']
	let selectedGuessSlot = 0

	let matchState = /** @type {MatchStateLike | null} */ (null)
	let lastKnownVersion = 0
	let syncStatus = 'idle'
	let syncMessage = ''
	let submitStatus = 'idle'
	let submitError = ''

	let toast = /** @type {ToastState} */ (null)
	let toastTimer = /** @type {ReturnType<typeof setTimeout> | null} */ (null)

	let debugLastRequest = /** @type {unknown} */ (null)
	let debugLastResponse = /** @type {unknown} */ (null)

	let apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
	let apiKeyHeaderName = import.meta.env.VITE_API_KEY_HEADER ?? 'X-API-Key'
	let apiKeyValue = import.meta.env.VITE_API_KEY ?? 'dev-submit-action-key'

	function currentClients() {
		const config = {
			baseUrl: apiBaseUrl,
			apiKeyHeaderName,
			apiKeyValue
		}

		return {
			matchClient: createMatchStateClient(config),
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

	async function refreshMatchState() {
		syncStatus = 'syncing'
		syncMessage = 'Synchronisation en cours'
		const { matchClient } = currentClients()

		const request = {
			endpoint: `GET ${matchClient.endpoint}`,
			matchId: runtimeConfig.matchId,
			actorId: runtimeConfig.actorId,
			apiKeyHeaderName,
			apiKeyValue
		}

		try {
			const result = await matchClient.getMatchState(runtimeConfig.matchId, runtimeConfig.actorId)
			if (typeof result.version === 'number' && Number.isFinite(result.version)) {
				lastKnownVersion = result.version
			}
			matchState = result
			syncStatus = 'ok'
			syncMessage = 'Données backend reçues'
			logDebug(request, { accepted: true, body: result })
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error)
			syncStatus = 'error'
			syncMessage = `Erreur backend: ${message}`
			logDebug(request, { accepted: false, error: message })
		}
	}

	/** @param {Record<string, unknown>} actionPayload */
	async function submitPayload(actionPayload) {
		if (syncStatus !== 'ok' || !matchState) {
			openToast('error', 'Backend non synchronisé')
			await refreshMatchState()
			return
		}

		submitStatus = 'loading'
		submitError = ''

		const { submitClient } = currentClients()
		const request = {
			endpoint: `POST ${submitClient.endpoint}`,
			matchId: runtimeConfig.matchId,
			actorId: runtimeConfig.actorId,
			expectedVersion: matchState?.version ?? lastKnownVersion,
			actionPayload,
			apiKeyHeaderName,
			apiKeyValue
		}

		try {
			const response = await submitClient.submitAction({
				matchId: runtimeConfig.matchId,
				actorId: runtimeConfig.actorId,
				expectedVersion: matchState?.version ?? lastKnownVersion,
				actionPayload
			})

			if (typeof response.version === 'number' && Number.isFinite(response.version)) {
				lastKnownVersion = response.version
				if (matchState) {
					matchState = {
						...matchState,
						version: response.version
					}
				}
			}

			submitStatus = 'success'
			logDebug(request, response)
			openToast('success', 'Action envoyée')
			await refreshMatchState()
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error)
			submitStatus = 'error'
			submitError = message
			logDebug(request, { accepted: false, error: submitError })
			openToast('error', `Échec envoi: ${submitError}`)
		}
	}

	function submitPlayerReady() {
		submitPayload({
			type: 'PLAYER_READY'
		})
	}

	function submitSecretCode() {
		const normalizedCode = normalizeSymbolSequence(setupSequence, CODE_LENGTH, symbolPalette)
		if (normalizedCode.length !== CODE_LENGTH) {
			openToast('error', 'Code secret invalide (4 symboles)')
			return
		}

		submitPayload({
			type: 'SECRET_CODE_SET',
			secretCode: normalizedCode
		})
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

	function submitGuess() {
		if (!canSubmitGuess) {
			return
		}

		const normalizedGuess = normalizeSymbolSequence(guessSequence, CODE_LENGTH, symbolPalette)
		if (normalizedGuess.length !== CODE_LENGTH) {
			openToast('error', 'Séquence de tentative invalide')
			return
		}

		submitPayload({
			type: 'SUBMIT_GUESS',
			guess: normalizedGuess
		})
	}

	function testConnection() {
		refreshMatchState().then(() => {
			if (syncStatus === 'ok') {
				openToast('success', 'Connexion backend valide')
			} else {
				openToast('error', 'Connexion backend indisponible')
			}
		})
	}

	/** @param {number | null | undefined} value */
	function formatTimestamp(value) {
		if (!value) {
			return '--:--:--'
		}
		return new Date(value).toLocaleTimeString('fr-FR')
	}

	function formatOutcome() {
		if (!matchState?.matchOutcomeStatus) {
			return 'EN COURS'
		}
		return matchState.matchOutcomeStatus
	}

	/** @param {unknown} value @returns {string[]} */
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

	$: isPrototype = syncStatus !== 'ok'
	$: isBackendReady = syncStatus === 'ok' && Boolean(matchState)
	$: ctaSessionLabel = roomCode.trim() ? 'REJOINDRE LE DUEL' : 'CRÉER UNE PARTIE'
	$: guessSequence = draftGuess.filter(Boolean)
	$: setupSlots = Array.from({ length: CODE_LENGTH }, (_, index) => setupSequence[index] ?? '_')
	$: turnActive = Boolean(matchState?.turnActive)
	$: canSubmitGuess = isBackendReady && turnActive && guessSequence.length === CODE_LENGTH && submitStatus !== 'loading'
	$: sendButtonLabel = submitStatus === 'loading'
		? 'TRANSMISSION...'
		: (!isBackendReady
			? 'BACKEND HORS LIGNE'
			: (!turnActive ? 'EN ATTENTE DU TOUR' : (guessSequence.length !== CODE_LENGTH ? 'SÉQUENCE INCOMPLÈTE' : 'ENVOYER L\'ACTION')))

	$: historyRows = (matchState?.actionLog ?? []).slice().reverse()
	$: latestAction = historyRows[0] ?? null

	onMount(() => {
		refreshMatchState()
		const timer = setInterval(refreshMatchState, POLL_MS)
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

	<TopBar {syncStatus} on:opendebug={() => showDebugDrawer = true} />

	<main class="screen">
		{#if activeTab === 'session'}
			<SessionScreen
				{isPrototype}
				{submitStatus}
				{isBackendReady}
				{ctaSessionLabel}
				bind:playerName
				bind:roomCode
				on:opendebug={() => showDebugDrawer = true}
				on:submitready={submitPlayerReady}
			/>
		{/if}

		{#if activeTab === 'partie'}
			<PartieScreen
				{matchState}
				{turnActive}
				{setupSequence}
				{setupSlots}
				{selectedSetupSlot}
				{draftGuess}
				{selectedGuessSlot}
				{guessSequence}
				{symbolPalette}
				{submitStatus}
				{isBackendReady}
				{canSubmitGuess}
				{sendButtonLabel}
				{latestAction}
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
			/>
		{/if}

		{#if activeTab === 'historique'}
			<HistoryScreen
				{runtimeConfig}
				{isPrototype}
				{historyRows}
				{asSymbols}
				{getSymbolVisual}
				{formatTimestamp}
				{asTimestamp}
				onRefresh={refreshMatchState}
			/>
		{/if}

		{#if activeTab === 'parametres'}
			<SettingsScreen
				bind:apiBaseUrl
				bind:apiKeyHeaderName
				bind:apiKeyValue
				{syncStatus}
				{matchState}
				{formatOutcome}
				onTestConnection={testConnection}
			/>
		{/if}
	</main>

	<BottomNav {activeTab} on:change={onNavChange} />

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

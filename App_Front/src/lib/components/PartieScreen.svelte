<script>
  import { createEventDispatcher } from 'svelte'

  /** @type {{ turnNumber?: number } | null} */
  export let matchState = null
  export let turnActive = false

  /** @type {string[]} */
  export let setupSequence = []
  /** @type {string[]} */
  export let setupSlots = []
  export let selectedSetupSlot = 0

  /** @type {string[]} */
  export let draftGuess = []
  export let selectedGuessSlot = 0
  /** @type {string[]} */
  export let guessSequence = []

  /** @type {string[]} */
  export let symbolPalette = []

  export let submitStatus = 'idle'
  export let isBackendReady = false
  export let canSubmitGuess = false
  export let sendButtonLabel = ''

  /** @type {{ actionType?: string, payloadSummary?: string, symbols?: unknown } | null} */
  export let latestAction = null
  /** @type {(value: unknown) => string[]} */
  export let asSymbols = (value) => []
  /** @type {(value: string | null | undefined) => { icon: string, token: string, toneClass: string }} */
  export let getSymbolVisual = (value) => ({ icon: '·', token: '_', toneClass: 'symbol-tone-muted' })

  const dispatch = createEventDispatcher()

  function setSetupSlot(index) {
    dispatch('selectsetupslot', { index })
  }

  function setGuessSlot(index) {
    dispatch('selectguessslot', { index })
  }

  function chooseSetup(symbol) {
    dispatch('choosesetupsymbol', { symbol })
  }

  function clearSetup() {
    dispatch('clearsetup')
  }

  function validateSecret() {
    dispatch('submitsecret')
  }

  function chooseGuess(symbol) {
    dispatch('chooseguesssymbol', { symbol })
  }

  function clearGuess() {
    dispatch('clearguess')
  }

  function submitGuess() {
    dispatch('submitguess')
  }
</script>

<section class="partie-screen">
  <article class="round-head panel">
    <div class="round-meta">
      <span>TOUR</span>
      <strong>{matchState?.turnNumber ?? 0}</strong>
    </div>
    <div class="phase">
      <h2>{turnActive ? 'À VOUS DE JOUER' : 'TOUR ADVERSE'}</h2>
      <p>{turnActive ? 'ANALYSE TACTIQUE REQUISE' : 'EN ATTENTE DE L\'ADVERSAIRE'}</p>
    </div>
  </article>

  <article class="panel">
    <div class="subhead">
      <span>Code secret</span>
      <small>{setupSequence.length}/4</small>
    </div>

    <div class="guess-slots">
      {#each setupSlots as symbol, idx}
        {@const visual = getSymbolVisual(symbol)}
        <button
          type="button"
          class="guess-slot {visual.toneClass} {selectedSetupSlot === idx ? 'active' : ''}"
          on:click={() => setSetupSlot(idx)}
        >
          <span class="symbol-icon">{visual.icon}</span>
          <span class="symbol-token">{visual.token}</span>
        </button>
      {/each}
    </div>

    <div class="palette">
      {#each symbolPalette as symbol}
        {@const visual = getSymbolVisual(symbol)}
        <button type="button" class="palette-btn {visual.toneClass}" on:click={() => chooseSetup(symbol)}>
          <span class="symbol-icon">{visual.icon}</span>
          <span class="symbol-token">{visual.token}</span>
        </button>
      {/each}
    </div>

    <div class="guess-actions">
      <button type="button" class="outline-btn" on:click={clearSetup}>RÉINITIALISER CODE</button>
      <button type="button" class="primary-btn" on:click={validateSecret} disabled={submitStatus === 'loading' || !isBackendReady || setupSequence.length !== 4}>
        VALIDER CODE SECRET
      </button>
    </div>
  </article>

  <article class="panel">
    <div class="subhead">
      <span>TENTATIVE EN COURS</span>
      <small>{guessSequence.length}/4</small>
    </div>

    <div class="guess-slots">
      {#each draftGuess as symbol, idx}
        {@const visual = getSymbolVisual(symbol || '_')}
        <button
          type="button"
          class="guess-slot {visual.toneClass} {selectedGuessSlot === idx ? 'active' : ''}"
          on:click={() => setGuessSlot(idx)}
        >
          <span class="symbol-icon">{visual.icon}</span>
          <span class="symbol-token">{visual.token}</span>
        </button>
      {/each}
    </div>

    <div class="palette">
      {#each symbolPalette as symbol}
        {@const visual = getSymbolVisual(symbol)}
        <button type="button" class="palette-btn {visual.toneClass}" on:click={() => chooseGuess(symbol)}>
          <span class="symbol-icon">{visual.icon}</span>
          <span class="symbol-token">{visual.token}</span>
        </button>
      {/each}
    </div>

    <div class="guess-actions">
      <button type="button" class="outline-btn" on:click={clearGuess}>RÉINITIALISER</button>
      <button
        type="button"
        class="primary-btn"
        disabled={!canSubmitGuess}
        on:click={submitGuess}
      >
        {sendButtonLabel}
      </button>
    </div>
  </article>

  {#if latestAction}
    <article class="panel">
      <div class="subhead">
        <span>Résultat précédent</span>
        <small>{latestAction.actionType}</small>
      </div>
      <p class="muted">{latestAction.payloadSummary}</p>
      {#if asSymbols(latestAction.symbols).length}
        <div class="symbol-row">
          {#each asSymbols(latestAction.symbols) as symbol}
            {@const visual = getSymbolVisual(symbol)}
            <span class="symbol-chip {visual.toneClass}">
              <span class="symbol-icon">{visual.icon}</span>
              <span class="symbol-token">{visual.token}</span>
            </span>
          {/each}
        </div>
      {/if}
    </article>
  {/if}
</section>

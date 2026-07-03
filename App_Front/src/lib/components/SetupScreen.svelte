<script>
  import { getSymbolVisual } from '../utils/symbolVisuals'

  export let matchState = /** @type {Record<string, any> | null} */ (null)
  export let submitStatus = 'idle'
  export let actorId = ''
  export let setupCodeText = ''
  export let parsedSymbols = /** @type {string[]} */ ([])
  export let onSetupCodeTextChange = /** @type {(event: Event | string) => void} */ (() => {})
  export let onSubmitSecretCodeSet = () => {}

  const symbolKeys = /** @type {Array<{label: string, value: string}>} */ ([
    { label: 'DIAMANT', value: 'D' },
    { label: 'HEXA', value: 'H' },
    { label: 'CERCLE', value: 'C' },
    { label: 'TRIANGLE', value: 'T' },
    { label: 'CARRE', value: 'Q' },
    { label: 'ETOILE', value: 'E' },
    { label: 'FLOCON', value: 'F' },
    { label: 'ECLAIR', value: 'L' }
  ])

  function appendSymbol(/** @type {string} */ symbol) {
    const current = Array.isArray(parsedSymbols) ? parsedSymbols.slice(0, 4) : []

    if (current.length >= 4) {
      return
    }

    onSetupCodeTextChange([...current, symbol].join(' '))
  }

  function clearSequence() {
    onSetupCodeTextChange('')
  }
</script>

<section class="setup-shell">
  <div class="setup-hero">
    <h3 class="screen-display">SECURISATION</h3>
    <p class="hint">INITIALISATION DE VOTRE SEQUENCE SECRETE</p>
  </div>

  <div class="card setup-code-panel">
    <div class="setup-code-header">
      <span class="section-kicker">VOTRE CODE</span>
      <span class="setup-code-count">{parsedSymbols.length}/4 DEFINI</span>
    </div>

    <div class="guess-slot-row setup-slots">
      {#each [0, 1, 2, 3] as slotIndex}
        {@const visual = getSymbolVisual(parsedSymbols[slotIndex])}
        <div class={`guess-slot setup-main-slot ${parsedSymbols[slotIndex] ? 'defense' : ''} ${visual.toneClass}`}>
          <span class="symbol-icon">{visual.icon}</span>
          <span class="symbol-token">{visual.token}</span>
        </div>
      {/each}
    </div>

    <p class="hint">Choisissez 4 symboles tactiques pour crypter votre position.</p>
  </div>

  <div class="setup-symbol-grid">
    {#each symbolKeys as symbol}
      {@const visual = getSymbolVisual(symbol.value)}
      <button type="button" class="setup-symbol-btn" on:click={() => appendSymbol(symbol.value)} disabled={parsedSymbols.length >= 4 || submitStatus === 'syncing'}>
        <span class={`setup-symbol-glyph ${visual.toneClass}`}>{visual.icon}</span>
        <span class="setup-symbol-token">{symbol.value}</span>
        <span class="setup-symbol-label">{symbol.label}</span>
      </button>
    {/each}
  </div>

  <div class="card setup-action-card">
    <label class="terminal-label" for="setup-code">Ajustement manuel</label>
    <div class="terminal-field">
      <span class="terminal-prefix">&gt;</span>
      <input
        id="setup-code"
        class="text-input terminal-input"
        type="text"
        placeholder="Ex: A B C D"
        value={setupCodeText}
        on:input={onSetupCodeTextChange}
      />
    </div>
    <button class="setup-confirm-btn" type="button" on:click={onSubmitSecretCodeSet} disabled={!matchState || submitStatus === 'syncing'}>
      CONFIRMER LA SEQUENCE
    </button>
    <button class="setup-reset-btn" type="button" on:click={clearSequence} disabled={submitStatus === 'syncing'}>
      REINITIALISER
    </button>
  </div>

  <div class="card setup-operator-footer">
    <div>
      <span class="section-kicker">OPERATEUR</span>
      <p>{actorId}</p>
    </div>
    <div>
      <span class="section-kicker">ADVERSAIRE</span>
      <p>EN ATTENTE...</p>
    </div>
  </div>
</section>

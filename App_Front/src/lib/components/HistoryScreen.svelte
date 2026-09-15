<script>
  /** @type {{ matchId: string }} */
  export let runtimeConfig = { matchId: 'local-match' }
  export let isPrototype = true
  /** @type {Array<Record<string, unknown>>} */
  export let historyRows = []

  /** @type {(value: unknown) => string[]} */
  export let asSymbols = (value) => []
  /** @type {(value: string | null | undefined) => { icon: string, token: string, toneClass: string }} */
  export let getSymbolVisual = (value) => ({ icon: '·', token: '_', toneClass: 'symbol-tone-muted' })
  /** @type {(value: number | null) => string} */
  export let formatTimestamp = (value) => '--:--:--'
  /** @type {(value: unknown) => number | null} */
  export let asTimestamp = (value) => null

  export let onRefresh = () => {}
</script>

<section class="historique-screen">
  <article class="panel">
    <div class="section-head">
      <span class="kicker">SESSION_ID: {runtimeConfig.matchId}</span>
      {#if isPrototype}
        <span class="prototype-badge">historique prototype</span>
      {/if}
    </div>
    <h2>Historique des tours</h2>

    <div class="history-list">
      {#if historyRows.length === 0}
        <p class="muted">Aucune action enregistrée pour le moment.</p>
      {:else}
        {#each historyRows as entry}
          <div class="history-row">
            <div class="history-col mono">{String(entry.turnNumber).padStart(2, '0')}</div>
            <div class="history-col">
              <div class="symbol-row">
                {#if asSymbols(entry.symbols).length === 0}
                  <span class="muted">--</span>
                {:else}
                  {#each asSymbols(entry.symbols) as symbol}
                    {@const visual = getSymbolVisual(symbol)}
                    <span class="symbol-chip {visual.toneClass}">
                      <span class="symbol-icon">{visual.icon}</span>
                      <span class="symbol-token">{visual.token}</span>
                    </span>
                  {/each}
                {/if}
              </div>
            </div>
            <div class="history-col grow">{entry.payloadSummary}</div>
            <div class="history-col mono">{formatTimestamp(asTimestamp(entry.recordedAtEpochMs))}</div>
          </div>
        {/each}
      {/if}
    </div>

    <button type="button" class="primary-btn" on:click={onRefresh}>
      RAFRAÎCHIR L'HISTORIQUE
    </button>
  </article>
</section>

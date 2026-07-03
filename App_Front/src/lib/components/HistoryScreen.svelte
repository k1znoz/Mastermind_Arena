<script>
  export let matchState = /** @type {Record<string, any> | null} */ (null)
  export let logEntries = /** @type {Array<Record<string, any>>} */ ([])

  $: primaryEntry = logEntries?.[0] ?? null
</script>

<div class="card history-shell history-shell-stitch">
  <div class="history-header">
    <div>
      <span class="history-kicker">SESSION_REPLAY_ACTIVE</span>
      <h3>MATCH HISTORY</h3>
    </div>
    <div class="history-filters">
      <button type="button" class="history-filter active">ALL EVENTS</button>
      <button type="button" class="history-filter">MES ACTIONS</button>
      <button type="button" class="history-filter">ACTIONS ADVERSES</button>
    </div>
  </div>

  <div class="history-subline">
    <span>status: {matchState?.status ?? 'unknown'} | version: {matchState?.version ?? '-'}</span>
    <span>turns: {matchState?.turnNumber ?? '-'}</span>
  </div>

  <div class="history-grid">
    <section class="card history-stream-panel">
      <div class="panel-subheader">
        <span>CORE_LOG_STREAM.stdout</span>
        <span class="status-dot"></span>
      </div>

      <div class="history-stream-list">
        {#if (logEntries ?? []).length > 0}
          {#each logEntries as entry}
            <p>
              <span class="log-time">[{entry.createdAt}]</span>
              <span class={`log-label ${entry.emphasis ?? 'muted'}`}>{entry.label}:</span>
              <span>{entry.detail}</span>
            </p>
          {/each}
        {:else}
          <p class="hint">Aucun evenement collecte pour le moment.</p>
        {/if}
      </div>
    </section>

    <aside class="card history-side-panel">
      <h3>ROUND_DETAILS_05</h3>
      {#if primaryEntry}
        <div class="history-side-card">
          <span class="history-kicker">INPUT_SEQUENCE</span>
          <p>{primaryEntry.label}</p>
        </div>

        <div class="history-side-card">
          <span class="history-kicker">FEEDBACK</span>
          <p>{primaryEntry.detail}</p>
        </div>

        <div class="history-side-metrics">
          <p>SYNC_STATE</p>
          <strong>STABLE</strong>
        </div>
      {:else}
        <p class="hint">Aucune entree detaillee.</p>
      {/if}
    </aside>
  </div>

  <div class="history-sync-pill">
    <span class="status-dot"></span>
    <span>PRET</span>
    <span>SYNC</span>
  </div>
</div>

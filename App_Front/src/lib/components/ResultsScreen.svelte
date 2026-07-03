<script>
  export let matchState = /** @type {Record<string, any> | null} */ (null)
  export let logEntries = /** @type {Array<Record<string, any>>} */ ([])

  function formatOutcome(matchState) {
    if (!matchState) {
      return 'No match state loaded'
    }

    if (matchState.status === 'CANCELLED') {
      return `Cancelled (${matchState.cancellationCode ?? 'no code'})`
    }

    if (matchState.matchOutcomeStatus) {
      return `${matchState.matchOutcomeStatus} (${matchState.matchOutcomeReason ?? 'no reason'})`
    }

    return 'Outcome pending'
  }

  $: compactLogs = (logEntries ?? []).slice(0, 6)
  $: finalSequence = Array.isArray(matchState?.visibleSecretCode) && matchState.visibleSecretCode.length > 0
    ? matchState.visibleSecretCode.join(' • ')
    : '-'
</script>

<section class="results-shell">
  <div class="results-hero">
    <div class="results-chip">MISSION ACCOMPLIE</div>
    <h3 class="screen-display">DOUBLE DECODE</h3>
    <p class="hint">Synchronisation terminee • 100% integrite</p>
  </div>

  <div class="card results-summary-card active-glow">
    <div class="panel-header">
      <div>
        <span class="history-kicker">PLAYER_01 VS ADVERSAIRE</span>
        <h3>ARENA LOG</h3>
      </div>
      <div class="role-pill">{matchState?.status ?? 'UNKNOWN'}</div>
    </div>

    <div class="results-stats-grid">
      <div>
        <span class="history-kicker">Outcome</span>
        <p>{formatOutcome(matchState)}</p>
      </div>
      <div>
        <span class="history-kicker">Turns</span>
        <p>{matchState?.turnNumber ?? '-'}</p>
      </div>
      <div>
        <span class="history-kicker">Version</span>
        <p>{matchState?.version ?? '-'}</p>
      </div>
    </div>

    <button class="results-share-btn" type="button">PARTAGER LE RAPPORT</button>
  </div>

  <section class="results-analysis">
    <h3 class="section-kicker">ANALYSE TACTIQUE DETAILLEE</h3>
    <div class="screen-grid">
      <div class="card results-analysis-card">
        <span class="history-kicker">SEQUENCE FINALE</span>
        <p>{finalSequence}</p>
      </div>

      <div class="card results-analysis-card">
        <span class="history-kicker">BONUS D'EFFICACITE</span>
        <p class="ok-state">+{Math.max((matchState?.turnNumber ?? 0) * 120, 0)} XP</p>
      </div>
    </div>
  </section>

  <div class="card results-log-stream">
    <div class="panel-subheader">
      <span>HISTORIQUE DES SEQUENCES</span>
      <span class="status-dot"></span>
    </div>

    <div class="results-log-list">
      {#if compactLogs.length > 0}
        {#each compactLogs as entry}
          <p>
            <span class="log-time">[{entry.createdAt}]</span>
            <span class={`log-label ${entry.emphasis ?? 'muted'}`}>{entry.label}</span>
            <span>{entry.detail}</span>
          </p>
        {/each}
      {:else}
        <p class="hint">Aucun log disponible.</p>
      {/if}
    </div>

    <div class="results-cta-stack">
      <button type="button" class="results-main-cta">REJOUER L'AFFRONTEMENT</button>
      <button type="button" class="results-secondary-cta">RETOUR AU HUB</button>
    </div>
  </div>
</section>

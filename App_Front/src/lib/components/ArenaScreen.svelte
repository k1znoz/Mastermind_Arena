<script>
  import { getSymbolVisual } from '../utils/symbolVisuals'

  export let matchState = /** @type {Record<string, any> | null} */ (null)
  export let submitStatus = 'idle'
  export let actorId = ''
  export let guessText = ''
  export let parsedGuess = /** @type {string[]} */ ([])
  export let defenseCode = /** @type {string[]} */ ([])
  export let attemptHistory = /** @type {Array<Record<string, any>>} */ ([])
  export let opponentAttempts = /** @type {Array<Record<string, any>>} */ ([])
  export let tacticalLog = /** @type {Array<Record<string, any>>} */ ([])
  export let onGuessTextChange = /** @type {(event: Event) => void} */ (() => {})
  export let onSubmitGuess = () => {}

  $: latestOpponentAttempt = opponentAttempts?.[0] ?? null
</script>

<div class="arena-stack">
  <div class="card arena-status-bar">
    <div class="arena-status-line">
      <span class="status-dot"></span>
      <span>moi: {actorId}</span>
    </div>
    <div class="arena-status-pill">turn {matchState?.turnNumber ?? '-'} | version {matchState?.version ?? '-'}</div>
    <div class="arena-status-line muted">target: {matchState?.currentActorId ?? '-'}</div>
  </div>

  <div class="screen-grid arena-panels">
    <section class="card arena-panel">
      <div class="panel-header">
        <div>
          <h3>OFFENSIVE : DECODAGE CIBLE</h3>
          <p class="hint">DECHIFFREMENT DU CODE ADVERSE</p>
        </div>
        <div class="role-pill">OPERATIONNEL</div>
      </div>

      <div class="attempt-list">
        {#if attemptHistory.length > 0}
          {#each attemptHistory as attempt, index}
            <div class="attempt-row">
              <span class="attempt-rank">#{attemptHistory.length - index}</span>
              <div class="attempt-code">
                {#each attempt.guess as symbol}
                  {@const visual = getSymbolVisual(symbol)}
                  <span class={`attempt-symbol ${visual.toneClass}`}>
                    <span class="symbol-icon">{visual.icon}</span>
                    <span class="symbol-token">{visual.token}</span>
                  </span>
                {/each}
              </div>
              <div class="attempt-meta">
                <span class:ok={attempt.accepted} class:error={!attempt.accepted}>{attempt.accepted ? 'ACCEPTED' : 'REJECTED'}</span>
                <span>v{attempt.version ?? '-'}</span>
              </div>
            </div>
          {/each}
        {:else}
          <p class="hint">Aucune tentative soumise pour le moment.</p>
        {/if}
      </div>

      <div class="guess-input-shell">
        <div class="arena-live-feed">
          <span>ANALYSING_VECTORS...</span>
        </div>
        <label class="terminal-label" for="arena-guess">Guess (4 symboles)</label>
        <div class="guess-slot-row">
          {#each [0, 1, 2, 3] as slotIndex}
            {@const visual = getSymbolVisual(parsedGuess[slotIndex])}
            <div class={`guess-slot ${visual.toneClass}`}>
              <span class="symbol-icon">{visual.icon}</span>
              <span class="symbol-token">{visual.token}</span>
            </div>
          {/each}
        </div>
        <div class="terminal-field">
          <span class="terminal-prefix">&gt;</span>
          <input
            id="arena-guess"
            class="text-input terminal-input"
            type="text"
            placeholder="Ex: A B C D"
            value={guessText}
            on:input={onGuessTextChange}
          />
        </div>
        <button type="button" on:click={onSubmitGuess} disabled={!matchState || submitStatus === 'syncing'}>
          TRANSMETTRE LA SEQUENCE
        </button>
      </div>
    </section>

    <section class="card arena-panel">
      <div class="panel-header">
        <div>
          <h3>DEFENSIVE : PROTECTION COEUR</h3>
          <p class="hint">PROTECTION DE L'INTEGRITE DU CODE</p>
        </div>
        <div class="role-pill ok-state">SYNC</div>
      </div>

      <div class="card arena-incoming-attempt">
        <div class="panel-header">
          <span>{latestOpponentAttempt ? `${latestOpponentAttempt.actorId}: TENTATIVE #${latestOpponentAttempt.turnNumber}` : 'AUCUNE TENTATIVE ADVERSE'}</span>
          <span class="role-pill">CORRECTION EN ATTENTE</span>
        </div>
        <div class="guess-slot-row">
          {#each [0, 1, 2, 3] as slotIndex}
            {@const visual = getSymbolVisual(latestOpponentAttempt?.guess?.[slotIndex])}
            <div class={`guess-slot ${visual.toneClass}`}>
              <span class="symbol-icon">{visual.icon}</span>
              <span class="symbol-token">{visual.token}</span>
            </div>
          {/each}
        </div>
        <div class="arena-feedback-mini-grid">
          <div>
            <span>EXACT</span>
            <p>--</p>
          </div>
          <div>
            <span>PRESENT</span>
            <p>--</p>
          </div>
          <div>
            <span>ABSENT</span>
            <p>--</p>
          </div>
        </div>
      </div>

      <div class="defense-code-shell">
        <span class="hint">VOTRE CLE PRIVEE</span>
        <div class="guess-slot-row defense">
          {#each [0, 1, 2, 3] as slotIndex}
            {@const visual = getSymbolVisual(defenseCode[slotIndex])}
            <div class={`guess-slot defense ${visual.toneClass}`}>
              <span class="symbol-icon">{visual.icon}</span>
              <span class="symbol-token">{visual.token}</span>
            </div>
          {/each}
        </div>
      </div>

      <div class="arena-log-shell">
        <div class="panel-subheader">
          <span>TACTICAL_LOGS_STREAM</span>
          <span class="status-dot"></span>
        </div>
        <div class="arena-log-list">
          {#if tacticalLog.length > 0}
            {#each tacticalLog as log, index}
              <article class={`log-entry ${log.emphasis ?? 'muted'}`}>
                <div class="log-entry-head">
                  <span class="log-index">{index + 1}</span>
                  <span class={`log-label ${log.emphasis ?? 'muted'}`}>{log.label}</span>
                  <span class="log-time">[{log.createdAt}]</span>
                </div>
                <p class="log-detail">{log.detail}</p>
              </article>
            {/each}
          {:else}
            <p class="hint">Aucun log pour le moment.</p>
          {/if}
        </div>
      </div>

      <button type="button" class="arena-correction-btn" disabled>
        CONFIRMER LA CORRECTION
      </button>
    </section>
  </div>
</div>

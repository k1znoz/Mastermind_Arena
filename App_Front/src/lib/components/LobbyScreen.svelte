<script>
  import MatchSnapshotCard from './MatchSnapshotCard.svelte'

  export let matchState = /** @type {Record<string, any> | null} */ (null)
  export let submitStatus = 'idle'
  export let onSubmitPlayerReady = () => {}

  function copyRoomId() {
    const value = String(matchState?.matchId ?? '')

    if (!value || typeof navigator === 'undefined' || !navigator.clipboard) {
      return
    }

    navigator.clipboard.writeText(value)
  }
</script>

<section class="lobby-shell">
  <section class="card lobby-room-id-card active-glow">
    <span class="section-kicker">Identification de salle</span>
    <button type="button" class="lobby-room-id-pill" on:click={copyRoomId}>
      <span>{matchState?.matchId ?? 'MMA-XXXX'}</span>
      <span class="lobby-copy-icon">⧉</span>
    </button>
    <p class="hint">Partagez ce code avec votre adversaire pour lancer la sequence de synchronisation.</p>
  </section>

  <div class="section-kicker-row">
    <span class="section-kicker">Operateurs connectes</span>
    <div class="section-line"></div>
  </div>

  <div class="lobby-operators-stack">
    <div class="card lobby-operator-card active-glow">
      <div class="lobby-operator-avatar">OP</div>
      <div class="lobby-operator-core">
        <h3>{matchState?.currentActorId ?? 'OPERATOR_01'}</h3>
        <p class="lobby-operator-state ok-state">Synchronise</p>
      </div>
      <div class="lobby-operator-rank">
        <span>RANK</span>
        <strong>DIAMOND III</strong>
      </div>
    </div>

    <div class="lobby-vs-divider">
      <span></span>
      <p>VS</p>
      <span></span>
    </div>

    <div class="card lobby-operator-card waiting-card">
      <div class="lobby-operator-avatar muted">?</div>
      <div class="lobby-operator-core">
        <h3>EN ATTENTE...</h3>
        <p class="lobby-operator-state">Hors ligne</p>
      </div>
      <div class="lobby-operator-rank">
        <span>PING</span>
        <strong>-- MS</strong>
      </div>
    </div>
  </div>

  <div class="screen-grid lobby-meta-grid">
    <div class="card lobby-meta-card">
      <span class="section-kicker">Mode de jeu</span>
      <p>PARALLELE 1V1</p>
    </div>
    <div class="card lobby-meta-card">
      <span class="section-kicker">Complexite</span>
      <p>NIVEAU 4 [BETA]</p>
    </div>
    <div class="card lobby-meta-card full">
      <span class="section-kicker">Limite de temps</span>
      <p>60 SEC / TOUR</p>
    </div>
  </div>

  <button class="lobby-launch-btn" type="button" on:click={onSubmitPlayerReady} disabled={!matchState || submitStatus === 'syncing'}>
    LANCER LA SEQUENCE
  </button>

  <div class="screen-grid lobby-grid">
    <div class="card operator-card">
      <h3>Session Logs</h3>
      <p class="hint">Acteur local branche au moteur.</p>
      <ul>
        <li>match created in backend runtime</li>
        <li>actor order: {(matchState?.actorOrder ?? []).join(' -> ') || '-'}</li>
        <li>turn active: {String(matchState?.turnActive ?? false)}</li>
      </ul>
    </div>
    <MatchSnapshotCard {matchState} />
  </div>
</section>

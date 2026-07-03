<script>
  export let matchState = /** @type {Record<string, any> | null} */ (null)
  export let syncStatus = 'idle'
  export let autoNavigationEnabled = true
  export let onRefresh = () => {}
  export let onToggleAutoNavigation = () => {}
</script>

<header class="topbar">
  <div class="brand-block">
    <span class="brand-mark">grid</span>
    <h1>MASTERMIND ARENA</h1>
  </div>

  <div class="topbar-match-pill desktop-only">
    <div class="topbar-pill-item">
      <span class="pill-dot danger"></span>
      <span>{String(matchState?.turnNumber ?? 0).padStart(2, '0')}:{String((matchState?.version ?? 0) % 60).padStart(2, '0')}</span>
    </div>
    <span class="topbar-pill-sep"></span>
    <div class="topbar-pill-item">
      <span class="pill-dot ok"></span>
      <span>SYNC {syncStatus === 'ok' ? 'NOMINAL' : syncStatus.toUpperCase()}</span>
    </div>
  </div>

  <div class="topbar-actions">
    <div class="network-pill topbar-sync-copy">
      <span>SYNC STATUS</span>
      <span class="accent-value">ACTIVE_LINK</span>
    </div>
    <button type="button" class="topbar-btn topbar-btn-ghost desktop-only" on:click={onRefresh}>Refresh</button>
    <button type="button" class="topbar-btn topbar-btn-tactical desktop-only" on:click={onToggleAutoNavigation}>
      AutoNav: {autoNavigationEnabled ? 'ON' : 'OFF'}
    </button>
    <div class="topbar-avatars desktop-only">
      <span>OP_01</span>
      <span>ADV</span>
    </div>
    <button type="button" class="antenna-btn" aria-label="network settings">◉</button>
  </div>
</header>

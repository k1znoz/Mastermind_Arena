<script>
  import { createEventDispatcher } from 'svelte'
  import { debugJson } from '../utils/diagnostics.js'

  export let open = false
  export let syncStatus = 'idle'
  /** @type {unknown} */
  export let debugLastRequest = null
  /** @type {unknown} */
  export let debugLastResponse = null
  export let syncMessage = ''
  export let submitError = ''

  const dispatch = createEventDispatcher()

  function close() {
    dispatch('close')
  }
</script>

{#if open}
  <button type="button" class="drawer-overlay" on:click={close} aria-label="Fermer debug"></button>
<aside class="debug-drawer" class:open={open}>
  <header>
    <div>
      <h3>DEBUG_CONSOLE</h3>
      <small>[BACKEND] {syncStatus === 'ok' ? 'SYNC_ACTIVE' : 'SYNC_LOST'}</small>
    </div>
    <button type="button" on:click={close}>FERMER</button>
  </header>

  <p class="debug-privacy">Les données privées sont masquées avant leur conservation dans ce diagnostic.</p>

  <section>
    <h4>Dernière requête</h4>
    <pre>{debugJson(debugLastRequest)}</pre>
  </section>

  <section>
    <h4>Dernière réponse</h4>
    <pre>{debugJson(debugLastResponse)}</pre>
  </section>

  <footer>
    <p>{syncMessage || 'Aucun signal backend'}</p>
    {#if submitError}
      <p class="error">{submitError}</p>
    {/if}
  </footer>
</aside>
{/if}

<style>
.drawer-overlay {
  position: fixed;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.52);
  backdrop-filter: blur(3px);
  z-index: 40;
}

.debug-drawer {
  position: fixed;
  top: 0;
  right: 0;
  width: min(520px, 100vw);
  height: 100dvh;
  z-index: 45;
  transform: translateX(105%);
  transition: transform 220ms ease;
  border-left: 1px solid rgba(0, 219, 233, 0.3);
  background: var(--panel-strong);
  display: grid;
  grid-template-rows: auto auto 1fr 1fr auto;
}

.debug-drawer.open {
  transform: translateX(0);
}

.debug-drawer header {
  padding: 14px;
  border-bottom: 1px solid var(--line-soft);
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.debug-drawer h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  color: var(--primary);
}

.debug-drawer small {
  font-family: "JetBrains Mono", monospace;
  color: var(--muted);
}

.debug-drawer header button {
  min-width: 96px;
  border: 1px solid var(--line-soft);
  background: transparent;
  color: var(--text);
  font-family: "JetBrains Mono", monospace;
  cursor: pointer;
}

.debug-privacy {
  margin:0;
  padding:10px 14px;
  border-bottom:1px solid var(--line-soft);
  color:var(--muted);
  font-size:12px;
  line-height:1.5;
}

.debug-drawer section {
  min-height: 0;
  padding: 10px 14px;
  border-bottom: 1px solid var(--line-soft);
  overflow: auto;
}

.debug-drawer h4 {
  margin: 0 0 8px;
  font-family: "JetBrains Mono", monospace;
  font-size: 11px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--muted);
}

.debug-drawer pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--primary-soft);
  font-size: 12px;
  font-family: "JetBrains Mono", monospace;
}

.debug-drawer footer {
  padding: 10px 14px;
  display: grid;
  gap: 6px;
  border-top: 1px solid var(--line-soft);
}

.debug-drawer footer p {
  margin: 0;
  font-family: "JetBrains Mono", monospace;
  font-size: 12px;
}


.error { color:var(--error); }
</style>

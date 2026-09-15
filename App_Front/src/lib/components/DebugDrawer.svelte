<script>
  import { createEventDispatcher } from 'svelte'

  export let open = false
  export let syncStatus = 'idle'
  export let maskSecrets = true
  /** @type {unknown} */
  export let debugLastRequest = null
  /** @type {unknown} */
  export let debugLastResponse = null
  /** @type {(value: unknown) => string} */
  export let debugJson = (value) => '{}'
  export let syncMessage = ''
  export let submitError = ''

  const dispatch = createEventDispatcher()

  function close() {
    dispatch('close')
  }
</script>

{#if open}
  <button type="button" class="drawer-overlay" on:click={close} aria-label="Fermer debug"></button>
{/if}

<aside class="debug-drawer" class:open={open}>
  <header>
    <div>
      <h3>DEBUG_CONSOLE</h3>
      <small>[BACKEND] {syncStatus === 'ok' ? 'SYNC_ACTIVE' : 'SYNC_LOST'}</small>
    </div>
    <button type="button" on:click={close}>FERMER</button>
  </header>

  <div class="debug-mask-toggle">
    <label>
      <input type="checkbox" bind:checked={maskSecrets} />
      Masquer secrets
    </label>
  </div>

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

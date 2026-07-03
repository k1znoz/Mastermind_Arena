<script>
  export let apiBaseUrl = ''
  export let apiKeyHeaderName = 'X-API-Key'
  export let apiKeyValue = ''

  export let syncStatus = 'idle'
  /** @type {{ status?: string } | null} */
  export let matchState = null
  export let formatOutcome = () => 'EN COURS'
  export let onTestConnection = () => {}
</script>

<section class="param-screen">
  <article class="panel">
    <h2>Paramètres</h2>
    <p class="muted">Configuration technique de l'API et des accès serveurs.</p>

    <label>
      <span>API BASE URL</span>
      <input type="text" bind:value={apiBaseUrl} placeholder="https://api.mastermind.arena" />
    </label>

    <label>
      <span>API KEY HEADER</span>
      <input type="text" bind:value={apiKeyHeaderName} placeholder="X-API-Key" />
    </label>

    <label>
      <span>API KEY VALUE</span>
      <input type="password" bind:value={apiKeyValue} placeholder="********" />
    </label>

    <button type="button" class="primary-btn" on:click={onTestConnection}>TESTER LA CONNEXION</button>

    <div class="diagnostic">
      <div>
        <span>STATUT RÉSEAU</span>
        <strong class={syncStatus === 'ok' ? 'ok' : 'error'}>{syncStatus === 'ok' ? 'ONLINE' : 'OFFLINE'}</strong>
      </div>
      <div>
        <span>ÉTAT MATCH</span>
        <strong>{matchState?.status ?? 'INCONNU'}</strong>
      </div>
      <div>
        <span>OUTCOME</span>
        <strong>{formatOutcome()}</strong>
      </div>
    </div>
  </article>
</section>

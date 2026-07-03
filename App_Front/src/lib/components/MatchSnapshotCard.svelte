<script>
  export let matchState = /** @type {Record<string, any> | null} */ (null)
</script>

<div class="card">
  <h3>Backend Match Snapshot</h3>
  {#if matchState}
    <ul>
      <li>status: {matchState.status}</li>
      <li>version: {matchState.version}</li>
      <li>turnNumber: {matchState.turnNumber}</li>
      <li>currentActorId: {matchState.currentActorId}</li>
      <li>turnActive: {String(matchState.turnActive)}</li>
    </ul>
  {:else}
    <p>No state loaded.</p>
  {/if}
</div>

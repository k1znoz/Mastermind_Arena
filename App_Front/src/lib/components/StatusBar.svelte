<script>
  export let syncStatus = 'idle'
  export let syncMessage = ''
  export let submitStatus = 'idle'
  export let submitMessage = ''
  export let matchId = ''
  export let actorId = ''
  export let currentActorId = ''
</script>

<section class="status-row status-telemetry">
  <p class={`sync status-chip ${syncStatus}`}>backend {syncStatus}{syncMessage ? ` | ${syncMessage}` : ''}</p>
  <p class={`sync status-chip ${submitStatus}`}>submit {submitStatus}{submitMessage ? ` | ${submitMessage}` : ''}</p>
  <p class="status-chip neutral">room: {matchId}</p>
  <p class="status-chip neutral">operator: {actorId}</p>
  {#if currentActorId}
    <p class="status-chip neutral">turn owner: {currentActorId}</p>
  {/if}
</section>

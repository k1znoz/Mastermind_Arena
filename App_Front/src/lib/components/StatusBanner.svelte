<script>
  export let status = 'active'
</script>

{#if status === 'active'}
  <p class="status-line">STATUS: TERMINAL ACCESS IN PROGRESS</p>
{:else if status === 'won'}
  <p class="status-line win">STATUS: MISSION ACCOMPLISHED</p>
{:else}
  <p class="status-line fail">STATUS: MISSION COMPROMISED</p>
{/if}

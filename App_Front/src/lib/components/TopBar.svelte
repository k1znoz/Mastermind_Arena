<script>
  import { createEventDispatcher } from 'svelte'

  export let syncStatus = 'idle'

  const dispatch = createEventDispatcher()

  function openDebug() {
    dispatch('opendebug')
  }
</script>

<header class="topbar">
  <div class="brand">
    <span class="brand-icon">⌁</span>
    <span class="brand-text">MASTERMIND ARENA</span>
  </div>
  <div class="topbar-right">
    <div class="latency">
      <span>LATENCE</span>
      <strong>{syncStatus === 'ok' ? '24ms' : '--'}</strong>
    </div>
    <button class="signal" type="button" on:click={openDebug}>◉</button>
  </div>
</header>

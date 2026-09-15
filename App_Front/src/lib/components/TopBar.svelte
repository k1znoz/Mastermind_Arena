<script>
  import { createEventDispatcher } from 'svelte'
  export let debugEnabled = false
  export let homeAvailable = false
  const dispatch = createEventDispatcher()
</script>

<header class="topbar">
  {#if homeAvailable}
  <button class="brand brand-home" type="button" on:click={() => dispatch('home')} aria-label="Retour au lobby" title="Retour au lobby">
    <span class="brand-icon" aria-hidden="true">◇</span>
    <span class="brand-text">MASTERMIND ARENA</span><span class="brand-home-hint"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 10.5 12 3l9 7.5"/><path d="M5.5 9.5V21h13V9.5"/><path d="M9.5 21v-7h5v7"/></svg> LOBBY</span>
  </button>
  {:else}
  <div class="brand"><span class="brand-icon" aria-hidden="true">◇</span><span class="brand-text">MASTERMIND ARENA</span></div>
  {/if}
  {#if debugEnabled}
    <button class="signal" type="button" on:click={() => dispatch('opendebug')} aria-label="Ouvrir les outils de développement">◉</button>
  {/if}
</header>
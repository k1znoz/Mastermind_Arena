<script>
  import { createEventDispatcher } from 'svelte'
  export let activeTab = 'partie'
  const dispatch = createEventDispatcher()
  function select(tab) { dispatch('change', { tab }) }
</script>

<nav class="bottom-nav" aria-label="Navigation principale">
  <button type="button" class:active={activeTab === 'partie'} on:click={() => select('partie')}>Plateau</button>
  <button type="button" class:active={activeTab === 'historique'} on:click={() => select('historique')}>Historique</button>
</nav>
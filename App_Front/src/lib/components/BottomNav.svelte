<script>
  import { createEventDispatcher } from 'svelte'

  export let activeTab = 'session'

  const dispatch = createEventDispatcher()

  function select(tab) {
    dispatch('change', { tab })
  }
</script>

<nav class="bottom-nav">
  <button type="button" class:active={activeTab === 'session'} on:click={() => select('session')}>Session</button>
  <button type="button" class:active={activeTab === 'partie'} on:click={() => select('partie')}>Partie</button>
  <button type="button" class:active={activeTab === 'historique'} on:click={() => select('historique')}>Historique</button>
  <button type="button" class:active={activeTab === 'parametres'} on:click={() => select('parametres')}>Paramètres</button>
</nav>

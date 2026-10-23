<script>
  import { createEventDispatcher } from 'svelte'
  export let activeTab = 'partie'
  export let compact = false
  const dispatch = createEventDispatcher()
  function select(tab) { dispatch('change', { tab }) }
</script>

<nav class="bottom-nav" class:compact aria-label="Navigation principale">
  <button type="button" aria-current={activeTab === 'partie' ? 'page' : undefined} class:active={activeTab === 'partie'} on:click={() => select('partie')}>Plateau</button>
  <button type="button" aria-current={activeTab === 'historique' ? 'page' : undefined} class:active={activeTab === 'historique'} on:click={() => select('historique')}>Historique</button>
</nav>

<style>

  .bottom-nav { position:fixed; inset:auto 0 0; z-index:25; height:62px; display:grid; grid-template-columns:repeat(2,1fr); background:var(--bg); border-top:1px solid var(--line); backdrop-filter:blur(12px); padding-bottom:env(safe-area-inset-bottom); }
  button { border:0; background:transparent; color:rgba(185,202,203,.66); font:14px "JetBrains Mono",monospace; letter-spacing:.04em; cursor:pointer; min-height:48px; }
  button.active { color:var(--primary); text-shadow:0 0 12px rgba(0,219,233,.7); }
  @media(max-width:650px) { .compact { height:50px; } .compact button { font-size:11px; } }

</style>

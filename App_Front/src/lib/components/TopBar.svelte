<script>
  import { createEventDispatcher } from 'svelte'
  export let debugEnabled = false
  export let homeAvailable = false
  export let theme = 'dark'
  export let compact = false
  const dispatch = createEventDispatcher()
</script>

<header class="topbar" class:compact>
  {#if homeAvailable}
  <button class="brand brand-home" type="button" on:click={() => dispatch('home')} aria-label="Retour au lobby" title="Retour au lobby">
    <span class="brand-icon" aria-hidden="true">◇</span>
    <span class="brand-text">MASTERMIND ARENA</span>
  </button>
  {:else}
  <div class="brand"><span class="brand-icon" aria-hidden="true">◇</span><span class="brand-text">MASTERMIND ARENA</span></div>
  {/if}
  {#if debugEnabled}
    <button class="signal" type="button" on:click={() => dispatch('opendebug')} aria-label="Ouvrir les outils de développement">◉</button>
  {/if}
  <button class="theme-toggle" type="button" on:click={() => dispatch('theme')} aria-label={theme === 'dark' ? 'Activer le thème clair' : 'Activer le thème sombre'}>{theme === 'dark' ? '☀ Clair' : '☾ Sombre'}</button>
</header>

<style>

  .topbar { position:fixed; inset:0 0 auto; z-index:20; height:64px; display:flex; align-items:center; justify-content:space-between; gap:8px; padding:0 16px; border-bottom:1px solid var(--line); background:var(--bg); backdrop-filter:blur(12px); }
  .brand { display:flex; align-items:center; gap:10px; }
  .brand-icon { color:var(--primary); font-size:24px; }
  .brand-text { font:700 20px "Space Grotesk",sans-serif; letter-spacing:-.02em; color:var(--primary); }
  .brand-home { min-width:0; border:0; padding:6px 8px; margin-left:-8px; background:transparent; cursor:pointer; text-align:left; }
  .brand-home:hover,.brand-home:focus-visible { background:rgba(0,219,233,.09); outline:1px solid var(--primary); }
  .signal { width:34px; height:34px; border-radius:999px; border:1px solid rgba(0,219,233,.4); background:transparent; color:var(--primary); cursor:pointer; }
  .theme-toggle { min-height:44px; flex:none; border:1px solid var(--line); border-radius:10px; background:var(--panel); color:var(--text); padding:8px 12px; cursor:pointer; }
  @media(max-width:650px) { .compact { height:44px; } .compact .brand-text { font-size:16px; } }
  @media(max-width:600px) { .topbar { padding:0 12px; } .brand { gap:6px; } .brand-text { font-size:16px; } .brand-icon { font-size:20px; } .theme-toggle { font-size:12px; padding:8px; } }

</style>

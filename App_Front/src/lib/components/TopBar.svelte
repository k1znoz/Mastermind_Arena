<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import diamond from '../assets/tabler/square-rotated.svg'
  import sun from '../assets/tabler/sun.svg'
  import moon from '../assets/tabler/moon.svg'
  export let debugEnabled = false
  export let homeAvailable = false
  export let theme = 'dark'
  export let duel = false
  export let arrival = false
  const dispatch = createEventDispatcher()
</script>

<header class="topbar" class:duel class:arrival>
  {#if homeAvailable}
  <button class="brand brand-home" type="button" on:click={() => dispatch('home')} aria-label="Retour au lobby" title="Retour au lobby">
    <span class="brand-icon"><Icon src={diamond} size={arrival ? 36 : 24} /></span>
    <span class="brand-text">MASTERMIND ARENA</span>
  </button>
  {:else}
  <div class="brand"><span class="brand-icon"><Icon src={diamond} size={arrival ? 36 : 24} /></span><span class="brand-text">MASTERMIND ARENA</span></div>
  {/if}
  {#if debugEnabled}
    <button class="signal" type="button" on:click={() => dispatch('opendebug')} aria-label="Ouvrir les outils de développement">◉</button>
  {/if}
  <button class="theme-toggle" type="button" on:click={() => dispatch('theme')} aria-label={theme === 'dark' ? 'Activer le thème clair' : 'Activer le thème sombre'}><Icon src={theme === 'dark' ? sun : moon} />{#if !arrival}<span>{theme === 'dark' ? 'Clair' : 'Sombre'}</span>{/if}</button>
</header>

<style>

  .topbar { position:fixed; inset:0 0 auto; z-index:20; height:64px; display:flex; align-items:center; justify-content:space-between; gap:8px; padding:0 16px; border-bottom:1px solid var(--line); background:var(--bg); backdrop-filter:blur(12px); }
  .brand { display:flex; align-items:center; gap:10px; }
  .brand-icon { display:flex; color:var(--primary); font-size:24px; }
  .brand-text { font:700 20px "Space Grotesk",sans-serif; letter-spacing:-.02em; color:var(--primary); }
  .brand-home { min-width:0; border:0; padding:6px 8px; margin-left:-8px; background:transparent; cursor:pointer; text-align:left; }
  .brand-home:hover,.brand-home:focus-visible { background:rgba(0,219,233,.09); outline:1px solid var(--primary); }
  .signal { width:34px; height:34px; border-radius:999px; border:1px solid rgba(0,219,233,.4); background:transparent; color:var(--primary); cursor:pointer; }
  .theme-toggle { display:flex; align-items:center; justify-content:center; gap:6px; min-height:44px; flex:none; border:1px solid var(--line); border-radius:10px; background:var(--panel); color:var(--text); padding:8px 12px; cursor:pointer; }
  .duel .brand-text { max-width:130px; font:600 13px/1.3 Inter,sans-serif; color:var(--text); }
  @media(min-width:851px) { .duel { left:216px; justify-content:flex-end; } .duel .brand { display:none; } }
  @media(max-width:600px) { .topbar { padding:0 12px; } .brand { gap:6px; } .brand-text { font-size:16px; } .brand-icon { font-size:20px; } .theme-toggle { font-size:12px; padding:8px; } }
  .arrival { height:104px; padding:20px 48px; border-bottom:0; background:var(--bg); backdrop-filter:none; }
  .arrival .brand { gap:14px; }
  .arrival .brand-text { max-width:130px; font:600 14px/1.3 Inter,sans-serif; color:var(--text); }
  .arrival .brand-icon { font-size:40px; }
  .arrival .theme-toggle { min-width:52px; min-height:48px; border-radius:12px; }
  @media(max-width:600px) { .arrival { height:80px; padding:16px 24px; } .arrival .brand { gap:10px; } .arrival .brand-text { max-width:120px; font-size:13px; } .arrival .brand-icon { font-size:32px; } }

</style>

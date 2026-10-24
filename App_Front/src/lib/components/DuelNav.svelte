<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import diamond from '../assets/tabler/square-rotated.svg'
  import circleDot from '../assets/tabler/circle-dot.svg'
  import chartBar from '../assets/tabler/chart-bar.svg'
  export let activeTab = 'partie'
  const dispatch = createEventDispatcher()
</script>

<nav class="duel-nav" aria-label="Navigation du duel">
  <button class="brand" type="button" aria-label="Retour au lobby" on:click={() => dispatch('home')}><Icon src={diamond} size={32} /><span>MASTERMIND<br />ARENA</span></button>
  <div class="destinations">
    <button type="button" aria-current={activeTab === 'partie' ? 'page' : undefined} class:active={activeTab === 'partie'} on:click={() => dispatch('change', { tab: 'partie' })}><Icon src={circleDot} /><span>Plateau</span></button>
    <button type="button" aria-current={activeTab === 'historique' ? 'page' : undefined} class:active={activeTab === 'historique'} on:click={() => dispatch('change', { tab: 'historique' })}><Icon src={chartBar} /><span>Historique</span></button>
  </div>
</nav>

<style>
  .duel-nav { position:fixed; inset:auto 0 0; z-index:25; height:calc(72px + env(safe-area-inset-bottom)); padding:8px 12px calc(8px + env(safe-area-inset-bottom)); border-top:1px solid var(--line); background:var(--bg); }
  .destinations { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; height:100%; }
  button { display:flex; justify-content:center; align-items:center; gap:12px; min-height:48px; padding:12px; border:0; border-radius:10px; background:transparent; color:var(--muted); font:500 14px Inter,sans-serif; cursor:pointer; }
  button.active { background:var(--accent-wash); color:var(--primary-soft); }
  .brand { display:none; }
  @media(min-width:851px) {
    .duel-nav { inset:0 auto 0 0; display:flex; flex-direction:column; gap:44px; width:216px; height:100dvh; padding:28px 16px; border-top:0; border-right:1px solid var(--line); }
    .brand { display:flex; justify-content:start; gap:12px; min-width:0; padding:4px 8px; color:var(--primary); text-align:left; }
    .brand span { color:var(--text); font-size:13px; line-height:1.3; }
    .destinations { height:auto; grid-template-columns:1fr; gap:12px; }
    .destinations button { justify-content:start; gap:16px; min-height:56px; padding:16px; font-size:16px; }
  }
</style>

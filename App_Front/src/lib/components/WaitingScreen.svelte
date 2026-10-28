<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import userCircle from '../assets/tabler/user-circle.svg'
  import refresh from '../assets/tabler/refresh.svg'
  import list from '../assets/tabler/list.svg'
  export let roomName = ''
  export let hostPseudo = ''
  export let locked = false
  export let error = ''
  export let refreshing = false
  const dispatch = createEventDispatcher()
</script>

<section class="waiting-screen">
  <header><p>Partie créée</p><h1 id="waiting-title" tabindex="-1">{roomName}</h1></header>
  <div class="players">
    <div class="player-card"><Icon src={userCircle} size={72} /><div><strong>{hostPseudo}</strong><p>Créateur de la partie</p></div></div>
    <div class="player-card available"><Icon src={userCircle} size={72} /><div><strong>Place disponible</strong><p>En attente d’un deuxième joueur</p></div></div>
  </div>
  <p class="availability">Votre partie apparaît dans les parties en attente.<br />{locked ? 'Un code d’accès est requis pour la rejoindre.' : 'Tout joueur peut la rejoindre.'}</p>
  {#if error}
    <div class="waiting-error" role="alert"><p>{error}</p><button type="button" on:click={() => dispatch('forget')}>Oublier cette reprise sur cet appareil</button></div>
  {/if}
  <div class="actions">
    <button class="primary" type="button" aria-label={refreshing ? 'Actualisation…' : 'Actualiser'} aria-disabled={refreshing} on:click={() => { if (!refreshing) dispatch('refresh') }}><Icon src={refresh} /><span role="status">{refreshing ? 'Actualisation…' : 'Actualiser'}</span></button>
    <button type="button" on:click={() => dispatch('lobby')}><Icon src={list} />Voir les parties</button>
  </div>
</section>

<style>
  .waiting-screen { display:grid; justify-items:center; gap:40px; width:100%; max-width:660px; margin:0 auto; padding:52px 0; text-align:center; }
  header { display:grid; gap:12px; width:100%; }
  h1 { margin:0; font:600 clamp(30px,4vw,42px)/1.2 Inter,sans-serif; letter-spacing:-.03em; overflow-wrap:anywhere; }
  p { margin:0; color:var(--muted); font:400 16px/1.5 Inter,sans-serif; }
  header p { font-size:20px; }
  .players { display:grid; grid-template-columns:1fr 1fr; gap:48px; width:100%; max-width:480px; }
  .player-card { display:grid; justify-items:center; align-content:center; gap:16px; min-width:0; padding:24px 20px; border:1px solid var(--line); border-radius:16px; background:var(--panel); color:var(--muted); }
  .player-card > div { display:grid; gap:8px; min-width:0; }
  strong { color:var(--text); font:600 18px/1.3 Inter,sans-serif; overflow-wrap:anywhere; }
  .player-card p { font-size:14px; }
  .available { border-style:dashed; border-color:var(--line-soft); background:transparent; }
  .availability { font-size:18px; }
  .actions { display:grid; grid-template-columns:1fr 1fr; gap:16px; width:100%; max-width:600px; }
  button { display:flex; justify-content:center; align-items:center; gap:12px; min-height:56px; padding:12px 20px; border:1px solid var(--line); border-radius:12px; background:var(--panel); color:var(--text); font:600 16px Inter,sans-serif; cursor:pointer; }
  .primary { background:var(--primary); color:var(--on-primary); border-color:var(--primary); }
  .waiting-error { display:grid; gap:12px; width:100%; padding:16px; border:1px solid var(--error); border-radius:12px; }
  .waiting-error p { color:var(--error); }
  @media(max-width:600px) {
    .waiting-screen { gap:28px; padding:16px 0; }
    .players { grid-template-columns:1fr; gap:16px; }
    .player-card { grid-template-columns:auto minmax(0,1fr); align-items:center; justify-items:start; gap:20px; padding:20px; text-align:left; }
    .availability { font-size:16px; }
    .actions { grid-template-columns:1fr; gap:12px; }
  }
</style>

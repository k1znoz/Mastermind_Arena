<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import userCircle from '../assets/tabler/user-circle.svg'
  export let playerName = ''
  export let canResume = false
  const dispatch = createEventDispatcher()
</script>

<section class="welcome-screen">
  <h1>Deux plateaux. Un duel.</h1>
  <p>Cherchez le code adverse en même temps.<br />Donnez ensuite les indices à la main.</p>
  <label><span>Votre pseudo</span><span class="pseudo-input"><Icon src={userCircle} /><input id="welcome-pseudo" type="text" maxlength="24" autocomplete="nickname" placeholder="Ex. Alex" bind:value={playerName} on:keydown={(event) => { if (event.key === 'Enter' && playerName.trim()) dispatch('continue') }} /></span></label>
  <button type="button" class="arrival-primary" disabled={!playerName.trim()} on:click={() => dispatch('continue')}>Voir les parties</button>
  {#if canResume}<button type="button" class="arrival-secondary" on:click={() => dispatch('resume')}>Reprendre ma partie</button>{/if}
  <div class="recovery-entry"><button id="open-recovery" type="button" on:click={() => dispatch('openrecovery')}>Reprendre une partie</button></div>
</section>
<style>
  .welcome-screen { display:grid; align-content:center; gap:24px; width:100%; max-width:620px; min-height:calc(100dvh - 200px); padding-bottom:72px; margin:0 auto; text-align:center; }
  h1 { margin:0; font:600 clamp(32px,4vw,52px)/1.15 Inter,sans-serif; letter-spacing:-.035em; }
  p { margin:0 0 30px; color:var(--muted); font-size:22px; line-height:1.5; }
  label { display:grid; gap:10px; text-align:left; color:var(--muted); font-size:16px; }
  label span { font:400 16px Inter,sans-serif; letter-spacing:normal; }
  .pseudo-input { display:block; position:relative; }
  .pseudo-input :global(> span) { position:absolute; left:20px; top:50%; transform:translateY(-50%); color:var(--muted); }
  .pseudo-input input { width:100%; padding-left:56px; }
  input { height:66px; padding:16px 20px; border:1px solid var(--line-soft); border-radius:14px; background:var(--panel-strong); color:var(--text); font:400 20px Inter,sans-serif; }
  button { min-height:64px; padding:12px 20px; border-radius:14px; cursor:pointer; font:600 18px Inter,sans-serif; }
  .arrival-primary { border:1px solid var(--primary); background:var(--primary); color:var(--on-primary); }
  .arrival-secondary { border:1px solid var(--line-soft); background:transparent; color:var(--text); }
  .recovery-entry { padding-top:16px; border-top:1px solid var(--line); text-align:center; }
  .recovery-entry button { min-height:44px; padding:8px; border:0; background:transparent; color:var(--primary-soft); font:400 16px Inter,sans-serif; text-decoration:underline; text-underline-offset:4px; }
  button:disabled { opacity:.4; cursor:default; }
  @media(max-width:600px) {
    .welcome-screen { min-height:calc(100dvh - 150px); align-content:start; padding:12vh 0 0; gap:24px; text-align:left; }
    h1 { font-size:30px; }
    p { font-size:16px; margin-bottom:28px; }
    label { font-size:15px; }
    input { height:58px; font-size:18px; }
    button { min-height:58px; font-size:16px; }
  }
</style>

<script>
  import { createEventDispatcher } from 'svelte'
  import RecoveryForm from './RecoveryForm.svelte'
  export let recoveryInput = ''
  export let recoveryError = ''
  export let busy = false
  export let playerName = ''
  export let canResume = false
  const dispatch = createEventDispatcher()
</script>

<section class="welcome-screen">
  <div class="arrival-step">01 / 02 · VOTRE IDENTITÉ</div>
  <h1>Bienvenue dans l’arène</h1>
  <p>Deux plateaux, un duel. Proposez vos codes en même temps, puis donnez vous-mêmes les indices à votre adversaire.</p>
  <label><span>VOTRE PSEUDO</span><input type="text" maxlength="24" autocomplete="nickname" placeholder="Ex. Alex" bind:value={playerName} on:keydown={(event) => { if (event.key === 'Enter' && playerName.trim()) dispatch('continue') }} /></label>
  <button type="button" class="arrival-primary" disabled={!playerName.trim()} on:click={() => dispatch('continue')}>VOIR LES PARTIES</button>
  {#if canResume}<button type="button" class="arrival-secondary" on:click={() => dispatch('resume')}>REPRENDRE MA PARTIE</button>{/if}
  <RecoveryForm bind:value={recoveryInput} {busy} error={recoveryError} on:recover={(event) => dispatch('recover', event.detail)} />
</section>
<style>
  .welcome-screen { display:grid; gap:16px; max-width:650px; width:100%; margin:6vh auto 0; padding:clamp(20px,5vw,44px); border:1px solid var(--line); background:var(--panel); }
  .arrival-step, label span { color:var(--primary); font:11px "JetBrains Mono",monospace; letter-spacing:.1em; }
  h1 { margin:0; font:700 clamp(30px,7vw,48px) "Space Grotesk",sans-serif; line-height:1.05; }
  p { margin:0; color:var(--muted); font-size:14px; line-height:1.5; }
  label { display:grid; gap:8px; margin-top:12px; }
  input { min-height:48px; padding:12px; border:1px solid var(--line-soft); background:var(--panel-strong); color:var(--text); font-size:16px; }
  button { min-height:44px; cursor:pointer; font:11px "JetBrains Mono",monospace; }
  .arrival-primary { border:1px solid var(--primary); background:rgba(0,219,233,.15); color:var(--primary-soft); }
  .arrival-secondary { border:1px solid var(--line-soft); background:transparent; color:var(--muted); }
  button:disabled { opacity:.4; cursor:default; }
</style>

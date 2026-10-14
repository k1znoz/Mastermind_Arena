<script>
  import { createEventDispatcher } from 'svelte'
  export let value = ''
  export let busy = false
  export let error = ''
  const dispatch = createEventDispatcher()
</script>

<details class="recovery-form">
  <summary>Retrouver une partie sur cet appareil</summary>
  <form on:submit|preventDefault={() => dispatch('recover', { code: value.trim() })}>
    <label>Votre code de reprise privé<input type="password" autocomplete="off" spellcheck="false" bind:value maxlength="200" placeholder="Collez le code conservé dans votre partie" /></label>
    <p>Il permet de retrouver votre place et votre plateau. Le code d’accès partagé de la partie est différent.</p>
    {#if error}<p role="alert" class="error">{error}</p>{/if}
    <button type="submit" disabled={busy || !value.trim()}>{busy ? 'Connexion…' : 'Reprendre ma place'}</button>
  </form>
</details>
<style>
  details { margin-top:16px; border-top:1px solid var(--line); padding-top:16px; }
  summary { cursor:pointer; min-height:44px; color:var(--muted); font-size:14px; }
  form,label { display:grid; gap:10px; }
  label { font-size:14px; }
  input { width:100%; min-height:48px; padding:12px; border:1px solid var(--line); background:var(--panel-strong); color:var(--text); font-size:16px; }
  p { font-size:13px; color:var(--muted); line-height:1.5; margin:0; }
  button { justify-self:start; min-height:44px; padding:10px 16px; color:var(--primary-soft); border:1px solid var(--line); background:var(--panel-strong); cursor:pointer; }
</style>

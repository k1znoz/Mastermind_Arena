<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import arrowLeft from '../assets/tabler/arrow-left.svg'
  import eye from '../assets/tabler/eye.svg'
  import eyeOff from '../assets/tabler/eye-off.svg'
  import lock from '../assets/tabler/lock.svg'
  export let value = ''
  export let busy = false
  export let error = ''
  let visible = false
  const dispatch = createEventDispatcher()
  function focusHeading(heading) {
    heading.focus()
    heading.scrollIntoView({ block: 'start' })
  }
  function submit() {
    if (!busy && value.trim()) dispatch('recover', { code: value.trim() })
  }
</script>

<section class="recovery-screen">
  <button class="back" type="button" on:click={() => dispatch('back')}><Icon src={arrowLeft} size={20} />Retour</button>
  <h1 tabindex="-1" use:focusHeading>Reprendre une partie</h1>
  <p class="sr-only" role="status">{busy ? 'Connexion…' : ''}</p>
  <form on:submit|preventDefault={submit} aria-busy={busy}>
    <label for="private-recovery-code">Code de reprise privé</label>
    <div class="private-input">
      <input id="private-recovery-code" type={visible ? 'text' : 'password'} autocomplete="off" autocapitalize="none" spellcheck="false" bind:value maxlength="200" aria-describedby="recovery-help" />
      <button class="visibility" type="button" aria-label={visible ? 'Masquer le code de reprise' : 'Afficher le code de reprise'} aria-pressed={visible} on:click={() => visible = !visible}><Icon src={visible ? eye : eyeOff} /></button>
    </div>
    <p id="recovery-help">Ce code retrouve votre place et votre plateau.<br />Il est différent du code d’accès partagé.</p>
    {#if error}<p role="alert" class="error">{error}</p>{/if}
    <button class="primary" type="submit" disabled={!busy && !value.trim()} aria-disabled={busy}>{busy ? 'Connexion…' : 'Reprendre ma place'}</button>
    <p class="private-note"><Icon src={lock} size={18} />Gardez ce code pour vous.</p>
  </form>
  <p class="identity-note">Votre pseudo et votre partie seront retrouvés après la reprise.</p>
</section>

<style>
  .recovery-screen { width:100%; max-width:800px; margin:0 auto; padding:0 0 40px; text-align:center; }
  button { display:flex; align-items:center; justify-content:center; gap:12px; min-height:48px; padding:12px 20px; border:0; border-radius:12px; font:500 16px Inter,sans-serif; cursor:pointer; }
  .back { padding:12px 0; background:transparent; color:var(--muted); }
  h1 { margin:40px 0; scroll-margin-top:120px; font:600 clamp(30px,4vw,44px)/1.2 Inter,sans-serif; letter-spacing:-.03em; }
  h1:focus { outline:none; }
  form { display:grid; width:100%; max-width:620px; margin:0 auto; }
  label { display:block; margin:0 0 12px; color:var(--text); text-align:left; font:500 18px Inter,sans-serif; }
  .private-input { position:relative; }
  input { width:100%; height:66px; padding:16px 68px 16px 20px; border:1px solid var(--line-soft); border-radius:14px; background:var(--panel-strong); color:var(--text); font:400 20px Inter,sans-serif; }
  .visibility { position:absolute; top:50%; right:6px; transform:translateY(-50%); width:48px; padding:8px; background:transparent; color:var(--text); }
  p { margin:0; color:var(--muted); font:400 16px/1.5 Inter,sans-serif; }
  #recovery-help { margin:18px 0 32px; }
  .primary { min-height:64px; background:var(--primary); color:var(--on-primary); font-weight:600; font-size:18px; }
  .primary:disabled,.primary[aria-disabled="true"] { opacity:.4; cursor:default; }
  .private-note { display:flex; align-items:center; justify-content:center; gap:12px; margin-top:24px; font-size:14px; }
  .identity-note { max-width:680px; margin:32px auto 0; padding-top:28px; border-top:1px solid var(--line); font-size:14px; }
  .error { margin:0 0 20px; color:var(--error); text-align:left; }
  @media(max-width:600px) {
    .recovery-screen { text-align:left; padding-bottom:16px; }
    h1 { margin:24px 0 36px; scroll-margin-top:96px; font-size:30px; }
    label { font-size:16px; }
    input { height:60px; font-size:16px; }
    p { font-size:14px; text-align:center; }
    .primary { min-height:58px; font-size:16px; }
    .private-note,.identity-note { font-size:13px; }
  }
</style>

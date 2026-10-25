<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import lock from '../assets/tabler/lock.svg'
  import copy from '../assets/tabler/copy.svg'
  import close from '../assets/tabler/x.svg'
  import trash from '../assets/tabler/trash.svg'
  import eye from '../assets/tabler/eye.svg'
  import eyeOff from '../assets/tabler/eye-off.svg'
  export let code = ''
  export let playerName = ''
  export let opponentName = ''
  const dispatch = createEventDispatcher()
  let open = false
  let visible = false
  let copyStatus = ''
  let dialog
  function show(node) {
    node.showModal()
    return { destroy() { node.close() } }
  }
  async function copyCode() {
    const copiedCode = code
    let message
    try {
      await navigator.clipboard.writeText(copiedCode)
      message = 'Code copié.'
    } catch {
      message = 'Copie impossible. Affichez le code pour le sélectionner et le copier.'
    }
    if (code === copiedCode && dialog?.isConnected && dialog.open) copyStatus = message
  }
</script>

<div class="private-recovery">
  <button class="launcher" type="button" on:click={() => { visible = false; copyStatus = ''; open = true }}><Icon src={lock} size={18} />Reprise privée</button>
  {#if open}
    <dialog class="recovery-panel" bind:this={dialog} use:show on:close={() => open = false} aria-labelledby="private-recovery-title">
      <button class="close" type="button" aria-label="Fermer la reprise privée" on:click={() => dialog.close()}><Icon src={close} /></button>
      <h2 id="private-recovery-title">Reprise privée</h2>
      <p>Utilisez ce code pour reprendre cette partie sur un autre appareil.</p>
      <label for="private-recovery-code">Votre code de reprise</label>
      <div class="code-row"><input id="private-recovery-code" type={visible ? 'text' : 'password'} readonly value={code} autocomplete="off" spellcheck="false" on:focus={(event) => event.currentTarget.select()} /><button type="button" disabled={!code} aria-label="Copier le code de reprise" on:click={copyCode}><Icon src={copy} /></button></div>
      <button class="visibility" type="button" aria-pressed={visible} on:click={() => visible = !visible}><Icon src={visible ? eyeOff : eye} size={18} />{visible ? 'Masquer le code' : 'Afficher le code'}</button>
      {#if copyStatus}<p role="status">{copyStatus}</p>{/if}
      <p>Ce code donne accès à la place de {playerName}. Ne le partagez pas avec {opponentName}.</p>
      <p class="retention">Partie supprimée après 7 jours sans action de jeu.</p>
      <div class="forget"><button class="danger" type="button" on:click={() => dispatch('forget')}><Icon src={trash} size={20} />Oublier ma place sur cet appareil</button><p>La partie ne sera pas supprimée. Conservez votre code avant d’oublier cette place.</p></div>
    </dialog>
  {/if}
</div>

<style>
  .private-recovery { display:flex; justify-content:flex-end; }
  button { display:inline-flex; align-items:center; justify-content:center; gap:10px; min-height:44px; border:1px solid var(--line); border-radius:10px; padding:10px 12px; background:var(--panel-strong); color:var(--text); font:500 14px Inter,sans-serif; cursor:pointer; }
  button:disabled { opacity:.5; cursor:default; }
  .launcher { background:transparent; border:0; color:var(--muted); }
  .recovery-panel { position:fixed; inset:0 0 0 auto; margin:0; width:420px; max-width:100%; height:100dvh; max-height:100dvh; overflow:auto; padding:68px 28px 28px; border:0; border-left:1px solid var(--line); background:var(--panel); color:var(--text); }
  .recovery-panel::backdrop { background:#0009; }
  .close { position:absolute; right:16px; top:16px; padding:10px; border:0; background:transparent; }
  h2 { font:600 27px Inter,sans-serif; text-transform:none; margin:0 0 14px; }
  p { font-size:14px; line-height:1.65; color:var(--muted); margin:0 0 22px; overflow-wrap:anywhere; }
  label { font-size:14px; margin:30px 0 12px; }
  .code-row { display:grid; grid-template-columns:minmax(0,1fr) 52px; gap:10px; }
  input { min-width:0; height:56px; border-radius:10px; font-size:16px; }
  .visibility { background:transparent; border:0; padding-left:0; margin:4px 0 12px; color:var(--muted); }
  .retention { font-size:13px; }
  .forget { border-top:1px solid var(--line); padding-top:26px; margin-top:28px; }
  .danger { width:100%; color:var(--error); border-color:var(--error); background:transparent; }
  .forget p { font-size:13px; margin-top:16px; }
  @media(min-width:1200px) { .private-recovery { position:absolute; top:26px; right:0; } }
  @media(max-width:650px) { .recovery-panel { width:100%; max-width:none; padding:76px 24px calc(24px + env(safe-area-inset-bottom)); } }
</style>

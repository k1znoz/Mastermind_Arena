<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import userCircle from '../assets/tabler/user-circle.svg'
  import refresh from '../assets/tabler/refresh.svg'
  import lock from '../assets/tabler/lock.svg'
  export let playerName = ''
  export let rooms = []
  export let busy = false
  export let operation = /** @type {{type: string, roomId?: string} | null} */ (null)
  export let refreshing = false
  export let error = ''
  export let listError = ''
  export let canResume = false
  export let ownRoomId = ''
  export let newName = ''
  export let newCode = ''
  let joinRoomId = ''
  let joinCode = ''
  const dispatch = createEventDispatcher()
  function focusCodeInput(input) { input.focus() }
  function join(room) {
    if (busy) return
    if (room.roomId === ownRoomId) { dispatch('resume'); return }
    if (room.locked) { joinRoomId = room.roomId; joinCode = ''; return }
    dispatch('joinroom', { roomId: room.roomId, accessCode: '' })
  }
</script>

<section class="lobby-screen">
  <header class="lobby-header">
    <div><h1 id="lobby-title" tabindex="-1">Bonjour {playerName}</h1><button class="change-name" type="button" on:click={() => dispatch('back')}>Changer de pseudo</button></div>
  </header>
  {#if error}<p class="lobby-error" role="alert">{error}</p>{/if}
  <p class="sr-only" role="status">{busy && operation?.type === 'create' ? 'Création en cours…' : busy && operation?.type === 'join' ? 'Connexion à la partie…' : refreshing ? 'Actualisation…' : ''}</p>
  <div class="lobby-grid">
    <section class="room-list">
      <div class="section-title"><h2>Parties en attente</h2><button type="button" aria-disabled={refreshing} on:click={() => { if (!refreshing) dispatch('refresh') }}><Icon src={refresh} size={18} />{refreshing ? 'Actualisation…' : 'Actualiser'}</button></div>
      {#if listError}<p class="lobby-error" role="alert">{listError}</p>{/if}
      {#each rooms as room (room.roomId)}
        <div class="room-table-row">
          <div class="room-identity"><span class="player-avatar"><Icon src={userCircle} size={40} /></span><div><strong>{room.name}</strong><span>Créée par {room.hostPseudo}</span></div></div>
          <span class="access-badge" class:locked={room.locked}>{#if room.locked}<Icon src={lock} size={14} />{/if}{room.locked ? 'Code requis' : 'Ouverte'}</span>
          {#if joinRoomId !== room.roomId}<button class="join-button" type="button" aria-disabled={busy} on:click={() => join(room)}>{busy && operation?.type === 'join' && operation.roomId === room.roomId ? 'Connexion à la partie…' : room.roomId === ownRoomId ? "Reprendre" : "Rejoindre"}</button>{/if}
        </div>
        {#if joinRoomId === room.roomId}
          <div class="room-code-entry"><label><span>Code d’accès</span><input type="password" maxlength="64" autocomplete="off" bind:value={joinCode} use:focusCodeInput /></label><button class="join-button" type="button" disabled={!busy && !joinCode.trim()} aria-disabled={busy} on:click={() => { if (!busy) dispatch('joinroom', { roomId: room.roomId, accessCode: joinCode }) }}>{busy && operation?.type === 'join' && operation.roomId === room.roomId ? 'Connexion à la partie…' : 'Entrer'}</button></div>
        {/if}
      {:else}
        {#if !listError}<p class="empty-list">{refreshing ? 'Chargement des parties…' : 'Aucune partie n’attend un deuxième joueur. Créez la vôtre.'}</p>{/if}
      {/each}
    </section>

    <section class="create-room">
      <h2>Créer une partie</h2>
      <label><span>Nom de la partie</span><input type="text" maxlength="60" placeholder="Ex. Duel du soir" bind:value={newName} /></label>
      <label><span>Code d’accès (optionnel)</span><input type="password" maxlength="64" autocomplete="new-password" bind:value={newCode} /></label>
      <p>Laissez vide pour une partie ouverte.</p>
      <button type="button" class="create-button" disabled={!busy && !newName.trim()} aria-disabled={busy} on:click={() => { if (!busy) dispatch('createroom', { name: newName, accessCode: newCode }) }}>{busy && operation?.type === 'create' ? 'Création en cours…' : 'Créer et attendre'}</button>
    </section>
  </div>
  {#if canResume}<button type="button" class="resume-button" on:click={() => dispatch('resume')}>Reprendre ma partie en cours</button>{/if}
  <div class="recovery-entry"><button id="open-recovery" type="button" on:click={() => dispatch('openrecovery')}>Reprendre une partie</button></div>
</section>
<style>
  .lobby-screen { display:grid; gap:32px; padding-top:24px; }
  .lobby-header { display:flex; align-items:center; gap:18px; padding:0; }
  .lobby-header > div { min-width:0; }
  .player-avatar { display:flex; color:var(--muted); }
  h1 { margin:0 0 8px; font:600 clamp(30px,4vw,48px)/1.2 Inter,sans-serif; letter-spacing:-.03em; overflow-wrap:anywhere; }
  p { margin:0; color:var(--muted); font-size:14px; line-height:1.5; }
  button { min-height:48px; padding:10px 18px; border:1px solid var(--line-soft); border-radius:10px; background:transparent; color:var(--text); cursor:pointer; font:500 15px Inter,sans-serif; }
  button:disabled,button[aria-disabled="true"] { opacity:.4; cursor:default; }
  .change-name { min-height:44px; padding:0; border:0; border-radius:0; color:var(--muted); text-decoration:underline; text-underline-offset:4px; }
  .lobby-grid { display:grid; grid-template-columns:minmax(0,1.2fr) minmax(300px,1fr); gap:24px; align-items:start; }
  .room-list,.create-room { min-width:0; padding:28px; border:1px solid var(--line); border-radius:18px; background:var(--panel); }
  .section-title { display:flex; justify-content:space-between; gap:12px; align-items:center; padding-bottom:20px; border-bottom:1px solid var(--line); }
  .section-title button { display:flex; align-items:center; gap:6px; border:0; padding:4px; font-size:14px; color:var(--muted); }
  h2 { margin:0; font:600 22px/1.3 Inter,sans-serif; text-transform:none; letter-spacing:normal; }
  .room-table-row { display:grid; grid-template-columns:minmax(0,1fr) auto auto; gap:16px; align-items:center; min-height:110px; padding:24px 0; border-bottom:1px solid var(--line); }
  .room-identity { display:flex; align-items:center; gap:14px; min-width:0; }
  .room-identity > div { display:grid; gap:6px; min-width:0; }
  .room-identity strong { font-size:18px; overflow-wrap:anywhere; }
  .room-identity span { font-size:14px; color:var(--muted); overflow-wrap:anywhere; }
  .access-badge { display:flex; align-items:center; gap:4px; padding:7px 12px; border-radius:24px; background:var(--accent-wash); color:var(--primary-soft); font-size:12px; white-space:nowrap; }
  .access-badge.locked { background:var(--panel-strong); color:var(--muted); }
  .join-button,.create-button { border-color:var(--primary); background:var(--primary); color:var(--on-primary); font-weight:600; }
  .empty-list { padding:28px 0 4px; }
  .create-room { display:grid; gap:24px; align-content:start; }
  .create-room h2 { padding-bottom:20px; border-bottom:1px solid var(--line); }
  label { display:grid; gap:10px; min-width:0; margin:0; }
  label span { color:var(--muted); font:400 15px Inter,sans-serif; letter-spacing:normal; }
  input { width:100%; height:58px; padding:12px 16px; border:1px solid var(--line-soft); border-radius:12px; background:var(--panel-strong); color:var(--text); font:400 18px Inter,sans-serif; }
  .create-room p { margin-top:-12px; }
  .create-button { min-height:60px; }
  .room-code-entry { display:flex; gap:12px; align-items:end; padding:20px; border:1px solid var(--line-soft); border-radius:12px; margin-top:12px; }
  .room-code-entry label { flex:1; }
  .lobby-error { padding:16px; color:var(--error); border:1px solid var(--error); border-radius:12px; }
  .resume-button { justify-self:start; }
  .recovery-entry { padding-top:16px; border-top:1px solid var(--line); text-align:center; }
  .recovery-entry button { padding:8px; border:0; color:var(--primary-soft); text-decoration:underline; text-underline-offset:4px; font-weight:400; }
  @media (max-width:850px) {
    .lobby-grid { grid-template-columns:1fr; }
  }
  @media (min-width:851px) and (max-width:1100px) {
    .room-table-row { grid-template-columns:minmax(0,1fr) auto; }
    .room-table-row .join-button { grid-column:1 / -1; width:100%; }
  }
  @media(max-width:600px) {
    .lobby-screen { gap:16px; padding-top:0; }
    h1 { margin-bottom:0; }
    .lobby-grid { gap:16px; }
    .room-list,.create-room { padding:16px; border-radius:16px; }
    h2 { font-size:18px; }
    button { min-height:44px; padding:8px 14px; }
    .section-title { gap:8px; padding-bottom:8px; }
    .section-title button { font-size:12px; }
    .room-table-row { grid-template-columns:minmax(0,1fr) auto; gap:8px; min-height:0; padding:12px 0; }
    .room-identity { gap:8px; }
    .room-identity strong { font-size:16px; }
    .room-identity span { font-size:12px; }
    .access-badge { padding:6px 10px; font-size:11px; }
    .room-table-row .join-button { grid-column:1 / -1; width:100%; }
    .room-code-entry { flex-direction:column; align-items:stretch; gap:8px; padding:0 0 8px; margin-top:8px; border:0; border-radius:0; }
    .create-room { gap:12px; }
    .create-room h2 { padding-bottom:10px; }
    .create-room p { margin-top:-4px; font-size:12px; }
    label { gap:6px; }
    label span { font-size:13px; }
    input { height:44px; font-size:16px; }
    .create-button { min-height:44px; }
  }
</style>

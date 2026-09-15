<script>
  import { createEventDispatcher } from 'svelte'
  export let playerName = ''
  export let rooms = []
  export let busy = false
  export let error = ''
  export let canResume = false
  export let ownRoomId = ''
  let newName = ''
  let newCode = ''
  let joinRoomId = ''
  let joinCode = ''
  const dispatch = createEventDispatcher()
  function join(room) {
    if (room.roomId === ownRoomId) { dispatch('resume'); return }
    if (room.locked) { joinRoomId = room.roomId; joinCode = ''; return }
    dispatch('joinroom', { roomId: room.roomId, accessCode: '' })
  }
</script>

<section class="lobby-screen">
  <header class="lobby-header">
    <div><span>02 / 02 · CHOISIR UNE PARTIE</span><h1>Parties en attente</h1><p>Bonjour {playerName}. Rejoignez un joueur ou ouvrez une nouvelle partie.</p></div>
    <button type="button" on:click={() => dispatch('back')}>CHANGER DE PSEUDO</button>
  </header>
  {#if error}<p class="lobby-error" role="alert">{error}</p>{/if}
  <div class="lobby-grid">
    <section class="room-list">
      <div class="section-title"><h2>REJOINDRE</h2><button type="button" on:click={() => dispatch('refresh')}>ACTUALISER</button></div>
      <div class="room-table-head"><span>PARTIE</span><span>CRÉATEUR</span><span>ACCÈS</span><span></span></div>
      {#each rooms as room (room.roomId)}
        <div class="room-table-row">
          <strong>{room.name}</strong><span>{room.hostPseudo}</span>
          <span class:locked={room.locked}>{room.locked ? 'CODE' : 'OUVERT'}</span>
          <button type="button" disabled={busy} on:click={() => join(room)}>{room.roomId === ownRoomId ? "REPRENDRE" : "REJOINDRE"}</button>
        </div>
        {#if joinRoomId === room.roomId}
          <div class="room-code-entry"><label><span>CODE D’ACCÈS</span><input type="password" maxlength="64" autocomplete="off" bind:value={joinCode} /></label><button type="button" disabled={busy || !joinCode.trim()} on:click={() => dispatch('joinroom', { roomId: room.roomId, accessCode: joinCode })}>ENTRER</button></div>
        {/if}
      {:else}
        <p class="empty-list">Aucune partie n’attend un deuxième joueur. Créez la vôtre.</p>
      {/each}
    </section>

    <section class="create-room">
      <h2>CRÉER UNE PARTIE</h2>
      <p>Vous serez Joueur 1. La partie commencera quand un Joueur 2 vous aura rejoint.</p>
      <label><span>NOM DE LA PARTIE</span><input type="text" maxlength="60" placeholder="Ex. Duel du soir" bind:value={newName} /></label>
      <label><span>CODE D’ACCÈS · FACULTATIF</span><input type="password" maxlength="64" autocomplete="new-password" placeholder="Laisser vide pour une partie ouverte" bind:value={newCode} /></label>
      <button type="button" class="create-button" disabled={busy || !newName.trim()} on:click={() => dispatch('createroom', { name: newName, accessCode: newCode })}>{busy ? 'EN COURS...' : 'CRÉER ET ATTENDRE'}</button>
    </section>
  </div>
  {#if canResume}<button type="button" class="resume-button" on:click={() => dispatch('resume')}>REPRENDRE MA PARTIE EN COURS</button>{/if}
</section>
<style>
  .lobby-screen { display:grid; gap:14px; }
  .lobby-header { display:flex; justify-content:space-between; align-items:start; gap:14px; padding:20px; border:1px solid var(--line); background:rgba(19,23,31,.9); }
  .lobby-header span, h2, label span, .room-table-head { font:11px "JetBrains Mono",monospace; letter-spacing:.08em; color:var(--primary); }
  h1 { margin:8px 0 4px; font:700 clamp(28px,5vw,40px) "Space Grotesk",sans-serif; }
  p { margin:0; color:var(--muted); font-size:13px; line-height:1.5; }
  button { min-height:34px; padding:6px 10px; border:1px solid var(--line-soft); background:transparent; color:var(--text); cursor:pointer; font:10px "JetBrains Mono",monospace; }
  button:disabled { opacity:.4; cursor:default; }
  .lobby-grid { display:grid; grid-template-columns:minmax(0,1.5fr) minmax(260px,1fr); gap:12px; }
  .room-list,.create-room { min-width:0; padding:16px; border:1px solid var(--line); background:rgba(19,23,31,.9); }
  .section-title { display:flex; justify-content:space-between; gap:8px; align-items:center; }
  h2 { margin:0 0 12px; }
  .room-table-head,.room-table-row { display:grid; grid-template-columns:minmax(0,1.5fr) minmax(0,1fr) 56px 88px; gap:8px; align-items:center; }
  .room-table-head { padding:8px 0; border-bottom:1px solid var(--line-soft); color:var(--muted); font-size:9px; }
  .room-table-row { min-height:56px; padding:8px 0; border-bottom:1px solid var(--line-soft); font-size:12px; }
  .room-table-row strong,.room-table-row span { min-width:0; overflow:hidden; text-overflow:ellipsis; }
  .room-table-row .locked { color:var(--secondary); }
  .room-table-row button { border-color:var(--primary); color:var(--primary-soft); }
  .empty-list { padding:22px 2px; }
  .create-room { display:grid; gap:12px; align-content:start; }
  .create-room h2 { margin:0; }
  label { display:grid; gap:6px; }
  label span { color:var(--muted); font-size:10px; }
  input { width:100%; min-height:40px; padding:8px; border:1px solid var(--line-soft); background:#10161f; color:var(--text); font-size:13px; }
  .create-button { border-color:var(--primary); background:rgba(0,219,233,.12); color:var(--primary-soft); min-height:40px; }
  .room-code-entry { display:flex; gap:8px; align-items:end; padding:10px; background:rgba(255,171,243,.06); }
  .room-code-entry label { flex:1; }
  .room-code-entry button { border-color:var(--secondary); color:var(--secondary); }
  .lobby-error { padding:10px; color:var(--error); border:1px solid var(--error); }
  .resume-button { justify-self:start; }
  @media (max-width:700px) {
    .lobby-header { flex-direction:column; padding:14px; }
    .lobby-grid { grid-template-columns:1fr; }
    .room-list,.create-room { padding:12px; }
    .room-table-head,.room-table-row { grid-template-columns:minmax(0,1.5fr) minmax(0,1fr) 50px 70px; gap:5px; }
    .room-table-row button { font-size:8px; padding:4px; }
  }
</style>
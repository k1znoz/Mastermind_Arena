<script>
  import CodePegs from './CodePegs.svelte'
  export let isPrototype = false
  export let historyRows = []
  export let asSymbols = () => []
  export let formatTimestamp = () => ''
  export let asTimestamp = () => null
  export let onRefresh = () => {}
  const labels = { PLAY_GUESS:'Essai envoyé', SEND_FEEDBACK:'Indices donnés', CORRECT_FEEDBACK:'Indices corrigés', CONTEST_FEEDBACK:'Vérification demandée', ABANDON:'Abandon de la manche', REQUEST_REMATCH:'Revanche proposée' }
</script>
<section class="history-screen">
  <header><div><h2>Historique de la manche</h2><p>Les actions et les corrections restent visibles.</p></div><button type="button" on:click={onRefresh}>Actualiser</button></header>
  {#if isPrototype}<p role="status">Connexion en cours. Les dernières actions reçues restent affichées.</p>{/if}
  <ol>
    {#each historyRows as entry (entry.version)}<li><div class="event-header"><strong>{labels[entry.actionType] || 'Action de partie'}</strong><time>{formatTimestamp(asTimestamp(entry.timestamp))}</time></div><p>Joueur {entry.actorId === 'p1' ? '1' : '2'} · Tour {entry.turnNumber}</p>{#if entry.actionType === 'PLAY_GUESS'}<CodePegs colors={asSymbols(entry.guess)} />{:else if ['SEND_FEEDBACK','CORRECT_FEEDBACK'].includes(entry.actionType)}<p>{entry.feedback.filter(p => p === 'BLACK').length} bien placés · {entry.feedback.filter(p => p === 'WHITE').length} mal placés</p>{/if}</li>{:else}<li>Aucune action enregistrée pour le moment.</li>{/each}
  </ol>
</section>
<style>
 .history-screen { max-width:760px; margin:auto; }
 header { display:flex; align-items:start; justify-content:space-between; gap:12px; }
 h2 { font:600 22px 'Space Grotesk',sans-serif; margin:0; }
 p { font-size:13px; color:var(--muted); line-height:1.5; }
 button { min-height:44px; padding:10px; background:var(--panel); color:var(--text); border:1px solid var(--line); border-radius:10px; cursor:pointer; }
 ol { list-style:none; padding:0; display:grid; gap:10px; }
 li { padding:16px; background:var(--panel); border:1px solid var(--line); border-radius:12px; }
 .event-header { display:flex; justify-content:space-between; gap:12px; font-size:14px; }
 time { color:var(--muted); font-size:12px; }
 li p { margin:8px 0; }
</style>

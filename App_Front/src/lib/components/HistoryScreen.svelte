<script>
  import { asSymbols, asTimestamp, formatTimestamp, referencedTurnNumber } from '../utils/presentation.js'
  import CodePegs from './CodePegs.svelte'
  import Icon from './Icon.svelte'
  import refresh from '../assets/tabler/refresh.svg'
  import arrow from '../assets/tabler/arrow-right-circle.svg'
  import message from '../assets/tabler/message-circle.svg'
  import pencil from '../assets/tabler/pencil.svg'
  import help from '../assets/tabler/help-circle.svg'
  import close from '../assets/tabler/x.svg'
  export let syncing = false
  export let historyRows = []
  export let actorId = 'p1'
  export let playerName = 'Vous'
  export let opponentName = 'Adversaire'
  export let onRefresh = () => {}
  const labels = { PLAY_GUESS:'a envoyé un essai', SEND_FEEDBACK:'a donné des indices', CORRECT_FEEDBACK:'a corrigé des indices', CONTEST_FEEDBACK:'demande une vérification', ABANDON:'a abandonné la manche', REQUEST_REMATCH:'propose une revanche' }
  const icons = { PLAY_GUESS:arrow, SEND_FEEDBACK:message, CORRECT_FEEDBACK:pencil, CONTEST_FEEDBACK:help, ABANDON:close, REQUEST_REMATCH:refresh }
  $: timelineRows = historyRows.slice().sort((a,b) => a.version - b.version)
</script>
<section class="history-screen">
  <header><div><h2 id="history-title" tabindex="-1">Historique de la manche</h2><p>Les actions et les corrections restent visibles.</p></div><button type="button" on:click={onRefresh}><Icon src={refresh} size={20} />Actualiser</button></header>
  {#if syncing}<p role="status">Connexion en cours. Les dernières actions reçues restent affichées.</p>{/if}
  <ol>
    {#each timelineRows as entry (entry.version)}
      {@const historicalFeedback = entry.originalFeedback ?? entry.feedback}
      <li><time>{formatTimestamp(asTimestamp(entry.timestamp))}</time><span class="event-icon"><Icon src={icons[entry.actionType] || arrow} size={26} /></span><div class="event-content"><small>{entry.actionType === 'REQUEST_REMATCH' ? 'Manche terminée' : `Tour ${referencedTurnNumber(entry,historyRows)}`}</small><strong>{entry.actorId === actorId ? playerName : opponentName} {labels[entry.actionType] || 'a effectué une action'}</strong>
      <div class="event-detail">{#if entry.actionType === 'PLAY_GUESS'}<CodePegs colors={asSymbols(entry.guess)} label="Essai envoyé" />
      {:else if ['SEND_FEEDBACK','CORRECT_FEEDBACK'].includes(entry.actionType)}
        {#if historicalFeedback.length}<CodePegs colors={historicalFeedback.map(peg => peg === 'BLACK' ? 'E' : 'F')} label="Indices déclarés" />{/if}
        <p>{historicalFeedback.filter(peg => peg === 'BLACK').length} bien placés · {historicalFeedback.filter(peg => peg === 'WHITE').length} mal placés</p>
        {#if entry.actionType === 'SEND_FEEDBACK' && historyRows.some(action => action.actionType === 'CORRECT_FEEDBACK' && action.feedbackVersion === entry.version)}<p>{entry.originalFeedback ? 'Indices initiaux avant correction.' : 'Valeurs actuelles après correction.'}</p>{/if}
      {/if}
      {#if entry.actionType === 'CORRECT_FEEDBACK'}<p>Résultat actualisé.</p>{/if}
      </div></div></li>
    {:else}<li class="empty">Aucune action enregistrée pour le moment.</li>{/each}
  </ol>
</section>
<style>
 .history-screen { width:100%; max-width:800px; margin:auto; }
 header { display:flex; align-items:start; justify-content:space-between; gap:12px; }
 h2 { font:600 clamp(25px,3vw,36px)/1.25 Inter,sans-serif; text-transform:none; margin:0; letter-spacing:-.03em; }
 p { font-size:13px; color:var(--muted); line-height:1.5; }
 button { display:flex; align-items:center; justify-content:center; gap:10px; min-height:44px; padding:10px 16px; background:var(--panel); color:var(--text); border:1px solid var(--line); border-radius:10px; cursor:pointer; font:500 14px Inter,sans-serif; }
 ol { list-style:none; padding:0; margin:24px 0; }
 li { display:grid; grid-template-columns:70px 36px minmax(0,1fr); gap:18px; padding:24px 0; border-top:1px solid var(--line); }
 .event-icon { position:relative; color:var(--muted); }
 .event-icon::after { content:''; position:absolute; left:12px; top:36px; bottom:-24px; border-left:1px solid var(--line); }
 li:last-child .event-icon::after { display:none; }
 time { color:var(--muted); font-size:12px; padding-top:5px; }
 .event-content { display:grid; grid-template-columns:minmax(0,1fr) auto; gap:10px 20px; min-width:0; --code-peg-size:38px; }
 .event-content>small,.event-content>strong { grid-column:1; }
 .event-detail { grid-column:2; grid-row:1 / 4; display:grid; align-content:center; gap:8px; max-width:220px; }
 .event-content strong { font-size:16px; line-height:1.5; font-weight:500; overflow-wrap:anywhere; }
 small { font-size:12px; color:var(--muted); }
 li p { margin:0; }
 .empty { display:block; font-size:14px; color:var(--muted); }
 @media(max-width:650px) { header { flex-direction:column; align-items:center; text-align:center; gap:4px; } li { grid-template-columns:58px 26px minmax(0,1fr); gap:10px; padding:20px 0; } .event-content { grid-template-columns:minmax(0,1fr); --code-peg-size:30px; } .event-detail { grid-column:1; grid-row:auto; max-width:none; } .event-content strong { font-size:14px; } }
</style>

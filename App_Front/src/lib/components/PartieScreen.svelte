<script>
  import { createEventDispatcher } from 'svelte'
  import CodePegs from './CodePegs.svelte'
  import Icon from './Icon.svelte'
  import circleCheck from '../assets/tabler/circle-check.svg'
  import close from '../assets/tabler/x.svg'
  import list from '../assets/tabler/list.svg'
  import userCircle from '../assets/tabler/user-circle.svg'
  import { deriveGameView } from '../state/gameView.js'
  import { asSymbols, referencedTurnNumber } from '../utils/presentation.js'
  import { getSymbolVisual } from '../utils/symbolVisuals.js'
  export let view = deriveGameView()
  export let noticeDismissals = { context: '', versions: [] }
  $: ({ matchState, isMyTurn, actorId, hasOpponentReady, hasSecretSet, opponentName, opponentStepDone,
    setupSequence, setupSlots, selectedSetupSlot, draftGuess, selectedGuessSlot, guessSequence, symbolPalette,
    isSecretSubmitting, isGuessSubmitting, isBackendReady, canSubmitSecret, canSubmitGuess,
    ownGuessSubmitted, ownFeedbackSubmitted, busy, turns, round, phaseKey, feedbackKey, preparation, finished, ownGuesses,
    opponentGuesses, given, received, canRespond, mySolved, theirSolved, result, currentOpponentGuess,
    myRematchAsked, theirRematchAsked, notices, researchSummaries, submissionMessage, pendingActionType } = view)
  const dispatch = createEventDispatcher()
  let selectedBoard = 'own'
  let lastPhase = ''
  let black = 0
  let white = 0
  let resetKey = ''
  let editing = null
  let editBlack = 0
  let editWhite = 0
  let confirmAbandon = false
  let correctionTrigger
  $: if (phaseKey !== lastPhase) {
    selectedBoard = matchState?.state === 'WAITING_FEEDBACK' ? 'opponent' : 'own'
    // eslint-disable-next-line no-useless-assignment
    lastPhase = phaseKey
  }
  $: if (feedbackKey !== resetKey) {
    black = 0; white = 0; editing = null
    // eslint-disable-next-line no-useless-assignment
    resetKey = feedbackKey
  }
  $: rows = (selectedBoard === 'own' ? ownGuesses : opponentGuesses).slice().reverse()
  $: feedbackRows = selectedBoard === 'own' ? received : given
  $: currentOwnGuess = ownGuesses.find(entry => entry.turnNumber === round + 1)
  $: editingFeedback = given.find(entry => entry.version === editing && entry.turnNumber === round)
  $: editingGuess = opponentGuesses.find(entry => entry.turnNumber === editingFeedback?.turnNumber)
  $: noticeContext = `${matchState?.matchId}:${matchState?.gameNumber}`
  $: visibleNotices = notices.filter(entry => noticeDismissals.context !== noticeContext || !noticeDismissals.versions.includes(entry.version))
  function dismissNotice(version) {
    noticeDismissals = { context: noticeContext, versions: [...(noticeDismissals.context === noticeContext ? noticeDismissals.versions : []), version] }
  }


  function adjust(field, amount, edit = false) {
    if (busy) return
    if (edit) {
      if (field === 'black') editBlack = Math.max(0, Math.min(4 - editWhite, editBlack + amount))
      else editWhite = Math.max(0, Math.min(4 - editBlack, editWhite + amount))
    } else {
      if (field === 'black') black = Math.max(0, Math.min(4 - white, black + amount))
      else white = Math.max(0, Math.min(4 - black, white + amount))
    }
  }
  function editFeedback(entry, event) {
    correctionTrigger = event.currentTarget
    editing = entry.version
    editBlack = entry.feedback.filter(peg => peg === 'BLACK').length
    editWhite = entry.feedback.filter(peg => peg === 'WHITE').length
  }
  function closeCorrection() {
    correctionTrigger?.focus()
    editing = null
  }
  function focusCorrection(node) { node.focus() }
  function sendCorrection() {
    if (busy || !isBackendReady || !editingFeedback) return
    const version = editing
    const key = feedbackKey
    dispatch('correctfeedback', { version, feedback: { bienPlaces: editBlack, malPlaces: editWhite },
      onConfirmed: () => { if (editing === version && feedbackKey === key) editing = null } })
  }
  function showAbandonDialog(node) {
    node.showModal()
    return { destroy() { node.close() } }
  }
</script>

<section class="partie-screen" class:preparation class:secretActionActive={preparation && !hasSecretSet} class:guessActionActive={isMyTurn} class:feedbackActionActive={canRespond}>
  <p class="sr-only" role="status">{submissionMessage}</p>
  {#if result && !editingFeedback}
    <section class="duel-result phase-panel" role="status"><h2 data-phase-heading={finished ? '' : undefined} tabindex="-1">{result}</h2><p>{finished ? 'La manche est terminée.' : mySolved ? 'Votre adversaire continue sa recherche. Donnez-lui les indices à chaque essai.' : 'Continuez à chercher pour le plaisir, ou abandonnez pour découvrir le code. Le point du duel est déjà attribué.'}</p></section>
    <div class="research-summaries">
      {#each researchSummaries as research (research.actorId)}
        <section class="research-card"><header><Icon src={userCircle} size={36} /><h3>{research.name}</h3>{#if research.winner}<span class="winner-badge">Vainqueur</span>{/if}</header>
          <p>{research.solved ? research.turnNumber ? `A trouvé au tour ${research.turnNumber}.` : 'A trouvé le code.' : research.abandoned ? 'A abandonné.' : finished ? 'Recherche arrêtée.' : 'Recherche en cours.'}</p>
          {#if research.solved && research.guess.length}<div class="found-guess"><small>Essai trouvé</small><CodePegs colors={asSymbols(research.guess)} label={`Essai trouvé par ${research.name}`} /></div>{/if}
        </section>
      {/each}
    </div>
    {#if finished}<button class="history-link" type="button" on:click={() => dispatch('history',{tab:'historique'})}><Icon src={list} size={20} />Voir l’historique</button>{/if}
  {/if}

  {#if editingFeedback}
    <section class="phase-panel correction-panel" aria-labelledby="correction-title">
      <h2 id="correction-title" tabindex="-1" use:focusCorrection>Corriger les indices du tour {editingFeedback.turnNumber}</h2>
      {#if turns.some(entry => entry.actionType === 'CONTEST_FEEDBACK' && entry.feedbackVersion === editingFeedback.version && entry.actorId !== actorId)}<p>{opponentName} demande une vérification.</p>{/if}
      <div class="correction-summary"><div><small>Essai de {opponentName}</small><CodePegs colors={asSymbols(editingGuess?.guess)} label="Essai concerné par la correction" /></div><div><small>Vos indices actuels</small><p>{editingFeedback.feedback.filter(peg => peg === 'BLACK').length} noirs · {editingFeedback.feedback.filter(peg => peg === 'WHITE').length} blancs</p></div></div>
      <form class="correction-form" on:submit|preventDefault={sendCorrection} aria-busy={busy}>
        <h3>Corriger avec vos indices</h3>
        <div class="feedback-counters">
          <div class="feedback-control"><span class="index-dot black"></span><span>Noirs</span><button type="button" aria-label="Retirer un indice noir de correction" aria-disabled={editBlack === 0 || busy} on:click={() => adjust('black',-1,true)}>−</button><strong aria-live="polite">{editBlack}</strong><button type="button" aria-label="Ajouter un indice noir de correction" aria-disabled={editBlack + editWhite === 4 || busy} on:click={() => adjust('black',1,true)}>+</button></div>
          <div class="feedback-control"><span class="index-dot white"></span><span>Blancs</span><button type="button" aria-label="Retirer un indice blanc de correction" aria-disabled={editWhite === 0 || busy} on:click={() => adjust('white',-1,true)}>−</button><strong aria-live="polite">{editWhite}</strong><button type="button" aria-label="Ajouter un indice blanc de correction" aria-disabled={editBlack + editWhite === 4 || busy} on:click={() => adjust('white',1,true)}>+</button></div>
        </div>
        <p class="correction-total">Total : {editBlack + editWhite} / 4. La somme doit être inférieure ou égale à 4.</p>
        <p class="correction-notice">Votre adversaire sera averti. Le résultat et le score seront recalculés.</p>
        <div class="action-buttons"><button type="button" disabled={busy} on:click={closeCorrection}>Annuler</button><button class="primary-action" type="submit" disabled={!isBackendReady} aria-disabled={busy}>{busy && pendingActionType === 'CORRECT_FEEDBACK' ? 'Enregistrement de la correction…' : 'Enregistrer la correction'}</button></div>
      </form>
    </section>
  {/if}
  {#if preparation}
    <section class="action-card secret-panel code-editor" class:secret-ready={hasSecretSet}>
      <h2 data-phase-heading tabindex="-1">{hasSecretSet ? 'Votre code est prêt' : 'Choisissez votre code secret'}</h2>
      {#if hasSecretSet}<div class="secret-confirmed"><Icon src={circleCheck} size={56} /></div><p class="secret-status" role="status">{hasOpponentReady ? 'Les deux codes sont prêts.' : `En attente du code de ${opponentName}.`}</p>
      {:else}<p>4 couleurs. Répétitions autorisées.</p>{/if}
      <div class="slot-picker">
        {#each setupSlots as color, index}
          <button type="button" class:selected={selectedSetupSlot === index && !hasSecretSet} disabled={hasSecretSet || busy} aria-pressed={!hasSecretSet && selectedSetupSlot === index} aria-label={`Case ${index + 1}, ${getSymbolVisual(color).token}`} on:click={() => dispatch('selectsetupslot',{index})}><span class="color-dot {getSymbolVisual(color).toneClass}"></span><small>{index + 1}</small></button>
        {/each}
      </div>
      {#if !hasSecretSet}
        <div class="palette-grid">{#each symbolPalette as symbol}<button type="button" disabled={!isBackendReady || busy} aria-label={`Placer ${getSymbolVisual(symbol).token} dans la case ${selectedSetupSlot + 1} du code secret`} on:click={() => dispatch('choosesetupsymbol',{symbol})}><span class="color-dot {getSymbolVisual(symbol).toneClass}"></span>{getSymbolVisual(symbol).token}</button>{/each}</div>
        <p class="secret-privacy">Votre adversaire ne verra pas votre code.</p>
        <div class="action-buttons"><button type="button" disabled={busy || !setupSequence.some(Boolean)} on:click={() => dispatch('clearsetup')}>Effacer</button><button class="primary-action" type="button" disabled={!canSubmitSecret && !isSecretSubmitting} aria-disabled={busy} on:click={() => { if (!busy) dispatch('submitsecret') }}>{isSecretSubmitting ? 'Envoi du code…' : 'Confirmer mon code'}</button></div>
      {:else}<p class="secret-privacy">Votre code reste privé.</p>{/if}
    </section>
  {:else}
    {#if !editingFeedback}
    {#if isMyTurn}
      <section class="action-card own-board code-editor">
        <h2 data-phase-heading tabindex="-1">Composez votre essai</h2><p>4 couleurs. Répétitions autorisées.</p>
        <div class="slot-picker">{#each Array(4) as _, index}<button type="button" class:selected={selectedGuessSlot === index} disabled={busy} aria-pressed={selectedGuessSlot === index} aria-label={`Case ${index + 1}, ${getSymbolVisual(draftGuess[index]).token}`} on:click={() => dispatch('selectguessslot',{index})}><span class="color-dot {getSymbolVisual(draftGuess[index]).toneClass}"></span><small>{index + 1}</small></button>{/each}</div>
        <div class="palette-grid">{#each symbolPalette as symbol}<button type="button" disabled={busy} aria-label={`Placer ${getSymbolVisual(symbol).token} dans la case ${selectedGuessSlot + 1} de l’essai`} on:click={() => dispatch('chooseguesssymbol',{symbol})}><span class="color-dot {getSymbolVisual(symbol).toneClass}"></span>{getSymbolVisual(symbol).token}</button>{/each}</div>
        <div class="action-buttons"><button type="button" disabled={busy || !guessSequence.some(Boolean)} on:click={() => dispatch('clearguess')}>Effacer</button><button class="primary-action" type="button" disabled={!canSubmitGuess && !isGuessSubmitting} aria-disabled={busy} on:click={() => { if (!busy) dispatch('submitguess') }}>{isGuessSubmitting ? 'Envoi de l’essai…' : 'Envoyer l’essai'}</button></div>
      </section>
    {:else if canRespond}
      <section class="action-card response-panel phase-panel">
        <h2 data-phase-heading tabindex="-1">Donnez les indices</h2>
        <p>Comparez l’essai de {opponentName} à votre code.</p>
        <div class="comparison"><div><small>Essai de {opponentName}</small><CodePegs colors={asSymbols(currentOpponentGuess?.guess)} label="Essai adverse" /></div><div><small>Votre code secret</small><CodePegs colors={setupSlots} label="Votre code secret" /></div></div>
        <div class="feedback-counters">
        <div class="feedback-control"><span class="index-dot black"></span><span>Bien placés</span><button type="button" aria-label="Retirer un indice noir" aria-disabled={busy || black === 0} on:click={() => adjust('black',-1)}>−</button><strong>{black}</strong><button type="button" aria-label="Ajouter un indice noir" aria-disabled={busy || black + white === 4} on:click={() => adjust('black',1)}>+</button></div>
        <div class="feedback-control"><span class="index-dot white"></span><span>Mal placés</span><button type="button" aria-label="Retirer un indice blanc" aria-disabled={busy || white === 0} on:click={() => adjust('white',-1)}>−</button><strong>{white}</strong><button type="button" aria-label="Ajouter un indice blanc" aria-disabled={busy || black + white === 4} on:click={() => adjust('white',1)}>+</button></div>
        </div>
        <p class="hint">Noir : bonne couleur, bonne place. Blanc : bonne couleur, autre place. Indices entièrement manuels.</p>
        <button class="primary-action full" type="button" aria-disabled={busy} on:click={() => { if (!busy) dispatch('submitfeedback',{bienPlaces:black,malPlaces:white}) }}>{busy ? 'Envoi des indices…' : 'Valider les indices'}</button>
      </section>
    {:else if !finished}
      {#if isBackendReady && matchState?.state === 'WAITING_GUESS' && ownGuessSubmitted && currentOwnGuess}
        <section class="phase-panel sent-panel" aria-label="Essai envoyé">
          <h2 data-phase-heading tabindex="-1">Votre essai est envoyé</h2><p role="status">En attente de l’essai de {opponentName}.</p>
          <div class="sent-guess"><small>Votre essai · tour {round + 1}</small><CodePegs colors={asSymbols(currentOwnGuess.guess)} label="Votre essai envoyé" /><p>Visible uniquement par vous pour le moment.</p></div>
        </section>
      {:else}
        <div class="waiting-note" role="status"><h2 data-phase-heading tabindex="-1">{!isBackendReady ? 'Connexion interrompue. Votre plateau est conservé.' : mySolved && !theirSolved ? `Vous avez trouvé. Attendez le prochain essai de ${opponentName}.` : ownFeedbackSubmitted ? `Vos indices sont envoyés. En attente des indices de ${opponentName}.` : matchState?.state === 'WAITING_FEEDBACK' ? `En attente des indices de ${opponentName}.` : 'Votre action est envoyée. En attente de votre adversaire.'}{#if opponentStepDone}<span> Son action est prête.</span>{/if}</h2></div>
      {/if}
    {/if}

    {/if}
    <details class="attempts-details" open={!finished}><summary hidden={!finished}>Consulter les essais et les indices</summary>
    <div class="board-tabs" role="group" aria-label="Choisir le plateau"><button type="button" aria-pressed={selectedBoard === 'own'} class:active={selectedBoard === 'own'} on:click={() => selectedBoard = 'own'}>Mes essais <small>{ownGuesses.length}</small></button><button type="button" aria-pressed={selectedBoard === 'opponent'} class:active={selectedBoard === 'opponent'} on:click={() => selectedBoard = 'opponent'}>Ses essais <small>{opponentGuesses.length}</small></button></div>
    <section class="board-surface">
      <h2 class="sr-only">{selectedBoard === 'own' ? 'Mes essais pour trouver son code' : 'Ses essais pour trouver mon code'}</h2>
      {#each rows as row (row.version)}
        {@const feedback = feedbackRows.find(entry => entry.turnNumber === row.turnNumber)}
        {@const blackCount = feedback?.feedback.filter(peg => peg === 'BLACK').length ?? 0}
        {@const whiteCount = feedback?.feedback.filter(peg => peg === 'WHITE').length ?? 0}
        <div class="attempt-row"><div class="attempt-content"><span class="attempt-number">{row.turnNumber}</span><CodePegs colors={asSymbols(row.guess)} label={`Essai ${row.turnNumber}`} /><div class="indices" role="img" aria-label={feedback ? `${blackCount} bien placés, ${whiteCount} mal placés` : 'Indices en attente'}>{#each Array(4) as _, peg}<span class="index-dot" class:black={feedback?.feedback[peg] === 'BLACK'} class:white={feedback?.feedback[peg] === 'WHITE'}></span>{/each}</div>{#if feedback}<span class="attempt-summary" aria-hidden="true">{blackCount} bien placé{blackCount > 1 ? 's' : ''}<br />{whiteCount} mal placé{whiteCount > 1 ? 's' : ''}</span>{/if}</div>
          {#if feedback && feedback.turnNumber === round}
            {#if selectedBoard === 'own'}
              <details class="feedback-options"><summary>Indices reçus · confiance par défaut</summary><p>Un doute ? Demandez à votre adversaire de vérifier ses indices.</p><button type="button" disabled={busy || !isBackendReady || turns.some(a => a.actionType === 'CONTEST_FEEDBACK' && a.feedbackVersion === feedback.version && a.actorId === actorId)} on:click={() => dispatch('contestfeedback',{version:feedback.version})}>Demander une vérification</button></details>
            {:else}
              <button class="subtle" type="button" disabled={busy || !isBackendReady} on:click={(event) => editFeedback(feedback,event)}>Corriger mes indices</button>
            {/if}
          {/if}
        </div>
      {:else}<p class="empty-board">Aucun essai envoyé sur ce plateau.</p>{/each}
    </section>
    <details class="private-code"><summary>Consulter mon code secret</summary><CodePegs colors={setupSlots} label="Votre code secret" /></details>
    </details>
    {#if matchState?.opponentSecretCode?.length}<section class="action-card"><h2>Le code adverse</h2><CodePegs colors={matchState.opponentSecretCode} label="Code adverse révélé après abandon" /></section>{/if}
  {/if}

  {#if visibleNotices.length}<aside class="notifications" aria-label="Vérifications et corrections">{#each visibleNotices as notice (notice.version)}<div class="game-notice"><p role="status">{notice.actorId === actorId ? 'Vous avez' : `${opponentName} a`} {notice.actionType === 'CORRECT_FEEDBACK' ? 'corrigé des indices. Le résultat a été actualisé.' : 'demandé une vérification des indices.'}</p><button type="button" aria-label={`Fermer la notification du tour ${referencedTurnNumber(notice,turns)}, événement ${notice.version}`} on:click={() => dismissNotice(notice.version)}><Icon src={close} size={18} /></button></div>{/each}</aside>{/if}
  {#if finished}<section class="rematch-panel"><div><h3 tabindex="-1">{myRematchAsked ? 'Vous proposez une revanche.' : theirRematchAsked ? `${opponentName} propose une revanche.` : 'Une nouvelle manche ?'}</h3><p>Les scores seront conservés.</p></div><button class="primary-action" type="button" disabled={!isBackendReady} aria-disabled={busy || myRematchAsked} on:click={() => { if (!busy && !myRematchAsked) dispatch('requestrematch') }}>{busy && pendingActionType === 'REQUEST_REMATCH' ? 'Envoi de la demande de revanche…' : myRematchAsked ? 'Revanche proposée · en attente' : theirRematchAsked ? 'Accepter la revanche' : 'Proposer une revanche'}</button></section>
    {@const lastGiven = given.find(entry => entry.turnNumber === round)}
    {#if lastGiven}<button class="end-correction" type="button" disabled={busy || !isBackendReady} on:click={(event) => editFeedback(lastGiven,event)}>Corriger les derniers indices</button>{/if}
  {:else if !preparation}<button class="subtle abandon" type="button" disabled={!isBackendReady || busy} on:click={() => confirmAbandon = true}>Abandonner la manche</button>{/if}
  {#if confirmAbandon}<dialog class="abandon-confirm" aria-labelledby="abandon-title" aria-describedby="abandon-description" use:showAbandonDialog on:close={() => confirmAbandon = false}><h2 id="abandon-title">Abandonner cette manche ?</h2><p id="abandon-description">La recherche s’arrêtera et les codes seront révélés. Le résultat du duel sera conservé s’il est déjà attribué.</p><div class="action-buttons"><button type="button" on:click={(event) => event.currentTarget.closest('dialog').close()}>Continuer à jouer</button><button class="danger-action" type="button" disabled={busy || !isBackendReady} on:click={(event) => { dispatch('abandon'); event.currentTarget.closest('dialog').close() }}>Confirmer l’abandon</button></div></dialog>{/if}
  <span class="sr-only">{hasSecretSet ? 'Votre code est prêt.' : ''}</span>
</section>

<style>
  .partie-screen { display:grid; width:100%; gap:16px; max-width:760px; margin:auto; }

  .action-card,.board-surface,.duel-result,.abandon-confirm { border:1px solid var(--line); background:var(--panel); border-radius:16px; padding:20px; }
  h2 { margin:0 0 8px; font:600 19px Inter,sans-serif; line-height:1.3; text-transform:none; }
  p { margin:0 0 14px; font-size:14px; color:var(--muted); line-height:1.6; }
  button { min-height:46px; padding:10px 14px; font:500 14px Inter,sans-serif; border:1px solid var(--line); border-radius:10px; color:var(--text); background:var(--panel-strong); cursor:pointer; }
  button:disabled,button[aria-disabled="true"] { opacity:.5; cursor:default; }
  .slot-picker { display:grid; grid-template-columns:repeat(4,1fr); gap:10px; margin:20px 0; }
  .slot-picker button { display:grid; justify-items:center; gap:8px; padding:12px 4px 8px; }
  .slot-picker small { color:var(--muted); font-size:11px; }
  .slot-picker .selected { border:2px solid var(--primary); padding:11px 3px 7px; box-shadow:0 0 0 3px var(--accent-wash); }
  .color-dot { display:block; width:32px; height:32px; border:2px solid var(--peg-border,var(--line)); border-radius:50%; background:var(--peg-color,var(--line-soft)); box-shadow:inset 0 2px 3px #0002; }
  .palette-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:8px; margin-bottom:18px; }
  .palette-grid button { display:flex; align-items:center; justify-content:center; gap:8px; min-height:56px; padding:8px; font-size:13px; }
  .palette-grid .color-dot { width:24px; height:24px; flex:none; }
  .action-buttons { display:grid; grid-template-columns:1fr 2fr; gap:8px; }
  .primary-action { background:var(--primary); color:var(--on-primary); border-color:var(--primary); font-weight:600; }
  .full { width:100%; margin-top:18px; }
  .comparison { display:flex; gap:24px; flex-wrap:wrap; margin:20px 0; }
  .comparison>div { display:grid; gap:10px; }
  .comparison small { color:var(--muted); }
  .feedback-control { display:flex; align-items:center; gap:10px; margin:8px 0; }
  .feedback-control>span:not(.index-dot) { flex:1; font-size:14px; }
  .feedback-control button { min-width:46px; padding:8px; font-size:20px; }
  .feedback-control strong { min-width:20px; text-align:center; }
  .index-dot { width:12px; height:12px; border:1px solid var(--line-soft); border-radius:50%; display:block; background:var(--line-soft); }
  .index-dot.black { background:#141414; border-color:#888; }
  .index-dot.white { background:#fff; border-color:#888; }
  .hint { font-size:13px; font-style:italic; }
  .board-tabs { display:grid; grid-template-columns:1fr 1fr; gap:6px; }
  .board-tabs .active { border-color:var(--primary); background:var(--accent-wash); }
  .board-tabs small { margin-left:6px; color:var(--muted); }
  .board-surface { padding:16px; }
  .board-surface h2 { font-size:15px; margin-bottom:16px; }
  .attempt-row { padding:14px 0; border-top:1px solid var(--line); }
  .attempt-content { display:flex; align-items:center; gap:12px; }
  .attempt-number { width:22px; flex:none; color:var(--muted); font:12px 'JetBrains Mono',monospace; }
  .indices { display:grid; grid-template-columns:repeat(2,12px); gap:5px; margin-left:auto; }
  .feedback-options { margin-top:14px; }
  summary { cursor:pointer; color:var(--muted); font-size:13px; padding:12px 0; min-height:44px; }
  .feedback-options p { margin:8px 0; }
  .subtle { font-size:13px; color:var(--muted); background:transparent; margin-top:8px; }
  .correction-form { padding:0; margin-top:20px; text-align:left; }
  .correction-form h3 { margin:0 0 18px; font-size:16px; }
  .correction-summary { display:grid; grid-template-columns:minmax(0,1.5fr) minmax(0,1fr); gap:24px; padding:24px; margin-top:28px; border:1px solid var(--line); border-radius:20px; background:var(--panel); text-align:left; --code-peg-size:48px; --code-peg-align:space-between; }
  .correction-summary>div { min-width:0; display:grid; gap:18px; align-content:start; }
  .correction-summary small { font-size:14px; color:var(--muted); overflow-wrap:anywhere; }
  .correction-summary p { margin:0; }
  .correction-total { text-align:center; font-size:13px; margin:16px 0; }
  .correction-notice { border:1px solid var(--line); border-radius:12px; padding:16px; background:var(--panel); }
  .correction-form .action-buttons button { min-height:54px; }
  .rematch-panel { display:flex; gap:24px; align-items:center; justify-content:space-between; padding:24px; border:1px solid var(--line); border-radius:20px; background:var(--panel); }
  .rematch-panel h3 { font-size:17px; margin:0 0 8px; overflow-wrap:anywhere; }
  .rematch-panel p { margin:0; }
  .rematch-panel button { min-width:180px; }
  .research-summaries { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:20px; }
  .research-card { min-width:0; padding:22px; border:1px solid var(--line); border-radius:20px; background:var(--panel); }
  .research-card header { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
  .research-card h3 { font-size:17px; margin:0; overflow-wrap:anywhere; }
  .research-card p { margin:12px 0 0; }
  .winner-badge { background:var(--primary); color:var(--on-primary); border-radius:20px; padding:5px 10px; font-size:12px; }
  .found-guess { display:grid; gap:12px; margin-top:20px; padding:16px; border:1px solid var(--line); border-radius:16px; --code-peg-size:40px; --code-peg-align:space-between; }
  .found-guess small { font-size:12px; color:var(--muted); }
  .history-link { display:flex; align-items:center; justify-self:center; gap:10px; background:transparent; border:0; text-decoration:underline; }
  .attempts-details { min-width:0; }
  .end-correction { text-align:left; background:var(--panel); }
  .notifications { padding:0; border:0; display:grid; gap:8px; }
  .game-notice { display:flex; gap:12px; align-items:center; padding:8px 12px 8px 16px; border:1px solid var(--line); border-radius:12px; background:var(--panel); }
  .game-notice p { flex:1; margin:0; font-size:13px; }
  .game-notice button { flex:none; min-width:44px; padding:8px; background:transparent; border:0; }
  .private-code,.notifications { border-top:1px solid var(--line); padding:4px 8px; }
  .private-code :global(.code-pegs) { margin:6px 0 16px; }
  .waiting-note { padding:16px; margin:0; color:var(--muted); border:1px dashed var(--line); border-radius:12px; font-size:14px; font-weight:400; line-height:1.6; }
  .waiting-note h2 { margin:0; font:inherit; }
  .duel-result p { margin:8px 0 0; }
  .empty-board { margin:0; }
  .abandon { justify-self:start; }
  .abandon-confirm { position:fixed; inset:0; margin:auto; width:min(620px,calc(100% - 32px)); max-height:calc(100dvh - 40px); overflow:auto; padding:32px; border-color:var(--line); background:var(--panel); color:var(--text); }
  .abandon-confirm::backdrop { background:#0009; }
  .abandon-confirm h2 { font:600 25px/1.3 Inter,sans-serif; text-transform:none; margin-bottom:20px; }
  .abandon-confirm p { margin-bottom:28px; }
  .abandon-confirm .action-buttons { grid-template-columns:1fr 1fr; }
  .danger-action { color:var(--error); border-color:var(--error); background:var(--panel-strong); }
  @media(max-width:420px) { .action-card { padding:16px; } .palette-grid button { gap:6px; } .attempt-content { gap:8px; } .comparison { gap:18px; } }

  .code-editor { border:0; background:transparent; padding:12px 0 24px; text-align:center; }
  .code-editor h2 { font:600 clamp(25px,3vw,38px)/1.25 Inter,sans-serif; text-transform:none; letter-spacing:-.035em; margin-bottom:12px; }
  .code-editor>p { font-size:17px; }
  .code-editor .slot-picker { width:100%; max-width:600px; margin:32px auto 24px; padding:20px; gap:16px; background:var(--panel-strong); border:1px solid var(--line); border-radius:40px; }
  .code-editor .slot-picker button { min-width:0; border:0; background:transparent; padding:6px 0; box-shadow:none; }
  .code-editor .slot-picker .color-dot { width:clamp(48px,7vw,88px); height:clamp(48px,7vw,88px); border-width:3px; outline:7px solid var(--bg); outline-offset:0; }
  .code-editor .slot-picker .selected .color-dot { outline:2px solid var(--primary); outline-offset:5px; }
  .code-editor .slot-picker .symbol-tone-muted { --peg-color:var(--bg); --peg-border:var(--line); }
  .code-editor .slot-picker small { color:var(--text); font-size:12px; margin-top:8px; }
  .code-editor .slot-picker button:disabled { opacity:1; }
  .code-editor .palette-grid { width:100%; grid-template-columns:repeat(6,minmax(0,1fr)); gap:10px; margin:0 auto 24px; }
  .code-editor .palette-grid button { flex-direction:column; min-height:112px; gap:12px; font-size:14px; border-radius:16px; background:var(--panel); }
  .code-editor .palette-grid .color-dot { width:52px; height:52px; }
  .code-editor .secret-privacy { font-size:14px; margin:24px 0; }
  .code-editor .action-buttons { width:100%; max-width:600px; margin:auto; grid-template-columns:1fr 1.7fr; gap:16px; }
  .code-editor .action-buttons button { min-height:56px; font-size:16px; }
  .code-editor .secret-status { margin:28px 0; }
  .secret-confirmed { color:var(--primary); margin-top:26px; }
  .secret-ready .slot-picker { margin-top:40px; }
  .own-board { padding:8px 0 10px; }
  .own-board h2 { font-size:32px; }
  .own-board .slot-picker { max-width:540px; margin:22px auto; }
  .own-board .palette-grid button { min-height:100px; }
  .attempt-summary { flex:none; color:var(--muted); font-size:12px; line-height:1.6; }
  .phase-panel { border:0; background:transparent; text-align:center; padding:12px 0 24px; }
  .phase-panel h2 { font:600 clamp(25px,3vw,38px)/1.25 Inter,sans-serif; text-transform:none; letter-spacing:-.035em; margin-bottom:12px; }
  .phase-panel>p { font-size:17px; }
  .comparison { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:20px; margin:30px 0 20px; text-align:left; }
  .comparison>div { min-width:0; gap:20px; padding:20px; border:1px solid var(--line); border-radius:24px; background:var(--panel); --code-peg-size:clamp(40px,4vw,60px); --code-peg-align:space-between; }
  .comparison small { font-size:16px; overflow-wrap:anywhere; }
  .feedback-counters { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:20px; max-width:640px; margin:auto; }
  .feedback-counters .feedback-control { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:14px; margin:0; padding:18px; border:1px solid var(--line); border-radius:24px; background:var(--panel); }
  .feedback-counters .index-dot { width:36px; height:36px; justify-self:center; }
  .feedback-counters .feedback-control>span:not(.index-dot) { grid-column:2 / 4; text-align:left; }
  .feedback-counters button { width:48px; height:48px; justify-self:center; border-radius:50%; }
  .feedback-counters strong { font-size:28px; }
  .response-panel .hint { font-style:normal; font-size:13px; margin:16px 0; }
  .response-panel .full { max-width:520px; min-height:56px; margin-top:12px; font-size:16px; }
  .sent-guess { max-width:600px; margin:30px auto 0; padding:24px; border:1px solid var(--line); border-radius:32px; background:var(--panel-strong); --code-peg-size:clamp(40px,6vw,76px); --code-peg-align:space-between; }
  .sent-guess small { display:block; margin-bottom:24px; color:var(--muted); }
  .sent-guess p { margin:24px 0 0; font-size:13px; }
  .board-tabs { border-bottom:1px solid var(--line); gap:0; padding-top:12px; }
  .board-tabs button { border:0; border-bottom:2px solid transparent; border-radius:0; background:transparent; }
  .board-tabs .active { border-bottom-color:var(--primary); background:transparent; color:var(--primary); }
  .board-surface { border:0; background:transparent; padding:0; }
  .board-surface h2 { font:500 14px Inter,sans-serif; text-transform:none; color:var(--muted); }
  .attempt-row { border:1px solid var(--line); border-radius:14px; padding:16px; margin-bottom:8px; background:var(--panel); }
  .private-code { border:1px solid var(--line); border-radius:12px; padding:0 16px; background:var(--panel); }
  .abandon { justify-self:end; color:var(--error); border:0; margin:0; }
  @media(max-width:650px) {
    .research-summaries { grid-template-columns:1fr; gap:12px; }
    .research-card { padding:16px; }
    .research-card p { margin-top:6px; }
    .found-guess { margin-top:12px; padding:12px; gap:8px; }
    .correction-summary { grid-template-columns:1fr; gap:20px; padding:18px; --code-peg-size:40px; }
    .correction-form .action-buttons { grid-template-columns:1fr; }
    .rematch-panel { flex-direction:column; align-items:stretch; padding:20px; }
    .abandon-confirm { inset:auto 0 0; margin:0; width:100%; max-width:none; max-height:calc(100dvh - 20px); border-radius:24px 24px 0 0; padding:24px 20px calc(24px + env(safe-area-inset-bottom)); }
    .abandon-confirm h2 { font-size:22px; }
    .abandon-confirm .action-buttons { grid-template-columns:1fr; gap:10px; }
    .code-editor { padding:18px 10px; }
    .code-editor>p { font-size:14px; }
    .code-editor .slot-picker { padding:16px 10px; gap:12px; border-radius:28px; margin:24px auto; }
    .code-editor .slot-picker .color-dot { width:48px; height:48px; }
    .code-editor .palette-grid { grid-template-columns:repeat(3,minmax(0,1fr)); gap:8px; margin-bottom:16px; }
    .code-editor .palette-grid button { min-height:94px; gap:9px; }
    .code-editor .palette-grid .color-dot { width:44px; height:44px; }
    .code-editor .secret-privacy { font-size:12px; margin:18px 0; }
    .code-editor .action-buttons { grid-template-columns:1fr; gap:8px; }
    .code-editor .action-buttons button { min-height:48px; font-size:14px; }
    .own-board { padding:8px 10px; }
    .own-board h2 { font-size:25px; }
    .own-board .slot-picker { margin:16px auto; }
    .own-board .palette-grid button { min-height:84px; }
    .own-board .action-buttons { grid-template-columns:1fr 1.5fr; }
    .attempt-summary { font-size:10px; }
    .phase-panel { padding:18px 10px; }
    .phase-panel>p { font-size:14px; }
    .comparison { grid-template-columns:1fr; gap:10px; margin:20px 0; }
    .comparison>div { padding:16px; gap:14px; --code-peg-size:44px; }
    .feedback-counters { grid-template-columns:1fr; gap:10px; }
    .feedback-counters .feedback-control { grid-template-columns:40px 1fr 44px 32px 44px; gap:10px; padding:16px 12px; border-radius:18px; }
    .feedback-counters .feedback-control>span:not(.index-dot) { grid-column:auto; font-size:12px; }
    .feedback-counters button { min-width:44px; width:44px; height:44px; }
    .feedback-counters strong { font-size:24px; }
    .response-panel .full { min-height:48px; font-size:14px; }
    .sent-guess { padding:20px 16px; border-radius:24px; }
    .attempt-row { padding:12px; }
    .attempt-content { --code-peg-size:26px; gap:8px; }
    .abandon { justify-self:center; }
  }

</style>

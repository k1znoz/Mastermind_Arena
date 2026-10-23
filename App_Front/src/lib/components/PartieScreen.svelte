<script>
  import { createEventDispatcher } from 'svelte'
  import CodePegs from './CodePegs.svelte'
  import { deriveGameView } from '../state/gameView.js'
  import { asSymbols } from '../utils/presentation.js'
  import { getSymbolVisual } from '../utils/symbolVisuals.js'
  export let view = deriveGameView()
  $: ({ matchState, isMyTurn, actorId, hasOpponentReady, hasSecretSet, opponentName, opponentStepDone,
    setupSequence, setupSlots, selectedSetupSlot, draftGuess, selectedGuessSlot, guessSequence, symbolPalette,
    isSecretSubmitting, isGuessSubmitting, isBackendReady, canSubmitSecret, secretButtonLabel, canSubmitGuess,
    sendButtonLabel, busy, turns, round, phaseKey, feedbackKey, preparation, finished, ownGuesses,
    opponentGuesses, given, received, canRespond, mySolved, theirSolved, result, currentOpponentGuess,
    myRematchAsked, theirRematchAsked, notices } = view)
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


  function adjust(field, amount, edit = false) {
    if (edit) {
      if (field === 'black') editBlack = Math.max(0, Math.min(4 - editWhite, editBlack + amount))
      else editWhite = Math.max(0, Math.min(4 - editBlack, editWhite + amount))
    } else {
      if (field === 'black') black = Math.max(0, Math.min(4 - white, black + amount))
      else white = Math.max(0, Math.min(4 - black, white + amount))
    }
  }
  function editFeedback(entry) {
    editing = entry.version
    editBlack = entry.feedback.filter(peg => peg === 'BLACK').length
    editWhite = entry.feedback.filter(peg => peg === 'WHITE').length
  }
</script>

<section class="partie-screen" class:secretActionActive={preparation && !hasSecretSet} class:guessActionActive={isMyTurn} class:feedbackActionActive={canRespond}>
  {#if result}
    <div class="duel-result" role="status"><strong>{result}</strong><p>{finished ? 'La manche est terminée.' : mySolved ? 'Votre adversaire continue sa recherche. Donnez-lui les indices à chaque essai.' : 'Continuez à chercher pour le plaisir, ou abandonnez pour découvrir le code. Le point du duel est déjà attribué.'}</p></div>
  {/if}

  {#if preparation}
    <section class="action-card secret-panel">
      <h2>{hasSecretSet ? 'Votre code est prêt' : 'Choisissez votre code secret'}</h2>
      <p>Quatre couleurs, répétitions autorisées. Votre adversaire ne verra pas votre code.</p>
      <div class="slot-picker">
        {#each setupSlots as color, index}
          <button type="button" class:selected={selectedSetupSlot === index && !hasSecretSet} disabled={hasSecretSet || busy} aria-pressed={!hasSecretSet && selectedSetupSlot === index} aria-label={`Case ${index + 1}, ${getSymbolVisual(color).token}`} on:click={() => dispatch('selectsetupslot',{index})}><span class="color-dot {getSymbolVisual(color).toneClass}"></span><small>{index + 1}</small></button>
        {/each}
      </div>
      {#if !hasSecretSet}
        <div class="palette-grid">{#each symbolPalette as symbol}<button type="button" disabled={!isBackendReady || busy} aria-label={`Placer ${getSymbolVisual(symbol).token} dans la case ${selectedSetupSlot + 1} du code secret`} on:click={() => dispatch('choosesetupsymbol',{symbol})}><span class="color-dot {getSymbolVisual(symbol).toneClass}"></span>{getSymbolVisual(symbol).token}</button>{/each}</div>
        <div class="action-buttons"><button type="button" disabled={busy || !setupSequence.some(Boolean)} on:click={() => dispatch('clearsetup')}>Effacer</button><button class="primary-action" type="button" disabled={!canSubmitSecret || busy} on:click={() => dispatch('submitsecret')}>{isSecretSubmitting ? 'Envoi…' : secretButtonLabel}</button></div>
      {:else}<p class="waiting-note">{hasOpponentReady ? 'Les deux codes sont prêts.' : `En attente du code de ${opponentName}.`}</p>{/if}
    </section>
  {:else}
    {#if isMyTurn}
      <section class="action-card own-board">
        <h2>Votre essai · tour {round + 1}</h2><p>Choisissez une case, puis sa couleur.</p>
        <div class="slot-picker">{#each Array(4) as _, index}<button type="button" class:selected={selectedGuessSlot === index} disabled={busy} aria-pressed={selectedGuessSlot === index} aria-label={`Case ${index + 1}, ${getSymbolVisual(draftGuess[index]).token}`} on:click={() => dispatch('selectguessslot',{index})}><span class="color-dot {getSymbolVisual(draftGuess[index]).toneClass}"></span><small>{index + 1}</small></button>{/each}</div>
        <div class="palette-grid">{#each symbolPalette as symbol}<button type="button" disabled={busy} aria-label={`Placer ${getSymbolVisual(symbol).token} dans la case ${selectedGuessSlot + 1} de l’essai`} on:click={() => dispatch('chooseguesssymbol',{symbol})}><span class="color-dot {getSymbolVisual(symbol).toneClass}"></span>{getSymbolVisual(symbol).token}</button>{/each}</div>
        <div class="action-buttons"><button type="button" disabled={busy || !guessSequence.some(Boolean)} on:click={() => dispatch('clearguess')}>Effacer</button><button class="primary-action" type="button" disabled={!canSubmitGuess || busy} on:click={() => dispatch('submitguess')}>{isGuessSubmitting ? 'Envoi…' : sendButtonLabel}</button></div>
      </section>
    {:else if canRespond}
      <section class="action-card response-panel">
        <h2>Donnez les indices · tour {round + 1}</h2>
        <p>Comparez cet essai à votre code. Les indices sont entièrement manuels.</p>
        <div class="comparison"><div><small>Son essai</small><CodePegs colors={asSymbols(currentOpponentGuess?.guess)} label="Essai adverse" /></div><div><small>Votre code</small><CodePegs colors={setupSlots} label="Votre code secret" /></div></div>
        <p class="hint">Noir : bonne couleur, bonne place. Blanc : bonne couleur, autre place.</p>
        <div class="feedback-control"><span class="index-dot black"></span><span>Bien placés</span><button type="button" aria-label="Retirer un indice noir" disabled={busy || black === 0} on:click={() => adjust('black',-1)}>−</button><strong>{black}</strong><button type="button" aria-label="Ajouter un indice noir" disabled={busy || black + white === 4} on:click={() => adjust('black',1)}>+</button></div>
        <div class="feedback-control"><span class="index-dot white"></span><span>Mal placés</span><button type="button" aria-label="Retirer un indice blanc" disabled={busy || white === 0} on:click={() => adjust('white',-1)}>−</button><strong>{white}</strong><button type="button" aria-label="Ajouter un indice blanc" disabled={busy || black + white === 4} on:click={() => adjust('white',1)}>+</button></div>
        <button class="primary-action full" type="button" disabled={busy} on:click={() => dispatch('submitfeedback',{bienPlaces:black,malPlaces:white})}>{busy ? 'Envoi…' : 'Valider les indices'}</button>
      </section>
    {:else if !finished}
      <div class="waiting-note" role="status">{!isBackendReady ? 'Connexion interrompue. Votre plateau est conservé.' : mySolved && !theirSolved ? `Vous avez trouvé. Attendez le prochain essai de ${opponentName}.` : 'Votre action est envoyée. En attente de votre adversaire.'}{#if opponentStepDone}<span> Son action est prête.</span>{/if}</div>
    {/if}

    <div class="board-tabs" role="group" aria-label="Choisir le plateau"><button type="button" aria-pressed={selectedBoard === 'own'} class:active={selectedBoard === 'own'} on:click={() => selectedBoard = 'own'}>Mes essais <small>{ownGuesses.length}</small></button><button type="button" aria-pressed={selectedBoard === 'opponent'} class:active={selectedBoard === 'opponent'} on:click={() => selectedBoard = 'opponent'}>Ses essais <small>{opponentGuesses.length}</small></button></div>
    <section class="board-surface">
      <h2>{selectedBoard === 'own' ? 'Mes essais pour trouver son code' : 'Ses essais pour trouver mon code'}</h2>
      {#each rows as row (row.version)}
        {@const feedback = feedbackRows.find(entry => entry.turnNumber === row.turnNumber)}
        <div class="attempt-row"><div class="attempt-content"><span class="attempt-number">{row.turnNumber}</span><CodePegs colors={asSymbols(row.guess)} label={`Essai ${row.turnNumber}`} /><div class="indices" role="img" aria-label={feedback ? `${feedback.feedback.filter(p => p === 'BLACK').length} bien placés, ${feedback.feedback.filter(p => p === 'WHITE').length} mal placés` : 'Indices en attente'}>{#each Array(4) as _, peg}<span class="index-dot" class:black={feedback?.feedback[peg] === 'BLACK'} class:white={feedback?.feedback[peg] === 'WHITE'}></span>{/each}</div></div>
          {#if feedback && feedback.turnNumber === round}
            {#if selectedBoard === 'own'}
              <details class="feedback-options"><summary>Indices reçus · confiance par défaut</summary><p>Un doute ? Demandez à votre adversaire de vérifier ses indices.</p><button type="button" disabled={busy || !isBackendReady || turns.some(a => a.actionType === 'CONTEST_FEEDBACK' && a.feedbackVersion === feedback.version && a.actorId === actorId)} on:click={() => dispatch('contestfeedback',{version:feedback.version})}>Demander une vérification</button></details>
            {:else}
              <button class="subtle" type="button" disabled={busy || !isBackendReady} on:click={() => editFeedback(feedback)}>Corriger mes indices</button>
              {#if editing === feedback.version}
                <form class="correction-form" on:submit|preventDefault={() => { dispatch('correctfeedback',{version:feedback.version,feedback:{bienPlaces:editBlack,malPlaces:editWhite}}); editing = null }}>
                  <p>Votre adversaire sera averti. Le résultat et le score seront recalculés.</p>
                  <div class="feedback-control"><span>Noirs</span><button type="button" aria-label="Retirer un indice noir de correction" disabled={editBlack === 0 || busy} on:click={() => adjust('black',-1,true)}>−</button><strong>{editBlack}</strong><button type="button" aria-label="Ajouter un indice noir de correction" disabled={editBlack + editWhite === 4 || busy} on:click={() => adjust('black',1,true)}>+</button></div>
                  <div class="feedback-control"><span>Blancs</span><button type="button" aria-label="Retirer un indice blanc de correction" disabled={editWhite === 0 || busy} on:click={() => adjust('white',-1,true)}>−</button><strong>{editWhite}</strong><button type="button" aria-label="Ajouter un indice blanc de correction" disabled={editBlack + editWhite === 4 || busy} on:click={() => adjust('white',1,true)}>+</button></div>
                  <div class="action-buttons"><button type="button" on:click={() => editing = null}>Annuler</button><button class="primary-action" type="submit" disabled={busy}>Enregistrer la correction</button></div>
                </form>
              {/if}
            {/if}
          {/if}
        </div>
      {:else}<p class="empty-board">Aucun essai envoyé sur ce plateau.</p>{/each}
    </section>
    <details class="private-code"><summary>Consulter mon code secret</summary><CodePegs colors={setupSlots} label="Votre code secret" /></details>
    {#if matchState?.opponentSecretCode?.length}<section class="action-card"><h2>Le code adverse</h2><CodePegs colors={matchState.opponentSecretCode} label="Code adverse révélé après abandon" /></section>{/if}
  {/if}

  {#if notices.length}<details class="notifications"><summary>Vérifications et corrections ({notices.length})</summary>{#each notices as notice}<p>{notice.actorId === actorId ? 'Vous avez' : `${opponentName} a`} {notice.actionType === 'CORRECT_FEEDBACK' ? 'corrigé des indices. Le résultat a été actualisé.' : 'demandé une vérification des indices.'}</p>{/each}</details>{/if}
  {#if finished}<button class="primary-action" type="button" disabled={busy || myRematchAsked || !isBackendReady} on:click={() => dispatch('requestrematch')}>{myRematchAsked ? 'Revanche proposée · en attente' : theirRematchAsked ? 'Accepter la revanche' : 'Proposer une revanche'}</button>
  {:else if !preparation}<button class="subtle abandon" type="button" disabled={!isBackendReady || busy} on:click={() => confirmAbandon = true}>Abandonner la manche</button>{/if}
  {#if confirmAbandon}<div class="abandon-confirm" role="alert"><strong>Abandonner cette manche ?</strong><p>La recherche s’arrêtera et les codes seront révélés. Le résultat du duel sera conservé s’il est déjà attribué.</p><div class="action-buttons"><button type="button" on:click={() => confirmAbandon = false}>Continuer à jouer</button><button type="button" disabled={busy} on:click={() => { dispatch('abandon'); confirmAbandon = false }}>Confirmer l’abandon</button></div></div>{/if}
  <span class="sr-only">{hasSecretSet ? 'Votre code est prêt.' : ''}</span>
</section>

<style>
  .partie-screen { display:grid; gap:16px; max-width:760px; margin:auto; }
  .action-card,.board-surface,.duel-result,.abandon-confirm { border:1px solid var(--line); background:var(--panel); border-radius:16px; padding:20px; }
  h2 { margin:0 0 8px; font:600 19px 'Space Grotesk',sans-serif; line-height:1.3; }
  p { margin:0 0 14px; font-size:14px; color:var(--muted); line-height:1.6; }
  button { min-height:46px; padding:10px 14px; font:500 14px Inter,sans-serif; border:1px solid var(--line); border-radius:10px; color:var(--text); background:var(--panel-strong); cursor:pointer; }
  button:disabled { opacity:.5; cursor:default; }
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
  .correction-form { padding:12px; background:var(--panel-strong); border:1px solid var(--line); border-radius:12px; margin-top:12px; }
  .private-code,.notifications { border-top:1px solid var(--line); padding:4px 8px; }
  .private-code :global(.code-pegs) { margin:6px 0 16px; }
  .waiting-note { padding:16px; color:var(--muted); border:1px dashed var(--line); border-radius:12px; font-size:14px; line-height:1.6; }
  .duel-result strong { font:600 20px 'Space Grotesk',sans-serif; }
  .duel-result p { margin:8px 0 0; }
  .empty-board { margin:0; }
  .abandon { justify-self:start; }
  .abandon-confirm { border-color:var(--error); }
  .abandon-confirm strong { display:block; margin-bottom:8px; }
  @media(max-width:420px) { .action-card { padding:16px; } .palette-grid button { gap:6px; } .attempt-content { gap:8px; } .comparison { gap:18px; } }

</style>

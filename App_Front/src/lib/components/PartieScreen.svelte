<script>
  import { createEventDispatcher } from 'svelte'

  /** @type {{ matchId?: string, turns?: Array<Record<string, unknown>>, state?: string } | null} */
  export let matchState = null
  export let isMyTurn = false
  export let actorId = 'p1'
  export let hasPlayerReady = false
  export let hasOpponentReady = false
  export let hasSecretSet = false

  /** @type {string[]} */ export let setupSequence = []
  /** @type {string[]} */ export let setupSlots = []
  export let selectedSetupSlot = 0
  /** @type {string[]} */ export let draftGuess = []
  export let selectedGuessSlot = 0
  /** @type {string[]} */ export let guessSequence = []
  /** @type {string[]} */ export let symbolPalette = []
  export let isSecretSubmitting = false
  export let isGuessSubmitting = false
  export let isBackendReady = false
  export let canSubmitSecret = false
  export let secretButtonLabel = ''
  export let canSubmitGuess = false
  export let sendButtonLabel = ''
  export let opponentActorId = null
  /** @type {(value: unknown) => string[]} */ export let asSymbols = () => []
  /** @type {(value: string | null | undefined) => { icon: string, token: string, toneClass: string }} */
  export let getSymbolVisual = () => ({ icon: '.', token: '_', toneClass: 'symbol-tone-muted' })

  const dispatch = createEventDispatcher()
  const boardRows = 10
  let bienPlaces = 0
  let malPlaces = 0
  let paletteTarget = 'secret'
  let lastFeedbackResetKey = ''

  const emit = (name, detail) => dispatch(name, detail)
  const adjustFeedback = (field, change) => {
    if (field === 'bienPlaces') bienPlaces = Math.max(0, Math.min(4 - malPlaces, bienPlaces + change))
    else malPlaces = Math.max(0, Math.min(4 - bienPlaces, malPlaces + change))
  }

  $: turns = matchState?.turns ?? []
  $: opponentStepDone = Boolean(opponentActorId && (matchState?.submittedPlayers ?? []).includes(opponentActorId))
  $: myStepDone = (matchState?.submittedPlayers ?? []).includes(actorId)
  $: myRematchAsked = (matchState?.rematchRequestedPlayers ?? []).includes(actorId)
  $: opponentRematchAsked = Boolean(opponentActorId && (matchState?.rematchRequestedPlayers ?? []).includes(opponentActorId))
  $: resultLabel = matchState?.winnerId === "DRAW" ? "MATCH NUL" : matchState?.winnerId === actorId ? "VICTOIRE" : matchState?.winnerId ? "DEFAITE" : "PARTIE TERMINEE"
  $: ownGuesses = turns.filter((entry) => entry?.actionType === 'PLAY_GUESS' && entry?.actorId === actorId)
  $: opponentGuesses = turns.filter((entry) => entry?.actionType === 'PLAY_GUESS' && entry?.actorId === opponentActorId)
  $: receivedFeedback = turns.filter((entry) => entry?.actionType === 'SEND_FEEDBACK' && entry?.actorId === opponentActorId)
  $: givenFeedback = turns.filter((entry) => entry?.actionType === 'SEND_FEEDBACK' && entry?.actorId === actorId)
  $: roundNumber = Math.floor((receivedFeedback.length + givenFeedback.length) / 2)
  $: ownGuessSubmitted = ownGuesses.length > roundNumber
  $: ownFeedbackSubmitted = givenFeedback.length > roundNumber
  $: ownActiveRowNumber = Math.min(ownGuesses.length + 1, boardRows)
  $: ownVisibleRow = Math.max(1, ownGuessSubmitted ? ownGuesses.length : ownActiveRowNumber)
  $: opponentVisibleRow = Math.max(1, opponentGuesses.length)
  $: ownBoard = Array.from({ length: boardRows }, (_, index) => ({
    number: index + 1,
    symbols: ownGuesses[index] ? asSymbols(ownGuesses[index].guess) : [],
    feedback: receivedFeedback[index] ? asSymbols(receivedFeedback[index].feedback) : [],
    isActive: isMyTurn && index + 1 === ownActiveRowNumber
  }))
  $: opponentBoard = Array.from({ length: boardRows }, (_, index) => ({
    number: index + 1,
    symbols: opponentGuesses[index] ? asSymbols(opponentGuesses[index].guess) : [],
    feedback: givenFeedback[index] ? asSymbols(givenFeedback[index].feedback) : []
  }))
  $: canRespond = isBackendReady && matchState?.state === 'WAITING_FEEDBACK' &&
    !ownFeedbackSubmitted && opponentGuesses.length > roundNumber
  $: phaseTitle = matchState?.state === 'PREPARATION' ? 'PREPAREZ VOS CODES' :
    matchState?.state === 'WAITING_GUESS' ? 'TENTATIVES EN PARALLELE' :
    matchState?.state === 'WAITING_FEEDBACK' ? 'REPONSES EN PARALLELE' : 'PARTIE TERMINEE'
  $: feedbackResetKey = (matchState?.matchId ?? '') + ':' + (matchState?.gameNumber ?? 1) + ':' + givenFeedback.length
  $: if (feedbackResetKey !== lastFeedbackResetKey) {
    bienPlaces = 0
    malPlaces = 0
    lastFeedbackResetKey = feedbackResetKey
  }
  $: if (hasSecretSet && paletteTarget === 'secret') paletteTarget = 'guess'

  function selectSetupSlot(index) {
    paletteTarget = 'secret'
    emit('selectsetupslot', { index })
  }

  function selectGuessSlot(index) {
    paletteTarget = 'guess'
    emit('selectguessslot', { index })
  }

  function choosePaletteSymbol(symbol) {
    emit(paletteTarget === 'secret' ? 'choosesetupsymbol' : 'chooseguesssymbol', { symbol })
  }
</script>

<section class="partie-screen board-layout" class:preparation={matchState?.state === "PREPARATION"}>
  <header class="board-status">
    <div class="turn-count"><span>MANCHE</span><strong>{roundNumber + 1}</strong></div>
    <div>
      <p class="eyebrow">MASTER MIND / PLATEAU</p>
      <h2>{phaseTitle}</h2>
      <p class="status-copy">{matchState?.state === 'PREPARATION' ? 'Chaque joueur valide son code.' : matchState?.state === 'WAITING_GUESS' ? 'Chaque joueur envoie sa tentative.' : 'Chaque joueur donne les indices de la tentative adverse.'}</p>
    </div>
    <div class:online={isBackendReady} class="connection-state">{isBackendReady ? 'SYNCHRONISE' : 'HORS LIGNE'}</div>
  </header>
  <div class="progress-strip" aria-live="polite">
    <span class:done={myStepDone}>VOUS : {myStepDone ? 'ETAPE TERMINEE' : 'EN COURS'}</span>
    <span class:done={opponentStepDone}>ADVERSAIRE : {opponentStepDone ? 'ETAPE TERMINEE' : 'EN COURS'}</span>
  </div>

  {#if matchState?.state === 'FINISHED'}
    <section class="result-banner" class:won={matchState?.winnerId === actorId} aria-live="assertive">
      <strong>{resultLabel}</strong>
      <span>SCORE · VOUS {matchState?.scores?.[actorId] ?? 0} — {matchState?.scores?.[opponentActorId] ?? 0} ADVERSAIRE</span>
      <button type="button" disabled={myRematchAsked || !isBackendReady} on:click={() => emit('requestrematch')}>
        {myRematchAsked ? 'REVANCHE DEMANDEE · ATTENTE ADVERSAIRE' : opponentRematchAsked ? 'ACCEPTER LA REVANCHE' : 'PROPOSER UNE REVANCHE'}
      </button>
    </section>
  {/if}

  <div class="game-columns">
    <div class="boards-column">
      <section class="board-surface own-board" aria-label="Mes tentatives sur le code adverse">
        <div class="board-caption"><span>MES TENTATIVES / SON CODE</span><small>{ownGuesses.length}/{boardRows}</small></div>
        <div class="board-grid">
          {#each ownBoard as row}
            <div class:active-row={row.isActive} class:played-row={row.symbols.length} class:older-row={row.number < ownVisibleRow} class:future-row={row.number > ownVisibleRow} class="board-row">
              <span class="row-number">{String(row.number).padStart(2, '0')}</span>
              <div class="board-pegs" aria-label={"Ma tentative " + row.number}>
                {#each Array(4) as _, slot}
                  {@const symbol = row.isActive ? draftGuess[slot] : row.symbols[slot]}
                  {@const visual = getSymbolVisual(symbol)}
                  {#if row.isActive}
                    <button type="button" class="peg {visual.toneClass} {selectedGuessSlot === slot ? 'selected' : ''}" on:click={() => selectGuessSlot(slot)} aria-label={"Case " + (slot + 1) + " de ma tentative"}><span>{visual.icon}</span></button>
                  {:else}
                    <span class="peg static {visual.toneClass}"><span>{visual.icon}</span></span>
                  {/if}
                {/each}
              </div>
              <div class="feedback-pegs" aria-label={"Indices recus pour la tentative " + row.number}>
                {#each Array(4) as _, peg}<span class="feedback-peg" class:black={row.feedback[peg] === 'BLACK'} class:white={row.feedback[peg] === 'WHITE'}></span>{/each}
              </div>
            </div>
          {/each}
        </div>
      </section>

      <section class="board-surface opponent-board" aria-label="Tentatives adverses sur mon code">
        <div class="board-caption"><span>SES TENTATIVES / MON CODE</span><small>{opponentGuesses.length}/{boardRows}</small></div>
        <div class="board-grid">
          {#each opponentBoard as row}
            <div class:played-row={row.symbols.length} class:older-row={row.number < opponentVisibleRow} class:future-row={row.number > opponentVisibleRow} class="board-row">
              <span class="row-number">{String(row.number).padStart(2, '0')}</span>
              <div class="board-pegs" aria-label={"Tentative adverse " + row.number}>
                {#each Array(4) as _, slot}
                  {@const visual = getSymbolVisual(row.symbols[slot])}
                  <span class="peg static {visual.toneClass}"><span>{visual.icon}</span></span>
                {/each}
              </div>
              <div class="feedback-pegs" aria-label={"Mes indices pour la tentative adverse " + row.number}>
                {#each Array(4) as _, peg}<span class="feedback-peg" class:black={row.feedback[peg] === 'BLACK'} class:white={row.feedback[peg] === 'WHITE'}></span>{/each}
              </div>
            </div>
          {/each}
        </div>
      </section>
    </div>

    <aside class="control-rail">
      <section class="control-panel secret-panel" class:locked={hasSecretSet}>
        <div class="panel-label"><span>VOTRE CODE</span><small>{hasSecretSet ? 'VERROUILLE' : setupSequence.length + '/4'}</small></div>
        <div class="secret-slots">
          {#each setupSlots as symbol, index}
            {@const visual = getSymbolVisual(symbol)}
            <button type="button" class="mini-peg {visual.toneClass} {selectedSetupSlot === index ? 'selected' : ''}" on:click={() => selectSetupSlot(index)} disabled={hasSecretSet} aria-label={"Case " + (index + 1) + " de mon code"}>{visual.icon}</button>
          {/each}
        </div>
        <div class="split-actions"><button type="button" class="text-button" on:click={() => emit('clearsetup')} disabled={hasSecretSet}>EFFACER</button><button type="button" class="text-button accent" disabled={!canSubmitSecret || setupSequence.length !== 4} on:click={() => emit('submitsecret')}>{isSecretSubmitting ? 'ENVOI...' : secretButtonLabel}</button></div>
      </section>

      <section class="control-panel palette-panel">
        <div class="panel-label"><span>PALETTE</span><small>{paletteTarget === 'secret' ? 'POUR VOTRE CODE' : 'POUR LA TENTATIVE'}</small></div>
        <div class="palette-grid">
          {#each symbolPalette as symbol}
            {@const visual = getSymbolVisual(symbol)}
            <button type="button" class="palette-peg {visual.toneClass}" on:click={() => choosePaletteSymbol(symbol)} disabled={!isBackendReady || (paletteTarget === 'secret' ? hasSecretSet : !isMyTurn)} aria-label={"Placer " + visual.token + " dans " + (paletteTarget === 'secret' ? 'mon code' : 'ma tentative')}><span>{visual.icon}</span></button>
          {/each}
        </div>
        <div class="active-line"><span>MA TENTATIVE</span><strong>{ownGuessSubmitted ? 'ENVOYEE' : guessSequence.length + '/4'}</strong></div>
        <div class="split-actions"><button type="button" class="text-button" on:click={() => emit('clearguess')} disabled={!isMyTurn}>EFFACER</button><button type="button" class="validate-button" disabled={!canSubmitGuess} on:click={() => emit('submitguess')}>{isGuessSubmitting ? 'ENVOI...' : sendButtonLabel}</button></div>
      </section>

      <section class="control-panel response-panel" class:response-ready={canRespond}>
        <div class="panel-label"><span>INDICES POUR SA TENTATIVE</span><small>{opponentActorId ?? 'ADVERSAIRE'}</small></div>
        <p>Comparez sa tentative a votre code, puis placez les pions.</p>
        <div class="feedback-control"><span class="black-dot"></span><span>BIEN PLACES</span><button type="button" on:click={() => adjustFeedback('bienPlaces', -1)} disabled={!canRespond || bienPlaces === 0}>-</button><strong>{bienPlaces}</strong><button type="button" on:click={() => adjustFeedback('bienPlaces', 1)} disabled={!canRespond || bienPlaces + malPlaces === 4}>+</button></div>
        <div class="feedback-control"><span class="white-dot"></span><span>MAL PLACES</span><button type="button" on:click={() => adjustFeedback('malPlaces', -1)} disabled={!canRespond || malPlaces === 0}>-</button><strong>{malPlaces}</strong><button type="button" on:click={() => adjustFeedback('malPlaces', 1)} disabled={!canRespond || bienPlaces + malPlaces === 4}>+</button></div>
        <button type="button" class="response-button" disabled={!canRespond} on:click={() => emit('submitfeedback', { bienPlaces, malPlaces })}>{ownFeedbackSubmitted ? 'INDICES ENVOYES' : 'VALIDER LES INDICES'}</button>
      </section>
    </aside>
  </div>

  <section class="history-strip">
    <div class="board-caption"><span>HISTORIQUE VISIBLE</span><small>{hasPlayerReady ? 'VOUS PRET' : 'EN ATTENTE'} / {hasOpponentReady ? 'ADVERSAIRE PRET' : 'ADVERSAIRE EN ATTENTE'}</small></div>
    <div class="history-events">
      {#each turns.slice().reverse().slice(0, 6) as entry}
        <div class="history-event"><span>{entry.actorId === opponentActorId ? 'ADVERSAIRE' : 'VOUS'}</span><strong>{entry.actionType === 'SEND_FEEDBACK' ? 'INDICES' : (entry.actionType ?? 'ACTION')}</strong></div>
      {:else}<p>Aucune action enregistree sur ce plateau.</p>{/each}
    </div>
  </section>
</section>
<style>
  .board-layout { gap: 16px; } .board-status { display: grid; grid-template-columns: auto 1fr auto; gap: 16px; align-items: center; padding: 16px; border: 1px solid var(--line); background: rgba(14, 19, 27, .82); } .turn-count { min-width: 76px; padding-right: 16px; border-right: 1px solid var(--line-soft); } .turn-count span, .eyebrow, .panel-label, .board-caption, .connection-state { font: 11px "JetBrains Mono", monospace; letter-spacing: .1em; } .turn-count span, .eyebrow, .status-copy, .control-panel p { color: var(--muted); } .turn-count strong { display: block; color: var(--primary-soft); font: 700 30px "Space Grotesk", sans-serif; } .eyebrow { margin: 0 0 4px; color: var(--primary); } .board-status h2 { margin: 0; color: #f4fbfc; font-size: 27px; } .status-copy { margin: 4px 0 0; font-size: 13px; } .connection-state { padding: 6px 8px; border: 1px solid var(--line-soft); color: var(--error); } .connection-state.online { color: var(--ok); border-color: color-mix(in srgb, var(--ok), transparent 55%); }
  .boards-column { display: grid; gap: 12px; min-width: 0; } .opponent-board { border-color: rgba(255, 171, 243, .45); } .opponent-board .board-caption { color: var(--secondary); }
  .game-columns { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(250px, .85fr); gap: 16px; align-items: start; } .board-surface, .control-panel, .history-strip { border: 1px solid var(--line); background: rgba(19, 23, 31, .84); } .board-surface, .control-panel, .history-strip { padding: 12px; } .board-caption, .panel-label { display: flex; justify-content: space-between; gap: 10px; color: var(--primary); } .board-caption { padding: 0 3px 10px; border-bottom: 1px solid var(--line-soft); } .board-caption small, .panel-label small { color: var(--muted); font-size: 10px; } .board-grid { display: grid; } .board-row { display: grid; grid-template-columns: 34px minmax(0, 1fr) 61px; gap: 8px; min-height: 48px; align-items: center; border-bottom: 1px solid rgba(132, 148, 149, .16); padding: 4px 2px; } .board-row.active-row { background: rgba(0, 219, 233, .08); box-shadow: inset 3px 0 var(--primary); } .row-number { color: #6a7a7d; font: 11px "JetBrains Mono", monospace; } .active-row .row-number { color: var(--primary-soft); } .board-pegs { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 5px; }
  .peg, .mini-peg, .palette-peg { display: grid; place-items: center; border: 1px solid var(--line-soft); background: #10161f; color: var(--muted); } .peg { aspect-ratio: 1; min-height: 34px; padding: 0; font-size: 15px; } .peg.static { pointer-events: none; } .peg.selected, .mini-peg.selected { outline: 2px solid var(--primary); outline-offset: 1px; } .feedback-pegs { display: grid; grid-template-columns: repeat(2, 1fr); gap: 4px; padding: 4px; border-left: 1px solid var(--line-soft); } .feedback-peg, .black-dot, .white-dot { width: 10px; height: 10px; border-radius: 50%; border: 1px solid #536064; background: transparent; } .feedback-peg.black, .black-dot { background: #101216; border-color: #8a999e; } .feedback-peg.white, .white-dot { background: #edf7f7; border-color: #edf7f7; }
  .control-rail { display: grid; gap: 12px; } .secret-slots { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin: 12px 0; } .mini-peg { aspect-ratio: 1; padding: 0; font-size: 17px; } .palette-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; margin: 12px 0; } .palette-peg { aspect-ratio: 1; padding: 0; font-size: 20px; cursor: pointer; transition: transform 140ms ease, border-color 140ms ease; } .palette-peg:hover { transform: translateY(-2px); border-color: currentColor; } .active-line { display: flex; justify-content: space-between; padding: 9px 0; border-top: 1px solid var(--line-soft); color: var(--muted); font: 10px "JetBrains Mono", monospace; letter-spacing: .08em; } .active-line strong { color: var(--primary-soft); } .split-actions { display: grid; grid-template-columns: 1fr 1.5fr; gap: 7px; margin-top: 8px; }
  .text-button, .validate-button, .response-button, .feedback-control button { border: 1px solid var(--line-soft); border-radius: 3px; background: transparent; color: var(--text); cursor: pointer; font: 10px "JetBrains Mono", monospace; letter-spacing: .05em; } .text-button, .validate-button, .response-button { min-height: 36px; padding: 5px 8px; } .text-button.accent, .validate-button, .response-button { border-color: rgba(0, 240, 255, .6); color: var(--primary-soft); } .validate-button, .response-button { background: rgba(0, 219, 233, .11); } button:disabled { opacity: .4; cursor: not-allowed; } .response-panel { opacity: .62; } .response-panel.response-ready { opacity: 1; border-color: rgba(255, 171, 243, .48); } .control-panel p { margin: 10px 0 12px; font-size: 12px; line-height: 1.4; } .feedback-control { display: grid; grid-template-columns: 12px 1fr 26px 20px 26px; gap: 7px; align-items: center; margin: 7px 0; color: var(--muted); font: 10px "JetBrains Mono", monospace; } .feedback-control button { width: 26px; height: 26px; padding: 0; font-size: 16px; } .feedback-control strong { color: var(--text); text-align: center; font-size: 14px; } .response-button { width: 100%; margin-top: 8px; }
  .history-events { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 7px; margin-top: 10px; } .history-event { min-width: 0; border-left: 2px solid var(--secondary); padding: 5px 8px; background: rgba(8, 13, 20, .52); } .history-event span, .history-event strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .history-event span { color: var(--muted); font: 9px "JetBrains Mono", monospace; } .history-event strong { margin-top: 3px; font: 11px "JetBrains Mono", monospace; color: var(--text); } .history-events p { grid-column: 1 / -1; margin: 4px 0; color: var(--muted); font-size: 12px; }
  @media (max-width: 650px) { .board-status { grid-template-columns: auto 1fr; } .connection-state { grid-column: 1 / -1; justify-self: stretch; text-align: center; } .game-columns { grid-template-columns: 1fr; } .control-rail { grid-template-columns: 1fr 1fr; } .response-panel { grid-column: 1 / -1; } .history-events { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 390px) { .control-rail { grid-template-columns: 1fr; } .board-row { grid-template-columns: 26px minmax(0, 1fr) 51px; gap: 5px; } .board-status h2 { font-size: 23px; } }
  @media (max-width: 650px) {
    .board-layout { gap: 7px; }
    .board-status { grid-template-columns: auto 1fr; gap: 8px; padding: 8px; }
    .turn-count { min-width: 42px; padding-right: 8px; }
    .turn-count strong { font-size: 22px; }
    .eyebrow, .board-status .status-copy:first-of-type, .connection-state { display: none; }
    .board-status h2 { font-size: 18px; }
    .status-copy { font-size: 10px; line-height: 1.3; }
    .game-columns { display: flex; flex-direction: column; gap: 7px; }
    .control-rail, .boards-column { display: contents; }
    .secret-panel { order: 1; }
    .own-board { order: 2; }
    .palette-panel { order: 3; }
    .opponent-board { order: 4; }
    .response-panel { order: 5; }
    .board-surface, .control-panel { width: 100%; padding: 8px; }
    .board-caption { padding-bottom: 5px; }
    .board-row { min-height: 38px; padding: 2px; }
    .board-row.older-row, .board-row.future-row { display: none; }
    .peg { min-height: 30px; }
    .secret-slots { margin: 6px 0; }
    .secret-panel.locked .split-actions { display: none; }
    .mini-peg { min-height: 34px; aspect-ratio: auto; }
    .palette-grid { grid-template-columns: repeat(8, minmax(0, 1fr)); gap: 3px; margin: 7px 0; }
    .palette-peg { min-height: 34px; aspect-ratio: auto; font-size: 16px; }
    .split-actions { margin-top: 5px; }
    .text-button, .validate-button, .response-button { min-height: 30px; }
    .active-line { padding: 5px 0; }
    .response-panel p { display: none; }
    .feedback-control { margin: 4px 0; }
    .response-button { margin-top: 4px; }
    .history-strip { display: none; }
  }
  .progress-strip { display:flex; gap:8px; flex-wrap:wrap; }
  .progress-strip span { flex:1; min-width:150px; padding:7px 10px; border:1px solid var(--line); color:var(--muted); font:10px "JetBrains Mono", monospace; letter-spacing:.06em; }
  .progress-strip span.done { color:var(--ok); border-color:color-mix(in srgb,var(--ok),transparent 55%); }
  .result-banner { display:grid; gap:8px; padding:16px; border:1px solid var(--secondary); background:rgba(255,171,243,.08); text-align:center; }
  .result-banner.won { border-color:var(--ok); background:rgba(89,232,166,.1); }
  .result-banner strong { font:700 32px "Space Grotesk",sans-serif; color:var(--secondary); }
  .result-banner.won strong { color:var(--ok); }
  .result-banner span { font:11px "JetBrains Mono",monospace; }
  .result-banner button { min-height:40px; border:1px solid var(--primary); background:rgba(0,219,233,.12); color:var(--primary-soft); cursor:pointer; }
  .result-banner button:disabled { opacity:.6; cursor:default; }
  .board-grid { display:flex; flex-direction:column-reverse; max-height:220px; overflow-y:auto; scrollbar-width:thin; scrollbar-color:var(--primary) transparent; }
  .board-row { flex:none; }
  .board-row.future-row { display:none; }
  @media (min-width:651px) {
    .game-columns { display:grid; grid-template-columns:minmax(0,1.4fr) minmax(250px,1fr); gap:12px; }
    .boards-column,.control-rail { display:contents; }
    .own-board { grid-column:1; grid-row:1; }
    .palette-panel { grid-column:2; grid-row:1; }
    .opponent-board { grid-column:1; grid-row:2 / span 2; align-self:start; }
    .secret-panel { grid-column:2; grid-row:2; }
    .response-panel { grid-column:2; grid-row:3; }
  }
  @media (max-width:650px) {
    .own-board { order:1; }
    .palette-panel { order:2; }
    .opponent-board { order:3; }
    .secret-panel { order:4; }
    .response-panel { order:5; }
    .board-grid { max-height:104px; }
    .board-row.older-row { display:grid; }
    .progress-strip span { min-width:0; font-size:9px; }
    .preparation .secret-panel { order:1; }
    .preparation .palette-panel { order:2; }
    .preparation .own-board { order:3; }
    .preparation .opponent-board { order:4; }
  }
</style>
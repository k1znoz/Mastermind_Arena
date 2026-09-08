<script>
  import { createEventDispatcher } from 'svelte'

  /** @type {{ turnNumber?: number, actionLog?: Array<Record<string, unknown>> } | null} */
  export let matchState = null
  export let isMyTurn = false
  export let turnActive = false
  export let hasPlayerReady = false
  export let hasOpponentReady = false
  export let hasSecretSet = false
  export let hasOpponentSecretSet = false
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
  export let getSymbolVisual = () => ({ icon: '·', token: '_', toneClass: 'symbol-tone-muted' })

  const dispatch = createEventDispatcher()
  const boardRows = 10
  let bienPlaces = 0
  let malPlaces = 0

  const emit = (name, detail) => dispatch(name, detail)
  const adjustFeedback = (field, change) => {
    if (field === 'bienPlaces') bienPlaces = Math.max(0, Math.min(4 - malPlaces, bienPlaces + change))
    else malPlaces = Math.max(0, Math.min(4 - bienPlaces, malPlaces + change))
  }

  $: actionLog = matchState?.actionLog ?? []
  $: playedGuesses = actionLog.filter((entry) => entry?.actionType === 'SUBMIT_GUESS')
  $: feedbackEntries = actionLog.filter((entry) => entry?.actionType === 'FEEDBACK_SENT')
  $: activeRowNumber = Math.min(playedGuesses.length + 1, boardRows)
  $: board = Array.from({ length: boardRows }, (_, index) => ({
    number: index + 1,
    symbols: playedGuesses[index] ? asSymbols(playedGuesses[index].symbols) : [],
    feedback: feedbackEntries[index] ? asSymbols(feedbackEntries[index].symbols) : [],
    isActive: !playedGuesses[index] && index + 1 === activeRowNumber
  }))
  $: canRespond = isBackendReady && turnActive && !isMyTurn && hasOpponentSecretSet
</script>

<section class="partie-screen board-layout">
  <header class="board-status">
    <div class="turn-count"><span>TOUR</span><strong>{matchState?.turnNumber ?? 0}</strong></div>
    <div>
      <p class="eyebrow">MASTER MIND / PLATEAU</p>
      <h2>{isMyTurn ? 'À VOTRE TOUR' : 'RÉPONSE ADVERSE'}</h2>
      <p class="status-copy">{isMyTurn ? 'Composez une tentative puis verrouillez la ligne.' : 'Attendez la proposition ou répondez avec les pions.'}</p>
    </div>
    <div class:online={isBackendReady} class="connection-state">{isBackendReady ? 'SYNCHRONISÉ' : 'HORS LIGNE'}</div>
  </header>

  <div class="game-columns">
    <section class="board-surface" aria-label="Plateau des tentatives">
      <div class="board-caption"><span>VOS TENTATIVES</span><small>{playedGuesses.length}/{boardRows} LIGNES</small></div>
      <div class="board-grid">
        {#each board as row}
          <div class:active-row={row.isActive} class:played-row={row.symbols.length} class="board-row">
            <span class="row-number">{String(row.number).padStart(2, '0')}</span>
            <div class="board-pegs" aria-label={`Tentative ${row.number}`}>
              {#each Array(4) as _, slot}
                {@const symbol = row.isActive ? draftGuess[slot] : row.symbols[slot]}
                {@const visual = getSymbolVisual(symbol)}
                {#if row.isActive}
                  <button type="button" class="peg {visual.toneClass} {selectedGuessSlot === slot ? 'selected' : ''}" on:click={() => emit('selectguessslot', { index: slot })} aria-label={`Emplacement ${slot + 1} de la tentative active`}><span>{visual.icon}</span></button>
                {:else}
                  <span class="peg static {visual.toneClass}"><span>{visual.icon}</span></span>
                {/if}
              {/each}
            </div>
            <div class="feedback-pegs" aria-label={`Indices du tour ${row.number}`}>
              {#each Array(4) as _, peg}<span class="feedback-peg" class:black={row.feedback[peg] === 'BLACK'} class:white={row.feedback[peg] === 'WHITE'}></span>{/each}
            </div>
          </div>
        {/each}
      </div>
    </section>

    <aside class="control-rail">
      <section class="control-panel secret-panel">
        <div class="panel-label"><span>VOTRE CODE</span><small>{hasSecretSet ? 'VERROUILLÉ' : `${setupSequence.length}/4`}</small></div>
        <div class="secret-slots">
          {#each setupSlots as symbol, index}
            {@const visual = getSymbolVisual(symbol)}
            <button type="button" class="mini-peg {visual.toneClass} {selectedSetupSlot === index ? 'selected' : ''}" on:click={() => emit('selectsetupslot', { index })}>{visual.icon}</button>
          {/each}
        </div>
        <div class="split-actions"><button type="button" class="text-button" on:click={() => emit('clearsetup')}>EFFACER</button><button type="button" class="text-button accent" disabled={!canSubmitSecret || setupSequence.length !== 4} on:click={() => emit('submitsecret')}>{isSecretSubmitting ? '...' : secretButtonLabel}</button></div>
      </section>

      <section class="control-panel palette-panel">
        <div class="panel-label"><span>PALETTE</span><small>CHOISIR UNE COULEUR</small></div>
        <div class="palette-grid">
          {#each symbolPalette as symbol}
            {@const visual = getSymbolVisual(symbol)}
            <button type="button" class="palette-peg {visual.toneClass}" on:click={() => emit('chooseguesssymbol', { symbol })} aria-label={`Placer ${visual.token} dans la tentative`}><span>{visual.icon}</span></button>
          {/each}
        </div>
        <div class="active-line"><span>LIGNE ACTIVE</span><strong>{guessSequence.length === 4 ? 'PRÊTE' : `${guessSequence.length}/4`}</strong></div>
        <div class="split-actions"><button type="button" class="text-button" on:click={() => emit('clearguess')}>EFFACER</button><button type="button" class="validate-button" disabled={!canSubmitGuess} on:click={() => emit('submitguess')}>{isGuessSubmitting ? 'ENVOI...' : sendButtonLabel}</button></div>
      </section>

      <section class="control-panel response-panel" class:response-ready={canRespond}>
        <div class="panel-label"><span>RÉPONSE ADVERSAIRE</span><small>{opponentActorId ?? 'JOUEUR 2'}</small></div>
        <p>Placez les pions correspondant à la tentative reçue.</p>
        <div class="feedback-control"><span class="black-dot"></span><span>BIEN PLACÉS</span><button type="button" on:click={() => adjustFeedback('bienPlaces', -1)} disabled={!canRespond || bienPlaces === 0}>−</button><strong>{bienPlaces}</strong><button type="button" on:click={() => adjustFeedback('bienPlaces', 1)} disabled={!canRespond || bienPlaces + malPlaces === 4}>+</button></div>
        <div class="feedback-control"><span class="white-dot"></span><span>MAL PLACÉS</span><button type="button" on:click={() => adjustFeedback('malPlaces', -1)} disabled={!canRespond || malPlaces === 0}>−</button><strong>{malPlaces}</strong><button type="button" on:click={() => adjustFeedback('malPlaces', 1)} disabled={!canRespond || bienPlaces + malPlaces === 4}>+</button></div>
        <button type="button" class="response-button" disabled={!canRespond} on:click={() => emit('submitfeedback', { bienPlaces, malPlaces })}>VALIDER LES INDICES</button>
      </section>
    </aside>
  </div>

  <section class="history-strip">
    <div class="board-caption"><span>HISTORIQUE VISIBLE</span><small>{hasPlayerReady ? 'VOUS PRÊT' : 'EN ATTENTE'} · {hasOpponentReady ? 'ADVERSAIRE PRÊT' : 'ADVERSAIRE EN ATTENTE'}</small></div>
    <div class="history-events">
      {#each actionLog.slice().reverse().slice(0, 6) as entry}
        <div class="history-event"><span>{entry.actorId === opponentActorId ? 'ADVERSAIRE' : 'VOUS'}</span><strong>{entry.actionType ?? 'ACTION'}</strong></div>
      {:else}<p>Aucune action enregistrée sur ce plateau.</p>{/each}
    </div>
  </section>
</section>

<style>
  .board-layout { gap: 16px; } .board-status { display: grid; grid-template-columns: auto 1fr auto; gap: 16px; align-items: center; padding: 16px; border: 1px solid var(--line); background: rgba(14, 19, 27, .82); } .turn-count { min-width: 76px; padding-right: 16px; border-right: 1px solid var(--line-soft); } .turn-count span, .eyebrow, .panel-label, .board-caption, .connection-state { font: 11px "JetBrains Mono", monospace; letter-spacing: .1em; } .turn-count span, .eyebrow, .status-copy, .control-panel p { color: var(--muted); } .turn-count strong { display: block; color: var(--primary-soft); font: 700 30px "Space Grotesk", sans-serif; } .eyebrow { margin: 0 0 4px; color: var(--primary); } .board-status h2 { margin: 0; color: #f4fbfc; font-size: 27px; } .status-copy { margin: 4px 0 0; font-size: 13px; } .connection-state { padding: 6px 8px; border: 1px solid var(--line-soft); color: var(--error); } .connection-state.online { color: var(--ok); border-color: color-mix(in srgb, var(--ok), transparent 55%); }
  .game-columns { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(250px, .85fr); gap: 16px; align-items: start; } .board-surface, .control-panel, .history-strip { border: 1px solid var(--line); background: rgba(19, 23, 31, .84); } .board-surface, .control-panel, .history-strip { padding: 12px; } .board-caption, .panel-label { display: flex; justify-content: space-between; gap: 10px; color: var(--primary); } .board-caption { padding: 0 3px 10px; border-bottom: 1px solid var(--line-soft); } .board-caption small, .panel-label small { color: var(--muted); font-size: 10px; } .board-grid { display: grid; } .board-row { display: grid; grid-template-columns: 34px minmax(0, 1fr) 61px; gap: 8px; min-height: 48px; align-items: center; border-bottom: 1px solid rgba(132, 148, 149, .16); padding: 4px 2px; } .board-row.active-row { background: rgba(0, 219, 233, .08); box-shadow: inset 3px 0 var(--primary); } .row-number { color: #6a7a7d; font: 11px "JetBrains Mono", monospace; } .active-row .row-number { color: var(--primary-soft); } .board-pegs { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 5px; }
  .peg, .mini-peg, .palette-peg { display: grid; place-items: center; border: 1px solid var(--line-soft); background: #10161f; color: var(--muted); } .peg { aspect-ratio: 1; min-height: 34px; padding: 0; font-size: 15px; } .peg.static { pointer-events: none; } .peg.selected, .mini-peg.selected { outline: 2px solid var(--primary); outline-offset: 1px; } .feedback-pegs { display: grid; grid-template-columns: repeat(2, 1fr); gap: 4px; padding: 4px; border-left: 1px solid var(--line-soft); } .feedback-peg, .black-dot, .white-dot { width: 10px; height: 10px; border-radius: 50%; border: 1px solid #536064; background: transparent; } .feedback-peg.black, .black-dot { background: #101216; border-color: #8a999e; } .feedback-peg.white, .white-dot { background: #edf7f7; border-color: #edf7f7; }
  .control-rail { display: grid; gap: 12px; } .secret-slots { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin: 12px 0; } .mini-peg { aspect-ratio: 1; padding: 0; font-size: 17px; } .palette-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; margin: 12px 0; } .palette-peg { aspect-ratio: 1; padding: 0; font-size: 20px; cursor: pointer; transition: transform 140ms ease, border-color 140ms ease; } .palette-peg:hover { transform: translateY(-2px); border-color: currentColor; } .active-line { display: flex; justify-content: space-between; padding: 9px 0; border-top: 1px solid var(--line-soft); color: var(--muted); font: 10px "JetBrains Mono", monospace; letter-spacing: .08em; } .active-line strong { color: var(--primary-soft); } .split-actions { display: grid; grid-template-columns: 1fr 1.5fr; gap: 7px; margin-top: 8px; }
  .text-button, .validate-button, .response-button, .feedback-control button { border: 1px solid var(--line-soft); border-radius: 3px; background: transparent; color: var(--text); cursor: pointer; font: 10px "JetBrains Mono", monospace; letter-spacing: .05em; } .text-button, .validate-button, .response-button { min-height: 36px; padding: 5px 8px; } .text-button.accent, .validate-button, .response-button { border-color: rgba(0, 240, 255, .6); color: var(--primary-soft); } .validate-button, .response-button { background: rgba(0, 219, 233, .11); } button:disabled { opacity: .4; cursor: not-allowed; } .response-panel { opacity: .62; } .response-panel.response-ready { opacity: 1; border-color: rgba(255, 171, 243, .48); } .control-panel p { margin: 10px 0 12px; font-size: 12px; line-height: 1.4; } .feedback-control { display: grid; grid-template-columns: 12px 1fr 26px 20px 26px; gap: 7px; align-items: center; margin: 7px 0; color: var(--muted); font: 10px "JetBrains Mono", monospace; } .feedback-control button { width: 26px; height: 26px; padding: 0; font-size: 16px; } .feedback-control strong { color: var(--text); text-align: center; font-size: 14px; } .response-button { width: 100%; margin-top: 8px; }
  .history-events { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 7px; margin-top: 10px; } .history-event { min-width: 0; border-left: 2px solid var(--secondary); padding: 5px 8px; background: rgba(8, 13, 20, .52); } .history-event span, .history-event strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .history-event span { color: var(--muted); font: 9px "JetBrains Mono", monospace; } .history-event strong { margin-top: 3px; font: 11px "JetBrains Mono", monospace; color: var(--text); } .history-events p { grid-column: 1 / -1; margin: 4px 0; color: var(--muted); font-size: 12px; }
  @media (max-width: 650px) { .board-status { grid-template-columns: auto 1fr; } .connection-state { grid-column: 1 / -1; justify-self: stretch; text-align: center; } .game-columns { grid-template-columns: 1fr; } .control-rail { grid-template-columns: 1fr 1fr; } .response-panel { grid-column: 1 / -1; } .history-events { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 390px) { .control-rail { grid-template-columns: 1fr; } .board-row { grid-template-columns: 26px minmax(0, 1fr) 51px; gap: 5px; } .board-status h2 { font-size: 23px; } }
</style>
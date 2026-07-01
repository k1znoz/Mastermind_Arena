<script>
  import { onDestroy, onMount } from 'svelte'
  import GameHeader from './lib/components/GameHeader.svelte'
  import StatusBanner from './lib/components/StatusBanner.svelte'
  import AttemptsHistory from './lib/components/AttemptsHistory.svelte'
  import ControlPanel from './lib/components/ControlPanel.svelte'
  import ResultOverlay from './lib/components/ResultOverlay.svelte'
  import { CODE_LENGTH, MAX_ATTEMPTS, SYMBOLS, SYMBOL_MAP } from './lib/game/constants'
  import { pickSecretCode, getFeedback, getFeedbackPegs } from './lib/game/engine'
  import { formatTime } from './lib/game/formatters'
  import './lib/game/game.css'

  let status = 'active'
  let secretCode = pickSecretCode()
  let attempts = []
  let activeGuess = []
  let startMs = Date.now()
  let endMs = null
  let nowMs = Date.now()

  $: elapsedMs = (status === 'active' ? nowMs : endMs ?? nowMs) - startMs
  $: attemptsLeft = MAX_ATTEMPTS - attempts.length
  $: canSubmit = status === 'active' && activeGuess.length === CODE_LENGTH

  function startNewGame() {
    status = 'active'
    secretCode = pickSecretCode()
    attempts = []
    activeGuess = []
    startMs = Date.now()
    endMs = null
  }

  function addSymbol(symbolId) {
    if (status !== 'active' || activeGuess.length >= CODE_LENGTH) {
      return
    }

    activeGuess = [...activeGuess, symbolId]
  }

  function removeLast() {
    if (status !== 'active' || activeGuess.length === 0) {
      return
    }

    activeGuess = activeGuess.slice(0, -1)
  }

  function submitGuess() {
    if (!canSubmit) {
      return
    }

    const feedback = getFeedback(secretCode, activeGuess)
    const nextAttempts = [...attempts, { guess: [...activeGuess], ...feedback }]

    attempts = nextAttempts
    activeGuess = []

    if (feedback.exact === CODE_LENGTH) {
      status = 'won'
      endMs = Date.now()
      return
    }

    if (nextAttempts.length >= MAX_ATTEMPTS) {
      status = 'lost'
      endMs = Date.now()
    }
  }

  let timerId
  onMount(() => {
    timerId = setInterval(() => {
      nowMs = Date.now()
    }, 200)
  })

  onDestroy(() => {
    clearInterval(timerId)
  })
</script>

<main class="terminal-shell">
  <div class="background-grid" aria-hidden="true"></div>
  <div class="background-glow" aria-hidden="true"></div>

  <GameHeader attemptsCount={attempts.length} maxAttempts={MAX_ATTEMPTS} timerText={formatTime(elapsedMs)} />

  <section class="board" aria-live="polite">
    <StatusBanner {status} />

    <AttemptsHistory
      {attempts}
      {activeGuess}
      {status}
      symbolMap={SYMBOL_MAP}
      {getFeedbackPegs}
    />

    {#if status !== 'active'}
      <ResultOverlay
        {status}
        attemptsCount={attempts.length}
        elapsed={formatTime(elapsedMs)}
        {secretCode}
        symbolMap={SYMBOL_MAP}
        onRestart={startNewGame}
      />
    {/if}

    {#if status === 'active'}
      <ControlPanel
        codeLength={CODE_LENGTH}
        {attemptsLeft}
        symbols={SYMBOLS}
        {canSubmit}
        onAddSymbol={addSymbol}
        onRemoveLast={removeLast}
        onSubmit={submitGuess}
      />
    {/if}
  </section>
</main>

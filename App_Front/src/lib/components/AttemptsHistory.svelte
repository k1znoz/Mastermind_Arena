<script>
  import GuessSlot from './GuessSlot.svelte'
  import FeedbackPegs from './FeedbackPegs.svelte'
  import { CODE_LENGTH } from '../game/constants'

  export let attempts = []
  export let activeGuess = []
  export let status = 'active'
  export let symbolMap = new Map()
  export let getFeedbackPegs = (feedback) => []
</script>

<div class="history">
  {#each attempts as attempt, index}
    <article class="attempt-row">
      <p class="attempt-id">{String(index + 1).padStart(2, '0')}</p>
      <div class="guess-track">
        {#each attempt.guess as symbolId}
          <GuessSlot symbol={symbolMap.get(symbolId)} />
        {/each}
      </div>
      <FeedbackPegs
        pegs={getFeedbackPegs(attempt)}
        ariaLabel={`Feedback: ${attempt.exact} exact, ${attempt.close} close`}
      />
    </article>
  {/each}

  {#if status === 'active'}
    <article class="attempt-row active-row">
      <p class="attempt-id">{String(attempts.length + 1).padStart(2, '0')}</p>
      <div class="guess-track">
        {#each Array(CODE_LENGTH) as _, index}
          <GuessSlot symbol={symbolMap.get(activeGuess[index])} />
        {/each}
      </div>
      <div class="feedback in-progress">WAITING SIGNAL</div>
    </article>
  {/if}
</div>

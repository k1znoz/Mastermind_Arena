<script>
  import GuessSlot from './GuessSlot.svelte'

  export let status = 'active'
  export let attemptsCount = 0
  export let elapsed = '00:00'
  export let secretCode = []
  export let symbolMap = new Map()
  export let onRestart = () => {}
</script>

<section class={`result-card ${status}`}>
  <h2>{status === 'won' ? 'MISSION ACCOMPLISHED' : 'MISSION COMPROMISED'}</h2>
  <p>
    {#if status === 'won'}
      Sequence decrypted in {attemptsCount} attempts.
    {:else}
      Attempts exhausted. Neural interface overload.
    {/if}
  </p>

  <div class="revealed">
    {#each secretCode as symbolId}
      <GuessSlot symbol={symbolMap.get(symbolId)} placeholder="?" />
    {/each}
  </div>

  <p class="reveal-label">SECRET SEQUENCE REVEALED</p>
  <p class="result-meta">time {elapsed} | rank {status === 'won' ? 'S' : 'F'}</p>
  <button class="primary" on:click={onRestart}>RESTART PROTOCOL</button>
</section>

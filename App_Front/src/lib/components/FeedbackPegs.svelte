<script>
  export let pegs = []
  export let ariaLabel = 'Feedback'
</script>

<div class="feedback" aria-label={ariaLabel}>
  {#each pegs as peg}
    <span class={`peg ${peg}`}></span>
  {/each}
</div>

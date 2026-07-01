<script>
  export let codeLength = 4
  export let attemptsLeft = 0
  export let symbols = []
  export let canSubmit = false

  export let onAddSymbol = () => {}
  export let onRemoveLast = () => {}
  export let onSubmit = () => {}
</script>

<section class="controls">
  <div class="legend">
    <span>code length {codeLength}</span>
    <span>symbol repetition enabled</span>
    <span>match = correct symbol and correct position</span>
    <span>close = correct symbol wrong position</span>
    <span>attempts left {attemptsLeft}</span>
  </div>

  <div class="picker" role="group" aria-label="Symbol picker">
    {#each symbols as symbol}
      <button
        type="button"
        class="pick"
        style={`--chip:${symbol.color}`}
        on:click={() => onAddSymbol(symbol.id)}
      >
        <strong>{symbol.glyph}</strong>
        <span>{symbol.label}</span>
      </button>
    {/each}
  </div>

  <div class="actions">
    <button type="button" class="ghost" on:click={onRemoveLast}>REMOVE LAST</button>
    <button type="button" class="primary" on:click={onSubmit} disabled={!canSubmit}>
      SUBMIT PROPOSAL
    </button>
  </div>
</section>

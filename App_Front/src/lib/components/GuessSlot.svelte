<script>
  export let symbol = null
  export let placeholder = '+'
</script>

{#if symbol}
  <span class="slot filled" style={`--chip:${symbol.color}`}>
    {symbol.glyph}
  </span>
{:else}
  <span class="slot">{placeholder}</span>
{/if}

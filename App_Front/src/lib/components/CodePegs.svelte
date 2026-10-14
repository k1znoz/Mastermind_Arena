<script>
  import { getSymbolVisual } from '../utils/symbolVisuals'
  export let colors = []
  export let label = 'Combinaison'
</script>
<div class="code-pegs" role="img" aria-label={label + ' : ' + colors.map(color => getSymbolVisual(color).token).join(', ')}>
  {#each colors as color}<span class="color-dot {getSymbolVisual(color).toneClass}" title={getSymbolVisual(color).token}></span>{/each}
</div>
<style>
  .code-pegs { display:flex; gap:8px; align-items:center; }
  .color-dot { display:block; width:30px; height:30px; flex:none; border-radius:50%; background:var(--peg-color,var(--line)); border:2px solid var(--peg-border,var(--line)); box-shadow:inset 0 2px 3px #0002; }
</style>

<script>
  /** @typedef {'home'|'lobby'|'setup'|'arena'|'correction'|'transition'|'results'|'history'|'recovery'} Screen */

  export let screens = /** @type {Record<string, string>} */ ({})
  export let currentScreen = ''
  export let onOpenScreen = /** @type {(screen: Screen) => void} */ (() => {})

  const orderedScreenKeys = [
    'ARENA',
    'HISTORY',
    'SETUP',
    'LOBBY'
  ]

  const labels = /** @type {Record<string, string>} */ ({
    ARENA: 'Arena',
    HISTORY: 'Timeline',
    SETUP: 'Tactics',
    LOBBY: 'Profile'
  })
</script>

<nav class="screen-nav" aria-label="Screens">
  {#each orderedScreenKeys as key}
    {#if screens[key]}
      <button
        type="button"
        class="nav-btn"
        class:active={currentScreen === screens[key]}
        on:click={() => onOpenScreen(screens[key])}
      >
        {labels[key] ?? key}
      </button>
    {/if}
  {/each}
</nav>

<script>
  import { createEventDispatcher } from 'svelte'

  export let isPrototype = true
  export let submitStatus = 'idle'
  export let isBackendReady = false
  export let hasPlayerReady = false
  export let ctaSessionLabel = 'CRÉER UNE PARTIE'

  export let playerName = ''
  export let roomCode = ''

  const dispatch = createEventDispatcher()

  function openDebug() {
    dispatch('opendebug')
  }

  function submitReady() {
    dispatch('submitready')
  }
</script>

<section class="session-screen">
  <article class="system-card">
    <h1>SYSTÈME OPÉRATIONNEL</h1>
    <p>Console de commande Mastermind Arena. En attente de signaux du backend.</p>
    <button type="button" class="primary-btn" on:click={openDebug}>OUVRIR CONSOLE DEBUG</button>
  </article>

  <article class="panel">
    <div class="section-head">
      <span class="kicker">Canal tactique sécurisé</span>
      {#if isPrototype}
        <span class="prototype-badge">prototype</span>
      {/if}
    </div>
    <h2>Entrer dans l'arène</h2>

    <label>
      <span>IDENTITÉ [B]</span>
      <input type="text" placeholder="NOM DU JOUEUR" bind:value={playerName} />
    </label>

    <label>
      <span>COORDONNÉES DE COMBAT [B]</span>
      <input type="text" placeholder="CODE DE SALLE" bind:value={roomCode} />
    </label>

    <p class="hint">Laissez vide pour initier un nouveau duel.</p>

    <button
      type="button"
      class="primary-btn {roomCode.trim() ? 'secondary-tone' : ''}"
      disabled={submitStatus === 'loading' || !isBackendReady || hasPlayerReady}
      on:click={submitReady}
    >
      {submitStatus === 'loading' ? 'INITIALISATION...' : (!isBackendReady ? 'BACKEND REQUIS' : (hasPlayerReady ? 'PRÊT DÉJÀ VALIDÉ' : ctaSessionLabel))}
    </button>
  </article>

  <article class="stats-row">
    <div>
      <span>SERVEURS</span>
      <strong>EU-WEST-01</strong>
    </div>
    <div>
      <span>ACTIVITÉ</span>
      <strong>2,401 DUELS</strong>
    </div>
    {#if isPrototype}
      <div>
        <span>MODE</span>
        <strong>PROTO BACKEND</strong>
      </div>
    {/if}
  </article>
</section>

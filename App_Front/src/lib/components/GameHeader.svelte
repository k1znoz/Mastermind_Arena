<script>
  export let attemptsCount = 0
  export let maxAttempts = 10
  export let timerText = '00:00'
</script>

<header class="top-bar">
  <p class="brand-tag">terminal</p>
  <h1>MASTERMIND ARENA</h1>
  <div class="session-stats">
    <p>ATTEMPT {attemptsCount + 1}/{maxAttempts}</p>
    <p>{timerText}</p>
  </div>
</header>

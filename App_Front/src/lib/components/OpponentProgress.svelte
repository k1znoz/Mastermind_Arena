<script>
  export let name = 'Adversaire'
  export let done = false
  $: statusText = done ? 'a terminé cette étape' : 'est encore sur cette étape'
</script>

<span class="opponent-progress" class:done title={name + ' ' + statusText} aria-label={name + ' ' + statusText}>
  <span class="progress-icon" aria-hidden="true">{done ? '✓' : '·'}</span>
  <span class="progress-name">{name}</span>
  <span class="progress-state">{done ? 'FAIT' : 'EN COURS'}</span>
</span>
<style>
  .opponent-progress { display:inline-flex; align-items:center; gap:5px; min-width:0; max-width:100%; color:var(--muted); font:9px "JetBrains Mono",monospace; letter-spacing:.04em; white-space:nowrap; }
  .progress-icon { display:grid; place-items:center; flex:none; width:14px; height:14px; border:1px solid var(--secondary); border-radius:50%; color:var(--secondary); font-size:14px; line-height:1; }
  .progress-name { overflow:hidden; text-overflow:ellipsis; }
  .progress-state { flex:none; font-size:8px; }
  .done { color:var(--ok); }
  .done .progress-icon { border-color:var(--ok); color:var(--ok); font-size:10px; }
  @media (max-width:420px) { .progress-state { display:none; } }
</style>

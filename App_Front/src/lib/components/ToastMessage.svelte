<script>
  /** @type {{ kind: 'success' | 'error', text: string } | null} */
  export let toast = null
</script>

{#if toast}
  <div class="toast {toast.kind}" role="status" aria-live="polite">{toast.text}</div>
{/if}

<style>
.toast {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 98px;
  z-index: 50;
  min-width: 240px;
  width: max-content;
  max-width: calc(100vw - 32px);
  text-align: center;
  border: 1px solid var(--line);
  padding: 12px 14px;
  backdrop-filter: blur(10px);
  background: var(--panel-strong);
  color: var(--text);
  border-color: var(--primary);
  font-family: "JetBrains Mono", monospace;
  font-size: 14px;
}

.toast.success {
  color: var(--ok);
  border-color: color-mix(in srgb, var(--ok), transparent 55%);
}

.toast.error {
  color: var(--error);
  border-color: color-mix(in srgb, var(--error), transparent 55%);
}


</style>

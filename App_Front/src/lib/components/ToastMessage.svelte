<script>
  /** @type {{ kind: 'success' | 'error', text: string } | null} */
  export let toast = null
</script>

{#if toast}
  <div class="toast {toast.kind}" role="status" aria-live="polite">{toast.text}</div>
{/if}

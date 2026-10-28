<script>
  import { createEventDispatcher } from 'svelte'
  import Icon from './Icon.svelte'
  import close from '../assets/tabler/x.svg'
  /** @type {{ kind: 'success' | 'error', text: string } | null} */
  export let toast = null
  export let inline = false
  const dispatch = createEventDispatcher()
</script>

{#if toast}
  <div class="toast {toast.kind}" class:inline role={toast.kind === 'error' ? 'alert' : 'status'}><span>{toast.text}</span>{#if toast.kind === 'error'}<button type="button" aria-label="Fermer le message d’erreur" on:click={() => dispatch('close')}><Icon src={close} size={18} /></button>{/if}</div>
{/if}

<style>
.toast {
  display:flex;
  align-items:center;
  gap:12px;
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
.toast button { flex-shrink:0; display:grid; place-items:center; width:44px; height:44px; padding:0; border:0; border-radius:8px; background:transparent; color:inherit; cursor:pointer; }
.toast.inline { position:relative; inset:auto; transform:none; width:100%; min-width:0; max-width:none; margin:0 0 20px; text-align:left; border-radius:12px; font-family:inherit; }
.toast.inline span { flex:1; min-width:0; overflow-wrap:anywhere; }

.toast.success {
  color: var(--ok);
  border-color: color-mix(in srgb, var(--ok), transparent 55%);
}

.toast.error {
  color: var(--error);
  border-color: color-mix(in srgb, var(--error), transparent 55%);
}


</style>

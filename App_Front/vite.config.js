import { defineConfig, loadEnv } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import { fileURLToPath } from 'node:url'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    build: {
      rolldownOptions: {
        input: {
          game: fileURLToPath(new URL('./index.html', import.meta.url)),
          mentions: fileURLToPath(new URL('./mentions-legales.html', import.meta.url)),
          privacy: fileURLToPath(new URL('./confidentialite.html', import.meta.url))
        }
      }
    },
    plugins: [svelte()],
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    }
  }
})

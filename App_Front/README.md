# Mastermind Arena Front

Svelte/Vite client for the local two-player Plateau match.

## Local setup

1. Start the backend with `docker compose up --build` from the repository root. Docker exposes it at `http://localhost:8081`.
2. Copy `.env.example` to `.env.local` in this folder. Set `VITE_API_KEY` to the same value as `APP_API_KEY` in the root `.env`.
3. Run `npm ci` and `npm run dev` in this folder. Open the URL printed by Vite; it may use port 5174 if 5173 is busy.

With `VITE_API_BASE_URL` empty, Vite forwards `/api/*` to `VITE_PROXY_TARGET`. The front uses `/api/local/match-state`, `/api/local/submit-action`, and `/api/health`.

For two local players, leave the room code empty in the first tab (`p1`), then enter `local-match` as the room code in a second tab (`p2`). Both tabs must use the same match ID. This identity selection is only a local test mechanism.

Run `npm run build` to check the production bundle.

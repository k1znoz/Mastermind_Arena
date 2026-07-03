# Mastermind Arena Front

Svelte front-end for Mastermind Arena.

## Run

1. Install dependencies: `npm install`
2. Start dev server: `npm run dev`
3. Build production bundle: `npm run build`

## Backend integration

The front calls the deduction engine submit-action endpoint through the Vite proxy:

- front URL: `/api/local/submit-action`
- proxied target: `http://localhost:8081/local/submit-action`

Expected backend headers and auth are configured through env vars.

## Front runtime env vars

Create `.env.local` in this folder when needed:

```
VITE_SUBMIT_ACTION_PATH=/api/local/submit-action
VITE_MATCH_STATE_PATH=/api/local/match-state
VITE_API_BASE_URL=
VITE_API_KEY_HEADER=X-API-Key
VITE_API_KEY=dev-submit-action-key
VITE_REQUEST_ID_HEADER=X-Request-Id
VITE_MATCH_ID=local-match
VITE_ACTOR_ID=p1
```

If `VITE_API_BASE_URL` is empty, requests use the current origin and go through the Vite proxy.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Svelte](https://marketplace.visualstudio.com/items?itemName=svelte.svelte-vscode).

## Need an official Svelte framework?

Check out [SvelteKit](https://github.com/sveltejs/kit#readme), which is also powered by Vite. Deploy anywhere with its serverless-first approach and adapt to various platforms, with out of the box support for TypeScript, SCSS, and Less, and easily-added support for mdsvex, GraphQL, PostCSS, Tailwind CSS, and more.

## Technical considerations

**Why use this over SvelteKit?**

- It brings its own routing solution which might not be preferable for some users.
- It is first and foremost a framework that just happens to use Vite under the hood, not a Vite app.

This template contains as little as possible to get started with Vite + Svelte, while taking into account the developer experience with regards to HMR and intellisense. It demonstrates capabilities on par with the other `create-vite` templates and is a good starting point for beginners dipping their toes into a Vite + Svelte project.

Should you later need the extended capabilities and extensibility provided by SvelteKit, the template has been structured similarly to SvelteKit so that it is easy to migrate.

**Why include `.vscode/extensions.json`?**

Other templates indirectly recommend extensions via the README, but this file allows VS Code to prompt the user to install the recommended extension upon opening the project.

**Why enable `checkJs` in the JS template?**

It is likely that most cases of changing variable types in runtime are likely to be accidental, rather than deliberate. This provides advanced typechecking out of the box. Should you like to take advantage of the dynamically-typed nature of JavaScript, it is trivial to change the configuration.

**Why is HMR not preserving my local component state?**

HMR state preservation comes with a number of gotchas! It has been disabled by default in both `svelte-hmr` and `@sveltejs/vite-plugin-svelte` due to its often surprising behavior. You can read the details [here](https://github.com/sveltejs/svelte-hmr/tree/master/packages/svelte-hmr#preservation-of-local-state).

If you have state that's important to retain within a component, consider creating an external store which would not be replaced by HMR.

```js
// store.js
// An extremely simple external store
import { writable } from 'svelte/store'
export default writable(0)
```

# Front Mastermind Arena

Client Svelte / Vite du jeu simultané. Voir le [README principal](../README.md) pour les variables d'environnement et le démarrage du backend.

```powershell
npm ci
npm run dev
npm test
npm run lint
npm run check
npm run build
```

Avec `VITE_API_BASE_URL` vide, le proxy Vite transmet `/api/*` au backend défini par `VITE_PROXY_TARGET`. En production, configurer l'URL HTTPS du backend séparé.

Le lobby permet de créer et rejoindre une partie. Chaque joueur possède un jeton privé ; le code privé de reprise permet de retrouver sa place sur un autre appareil, sans choisir librement `p1` ou `p2`. Le code d'accès facultatif à une partie ne remplace pas ce jeton.

Téléphone prioritaire : action courante, palette de six couleurs nommées, choix entre les deux plateaux, corrections secondaires, thème clair/sombre. Les indices sont donnés par les joueurs, jamais calculés dans le navigateur.

## Pages d’information

Vite construit le jeu (`index.html`), `mentions-legales.html` et `confidentialite.html` via `build.rolldownOptions.input`. Les deux documents utilisent `src/information.js` et `PublicInformation.svelte`, sans démarrer le moteur d’écran, la session ou les requêtes du jeu. Le pied de page ouvre ces documents en nouvel onglet pour préserver les brouillons.

Les trois familles de polices sont servies depuis `public/fonts`, avec leurs licences. [Validation et informations encore manquantes avant publication](../docs/INFORMATION_PAGES.md).

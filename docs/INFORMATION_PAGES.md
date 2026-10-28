# Pages d’information — preuve locale du 28 octobre 2026

Deux documents autonomes : `/mentions-legales.html` et `/confidentialite.html`. Vite construit trois entrées HTML, dont le jeu. `information.js` monte `PublicInformation` et réutilise la barre, les thèmes et les polices ; il ne charge ni App, ni session, ni client API. Aucun paquet ajouté.

Les liens du pied de page ouvrent un nouvel onglet avec `rel="noopener"` et une indication accessible. Le jeu reste monté dans son onglet. Les textes reprennent les faits confirmés, sans contact fictif ni promesse de conformité.

## Vérifications

- 85 tests front réussis, aucun ignoré ; lint réussi, Svelte Check : zéro erreur et avertissement. Build final : 181 modules, trois documents HTML.
- Distribution HTTP locale : trois entrées et licences accessibles en 200 ; ressources construites présentes.
- Rendu serveur des deux documents : titres, identité, conservation et licences présents. Pied de page : deux liens distincts, nouvel onglet et libellés accessibles. Contrôle temporaire dans target, hors livraison.
- Chrome sur pages locales : mentions sombres et confidentialité claire examinées à 479 px ; absence de débordement horizontal. Dans une iframe de contrôle de 320 puis 900 px : largeur utile 312 puis 892 px, scrollWidth identique. Le cas 900 px est une preuve de géométrie, pas une capture visuelle complète de bureau.
- Activation clavier du lien Mentions depuis l’accueil : nouvel onglet correct, pseudo synthétique « Alice test » conservé dans l’onglet du jeu. Front de test configuré vers une API loopback ; aucune partie réelle créée.

Captures : [mentions sombres](screenshots/information-20261028/mentions-sombre-479.png), [confidentialité claire](screenshots/information-20261028/confidentialite-clair-479.png), [mentions dans le cadre de 320 px](screenshots/information-20261028/mentions-320.png). Les captures montrent la portion visible, pas l’intégralité de chaque document.

## Limites et suite

Code local non publié. Contact volontairement reporté par le concepteur. Coordonnées complètes d’hébergement, fondements, droits, journaux et transferts à finaliser selon [la préparation L3](PUBLIC_INFORMATION_PREPARATION.md). L3 reste ouvert ; ces pages partielles ne constituent pas une attestation juridique. La recette publique L4 acquise ne couvre pas ce nouveau contenu.

Le premier réglage Vite `input` à la racine n’a produit qu’une entrée avec la version installée. Correction : `build.rolldownOptions.input` ; les trois sorties ont ensuite été vérifiées. Le premier contrôle SSR utilisait deux instances du runtime Svelte : harness corrigé pour charger aussi `svelte/server` via Vite, sans assouplir les assertions ni modifier le produit.

## Complément de rédaction juridique

Le 28 octobre, le concepteur maintient deux pages et son nom, avec finalisation des coordonnées avant publication. [Analyse sourcée](LEGAL_REVIEW_2026-10-28.md). Confidentialité inclut désormais droits et délai de réponse, recours CNIL, données nécessaires/facultatives, traces techniques et transferts éventuels. Lint/check/build et assertions SSR repassés avec succès ; même structure de 181 modules et trois HTML. Les 85 tests acquis précédemment ne sont pas présentés comme relancés pour ce texte. Contrôle Chrome de la notice claire : largeur 471 px sans débordement ; [capture du recours et du délai](screenshots/information-20261028/confidentialite-droits-479.png). L3 reste ouvert sur les informations et qualifications manquantes ; aucune troisième page ou nouvelle acceptation.

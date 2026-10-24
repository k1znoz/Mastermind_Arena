# Contrôle des messages — 24 octobre 2026

Application Svelte actuelle servie par Vite sur 127.0.0.1:5176, API synthétique dédiée sur 127.0.0.1:8083. Aucune connexion à Supabase ou donnée réelle. Chrome avec dimensions explicites : téléphone 390 × 844, ordinateur 1280 × 900 ; deux thèmes activés par les commandes de l'application. Taille restaurée et onglet fermé après contrôle.

## Synchronisation : 32 contrôles

Chaque scénario passe par le vrai client HTTP et le traducteur de l'application. Texte attendu visible, alerte entièrement dans la fenêtre, absence de débordement horizontal et quatre cases bleues du brouillon conservées contrôlés dans le DOM. Captures enregistrées pour chaque cas ; revue visuelle représentative des deux tailles et deux thèmes. Résultats détaillés dans [error-visual-checks.json](error-visual-checks.json).

| Réponse injectée | Téléphone clair | Téléphone sombre | Ordinateur clair | Ordinateur sombre |
|---|---|---|---|---|
| 403 ROOM_ACCESS_DENIED | [capture](errors-access-mobile-light.jpg) | [capture](errors-access-mobile-dark.jpg) | [capture](errors-access-desktop-light.jpg) | [capture](errors-access-desktop-dark.jpg) |
| 200 vide | [capture](errors-empty-mobile-light.jpg) | [capture](errors-empty-mobile-dark.jpg) | [capture](errors-empty-desktop-light.jpg) | [capture](errors-empty-desktop-dark.jpg) |
| 200 JSON illisible | [capture](errors-unreadable-mobile-light.jpg) | [capture](errors-unreadable-mobile-dark.jpg) | [capture](errors-unreadable-desktop-light.jpg) | [capture](errors-unreadable-desktop-dark.jpg) |
| 200 contrat incompatible | [capture](errors-contract-mobile-light.jpg) | [capture](errors-contract-mobile-dark.jpg) | [capture](errors-contract-desktop-light.jpg) | [capture](errors-contract-desktop-dark.jpg) |
| 500 SERVER_ERROR | [capture](errors-server-mobile-light.jpg) | [capture](errors-server-mobile-dark.jpg) | [capture](errors-server-desktop-light.jpg) | [capture](errors-server-desktop-dark.jpg) |
| 418 code inconnu | [capture](errors-unknown-mobile-light.jpg) | [capture](errors-unknown-mobile-dark.jpg) | [capture](errors-unknown-desktop-light.jpg) | [capture](errors-unknown-desktop-dark.jpg) |
| Connexion interrompue | [capture](errors-network-mobile-light.jpg) | [capture](errors-network-mobile-dark.jpg) | [capture](errors-network-desktop-light.jpg) | [capture](errors-network-desktop-dark.jpg) |
| 504 REQUEST_TIMEOUT synthétique | [capture](errors-timeout-mobile-light.jpg) | [capture](errors-timeout-mobile-dark.jpg) | [capture](errors-timeout-desktop-light.jpg) | [capture](errors-timeout-desktop-dark.jpg) |

Le cas REQUEST_TIMEOUT vérifie uniquement la présentation : le code est injecté par la réponse de test. Une tentative avec une réponse réellement retardée n'a pas permis de capturer ce message avec les attentes du navigateur ; elle n'est pas comptée comme validation du délai réel. Les tests de transport existants restent la preuve distincte du timeout de quinze secondes. Le cas réseau interrompt réellement la socket de l'API synthétique.

## Soumission : huit captures complémentaires

| Message injecté | Téléphone clair | Téléphone sombre | Ordinateur clair | Ordinateur sombre |
|---|---|---|---|---|
| FEEDBACK_NOT_EDITABLE, message long | [capture](errors-submit-mobile-light.jpg) | [capture](errors-submit-mobile-dark.jpg) | [capture](errors-submit-desktop-light.jpg) | [capture](errors-submit-desktop-dark.jpg) |
| Code inconnu, acceptation non confirmée | [capture](errors-uncertain-mobile-light.jpg) | [capture](errors-uncertain-mobile-dark.jpg) | [capture](errors-uncertain-desktop-light.jpg) | [capture](errors-uncertain-desktop-dark.jpg) |

Le refus d'indices est volontairement injecté dans le formulaire du secret pour contrôler son rendu dans la notification commune. Ce scénario ne prétend pas prouver une transition métier possible : les conditions métier et quinze refus réels sont vérifiés séparément par HTTP/JDBC. Les codes de reprise restent fermés et ne figurent dans aucune capture.

## Limites et suite UX

Ces contrôles couvrent les surfaces communes d'erreur et un échantillon de textes de soumission ; ils ne prouvent pas chaque texte dans chaque phase du jeu, une compatibilité avec un téléphone physique, le lecteur d'écran ou le contraste WCAG. La notification de soumission disparaît après 2,4 secondes dans App.svelte ; cette durée paraît trop courte pour certains messages longs. Examiner une présentation persistante ou une durée adaptée pendant la refonte UX, sans changer ce comportement dans ce lot de validation. La validation de production et le parcours final restent ouverts.

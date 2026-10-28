# Revue des erreurs et messages — 24 octobre 2026

## Complément du 28 octobre — admissions limitées par HTTP 429

Comportement approuvé puis implémenté : sur une réponse 429 lisible, roomClient lève ROOM_RATE_LIMITED avant de consulter le code du corps. Texte : « Trop de tentatives rapprochées. Patientez un moment avant de réessayer. » Fonctionne pour création, jointure et reprise, même si le pare-feu renvoie du HTML, un corps vide ou un JSON contenant un autre code. Aucun corps serveur affiché, délai chiffré inventé ou retry automatique.

Les refus reviennent dans le circuit d'erreur d'admission existant, sans nouvelle place ni réinitialisation du formulaire par ce correctif. Les composants et App gardent leurs brouillons existants. Une erreur réseau/TypeError, notamment si CORS empêche de lire la réponse, conserve sa formulation prudente : aucun diagnostic de quota déduit de cet échec.

Preuve : tests de transport pour les trois admissions et trois corps de réponse ; intégration du vrai client avec roomConnection pour message, source d'erreur, fin d'attente, absence d'admission et une seule requête. Échecs reproduits avant correctif, suite finale 85 tests réussis, 0 ignoré ; lint/check/build réussis, check 0 erreur/avertissement, 176 modules, index-eBB4oJeu.js et index-BRWWkcpR.css. Pas de nouveau contrôle visuel navigateur ni de test de dépassement du quota Vercel : ces preuves restent distinctes. Aucun changement Java/SQL ou du contrat serveur.

## Complément du 27 octobre — partie indisponible

Texte approuvé puis intégré pour ROOM_NOT_FOUND et MATCH_NOT_FOUND : « Cette partie est introuvable ou n’est plus disponible. Les parties sont supprimées après 7 jours sans action de jeu. » Cette règle générale ne prouve pas que la partie demandée a expiré ; aucune trace supplémentaire de purge n'est conservée. Les autres refus, notamment ROOM_RESUME_INVALID, gardent leur distinction.

Test des deux codes dans les contextes salon/synchronisation/soumission, y compris préfixes HTTP/ENGINE : texte exact, code/statut non mutés. Suite front à 83 tests, lint/check/build réussis. Contrôle Chrome en clair dans un cadre de 390 pixels, réponse ROOM_NOT_FOUND/404 synthétique : texte lisible, alerte sans débordement interne (clientWidth=scrollWidth=291). [Capture locale](screenshots/ux-message-20261027/partie-introuvable-mobile.png). Ce contrôle vérifie le rendu du traducteur, pas une suppression Supabase ni toutes les surfaces.

## Périmètre et constat

Revue des routes exécutées, de SubmitActionOrchestrator, des règles de plateau, de JdbcRoomDirectory, du mapper HTTP et de apiErrorText. Quinze refus métier identifiés disposent désormais des formulations approuvées ci-dessous ; les messages existants d'accès, réseau, timeout, contrat, conflit et saisie sont préservés.

Ce message reste adapté à une réponse perdue ou à une erreur inconnue : le navigateur ne peut pas conclure que l'action a échoué. Il est moins précis quand une réponse métier valide confirme que la nouvelle action a été refusée. Distinguer ces cas ne demande ni changement des règles, ni modification du contrat HTTP, ni nouvelle dépendance.

## Messages approuvés et intégrés

Le concepteur a validé la distinction entre refus explicite et résultat incertain. Les quinze formulations ci-dessous sont intégrées dans apiErrorText. Les statuts restent ceux du mapper existant ; aucune règle ou stratégie de retry modifiée.

| Code public actuel | HTTP | Message affiché |
|---|---:|---|
| ROOM_WAITING_FOR_PLAYER | 409 | En attente du second joueur. |
| MATCH_ALREADY_TERMINAL | 409 | Cette manche est terminée. Actualisez le plateau. |
| INVALID_STATE_FOR_READY_SECRET | 400 | Cette action n’est pas disponible dans la phase actuelle. Actualisez le plateau. |
| INVALID_STATE_FOR_PLAY_GUESS | 400 | Cette action n’est pas disponible dans la phase actuelle. Actualisez le plateau. |
| INVALID_STATE_FOR_SEND_FEEDBACK | 400 | Cette action n’est pas disponible dans la phase actuelle. Actualisez le plateau. |
| SECRET_ALREADY_SET | 400 | Votre combinaison secrète est déjà enregistrée. |
| PLAYER_ALREADY_SOLVED | 400 | Vous avez déjà trouvé le code. Continuez à donner les indices à votre adversaire. |
| GUESS_ALREADY_SUBMITTED | 409 | Votre proposition est déjà enregistrée pour ce tour. |
| FEEDBACK_NOT_REQUIRED | 400 | Aucun indice n’est attendu de votre part pour ce tour. |
| FEEDBACK_ALREADY_SUBMITTED | 409 | Vos indices sont déjà enregistrés pour ce tour. |
| FEEDBACK_ALREADY_CONTESTED | 400 | Vous avez déjà demandé une vérification de ces indices. |
| FEEDBACK_NOT_EDITABLE | 400 | Seuls les indices du dernier tour terminé peuvent être corrigés ou contestés. Actualisez le plateau. |
| ABANDON_REQUIRES_STARTED_GAME | 400 | L’abandon sera disponible après la préparation des deux secrets. |
| REMATCH_REQUIRES_FINISHED_MATCH | 400 | La manche doit être terminée pour demander une revanche. |
| REMATCH_ALREADY_REQUESTED | 400 | Votre demande de revanche est déjà enregistrée. |

Correction de la revue initiale : DECLARE_DISCOVERY passait la liste de SubmitActionCommand puis était refusée par le switch en DISCOVERY_REQUIRES_MANUAL_FEEDBACK ; elle n'était donc pas rejetée à la construction. Après accord du concepteur le 24 octobre, cette ancienne commande et son refus dédié sont retirés du chemin exécuté : commande inconnue, 400/INVALID_REQUEST après les gardes d'accès/admission. Les découvertes restent déterminées par les indices manuels. [Inventaire des états, actions, événements et codes](WORKFLOW_CONTRACT.md).

### Correction et contestation d'anciens indices

FEEDBACK_NOT_EDITABLE est émis par editableFeedback lorsque la cible ne désigne pas des indices du dernier tour terminé. Le concepteur a approuvé sa traduction explicite, désormais intégrée. Le parcours réel vérifie correction et contestation d'indices de la manche précédente après une revanche : HTTP 400, message dédié et plateau courant inchangé. Aucune règle modifiée.

## Classification des erreurs techniques — corrigée

La revue a reproduit une divergence avec API_CONTRACT : SubmitActionApplicationService capturait tous les IllegalStateException, y compris les pannes JDBC, puis publiait leur message comme rejet ENGINE. Une soumission valide dont la lecture SQL échouait recevait HTTP 400 au lieu de 500. Les tests HTTP et en mémoire ont échoué avant le correctif, puis réussi après.

Le concepteur a choisi une exception métier dédiée. `ActionRejectedException` porte les codes des refus attendus du workflow et des règles ; seul ce type est traduit en rejet applicatif. Les autres erreurs pendant l'exécution sont des incidents internes. La validation de commande reste à l'entrée, donc une requête invalide conserve 400/INVALID_REQUEST. Le lecteur JDBC enveloppe aussi les documents stockés invalides comme panne interne : ils ne deviennent pas des erreurs de saisie dans GET ou POST.

Les quinze refus métier gardent leurs codes/statuts et messages. Les incidents renvoient seulement `{code:SERVER_ERROR}` ou `{code:ROOM_SERVER_ERROR}` selon la route, sans publier le diagnostic de stockage. Les causes techniques restent disponibles dans la chaîne interne d'exception.

| Cas de protocole | HTTP/code | Présentation actuelle |
|---|---|---|
| Clé API absente/invalide | 401 / UNAUTHORIZED | Accès serveur refusé, rechargement proposé |
| Place non autorisée | 403 / ROOM_ACCESS_DENIED | Reprise privée proposée, sans modifier la session |
| Origine CORS refusée | 403 / CORS_ORIGIN_NOT_ALLOWED côté serveur | Le navigateur peut ne recevoir qu'un échec réseau ; pas de diagnostic CORS affirmé à partir d'un TypeError |
| Méthode incorrecte | 405 / METHOD_NOT_ALLOWED | Fallback sûr selon le contexte |
| Corps/query/commande invalides | 400 / INVALID_REQUEST ou INVALID_MATCH_ID selon la route | Fallback sûr selon le contexte ; aucun détail arbitraire affiché |
| Partie absente, si l'autorisation permet d'atteindre ce contrôle | 404 / MATCH_NOT_FOUND ou ROOM_NOT_FOUND | Partie inexistante ou indisponible ; ne pas promettre de distinguer une purge |
| Incident du jeu | 500 / SERVER_ERROR | Problème serveur, actualisation proposée |
| Incident du lobby/reprise | 500 / ROOM_SERVER_ERROR | Ouverture impossible ; diagnostic technique non affiché |

Cette table décrit les codes existants ; le complément du 27 octobre précise la présentation d'une partie absente sans changer son contrat HTTP. Une réponse réseau perdue n'est pas un refus métier, et un statut inconnu ne doit pas provoquer un retry automatique.

## Preuves et vérifications restantes

- **Acquis localement :** 61 tests front et contrôles lint/Svelte/build pour les messages ; test paramétré du vrai client/validateur/traducteur, refus connus distincts du fallback, erreurs inconnues/secret/propriétés héritées masqués, métadonnées conservées et annulation silencieuse.
- **Acquis localement :** parcours HTTP/JDBC des quinze refus, correction, revanche et reprise ; vues du joueur inchangées après refus. Contrôle de volumes agrégés consigné dans [VALIDATION](VALIDATION.md), sans extrapoler à tous les horodatages.
- **Acquis localement après correction technique :** 52 tests backend dont 21 PostgreSQL/HTTP, zéro ignoré, qualité Maven réussie. Panne de lecture et document stocké invalide en GET/POST : 500 au corps exact sans détails. Panne d'écriture injectée à chacun des trois stades état/événement/reçu : rollback de l'état, de l'activité et des deux journaux, puis nouvelle action acceptée après retrait de la panne. Les tests des corps mal formés conservent 400.
- **Contrôle visuel ciblé :** SECRET_ALREADY_SET visible dans Chrome desktop sombre via API synthétique ; [capture](screenshots/refusal-secret-desktop-dark.jpg). Cette capture ne valide pas l'affichage de chaque message.
- **Complément navigateur du 24 octobre :** 32 contrôles des surfaces de synchronisation, téléphone 390 × 844 et ordinateur 1280 × 900, clair/sombre ; huit captures de soumission longue/inconnue. Textes visibles, alerte dans la fenêtre, absence de débordement horizontal et quatre cases du brouillon conservées vérifiés. [Matrice des 40 captures](screenshots/ERROR_CHECKS.md). API synthétique : le message de timeout est testé par injection du code, sans nouvelle preuve du délai réseau réel.
- **Inventaire courant réalisé :** quatre états, sept commandes, 21 refus métier ; admission/protocole, erreurs locales du front et noms historiques distingués dans [WORKFLOW_CONTRACT](WORKFLOW_CONTRACT.md). Revue statique complétée par les tests du retrait de commande, sans prétendre exécuter chaque branche en production.
- **Encore ouvert :** accessibilité complète, parcours final et contrôle de production après autorisation. La notification de soumission de 2,4 secondes est un point à revoir au lot UX ; ce lot de validation ne change pas son comportement.

# Contrat HTTP courant

Extension additive du 25 octobre : `turns[].originalFeedback` contient les compteurs JSON initialement enregistrés pour SEND_FEEDBACK, ou null pour les autres actions. `feedback` reste la valeur effective utilisée par le plateau après corrections. Les CORRECT_FEEDBACK successifs conservent leurs nouvelles valeurs dans `feedback`. Le champ initial est ajouté uniquement aux entrées ayant déjà franchi le filtre de confidentialité ; ni actions cachées ni secrets supplémentaires ne sont publiés. Aucun changement de stockage ou migration.

Le front accepte l'absence/null de ce champ pour un ancien backend. Lorsqu'il existe, ses compteurs sont validés et décodés sans fabriquer de valeur de remplacement. L'historique utilise la valeur initiale ; les plateaux gardent la valeur effective. Si un ancien backend ne fournit pas l'original après correction, l'historique signale explicitement les valeurs actuelles.

Version de travail du 14 octobre 2026. Source : handlers JDK, mappers et validateurs front. Toutes les requêtes métier utilisent `X-API-Key` (nom configurable). Cette clé est publique côté navigateur ; l'accès à une place exige en plus `X-Room-Token`.

Découpage du 15 octobre sans évolution de format : routes câblées par `LocalSubmitActionHttpServer`, handlers dans `GameHttpHandlers` et `RoomHttpHandlers`, outils de transport dans `HttpExchangeSupport`. Les tests PostgreSQL/HTTP caractérisent le masquage des actions en cours, le refus des corrections par un autre auteur, CORS/OPTIONS, clé d'API et identifiant de requête.

Les chemins ci-dessous appartiennent au backend. En développement, Vite préfixe `/api` puis retire ce préfixe dans le proxy. En production, le front appelle l'URL du backend directement. Ne jamais utiliser l'API automatique Supabase pour lire les parties.

Les règles d'évolution et l'ordre des publications séparées sont définis dans [DEPLOYMENT_COMPATIBILITY](DEPLOYMENT_COMPATIBILITY.md) : une extension additive de réponse n'autorise pas à changer une phase ou le type d'un champ requis. La compatibilité avec les versions en production doit être vérifiée avant promotion.

| Méthode / chemin | Entrée | Réponse réussie |
|---|---|---|
| GET `/local/rooms` | Pas de jeton de place | `{ "rooms": [...] }`, 200 ; parties en attente uniquement |
| GET `/local/rooms?roomId=...` | Identifiant | Métadonnées publiques d'une partie, 200 |
| POST `/local/rooms` | `{name,pseudo,accessCode}` ; code partagé facultatif | Admission du joueur p1, 201 |
| POST `/local/rooms/join` | `{roomId,pseudo,accessCode}` | Admission du joueur p2, 200 |
| POST `/local/rooms/resume` | `{recoveryCode}` | Admission de la place existante, 200 |
| GET `/local/match-state?matchId=...&actorId=...` | Jeton correspondant à cet acteur dans cette partie | Vue autorisée du plateau, 200 |
| POST `/local/submit-action` | Jeton de place et commande ci-dessous | `{accepted:true,matchId,version,status,emittedEvents,...}`, 200 |

Admission : `{room,actorId,token}`. `room` contient `roomId`, `name`, `hostPseudo`, `guestPseudo` (null avant jointure), `locked`, `createdAt` et `joined`. `token` et `recoveryCode` sont privés ; pas de journaux ni d'exemples contenant une valeur réelle. La reprise est envoyée en POST, jamais dans une URL.

Contrôles front au 18 octobre : `createdAt` est un entier positif ou nul en millisecondes ; `joined` correspond à la présence d'un `guestPseudo` non vide. Une liste ne contient que des parties en attente, sans doublon de `roomId`. Un statut doit concerner l'identifiant demandé. La création rend p1 ; la jointure rend p2 dans la partie demandée ; la reprise rend la même partie, le même acteur et le même jeton que le code privé fourni. Les champs supplémentaires restent permis pour des évolutions additives. Réponses incompatibles et listes vides valides sont distinguées.

Les fixtures partagées `contracts/room-admission.json` et `contracts/action-accepted.json`, dans les ressources de test Java, sont vérifiées par les deux langages. Elles utilisent un jeton fictif explicite. La validation du succès de soumission vérifie aussi identité, progression de version et statut ; elle ne remplace pas une lecture autorisée du plateau.

## Commandes

```json
{
  "matchId": "example-room",
  "actorId": "p1",
  "expectedVersion": 2,
  "idempotencyKey": "example-operation-unique-id",
  "actionPayload": {
    "actionType": "PLAY_GUESS",
    "payload": "[\"A\",\"B\",\"E\",\"F\"]",
    "feedback": null
  }
}
```

Le format courant utilise des chaînes JSON pour `payload` et `feedback`, à l'intérieur de l'enveloppe JSON. Ne pas changer silencieusement ce contrat : une évolution doit modifier mapper, client, fixtures et tests ensemble.

Validation HTTP renforcée le 18 octobre : `matchId`, `actorId`, `idempotencyKey` et `actionPayload.actionType` sont des chaînes non blanches obligatoires ; `expectedVersion` est obligatoire, entier JSON positif ou nul et représentable sur un long Java. Pas de conversion de chaîne, de booléen ou de décimal, ni de valeur zéro implicite si la version manque. `actionPayload` est un objet ; ses champs `payload` et `feedback` sont des chaînes, null ou absents. La validité de l'action, de la combinaison et des compteurs reste contrôlée par le métier.

Pour création/jointure/reprise, les champs texte présents et non null doivent être des chaînes JSON : aucun nombre ou booléen n'est converti en pseudo, nom ou code. Les traitements existants des champs absents/null, des longueurs et des codes métier du lobby sont conservés. Les clés dupliquées et les contenus supplémentaires après l'objet JSON sont refusés en 400/INVALID_REQUEST sur les quatre routes POST.

La même exigence de document JSON unique sans clé dupliquée s'applique aux chaînes de combinaison, d'indices et de cible de correction/contestation. Les codes métier `INVALID_CODE`, `INVALID_FEEDBACK` et `INVALID_FEEDBACK_TARGET` sont conservés. Les espaces autour d'un document valide sont autorisés.

Sur les lectures de parties et d'état, un paramètre de requête répété est refusé en 400/INVALID_REQUEST, même si les valeurs sont identiques ou le nom encodé différemment (`actorId` et `%61ctorId`). Les noms et valeurs sont décodés une fois avant contrôle ; le serveur ne choisit pas la dernière valeur. Les comportements existants pour liste sans roomId, identifiants absents et refus d'accès sont conservés.

| Action | payload | feedback |
|---|---|---|
| READY_SECRET / PLAY_GUESS | Chaîne JSON, tableau de quatre valeurs A–F | null |
| SEND_FEEDBACK | null | Chaîne JSON `{bienPlaces,malPlaces}` |
| CORRECT_FEEDBACK | Chaîne JSON `{feedbackVersion}` | Chaîne JSON `{bienPlaces,malPlaces}` |
| CONTEST_FEEDBACK | Chaîne JSON `{feedbackVersion}` | null |
| ABANDON / REQUEST_REMATCH | null | null |

Les compteurs sont des entiers positifs ou nuls et leur somme ne dépasse pas quatre. Les règles de phase et d'auteur sont validées côté serveur. Le backend ne calcule pas les indices.

Précision du 24 octobre : seules les sept commandes du tableau sont admises. L'ancienne DECLARE_DISCOVERY est retirée après accord du concepteur ; à accès autorisé et adversaire présent, elle reçoit 400 avec le corps `{ "code": "INVALID_REQUEST" }`, sans modification de l'état, de l'activité, des événements ou des reçus. Son ancien rejet ENGINE/DISCOVERY_REQUIRES_MANUAL_FEEDBACK n'est plus produit. Les états, transitions, événements effectifs et 21 refus métier sont recensés dans [WORKFLOW_CONTRACT](WORKFLOW_CONTRACT.md).

`expectedVersion` est la version complète du match, pas le numéro de tour. Une clé idempotente n'est réutilisée que pour la même requête exacte. Après perte de réponse, le front rejoue cette requête avant d'envisager une nouvelle intention. Après conflit confirmé, il actualise puis ne retente que si manche, tour et phase sont encore les mêmes, avec une nouvelle clé.

Le reçu de soumission conserve la version, le status et les événements de l'acceptation initiale. Un replay exact restitue ces valeurs même si la partie a avancé, a été corrigée ou qu'une revanche a démarré ; il ne rejoue pas l'action, ne publie pas de nouvel événement et ne prolonge pas la conservation. La lecture d'état reste indépendante et renvoie le plateau courant. Le stockage compact livré les 23–24 octobre change le contrat interne et le format persistant, sans ajouter ni retirer de champ HTTP. Après purge de la partie, son reçu n'est plus disponible et ses anciens jetons ne donnent plus accès au jeu.

## Vue de partie

- `state` : PREPARATION, WAITING_GUESS, WAITING_FEEDBACK ou FINISHED.
- `version` : entier croissant après chaque action acceptée ; comparaison dans le même match.
- `gameNumber` : manche commençant à 1. `roundNumber` : index métier commençant à 0. `turns[].turnNumber` : numéro présenté commençant à 1.
- `players` : p1 et p2 ; `readyPlayers`, `submittedPlayers`, `requiredPlayers`, `solvedPlayers`, `rematchRequestedPlayers` : sous-ensembles de ces acteurs.
- `scores` : points par acteur ; absence d'une clé signifie zéro. `winnerId` : null, p1, p2 ou DRAW.
- `visibleSecretCode` : secret propre, tableau vide avant confirmation. `opponentSecretCode` : vide sauf révélation prévue après abandon.
- `turns` : actions visibles avec acteur, type, version, timestamp en millisecondes, numéro de tour, `guess` et `feedback` (chaînes JSON ou null). READY_SECRET n'est jamais publié dans cette liste. Les corrections modifient la vue effective des indices et sont également historisées comme notifications.

Les actions adverses en cours restent masquées avant publication de la phase, même si une notification est ajoutée ensuite. Le front rejette une réponse mal formée au lieu d'afficher un plateau vide plausible. Les réponses d'une autre partie, place ou génération de session ne modifient pas l'état courant.

Au 18 octobre, les contenus JSON de `turns[].guess` et `feedback` sont aussi contrôlés selon l'action avant conversion pour l'affichage. Une combinaison invalide, des compteurs absents, négatifs, décimaux ou de somme supérieure à quatre sont refusés ; aucun arrondi ou remplacement par zéro. Les notifications de correction/contestation portent un `feedbackVersion` entier positif inférieur à leur propre version. READY_SECRET est interdit dans `turns` : les secrets passent exclusivement par les champs de vue dédiés.

## Erreurs et limites

`GET /health` reste public, de type liveness : `{status,requestsOk,requestsKo,averageLatencyMs}`. Les compteurs couvrent les réponses des handlers enregistrés, dont lobby, refus de place et OPTIONS ; la requête de santé elle-même est incluse. Ils sont locaux à chaque processus et réinitialisés au redémarrage. Ils ne représentent ni un total multi-instance, ni une disponibilité de la base ; voir les limites de durée et de couverture dans [ARCHITECTURE_V2](ARCHITECTURE_V2.md).

Erreurs du transport : `{code}`. Rejets de soumission : `{accepted:false,rejectionOrigin,rejectionCode,version,status,...}` lorsque le contexte existe.

Les rejets de soumission sont vérifiés avant d'attacher des métadonnées à une erreur : origine ENGINE/RULESET et code stable, état absent (matchId/version/status tous null) ou associé au match demandé. Une réponse incohérente produit `HTTP:INVALID_ERROR_RESPONSE` ; son contenu n'est pas affiché ni utilisé comme preuve de conflit. Le retry de conflit requiert une réponse ENGINE/VERSION_CONFLICT et HTTP 409. Les succès incompatibles conservent `HTTP:INVALID_RESPONSE_BODY` et le traitement existant de réponse perdue par replay exact.

Au 23 octobre, le transport front distingue un succès HTTP vide (y compris espaces seuls) d'un succès au JSON illisible via la métadonnée locale `responseBodyState` (`empty`/`unreadable`). Le message d'erreur technique reste `HTTP:INVALID_RESPONSE_BODY` pour conserver le replay exact ; la présentation traduit la métadonnée sans afficher le corps reçu. Un JSON valide `null` ne compte pas comme un corps vide et reste soumis au contrat d'endpoint. Les erreurs HTTP non réussies conservent les codes et fallbacks précédents.

401 : clé d'API absente/invalide. 403 : origine refusée, mauvais code partagé/reprise ou place non autorisée. 404 : partie absente. 409 : place occupée, attente d'adversaire, conflit de version/idempotence ou action déjà appliquée. 400 : requête invalide ou rejet moteur selon code. 422 : enveloppe de rejet RULESET conservée par le mapper, sans émetteur dans le workflow Mastermind courant. 500 : incident interne, sans détails sensibles dans la réponse.

Les corps POST doivent être des objets JSON : les racines null/tableau/booléen, corps vide et JSON mal formé produisent 400/INVALID_REQUEST. Une exception interne inattendue produit 500/SERVER_ERROR sur les routes de jeu ou 500/ROOM_SERVER_ERROR sur les routes de parties. Une panne SQL n'est pas présentée comme une faute de saisie ; la réponse ne contient ni requête SQL, ni message d'exception. Les échanges sont fermés après réponse. Les exceptions d'écriture réseau restent des erreurs I/O et ne déclenchent pas une deuxième écriture HTTP.

Correction vérifiée le 24 octobre : seuls les ActionRejectedException du métier sont traduits en rejets ENGINE. Les pannes techniques en soumission, auparavant absorbées par une capture générique de IllegalStateException, suivent désormais le 500 prévu ci-dessus. Les documents persistants invalides ne sont pas assimilés à une erreur de corps entrant. Tests réels : lecture indisponible, document invalide GET/POST et rollback à chaque stade d'écriture état/événement/reçu ; codes et statuts des quinze refus métier préservés.

Corps entrants limités à 65 536 octets. Le délai du client est de 15 secondes, couvrant réception du corps. Une annulation de contexte est distincte d'un timeout. La règle WAF Vercel choisie pour les admissions est décrite dans VERCEL_RATE_LIMIT_SETUP ; sa preuve de dépassement reste ouverte. CORS et la clé publique ne remplacent pas cette protection.

Les tests front de contrat utilisent des données fictives. Les tests HTTP PostgreSQL vérifient admissions, reprise, accès refusés et erreurs réelles. L'exécution CI distante et les URL publiées doivent encore être contrôlées séparément.

### Refus 429 des admissions

Le client reconnaît un statut HTTP 429 lisible pour création, jointure et reprise, même avec un corps HTML, vide ou JSON sans code métier. Il affiche « Trop de tentatives rapprochées. Patientez un moment avant de réessayer. », conserve les saisies et ne relance pas automatiquement l’admission. Il s’agit d’une traduction du refus de bordure, sans nouveau contrat JSON Java. Si CORS masque la réponse, le message réseau prudent reste utilisé. Aucun quota n’est implémenté dans le handler Java par ce changement.

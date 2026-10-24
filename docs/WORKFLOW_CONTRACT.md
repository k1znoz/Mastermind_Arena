# États, actions, événements et refus du jeu exécuté

Inventaire du 24 octobre 2026, limité au chemin HTTP/JDBC de Mastermind. Sources : SubmitActionCommand, SubmitActionOrchestrator, MastermindBoardRules, MatchRuntimeState, mappers/handlers HTTP, JdbcRoomDirectory et validateurs front. Les modules génériques historiques ne constituent pas une fonctionnalité de plateforme livrée.

## États et transitions

| État public | Actions attendues et progression |
|---|---|
| PREPARATION | Chaque joueur enregistre READY_SECRET une seule fois. Première acceptation : même état ; seconde : WAITING_GUESS. |
| WAITING_GUESS | Chaque joueur encore en recherche envoie PLAY_GUESS. Quand tous les chercheurs ont proposé : WAITING_FEEDBACK. Un joueur ayant trouvé ne propose plus. |
| WAITING_FEEDBACK | Les adversaires des chercheurs donnent SEND_FEEDBACK manuellement. Quand tous les indices requis sont reçus : tour suivant et WAITING_GUESS, ou FINISHED si plus personne ne cherche. |
| FINISHED | Recherche terminée par découvertes ou abandon. Deux REQUEST_REMATCH conduisent à PREPARATION, nouvelle manche et secrets effacés ; scores conservés. |

CORRECT_FEEDBACK peut recalculer le résultat, retirer/transférer un point, rouvrir une recherche et annuler les accords de revanche. CONTEST_FEEDBACK informe sans bloquer. Ces deux actions ciblent les indices du dernier tour terminé, dans la manche courante ; la correction appartient à l'auteur et la contestation au receveur. ABANDON est possible après préparation, avant FINISHED ; il révèle les codes selon les règles et conserve un résultat déjà attribué.

Le résultat winnerId est distinct de FINISHED : un joueur peut avoir son point pendant que l'autre cherche encore. DRAW concerne une découverte au même tour ; aucune limite de tours ou de durée ne crée un résultat. L'état, la version du match, la manche et le numéro de tour sont distincts : voir [API_CONTRACT](API_CONTRACT.md).

## Sept actions admises

| Commande | Charge métier | Particularité |
|---|---|---|
| READY_SECRET | Tableau JSON de quatre valeurs A–F dans payload | Secret privé ; action exclue de turns HTTP. |
| PLAY_GUESS | Même format dans payload | Publication après les propositions requises. |
| SEND_FEEDBACK | Compteurs bienPlaces/malPlaces dans feedback | Entiers non négatifs, somme au plus quatre ; aucun calcul automatique. |
| CORRECT_FEEDBACK | Cible feedbackVersion dans payload, compteurs dans feedback | Applique des indices effectifs et conserve une notification de correction. |
| CONTEST_FEEDBACK | Cible feedbackVersion dans payload | Demande informative, doublon refusé pour la même cible et le même receveur. |
| ABANDON | Aucune charge nécessaire | Fin de recherche, aucun effacement des données de la partie. |
| REQUEST_REMATCH | Aucune charge nécessaire | Premier accord enregistré ; second redémarre la manche avec une nouvelle version et un nouveau journal de manche. |

DECLARE_DISCOVERY a été retirée après accord du concepteur : entrée inconnue, HTTP 400/INVALID_REQUEST après les contrôles d'accès/admission. Le front ne l'émettait pas. Avant retrait, elle passait bien SubmitActionCommand puis était rejetée par le switch : l'ancienne affirmation selon laquelle elle était déjà rejetée à la construction était incorrecte. La découverte reste issue exclusivement des indices manuels effectifs. Aucun format persistant ni schéma n'est modifié par ce retrait.

## Événements réellement émis

Une nouvelle action acceptée émet un événement, sauvegardé atomiquement avec état et reçu. Un refus n'émet rien ; un replay de reçu rend les anciens noms sans les publier à nouveau. Ces noms sont des catégories historiques, pas une description suffisante de la transition.

| Nom | Émission actuelle |
|---|---|
| GAME_STARTED | Chaque READY_SECRET accepté ; chaque accord REQUEST_REMATCH lorsque l'état résultant n'est pas terminal. Ne signifie donc pas exclusivement « les deux joueurs sont prêts ». |
| GUESS_PLAYED | PLAY_GUESS accepté. |
| FEEDBACK_SENT | SEND_FEEDBACK, CORRECT_FEEDBACK ou CONTEST_FEEDBACK acceptés, sauf remplacement ci-dessous. |
| GAME_FINISHED | Toute action acceptée dont l'état résultant est FINISHED : fin par indices/abandon, mais aussi premier accord de revanche ou contestation/correction maintenant cet état. |

L'orchestrateur remplace l'événement choisi par GAME_FINISHED si l'état final est terminal. PLAYER_JOINED et TURN_CHANGED restent dans GameEvent et dans le mapping WebSocket historique, mais ne sont pas publiés par ce workflow ; JdbcRoomDirectory ne publie pas d'événement de jointure. Le front utilise le polling et le plateau, pas ces noms pour déduire seul les règles. Aucun renommage ou retrait d'événement n'est décidé par cet inventaire.

## Refus métier émis par le workflow

| HTTP | Codes publics |
|---|---|
| 404 | MATCH_NOT_FOUND |
| 403 | ACTOR_NOT_AUTHORIZED |
| 409 | VERSION_CONFLICT, IDEMPOTENCY_CONFLICT, MATCH_ALREADY_TERMINAL, GUESS_ALREADY_SUBMITTED, FEEDBACK_ALREADY_SUBMITTED |
| 400, phase | INVALID_STATE_FOR_READY_SECRET, INVALID_STATE_FOR_PLAY_GUESS, INVALID_STATE_FOR_SEND_FEEDBACK |
| 400, saisie | INVALID_CODE, INVALID_FEEDBACK, INVALID_FEEDBACK_TARGET |
| 400, action non disponible | SECRET_ALREADY_SET, PLAYER_ALREADY_SOLVED, FEEDBACK_NOT_REQUIRED, FEEDBACK_NOT_EDITABLE, FEEDBACK_ALREADY_CONTESTED, ABANDON_REQUIRES_STARTED_GAME, REMATCH_REQUIRES_FINISHED_MATCH, REMATCH_ALREADY_REQUESTED |

Soit 21 codes métier distincts. Ils passent par ActionRejectedException puis l'enveloppe accepted:false/rejectionOrigin:ENGINE ; les métadonnées peuvent être nulles pour ces refus. Les gardes HTTP de jeton et d'attente peuvent refuser avant ce workflow. Une panne JDBC ou un document stocké invalide est une erreur serveur, sans publication de sa cause. Le garde interne UNSUPPORTED_ADMITTED_ACTION signalerait une incohérence de programmation entre les sept commandes admises et le switch ; il n'est pas un code de refus joueur.

## Admission et protocole

| HTTP | Codes servis par les routes actives |
|---|---|
| 400 | INVALID_REQUEST ; INVALID_MATCH_ID au point de contrôle de lecture ; ROOM_NAME_REQUIRED, PSEUDO_REQUIRED, FIELD_TOO_LONG pour l'admission |
| 401 | UNAUTHORIZED |
| 403 | CORS_ORIGIN_NOT_ALLOWED, ROOM_ACCESS_DENIED ; ROOM_CODE_INVALID et ROOM_RESUME_INVALID pour l'admission |
| 404 | ROOM_NOT_FOUND, MATCH_NOT_FOUND |
| 405 | METHOD_NOT_ALLOWED |
| 409 | ROOM_FULL ; ROOM_WAITING_FOR_PLAYER avant soumission |
| 500 | SERVER_ERROR pour le jeu, ROOM_SERVER_ERROR pour le lobby/reprise |

L'ordre des gardes importe : un jeton absent/invalide ou une partie en attente peut être refusé avant le contrôle de commande ; ne pas promettre INVALID_REQUEST à un appel non autorisé. Les diagnostics internes ROOM_CREATE_FAILED, ROOM_JOIN_FAILED, ROOM_LIST_FAILED, ROOM_LOOKUP_FAILED et ROOM_AUTH_FAILED sont masqués en ROOM_SERVER_ERROR, comme les autres incidents techniques de cette route.

## Erreurs locales du front et historique

- REQUEST_TIMEOUT et REQUEST_ABORTED sont des erreurs de transport locales ; l'annulation est silencieuse. Un TypeError peut indiquer une erreur réseau, sans preuve suffisante pour conclure à CORS.
- INVALID_ROOM_RESPONSE, INVALID_ADMISSION_RESPONSE, INVALID_MATCH_RESPONSE, WRONG_MATCH_RESPONSE, INVALID_RESPONSE_BODY et INVALID_ERROR_RESPONSE sont des contrôles locaux du contrat. Leur préfixe HTTP décrit le transport du client, pas un code forcément reçu du serveur.
- API_KEY_HEADER_MISSING, API_KEY_MISSING, API_KEY_WEAK_DEFAULT, INVALID_MATCH_ID, INVALID_ACTOR_ID, INVALID_EXPECTED_VERSION et INVALID_ACTION_PAYLOAD avec préfixe CLIENT décrivent un défaut de configuration ou d'entrée du client. HTTP_ERROR et ROOM_HTTP_ERROR sont des fallbacks lorsque le corps d'une réponse HTTP échouée ne donne pas de code exploitable.
- Le mapper et les validateurs conservent l'enveloppe RULESET/422, mais le SubmitActionOrchestrator actuel ne produit aucune rejection RULESET. MATCH_NOT_IN_PROGRESS et TURN_NOT_ACTIVE sont encore mappés en 409 sans émetteur dans le workflow courant. Les autres codes de l'ancien validateur de directives restent hors du chemin HTTP/JDBC de ce jeu. Aucun de ces noms ne doit être présenté comme une nouvelle règle ou retiré sans revue de ses références.

Traductions et matrice de rendu : [ERROR_MESSAGES](ERROR_MESSAGES.md). Preuves d'exécution, test du retrait de commande et limites distantes : [VALIDATION](VALIDATION.md). Cet inventaire est une revue statique des chemins actuels ; il ne prouve pas que chaque branche a été exécutée en production.

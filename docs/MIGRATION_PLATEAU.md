# Migration vers le jeu de plateau en ligne

## Objectif

Faire évoluer Mastermind Arena vers un MVP qui reproduit une partie de Mastermind sur plateau physique entre deux joueurs en ligne.

Le MVP doit permettre à chaque joueur de définir une combinaison secrète, de formuler des propositions sur son propre plateau et de fournir manuellement les pions d'indice à l'adversaire. Le système orchestre la partie, protège les informations privées et synchronise le plateau, sans interpréter les règles de déduction.

La référence fonctionnelle est [GAME_RULES.md](GAME_RULES.md).

## Fonctionnalités conservées

| Fonctionnalité | Conservation dans le MVP plateau |
| --- | --- |
| Création et identification d'une partie | Conservée pour réunir les deux participants. |
| Gestion de l'état d'une partie | Conservée pour piloter la préparation, le tour de proposition, le tour d'indices et la fin de manche. |
| Gestion des tours | Conservée et simplifiée pour imposer l'alternance entre joueur actif et donneur d'indices. |
| Soumission d'actions | Conservée comme mécanisme de transport des intentions de jeu : confirmer une combinaison secrète, soumettre une proposition, valider des indices, annoncer une découverte. |
| Persistance et lecture de l'état | Conservées pour restaurer le plateau et afficher la partie aux deux joueurs. |
| Contrôle de concurrence et idempotence | Conservés afin d'éviter les doubles soumissions et les conflits entre clients. |
| Notifications et synchronisation temps réel | Conservées pour diffuser les changements de plateau et de tour. |
| Confidentialité des données | Conservée : une combinaison secrète reste privée jusqu'à la fin de la manche. |

## Fonctionnalités mises en sommeil

Ces fonctionnalités ne sont pas nécessaires au MVP plateau. Elles restent compatibles avec une évolution ultérieure, mais ne doivent pas orienter les décisions d'implémentation de la V1.

| Fonctionnalité | Motif de mise en sommeil | Condition de réactivation |
| --- | --- | --- |
| Moteur générique multi-jeux | Le MVP ne cible qu'un seul jeu et un seul flux de plateau. | Ajout concret d'un second jeu partageant réellement le contrat moteur. |
| RuleSet métier extensible | La V1 ne demande aucune décision automatique spécifique à Mastermind. | Introduction de règles automatisées ou de variantes de jeu. |
| `EvaluationResult` opaque | Les indices sont déclarés par les joueurs, non calculés ni interprétés. | Retour d'une évaluation automatique par une règle de jeu. |
| `MatchOutcome` détaillé et résultats par participant | La fin de manche est une annonce, sans gagnant calculé par le système. | Mise en place d'un classement ou de règles de victoire vérifiables. |
| Gestion avancée des abandons, forfaits et reconnexions | Hors du parcours minimal de jeu à deux. | Définition d'une politique compétitive ou de sessions longues. |
| Historique fonctionnel enrichi | Le plateau courant est prioritaire pour le MVP. | Besoin produit de rejouer, analyser ou partager des manches. |

## Fonctionnalités supprimées du MVP

Les éléments suivants sont volontairement exclus du MVP. Ils ne doivent pas être exposés dans le parcours utilisateur ni appelés par le serveur comme automatisme implicite.

| Fonctionnalité retirée | Décision MVP |
| --- | --- |
| Calcul des pions noirs et blancs par le serveur | Supprimé : le donneur d'indices les place manuellement. |
| Comparaison automatique d'une proposition avec la combinaison secrète | Supprimée : le serveur ne lit pas la sémantique de la proposition. |
| Validation de la cohérence des indices | Supprimée : les indices relèvent de la responsabilité du donneur d'indices. |
| Détection automatique de combinaison trouvée | Supprimée : un joueur annonce lui-même sa découverte. |
| Attribution automatique d'un gagnant, d'un score ou d'un rang | Supprimée : aucune décision de résultat n'est calculée en V1. |
| IA adversaire ou assistant de déduction | Supprimée du MVP ; réservée à la V2. |
| Classement, statistiques et modes compétitifs | Supprimés du MVP ; réservés à la V2. |
| Variantes Mastermind et paramétrage compétitif | Supprimés du MVP afin de stabiliser un seul déroulé de jeu. |

## Ordre des futurs commits

Les commits doivent rester petits, testables et ordonnés par dépendance. Le contenu exact peut évoluer, mais cet ordre préserve un chemin de migration livrable.

| Ordre | Commit proposé | Résultat attendu |
| --- | --- | --- |
| 1 | `docs: align game rules and plateau migration` | Les règles V1 et le périmètre de migration deviennent la référence partagée. |
| 2 | `feat: model plateau match lifecycle` | Les états préparation, attente de proposition, attente d'indices et terminée sont représentés. |
| 3 | `feat: support private secret setup for two players` | Chaque joueur peut confirmer une combinaison secrète, invisible à son adversaire. |
| 4 | `feat: persist manual guesses and feedback pegs` | Les propositions et les pions noirs/blancs saisis manuellement sont persistés et verrouillables. |
| 5 | `feat: alternate plateau player roles` | Le passage proposition, indices, puis inversion des rôles est appliqué par le serveur. |
| 6 | `feat: reveal secrets when discovery is declared` | L'annonce de découverte termine la manche et révèle les deux combinaisons. |
| 7 | `feat: render synchronized two-player board` | Le client restitue le plateau, le tour actif et les informations autorisées pour chaque joueur. |
| 8 | `test: cover plateau lifecycle and privacy rules` | Les transitions, verrouillages, droits d'action et confidentialité sont couverts. |
| 9 | `chore: remove automated mastermind evaluation from mvp paths` | Les parcours du MVP ne déclenchent plus aucun calcul automatique d'indices ou de victoire. |

## Risques techniques

| Risque | Impact | Mesure de maîtrise |
| --- | --- | --- |
| Fuite d'une combinaison secrète dans une réponse API, un événement temps réel ou un journal | Rupture immédiate de la partie et perte de confiance. | Utiliser des projections par destinataire ; tester explicitement qu'un adversaire ne reçoit jamais le secret avant la révélation. |
| Soumissions concurrentes depuis deux clients ou doubles clics | Double ligne, inversion de tour erronée ou état divergent. | Conserver `expectedVersion`, les clés d'idempotence et des transitions d'état transactionnelles. |
| Transition de tour avant validation des indices | Le joueur suivant agit sur une information incomplète. | N'autoriser l'alternance qu'après verrouillage explicite des indices. |
| Réutilisation d'une logique d'évaluation automatique existante | Le comportement réel diverge des règles V1 et le serveur arbitre à tort. | Isoler ou retirer cette logique des flux MVP ; ajouter des tests négatifs attestant l'absence de calcul d'indices et de victoire. |
| Modèle de données trop dépendant des anciens résultats calculés | Complexité inutile et migration difficile du plateau. | Modéliser les propositions, pions manuels et déclarations de fin comme faits de plateau distincts. |
| Synchronisation temps réel incomplète | Les deux joueurs n'observent pas le même plateau ou le même tour. | Diffuser chaque transition validée et prévoir un rechargement de l'état courant après reconnexion. |
| Évolution V2 anticipée dans le MVP | Surconception, règles ambiguës et ralentissement de la livraison. | Limiter les contrats V1 aux actions et états documentés ; versionner séparément tout futur automatisme. |

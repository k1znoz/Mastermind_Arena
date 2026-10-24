# Partie longue et polling — référence locale du 23 octobre 2026

Mesure séquentielle sur Windows, Java/JDBC et PostgreSQL 16 jetable sur 127.0.0.1:54330, API sur :8082, schéma `measure_validation`. Rôle propriétaire utilisé, migrations au démarrage ; ce n'est pas une mesure du rôle restreint, de Supabase ou de plusieurs parties concurrentes. Aucun profil existant modifié.

## Reproduction

Préparer une API dédiée et une base jetable selon [LOCAL_CONTRACT_CHECK](LOCAL_CONTRACT_CHECK.md), puis :

```powershell
$env:ARENA_INTEGRATION_HTTP_URL = 'http://127.0.0.1:8082'
$env:ARENA_INTEGRATION_API_KEY = '<clé locale de test>'
node scripts/measure-local-game.mjs
```

Le script crée deux places et effectue 100 tours sans découverte, avec des indices manuels nuls. À 0, 10, 30 et 100 tours, il mesure 40 lectures (20 par acteur), incluant réception du corps, parsing et validation front. Il vérifie version, numéro de tour, intégralité des actions visibles, phase et secret adverse masqué. Les requêtes de soumission utilisent le vrai client front ; aucune donnée de jeu ou code privé n'est imprimé.

## Résultats obtenus

| Tours | Actions visibles | Lecture médiane | Lecture p95 | Corps JSON maximal | Charge estimée de deux joueurs par minute |
|---|---:|---:|---:|---:|---:|
| 0 | 0 | 29,86 ms | 35,80 ms | 408 octets | 8 160 octets |
| 10 | 40 | 28,25 ms | 30,96 ms | 6 606 octets | 132 120 octets |
| 30 | 120 | 26,15 ms | 29,17 ms | 19 110 octets | 382 200 octets |
| 100 | 400 | 28,39 ms | 34,47 ms | 63 075 octets | 1 261 500 octets |

402 actions acceptées (deux secrets puis 400 actions de tour) : soumission médiane 48,51 ms, p95 55,81 ms. Aucun seuil de performance n'est imposé au jeu par ce scénario ; 100 tours est un échantillon de test, pas une limite produit.

La dernière colonne estime 20 lectures par minute pour deux joueurs à six secondes, sur la taille brute maximale du corps. Le vrai timer attend six secondes après la fin de la lecture : la cadence réelle est donc légèrement inférieure. Ce n'est pas un relevé de bande passante : headers, TLS, compression, appels après action et éventuelles erreurs ne sont pas inclus. Pas de nouvelle simulation de concurrence ou de coupure réseau.

## Stockage après 100 tours

Lecture SQL de volumes agrégés uniquement :

| Élément | Valeur |
|---|---:|
| JSON courant cumulé dans match_state (partie et seed) | 64 044 octets |
| JSON cumulé dans idempotency.result_payload | 13 003 156 octets |
| Entrées idempotentes | 402 |
| Événements | 402 |
| Taille totale physique idempotency (index/TOAST inclus) | 1 687 552 octets |
| Taille totale physique match_state (index/TOAST inclus) | 966 656 octets |

Les octets JSON non compressés ne sont pas l'occupation disque. Les tailles physiques dépendent de TOAST, des versions de lignes et du vacuum ; ces valeurs ponctuelles ne prédisent pas un stockage stabilisé.

Lors de la mesure initiale, `JdbcIdempotencyStore.save` encodait le `SubmitActionResult` complet via `BoardPersistenceCodec`, avec son état historique. La somme des instantanés explique une croissance quadratique du JSON cumulé lorsque les actions s'accumulent. Le coût a motivé une représentation persistante compacte, validée ci-dessous. La mesure initiale ne changeait ni le format ni le comportement.

Les lectures locales ne montrent pas de dégradation nette sur cet échantillon. Pas de cache, WebSocket ou troncature d'historique ajouté sur cette seule base. La purge ne remplace pas une correction de cette duplication : une partie activement jouée peut durer et doit garder son historique.

## Optimisation validée le 24 octobre — reçus compacts

Choix approuvé par le concepteur après comparaison des deux approches. `ActionReceipt` conserve matchId, version, status et emittedEvents ; `IdempotencyStore.Entry` contient le fingerprint et ce reçu, sans historique ni commande complète. Le moteur retourne l'état courant réel et le reçu original dans deux champs distincts. Le mapper de soumission utilise exclusivement le reçu : la réponse HTTP reste identique lors d'un replay, même si le plateau a avancé ou qu'une revanche a commencé. La lecture du plateau reste séparée.

`BoardPersistenceCodec` conserve le format 1 pour l'état et utilise un document format 2 pour le reçu. V7 exige une installation vide pour cette transition initiale, sans conversion ou effacement implicite, et contraint le format des reçus en SQL. Les anciens formats de résultats ne sont plus lus. Les adaptateurs mémoire, fichier et préférences sont également adaptés ; fichier/préférences restent hors du serveur HTTP JDBC. Les empreintes exactes de commandes continuent de contenir les valeurs nécessaires au contrôle d'idempotence : la table reste privée, même si le reçu lui-même ne contient ni secret ni proposition.

### Nouvelle mesure, même scénario de 100 tours

API et PostgreSQL 16 jetables, schéma dédié compact_measure_validation, 402 actions et 40 lectures par palier. Même script et mêmes contrôles de règles/confidentialité que la référence. Aucun profil .env ni donnée de développement utilisé.

| Mesure | Avant | Après |
|---|---:|---:|
| JSON brut cumulé des résultats, 402 entrées | 13 003 156 octets | 61 794 octets |
| JSON de l'état courant, partie et seed | 64 044 octets | 64 044 octets |
| Taille physique idempotency, index/TOAST inclus | 1 687 552 octets | 278 528 octets |
| Taille physique match_state | 966 656 octets | 999 424 octets |

Réduction du JSON des résultats d'environ **99,52 %**, sans troncature de l'historique. Le volume brut des reçus ne recopie plus un historique croissant. Les tailles physiques sont deux observations ponctuelles ; les nouvelles associations/index de V6, le vacuum et les versions de lignes empêchent d'attribuer leur différence au seul codec.

| Tours | Lecture médiane | Lecture p95 | Corps JSON maximal |
|---|---:|---:|---:|
| 0 | 31,40 ms | 36,08 ms | 408 octets |
| 10 | 28,75 ms | 33,72 ms | 6 606 octets |
| 30 | 26,98 ms | 33,56 ms | 19 110 octets |
| 100 | 26,85 ms | 29,98 ms | 63 075 octets |

Soumissions : médiane 47,37 ms, p95 55,47 ms. Les corps HTTP gardent exactement les tailles de référence ; les latences proches ne démontrent ni accélération significative ni capacité concurrente.

### Preuves et limites

Tests HTTP/PostgreSQL : réponse initiale identique après actions ultérieures, victoire, correction et revanche ; fingerprint différent refusé ; aucun changement d'état, d'événements ou d'activité lors du replay ; lecture indépendante du plateau courant. Tests existants de rollback, concurrence, purge et rôle restreint rejoués. Tests mémoire et fichier rouvert : reçu ancien, état courant réel et même réponse HTTP ; codec compact validé sans état/commande, formats invalides refusés.

Après redémarrage sur une autre base obtenue par pg_dump/pg_restore : reprise privée, état/score, replay exact et revanche réussis. Le script vérifie désormais aussi le replay de l'ancien abandon après cette revanche, sans modifier la nouvelle manche.

Pas de cache, WebSocket ou limite de tours ajoutés. Le reset initial reste différé ; cette transition ne prévoit aucune reprise des anciens jeux de test et ne donne pas d'autorisation de reset des futures parties. Mesures distantes, charge concurrente et validation de production restent ouvertes. Aucune opération Supabase ni publication.

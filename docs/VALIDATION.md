# Plan de validation de la finalisation

## Résultats locaux du 13 octobre 2026

- Backend : 26 tests JUnit réussis, dont cinq tests PostgreSQL/HTTP réels ; `mvn -Pquality clean verify` réussi, contrôles PMD et SpotBugs inclus.
- Frontend : `npm run lint`, `npm run check` et `npm run build` réussis ; Svelte Check ne signale aucune erreur ni aucun avertissement.
- Navigateur, deux clients : secrets, essais simultanés, indices masqués, victoire avec poursuite, correction de quatre à trois noirs retirant le point et rouvrant le duel, notification, contestation, abandon avec révélation et revanche conservant le score vérifiés.
- Reprise du joueur 2 depuis une autre origine locale avec son code privé vérifiée. Thèmes clair/sombre inspectés à 390 × 844 ; rendu ordinateur inspecté. Captures dans `screenshots/`.
- Ce parcours utilise `UiValidationServer`, un adaptateur mémoire réservé aux tests, avec le moteur et le mapper réels. Il ne prouve pas les transactions JDBC, la persistance après redémarrage ou le fonctionnement des déploiements Vercel.
- PostgreSQL 16 jetable : migrations répétées, reprise avec vérification des jetons, réouverture des composants, idempotence, rollback de l'état et des événements après échec du journal, concurrence entre deux instances et entre deux invités validés.
- Serveur HTTP réel avec PostgreSQL : admission, reprise, refus du mauvais jeton et de l'usurpation de place, JSON invalide et corps trop volumineux validés sans mutation.
- Le parcours visuel complet reste à reproduire sur PostgreSQL puis en production. Accessibilité complète, navigation clavier et coupures réseau restent à vérifier.

Les tests `PostgresWorkflowTest` nécessitent `ARENA_TEST_DB_URL`, `ARENA_TEST_DB_USER` et `ARENA_TEST_DB_PASSWORD`. Utiliser exclusivement une base jetable : chaque test crée un schéma isolé conservé pour inspection jusqu'à la suppression de cette base. Sans ces variables, les cinq tests sont explicitement ignorés. Le workflow CI provisionne son propre PostgreSQL, mais son exécution distante n'a pas encore été vérifiée.

## Métier

- Deux joueurs confirment leur secret, leurs propositions et leurs indices dans des ordres différents.
- Les propositions et indices adverses restent cachés avant publication de la phase ; les secrets restent privés.
- 30 tours sans découverte : la manche continue, les deux secrets et les 122 actions sont conservés.
- Première découverte : un point, recherche du perdant poursuivie seul, aucun point supplémentaire.
- Découverte au même tour : Égalité sans point.
- Correction du dernier tour terminé : bon auteur, compteurs valides, résultat et score réconciliés, ancienne action conservée.
- Correction d'un tour plus ancien ou en cours : rejet sans mutation.
- Correction après une victoire/Égalité : recherche Éventuellement rouverte et consentements de revanche annulés.
- Abandon : résultat cohérent, révélation, puis revanche avec deux consentements et scores conservés.
- Payload invalide, acteur inconnu, version obsolète et clé idempotente conflictuelle : rejet.

## HTTP / PostgreSQL (À exécuter sur une base isolée)

- Migrations de schéma puis création/jointure de room atomiques.
- Même jeton depuis un autre appareil : reprise ; faux jeton et place adverse : refus.
- Deux jointures concurrentes : un seul deuxième joueur admis.
- Deux instances soumettant la même version : un seul effet, l'autre doit se resynchroniser.
- Échec d'écriture d'Événement ou d'idempotence : rollback de l'État.
- JSON invalide ou trop volumineux : erreur structurée, aucune mutation.
- Redémarrage backend : secrets, historique, scores et code de reprise conservés.

## Interface / parcours

Deux clients distincts, téléphone 390×844 et ordinateur : préparation, propositions, indices, victoire, poursuite, correction/contestation, abandon, reprise privée et revanche. Vérifier thème clair/sombre, navigation clavier, couleurs nommées, absence de débordement et erreurs réseau. Les tests avec un adaptateur mémoire ne valident pas JDBC.

## Production

Après publication compatible des deux projets Vercel : répéter la manche à deux clients, tester une reconnexion et vérifier Supabase actif. Ne pas conclure à une validation de production depuis une compilation locale ou un écran de lobby fonctionnel.

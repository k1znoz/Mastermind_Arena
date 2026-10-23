# Base propre et protection des données

Décision du concepteur, 14 octobre 2026 : les anciennes parties peuvent être abandonnées ; aucune reprise de leur historique n'est exigée. Cette autorisation concerne la transition initiale, pas les parties futures.

## Installation locale livrée

Le profil racine `.env.local` sélectionne maintenant `APP_DB_SCHEMA=arena_v1`. Les valeurs précédentes restent présentes au-dessus, sans modification des fichiers Supabase d'origine. Le lanceur charge la dernière valeur. Le schéma neuf est créé dans PostgreSQL local ; l'ancien schéma public est conservé mais n'est plus utilisé par ce backend.

Le stockage JDBC utilise `BoardPersistenceCodec`, format JSON explicite `formatVersion:1`. L'état de partie et le résultat idempotent sont des documents versionnés ; l'ancien codec à séparateurs/Base64 n'est plus utilisé par JDBC. Les anciens adaptateurs fichier/préférences restent présents mais ne participent pas au serveur déployé et pourront être retirés après revue de leurs références.

Les migrations enregistrent leur version et ne rejouent pas les transformations déjà appliquées. Le passage au format JSON exige un schéma vide : si des parties existent avant la migration 5, le démarrage refuse avec `EMPTY_SCHEMA_REQUIRED_FOR_NEW_STORAGE`. Il n'efface pas silencieusement les données.

## Préparation de Supabase

Rien n'a été appliqué à Supabase par ce chantier. Avant publication :

1. Identifier le rôle JDBC, les propriétaires des tables et les droits de création/migration. Le chemin validé utilise un rôle propriétaire ; un rôle restreint nécessitera des grants explicites.
2. Confirmer la désactivation de la Data API si elle n'est utilisée par aucun autre composant. Son statut actuel n'a pas été vérifié depuis les captures.
3. Choisir un schéma neuf `arena_v1` et régler `APP_DB_SCHEMA` du backend sur ce schéma. Ne pas l'ajouter aux schémas exposés par la Data API.
4. Appliquer les migrations avec le rôle adéquat, vérifier les cinq versions, RLS et les permissions des cinq tables ; tester admission, secret, correction, reprise et idempotence.
5. Publier les versions front/back compatibles, puis effectuer une manche réelle. Les anciens codes de reprise ne fonctionnent pas dans cette nouvelle base ; les oublier côté navigateur.
6. Retirer les anciennes tables seulement après la bascule validée. Aucune suppression des tables Supabase n'a été exécutée localement.

## Garanties et limites

RLS activée sur rooms, match_state, idempotency, workflow_event et schema_version. Droits révoqués à PUBLIC et, lorsqu'ils existent, anon/authenticated. Aucune politique permettant l'accès des navigateurs n'est créée. Le propriétaire JDBC conserve son accès ; les rôles privilégiés peuvent contourner RLS, leurs credentials restent exclusivement serveur.

Les droits par défaut sont corrigés pour les tables créées par le rôle qui exécute la migration. Ils ne couvrent pas automatiquement les futurs objets créés par un autre propriétaire. Une nouvelle table ou un nouveau rôle exige une revue explicite.

Les tests simulent les deux rôles Supabase sur un PostgreSQL jetable et vérifient lecture refusée, protection RLS même après un grant accidentel, ainsi que le jeu côté JDBC. Ils ne prouvent pas le paramétrage du projet Supabase réel.

Pour les futures évolutions, définir conservation, sauvegardes et restauration ; ne pas réutiliser l'autorisation de reset initial pour supprimer des parties actives. La séparation rôle de migration/rôle d'exécution, les purges et la restauration automatisée restent ouvertes dans la roadmap.

# Base propre et protection des données

## État courant — 27 octobre 2026

**Nettoyage terminé selon les résultats fournis :** seules les cinq tables arena_v1 restent dans le contrôle, aucune des cinq anciennes tables public. Le concepteur confirme ensuite le fonctionnement du jeu. Les instructions de préparation ci-dessous sont historiques ; ne pas relancer le script de suppression. Preuve dans [VALIDATION](VALIDATION.md).

Rapport reçu : seules dépendances internes attendues, aucune dépendance externe signalée. La procédure candidate arena_v1 utilise le nouveau schéma dans V8. [remove-legacy-public.sql](../scripts/remove-legacy-public.sql) est prêt : suppression définitive des cinq anciennes tables dans une transaction avec RESTRICT et garde arena_v1 V1–V8. Test isolé réussi, y compris refus et rollback en présence d'une vue externe. Exécuter le fichier entier sous postgres dans SQL Editor, puis fournir les cinq lignes arena_v1 retournées et vérifier la liste des salons dans l'application. Ne pas ajouter CASCADE en cas de refus ; transmettre l'erreur. Exécution distante encore à confirmer.

Installation arena_v1 et jeu après bascule des variables confirmés par le concepteur ; droits et premier cron réussi dans [VALIDATION](VALIDATION.md). Les étapes ci-dessous décrivent la préparation initiale.

Nettoyage autorisé : uniquement public.rooms, public.match_state, public.idempotency, public.workflow_event et public.schema_version, après revue des dépendances. Exécuter [audit-legacy-public.sql](../scripts/audit-legacy-public.sql) sous postgres dans SQL Editor et fournir le résultat. Le rapport inclut dépendances catalogue, séquences possédées et routines candidates ; il ne détecte pas entièrement les références dynamiques ou les clients externes. Ne pas utiliser DROP SCHEMA public ni CASCADE pour contourner une dépendance. Aucun effacement distant effectué ; arena_v1, auth et storage restent hors du nettoyage.

Décision du concepteur, 14 octobre 2026 : les anciennes parties peuvent être abandonnées ; aucune reprise de leur historique n'est exigée. Cette autorisation concerne la transition initiale, pas les parties futures.

## Installation locale livrée

Le profil racine `.env.local` sélectionne maintenant `APP_DB_SCHEMA=arena_v1`. Les valeurs précédentes restent présentes au-dessus, sans modification des fichiers Supabase d'origine. Le lanceur charge la dernière valeur. Le schéma neuf est créé dans PostgreSQL local ; l'ancien schéma public est conservé mais n'est plus utilisé par ce backend.

Le stockage JDBC utilise `BoardPersistenceCodec` : format JSON 1 pour l'état, format 2 pour les reçus idempotents compacts. V7 impose ce nouveau format sur une installation vide, sans importer les anciens instantanés de résultats. L'ancien codec à séparateurs/Base64 n'est plus utilisé par JDBC. Les adaptateurs fichier/préférences restent présents et ont été adaptés aux reçus, mais ne participent pas au serveur déployé et pourront être retirés après revue de leurs références.

Les migrations enregistrent leur version et ne rejouent pas les transformations déjà appliquées. Le passage au format JSON exige un schéma vide : si des parties existent avant la migration 5, le démarrage refuse avec `EMPTY_SCHEMA_REQUIRED_FOR_NEW_STORAGE`. Il n'efface pas silencieusement les données.

## Préparation de Supabase

### Installation depuis le SQL Editor

Si la valeur précédente de APP_DB_SCHEMA est masquée dans Vercel, il n'est pas nécessaire de la deviner : la cible dédiée prévue ici est explicitement `arena_v1`. Préparer le SQL depuis les migrations du dépôt :

```powershell
node scripts/prepare-supabase.mjs
```

Le fichier généré `target/supabase-arena_v1.sql`, ignoré par Git, contient V1–V8 et les révocations des rôles API lorsqu'ils existent. Le générateur ne se connecte à aucune base. Le SQL s'exécute avec le propriétaire dans le SQL Editor Supabase, dans une transaction ; il refuse un schéma arena_v1 déjà présent et ne touche pas à public, auth ou storage. Il n'active pas le cron et ne provisionne pas un rôle d'exécution distinct.

Après réussite, contrôler les huit versions et le propriétaire retournés, puis régler explicitement `APP_DB_SCHEMA=arena_v1` et `APP_DB_MIGRATE_ON_START=false` dans le backend Vercel. Le rôle JDBC du backend doit être le propriétaire ou un rôle provisionné selon [DATABASE_ROLES](DATABASE_ROLES.md). Redéployer le backend, vérifier le démarrage et l'admission avant le nettoyage des anciennes tables. Une connexion JDBC erronée n'est pas réparée par le seul choix du schéma.

Validation du 25 octobre après démarrage de Docker : fichier assemblé exécuté avec succès sur PostgreSQL 16 jetable, sans réseau ni port exposé. Versions 1–8, RLS des cinq tables, absence de droits des rôles API et interdiction d'exécution de la purge contrôlées ; tables témoins public/auth/storage intactes. Second lancement refusé sans modification ; erreur injectée avant V8 annulant toute l'installation. Aucune installation Supabase n'est attestée par ces tests isolés.

La candidate exige désormais V1–V8. V8 ajoute la procédure de purge commune sans effacer les parties au format V7. Le reset initial reste lié aux anciennes transitions V5–V7 ; V8 n'ajoute aucun reset. La procédure est réservée au propriétaire, sans EXECUTE pour PUBLIC ou le rôle API. L'installation de pg_cron et son activation quotidienne seront vérifiées séparément au déploiement.

Rien n'a été appliqué à Supabase par ce chantier. Avant publication :

1. Identifier le rôle JDBC, les propriétaires des tables et les droits de création/migration. Le chemin validé utilise un rôle propriétaire ; un rôle restreint nécessitera des grants explicites.
2. Confirmer la désactivation de la Data API si elle n'est utilisée par aucun autre composant. Son statut actuel n'a pas été vérifié depuis les captures.
3. Choisir un schéma neuf `arena_v1` et régler `APP_DB_SCHEMA` du backend sur ce schéma. Ne pas l'ajouter aux schémas exposés par la Data API.
4. Appliquer les migrations avec le rôle adéquat, vérifier les huit versions, RLS et les permissions des cinq tables ; tester admission, secret, correction, reprise et idempotence.
5. Publier les versions front/back compatibles, puis effectuer une manche réelle. Les anciens codes de reprise ne fonctionnent pas dans cette nouvelle base ; les oublier côté navigateur.
6. Retirer les anciennes tables seulement après la bascule validée. Aucune suppression des tables Supabase n'a été exécutée localement.

## Garanties et limites

RLS activée sur rooms, match_state, idempotency, workflow_event et schema_version. Droits révoqués à PUBLIC et, lorsqu'ils existent, anon/authenticated. Aucune politique permettant l'accès des navigateurs n'est créée. Le propriétaire JDBC conserve son accès ; les rôles privilégiés peuvent contourner RLS, leurs credentials restent exclusivement serveur.

Les droits par défaut sont corrigés pour les tables créées par le rôle qui exécute la migration. Ils ne couvrent pas automatiquement les futurs objets créés par un autre propriétaire. Une nouvelle table ou un nouveau rôle exige une revue explicite.

Les tests simulent les deux rôles Supabase sur un PostgreSQL jetable et vérifient lecture refusée, protection RLS même après un grant accidentel, ainsi que le jeu côté JDBC. Ils ne prouvent pas le paramétrage du projet Supabase réel.

Pour les futures évolutions, définir conservation, sauvegardes et restauration ; ne pas réutiliser l'autorisation de reset initial pour supprimer des parties actives. La séparation rôle de migration/rôle d'exécution, les purges et la restauration automatisée restent ouvertes dans la roadmap.

Mise à jour du 23 octobre : [séparation des rôles](DATABASE_ROLES.md) et [restauration logique isolée](DATABASE_RECOVERY.md) implémentées et testées localement. Leur configuration effective sur Supabase, les sauvegardes planifiées et le déclenchement périodique de la purge restent ouverts ; le profil existant ne change pas automatiquement de rôle.

# Séparer migrations et exécution JDBC

Implémentation locale du 23 octobre 2026. Aucun rôle ou réglage Supabase n'a été modifié. Le mode historique conserve les migrations au démarrage par défaut ; la séparation exige une configuration explicite avant publication.

## Deux responsabilités

Le propriétaire du schéma exécute `DatabaseMigrationMain` dans une étape administrative distincte. L'API utilise ensuite un rôle dédié, non propriétaire, sans superuser, BYPASSRLS, CREATEDB, CREATEROLE ni appartenance à d'autres rôles. Ce rôle doit être nouvellement créé avec des credentials privés ; le provisionnement ci-dessous ne révoque pas des droits préexistants sur une base entière.

| Objet | Droits du rôle de jeu |
|---|---|
| Schéma du jeu | USAGE |
| rooms, match_state | SELECT, INSERT, UPDATE |
| idempotency, workflow_event | SELECT, INSERT |
| schema_version | SELECT |
| workflow_event_event_id_seq | USAGE |

Aucun DELETE, TRUNCATE, droit de modification des tables/politiques ou d'écriture dans schema_version n'est ajouté. Les grants ne portent pas sur de futures tables. Les droits CONNECT/TEMP de la base, les grants PUBLIC et les autres objets doivent être audités séparément sur la cible ; ces droits ne sont pas réécrits globalement par l'outil.

RLS reste activée. Les politiques ajoutées concernent uniquement le rôle serveur nommé et vérifient `current_user` ; elles ne donnent aucun accès à PUBLIC, anon ou authenticated. Le serveur doit pouvoir accéder aux parties des deux joueurs : RLS isole ici le rôle serveur des rôles navigateur, et l'autorisation par place reste appliquée par l'API. La confidentialité dépend aussi de la protection du credential JDBC serveur.

## Préparer et migrer

1. Avec l'administration PostgreSQL, créer un rôle dédié de nom simple en minuscules (40 caractères maximum), sans privilège élevé, propriétaire ou membership. Renseigner son mot de passe par un canal privé ; ne pas le mettre dans une commande versionnée ou un diagnostic. Les noms PUBLIC, anon, authenticated et service_role sont refusés.
2. Dans un processus administratif séparé, fournir APP_DB_URL au format JDBC, APP_DB_USER et APP_DB_PASSWORD du propriétaire, APP_DB_SCHEMA du jeu, puis APP_DB_RUNTIME_ROLE avec le nom du rôle dédié. Ne pas copier ces credentials de propriétaire dans le processus API.
3. Depuis la racine, compiler puis lancer la commande explicite :

```powershell
mvn -pl deduction-engine compile
mvn -pl deduction-engine exec:java '-Dexec.mainClass=io.mastermindarena.deduction.infrastructure.jdbc.DatabaseMigrationMain'
```

La commande ne charge aucun fichier `.env` : les quatre variables APP_DB_* de connexion/schéma doivent être fournies au processus. Sans APP_DB_RUNTIME_ROLE, elle applique seulement les migrations. Avec ce nom, elle vérifie le rôle puis accorde les droits ci-dessus et installe ses politiques dans une transaction. Les valeurs sont transmises par requêtes préparées à un script PostgreSQL fixe ; `format(%I/%L)` échappe les identifiants et littéraux. Une seconde exécution conserve le même résultat. La commande ne crée ni mot de passe ni rôle et ne modifie pas les anciennes migrations V1–V5.

## Démarrer l'API limitée

Dans un autre processus, fournir les credentials du rôle de jeu dans APP_DB_USER/APP_DB_PASSWORD, la même URL/schéma, la configuration HTTP habituelle et :

```powershell
$env:APP_DB_MIGRATE_ON_START = 'false'
mvn -pl deduction-engine exec:java '-Dexec.mainClass=io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServerMain'
```

Valeurs acceptées : exactement `true` ou `false`. Une propriété JVM `submitAction.db.migrateOnStart` conserve la priorité habituelle sur l'environnement. Valeur absente : `true`, pour préserver les profils existants. Ne pas confondre ce défaut de compatibilité avec la configuration cible de production.

En mode false, l'API ne migre pas et ne crée pas de partie seed. Elle lit schema_version et exige exactement les versions 1 à 8 avant de réserver le port HTTP. Schéma absent, illisible ou version incorrecte : échec de démarrage sans conversion ni suppression. Ce contrôle de métadonnées ne remplace pas une vérification de toutes les tables/permissions. Pour une nouvelle migration, exécuter d'abord l'étape propriétaire et vérifier la compatibilité du binaire ; ne pas rendre le rôle API propriétaire pour contourner un échec.

## Preuves et limites

V8 ajoute une procédure de purge réservée au propriétaire, avec les droits de l'appelant, sans SECURITY DEFINER. Les droits EXECUTE de PUBLIC et des rôles explicitement présents dans les ACL par défaut sont retirés à sa création. Le provisionnement du rôle API ne donne aucun droit sur cette procédure ; appels avec ou sans paramètres refusés avec SQLSTATE 42501 dans le test PostgreSQL. Auditer séparément les superutilisateurs, appartenances et privilèges de la cible ; aucun réglage distant n'est appliqué ici.

Sur PostgreSQL 16 jetable, un vrai rôle LOGIN restreint a démarré l'API sans migration, créé/joint une partie, enregistré les secrets et événements/idempotence, accepté un abandon avec score, lu l'état et repris une place. Création de table, désactivation de RLS, DELETE et modification de schema_version/idempotency refusés en SQLSTATE 42501. Provisionnement rejoué, rôle API public et propriétaire refusés, métadonnées incomplètes bloquant le démarrage.

Les tests existants de refus des rôles navigateur, rollback et concurrence restent exécutés. La restauration sous un propriétaire a sa preuve distincte dans [DATABASE_RECOVERY](DATABASE_RECOVERY.md) ; restauration et changements de propriétaire doivent réinstaller/revérifier ces grants et politiques. Les rôles et permissions Supabase effectifs restent à contrôler avant bascule. Les profils locaux existants ne sont pas modifiés par ce lot.

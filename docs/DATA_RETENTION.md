# Conservation — décision du 23 octobre 2026

Complément du 28 octobre : perte des futures parties et scores en cas d'incident acceptée par le concepteur ; pas de sauvegarde récupérable requise pour cette livraison, aucune restauration promise. Cela ne change ni le seuil de sept jours, ni le cron, ni les garanties de confidentialité/concurrence. Voir [DATABASE_RECOVERY](DATABASE_RECOVERY.md).

## État courant — 27 octobre 2026

Le concepteur confirme ensuite le contrôle de désactivation/réactivation demandé. Ce contrôle clôt le critère de pilotage du job ; aucune suppression réelle d'une partie expirée n'a été mesurée dans les résultats fournis.

Job Supabase Cron 1 actif, nom exact mastermind-arena-purgE, postgres, 0 3 * * *, CALL arena_v1.purge_expired_matches();. Premier passage automatique à 03 h UTC le 27 octobre : succeeded. « 1 row » ne donne pas le nombre supprimé. Désactivation et suppression effective d'une partie expirée non observées. Ce statut remplace les mentions de planification non activée dans la préparation antérieure ci-dessous. Preuves dans [VALIDATION](VALIDATION.md).

Le concepteur demande de supprimer toutes les parties après **sept jours depuis leur dernière action acceptée**, y compris les parties non terminées. Une partie régulièrement jouée reste donc conservée. Sans action acceptée, la création sert de point de départ. Une simple lecture, un polling ou une reconnexion ne prolonge pas le délai.

**Commande administrative implémentée et testée localement ; aucune purge ni planification activée en production.** La suppression rend les codes de reprise inutilisables et supprime l'état, l'admission, les événements et les entrées idempotentes de la partie. Une opération idempotente d'une partie conservée reste disponible pour rejouer une réponse perdue.

## Schéma et exécution

La migration V6 ajoute les associations obligatoires des événements et résultats idempotents à match_state, et une clé étrangère pour rooms. La suppression de match_state cascade sur ces trois tables dans la même transaction. Les versions V1–V5 ne sont pas réécrites. V6 refuse une ancienne installation contenant des parties, admissions, événements ou résultats ; elle ne convertit ni ne supprime ces données. Toutes les données actuelles sont jetables selon confirmation du concepteur : le reset sera exécuté séparément lorsque la version sera prête, sans lecteur historique ni tentative de rattachement des anciens événements.

L'horloge PostgreSQL fixe une borne d'exécution : `clock_timestamp() - INTERVAL '168 hours'`. Une partie est éligible si `updated_at <= borne`, soit sept jours révolus, indépendamment d'un changement d'heure. Cet horodatage est initialisé à la création et actualisé à la sauvegarde d'une nouvelle action acceptée. Les lectures, admissions de l'adversaire, reprises et replays ne le modifient pas.

Depuis V8, `JdbcMatchRetention` appelle la procédure administrative commune `purge_expired_matches`. Celle-ci sélectionne les candidates puis, pour chacune, prend le même verrou transactionnel par partie que les actions et recontrôle l'expiration dans une instruction distincte après acquisition du verrou. Une action fraîchement acceptée entre sélection et verrou protège donc la partie. Une action arrivant après suppression ne retrouve plus d'état et ne le recrée pas. La transaction et le rollback portent sur **une partie et toutes ses données liées**, pas sur l'ensemble d'un lot : si une partie échoue, les suppressions précédemment validées restent effectives. La commande peut être relancée.

`DatabaseRetentionMain` vérifie le schéma V1–V8 en lecture seule, sans migration ni seed. Elle ne charge pas les fichiers .env ; fournir au processus administratif APP_DB_URL, APP_DB_USER, APP_DB_PASSWORD et APP_DB_SCHEMA, sans imprimer leurs valeurs. Utiliser le rôle propriétaire ; le rôle de jeu reste sans DELETE ni EXECUTE sur la procédure. Aucun endpoint HTTP de purge n'est ajouté.

V8 ajoute la procédure sans transformer ni effacer les parties au format V7. La procédure utilise les droits de l'appelant (`SECURITY INVOKER`), sans élévation de privilèges. Les droits EXECUTE de PUBLIC et les grants explicites issus des privilèges par défaut sont retirés à sa création. Ne pas ajouter de grant au rôle API. Les superutilisateurs et rôles capables d'adopter l'identité du propriétaire restent privilégiés : la procédure ne remplace pas l'audit des rôles de production.

```powershell
# Aperçu uniquement, sans argument : nombre de parties et borne, aucune identité ni donnée privée.
mvn -pl deduction-engine exec:java '-Dexec.mainClass=io.mastermindarena.deduction.infrastructure.jdbc.DatabaseRetentionMain'

# Suppression explicite ; à exécuter seulement sur la base effectivement autorisée.
mvn -pl deduction-engine exec:java '-Dexec.mainClass=io.mastermindarena.deduction.infrastructure.jdbc.DatabaseRetentionMain' '-Dexec.args=--apply'
```

L'aperçu est indicatif : une activité concurrente peut changer le nombre réellement supprimé. La commande affiche seulement la borne et les nombres ; ses erreurs ne publient pas les diagnostics JDBC. Elle n'offre pas de restauration après suppression.

## Déclenchement retenu le 24 octobre

Supabase Cron exécutera la purge une fois par jour. GitHub Actions n'est pas retenu pour cette tâche. Le seuil reste fixé à 168 heures depuis la dernière action acceptée ; avec une exécution quotidienne réussie, la suppression intervient normalement entre sept et huit jours d'inactivité. Un incident ou une suspension du service peut prolonger ce délai.

Horaire approuvé le 26 octobre : **03 h UTC chaque jour**, expression `0 3 * * *`, soit 04 h en hiver et 05 h en été à Paris. Cet accord fixe l'horaire ; aucune activation n'est attestée. Vérifier le fuseau effectif du planificateur avant de créer la tâche et conserver l'appel de procédure de premier niveau décrit ci-dessous.

Diagnostic Supabase reçu le 26 octobre : procédure présente et non exécutable par anon/authenticated ; cron.timezone vaut GMT, pg_cron n'est pas installé. Prochaine opération de configuration : activer l'extension dans Integrations → Cron, puis programmer une commande SQL unique `CALL arena_v1.purge_expired_matches();` à `0 3 * * *` sous le propriétaire. Contrôler la définition et l'historique du job ; statut et résultats distants encore ouverts. [Installation officielle](https://supabase.com/docs/guides/cron/install), [création et historique](https://supabase.com/docs/guides/cron/quickstart).

La tâche sera configurée et activée lors du déploiement, après le reset initial et la vérification du nouveau schéma. La procédure SQL versionnée en V8 est partagée par la commande Java et le futur cron, selon l'organisation approuvée le 24 octobre. Le cron devra appeler une seule instruction `CALL arena_v1.purge_expired_matches()` avec le rôle propriétaire, en adaptant le schéma à la cible. Les valeurs par défaut fixent la borne à l'horloge PostgreSQL moins 168 heures et retournent le nombre supprimé. Ne pas entourer CALL d'un BEGIN/COMMIT ni l'appeler depuis une fonction : ses validations par partie nécessitent un appel de premier niveau. Le paramètre de borne explicite sert aux contrôles administratifs/tests ; le cron doit utiliser la valeur par défaut.

Le dossier de déploiement devra préciser l'horaire et son fuseau, l'installation de pg_cron, les permissions, la consultation des résultats, le contrôle d'une exécution réelle et la désactivation de la tâche. Aucun de ces réglages distants n'a encore été vérifié ou appliqué. [Documentation officielle Supabase Cron](https://supabase.com/docs/guides/cron).

## Validation et suite

Le 24 octobre, le concepteur reporte l'information des joueurs et les messages de partie indisponible à la refonte UX (lot 5). Les propositions de textes et d'emplacement près du code de reprise ne sont pas approuvées pour un ajout immédiat. Le front conserve ses messages actuels dans l'intervalle.

Tests PostgreSQL isolés : borne juste avant/à/après expiration ; suppression des quatre tables sans effet sur une partie conservée ; reprise et replay impossibles après purge ; lectures/jointure/reprise/replay sans prolongation ; nouvelle action protectrice ; purge bloquée sur le verrou puis annulée après une action concurrente ; incident de cascade avec rollback et nouvelle exécution ; ancienne installation remplie refusée sans effacement. Les permissions du rôle API interdisent DELETE sur rooms et match_state.

Le choix du planificateur et de sa fréquence est arrêté ; la configuration de pg_cron et son activation restent ouvertes. Les preuves locales de la procédure commune sont consignées dans [VALIDATION](VALIDATION.md) ; elles ne prouvent pas une exécution Supabase Cron. En attendant, aucune suppression ne se déclenche automatiquement à sept jours : l'expiration rend une partie éligible à la commande. Sans conservation de tombstones, l'API ne distingue pas une partie purgée d'une partie inexistante. Le reset initial autorisé n'est pas la procédure de purge récurrente.

La rétention des journaux d'infrastructure (Vercel), les durées des sauvegardes et leur fréquence restent distinctes et non décidées par ce choix de sept jours. Une sauvegarde peut contenir des parties expirées : sa politique doit être explicitée, pas supposée alignée automatiquement.

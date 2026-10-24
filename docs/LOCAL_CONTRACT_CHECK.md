# Vérification combinée des clients front et du backend

`scripts/verify-local-contract.mjs` utilise les vrais clients JavaScript contre un serveur Java/JDBC local déjà démarré. Il crée deux parties de test et joue les deux places ; il ne pilote pas de navigateur.

## Préparation

Démarrer une instance API dédiée sur un port libre, par exemple 8082, avec `APP_DB_URL`, `APP_DB_USER`, `APP_DB_PASSWORD` et `APP_DB_SCHEMA` pointant vers un PostgreSQL jetable. Utiliser une clé d'API dédiée dans `APP_API_KEY` et `APP_HTTP_PORT=8082`. Ne pas charger le profil de développement habituel pour cette instance. Les migrations sont appliquées au schéma de test par le serveur.

Le script refuse les URL distantes. Cette vérification de l'adresse HTTP ne permet pas de déduire quelle base utilise le serveur : son profil JDBC doit donc être explicitement isolé.

## Exécution depuis la racine

```powershell
$env:ARENA_INTEGRATION_HTTP_URL = 'http://127.0.0.1:8082'
$env:ARENA_INTEGRATION_API_KEY = '<clé de test dédiée à cette API>'
node scripts/verify-local-contract.mjs
```

Le script vérifie création/liste/jointure/statut, secrets, replay idempotent, rejet d'une autre intention à version périmée, essais et indices cachés, victoire et poursuite, correction retirant le point, contestation pendant un essai masqué, abandon et révélation, accord de revanche et reprise privée. Un succès affiche une seule ligne `PASS`, sans identifiant privé ni jeton. Un échec conserve une assertion explicite ; il ne transforme pas une réponse invalide en succès.

Depuis le 24 octobre, il provoque aussi les quinze refus traduits de [ERROR_MESSAGES](ERROR_MESSAGES.md) : attente, phases, actions déjà soumises, poursuite, revanche et indices de la manche précédente non modifiables. Ces refus utilisent de vrais POST HTTP, avec délai maximal de quinze secondes incluant le corps ; statut 400/409, code et origine vérifiés, puis passage dans le validateur et le traducteur front. Le message doit être dédié, les métadonnées intactes et la vue du joueur identique avant/après. Une seconde partie exerce les indices non attendus pendant la poursuite sans invalider le scénario de correction de la première. Aucune réponse simulée dans ce script.

Après le test, arrêter l'API dédiée et supprimer uniquement le PostgreSQL jetable qui lui était réservé. Les profils `.env` ne sont pas modifiés par ce script. Ce parcours ne vérifie ni le rendu, ni le clavier, ni la navigation dans un navigateur, ni un déploiement distant.

## Reprise après arrêt et redémarrage réels

`scripts/verify-local-restart.mjs` lance directement deux processus Java successifs, sur le port 8082 qui doit être libre. Il nécessite Java, les classes compilées et les dépendances Maven disponibles. Utiliser exclusivement une base PostgreSQL jetable distincte de la base de développement ; le script exige une URL JDBC explicite sur 127.0.0.1 et crée un schéma de test unique. Il ne charge aucun fichier `.env`.

```powershell
mvn -pl deduction-engine compile dependency:build-classpath '-Dmdep.outputFile=target/runtime-classpath.txt'
$env:ARENA_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:54330/arena_test'
$env:ARENA_TEST_DB_USER = '<utilisateur de la base jetable>'
$env:ARENA_TEST_DB_PASSWORD = '<mot de passe de la base jetable>'
node scripts/verify-local-restart.mjs
```

Les vrais clients front créent deux places, déposent les secrets et terminent une manche par abandon avec un score non nul. Le premier processus Java est arrêté et un nouveau PID démarre sur le même schéma. Le scénario reprend les deux places avec leurs codes privés, compare l'état complet, rejoue exactement la commande acceptée, vérifie l'absence de changement d'état/score, puis démarre une revanche commune. Les codes privés restent en mémoire ; aucune sauvegarde de credentials sur disque ni impression des corps serveur.

Le script arrête ses processus Java dans un `finally`. Il conserve son schéma de test pour inspection ; arrêter ensuite uniquement le conteneur PostgreSQL jetable qui lui était réservé. Le fichier de classpath est généré sous `target/`, ignoré par Git. Un PASS prouve ce scénario de redémarrage local ; il ne prouve ni une restauration de sauvegarde, ni la conservation des données après perte du volume PostgreSQL, ni le comportement de Vercel/Supabase.

Depuis le lot de reçus compacts, le script rejoue également l'ancien abandon après la revanche : même réponse de la manche précédente et nouveau plateau inchangé. Le mode restauration exerce ce contrôle sur les reçus de format 2 réellement restaurés, après changement de processus Java.

Le mode `node scripts/verify-local-restart.mjs --restore` ajoute un dump logique puis une restauration dans une autre base du conteneur dédié `mastermind-restore-validation`, avant de vérifier la reprise. Prérequis, nettoyage, garanties et limites sont détaillés dans [DATABASE_RECOVERY](DATABASE_RECOVERY.md). La commande sans option conserve le scénario de redémarrage simple.

## Purge administrative

Depuis V8, la commande Java et le futur cron utilisent la même procédure PostgreSQL. Le test `failureOnALaterMatchKeepsEarlierDeletionsCommitted` a été exécuté avant puis après déplacement de la logique : il protège les transactions par partie. L'appel direct `CALL schema.purge_expired_matches()` est testé sans transaction englobante ; ne pas l'entourer d'un BEGIN/COMMIT. Le scénario de restauration contrôle aussi que la procédure restaurée conserve les parties récentes et retourne zéro. Preuves du 24 octobre dans [VALIDATION](VALIDATION.md), configuration Supabase Cron non couverte par ces tests locaux.

Sur PostgreSQL jetable, `JdbcMatchRetentionTest` couvre l'expiration à sept jours, l'isolation, la cascade, le rollback et la concurrence avec une action. Les mêmes variables ARENA_TEST_DB_* activent ces tests dans `mvn -Pquality clean verify`. La commande de purge utilise ses propres variables administratives APP_DB_* explicites, donne un aperçu sans argument et exige `--apply` pour supprimer ; procédure dans [DATA_RETENTION](DATA_RETENTION.md). Ne jamais la lancer sur Supabase pour ce contrôle. La validation du 23 octobre comprend également une exécution réelle de cette commande sur deux états synthétiques vieillis, suivie d'un rejeu sans nouvelle suppression.

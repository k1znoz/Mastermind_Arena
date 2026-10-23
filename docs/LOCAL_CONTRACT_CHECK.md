# Vérification combinée des clients front et du backend

`scripts/verify-local-contract.mjs` utilise les vrais clients JavaScript contre un serveur Java/JDBC local déjà démarré. Il crée une partie de test et joue les deux places ; il ne pilote pas de navigateur.

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

Après le test, arrêter l'API dédiée et supprimer uniquement le PostgreSQL jetable qui lui était réservé. Les profils `.env` ne sont pas modifiés par ce script. Ce parcours ne vérifie ni le rendu, ni le clavier, ni la navigation dans un navigateur, ni un déploiement distant.

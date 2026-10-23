# Mastermind Arena

Jeu de déduction à deux plateaux simultanés : six couleurs, codes de quatre cases, propositions et indices manuels, essais sans limite. Le premier joueur à trouver gagne le duel ; l'autre peut poursuivre sans point supplémentaire. Corrections du dernier tour, abandon, revanche commune et reprise privée sont décrits dans [les règles](docs/GAME_RULES.md).

## Architecture et décisions

Pour les prochaines interventions : [cadre de travail](AGENTS.md), [roadmap de maintenance](docs/MAINTENANCE_ROADMAP.md) et [exigences de qualité](docs/CODE_QUALITY.md). Ces documents décrivent aussi les critères à satisfaire avant de déclarer un lot terminé.

- Front : Svelte 5 / Vite, JavaScript.
- Backend : Java 21, serveur HTTP du JDK, Jackson, JDBC PostgreSQL.
- Hébergement : deux projets Vercel (front et backend), PostgreSQL sur Supabase ; les deux projets suivent `main`.
- Synchronisation : HTTP et polling toutes les six secondes. Aucun WebSocket réseau n'est actuellement livré.

Voir [les décisions actuelles](decision.md), [l'architecture](docs/ARCHITECTURE_V2.md), [la roadmap](docs/ROADMAP.md) et [les preuves CDA](docs/CDA_COVERAGE.md). Les documents dans `docs/archive` et les notes initiales d'architecture expliquent l'historique du projet, pas le comportement actuel.

## Démarrer en local

### Profil local isolé (Windows)

Les fichiers existants `.env` et `App_Front/.env.local` sont conservés. Le profil ajouté utilise `.env.local` à la racine pour PostgreSQL local, et `App_Front/.env.development.local` pour le front en développement. Ces fichiers sont ignorés par Git. Docker Desktop doit être démarré.

Depuis la racine, ouvrir deux terminaux :

```powershell
# Terminal 1 : PostgreSQL local puis backend Java (8081)
./scripts/start-local.ps1 Backend
# Terminal 2 : interface (5173)
./scripts/start-local.ps1 Frontend
```

Ouvrir `http://127.0.0.1:5173`. Le frontend passe par le proxy Vite vers `127.0.0.1:8081`, et Java utilise PostgreSQL sur `127.0.0.1:54329`, schéma neuf `arena_v1`. Aucune connexion Supabase n'est utilisée par ce lanceur. Les données locales restent dans un volume Docker distinct. Arrêter les terminaux avec Ctrl+C ; arrêter PostgreSQL sans supprimer ses données :

```powershell
docker compose --project-name mastermind-local --env-file .env.local -f docker-compose.local.yml stop
```

Le profil de développement surcharge automatiquement les valeurs du front avec `npm run dev`. Il ne s'applique pas à `npm run build`, qui conserve le profil existant. Le lanceur charge explicitement les variables pour Java et restaure celles du terminal à sa fermeture.

### Configuration manuelle ou conteneur backend

Prérequis : Java 21, Maven 3.9+, Node.js 20+ et npm. Pour utiliser Docker Compose, Docker Desktop doit Être démarré.

1. Copier `.env.example` vers `.env` ; renseigner la clé d'API et les accès à une base PostgreSQL de développement.
2. Copier `App_Front/.env.example` vers `App_Front/.env.local`. Renseigner la même clé d'API ; laisser `VITE_API_BASE_URL` vide et `VITE_PROXY_TARGET=http://localhost:8081`.
3. Depuis la racine : `docker compose up --build` (API sur le port 8081).
4. Depuis `App_Front` : `npm ci`, puis `npm run dev`.
5. Choisir un pseudo, créer une partie et la rejoindre depuis un deuxième navigateur. Pour retrouver une place dans un autre onglet, utiliser son **code privé de reprise**, accessible dans la partie.

Pour lancer le backend sans Docker : fournir les variables d'environnement de `.env.example` au processus Java, puis exécuter :

```powershell
mvn -pl deduction-engine exec:java "-Dexec.mainClass=io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServerMain"
```

Le backend direct utilise le port 8080 ; régler `VITE_PROXY_TARGET` en conséquence. Le processus Java ne charge pas automatiquement un fichier `.env`.

## Vérifier

Depuis la racine :

```powershell
mvn -pl deduction-engine clean test
mvn -pl deduction-engine -Pquality verify
```

Depuis `App_Front` :

```powershell
npm run lint
npm run check
npm run build
```

Les tests métier mémoire et les contrôles statiques ne remplacent pas les tests PostgreSQL et le parcours à deux clients. Voir [le plan de validation](docs/VALIDATION.md).

## Déployer et démontrer

Le front appelle `VITE_API_BASE_URL` (URL HTTPS du projet backend). `APP_CORS_ALLOWED_ORIGINS` doit inclure l'origine HTTPS du projet front. Une clé `VITE_API_KEY` est visible par le navigateur : l'autorisation d'une partie repose sur le jeton privé de chaque joueur.

Le front et le backend doivent Être publiés ensemble lorsque le contrat de jeu change. Aucun push, déploiement ou changement de base de production n'est effectué par les vérifications locales.

Avant une démonstration : vérifier les deux déploiements, l'État actif de Supabase, la création/jointure d'une partie et le code de reprise de chaque joueur. Prévoir une démonstration locale de secours. Cible de présentation : **23 novembre 2026**.

Les parties créées avec l'ancienne palette à huit symboles doivent Être terminées puis suivies d'une nouvelle manche ; les données historiques ne sont pas converties silencieusement.

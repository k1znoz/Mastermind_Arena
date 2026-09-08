# Mastermind Arena

## Vue d'ensemble

Mastermind Arena est un projet full-stack composé d'un backend Java pour le moteur de jeu et d'un front Svelte pour l'expérience utilisateur.

## Prérequis

- Java 21
- Maven 3.9+
- Node.js 20+
- npm

## Démarrage backend

Depuis la racine du projet :

```bash
mvn -q -pl deduction-engine test
mvn -q -pl deduction-engine exec:java -Dexec.mainClass=io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServerMain
```

Le backend écoute par défaut sur :

- http://localhost:8080/local/submit-action
- http://localhost:8080/local/match-state

## Démarrage frontend

Depuis le dossier App_Front :

```bash
npm install
npm run dev -- --host 0.0.0.0
```

Le front est servi par défaut sur :

- http://localhost:5173

## Variables d'environnement

Le backend utilise des variables comme :

```bash
APP_HTTP_PORT=8080
APP_API_KEY_HEADER=X-API-Key
APP_API_KEY=change-me-dev-key
APP_DB_URL=jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require
APP_DB_USER=postgres
APP_DB_PASSWORD=change-me-db-password
APP_DB_SCHEMA=public
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Le frontend peut utiliser un `.env.local` dans App_Front :

```bash
VITE_API_BASE_URL=
VITE_SUBMIT_ACTION_PATH=/api/local/submit-action
VITE_MATCH_STATE_PATH=/api/local/match-state
VITE_API_KEY_HEADER=X-API-Key
VITE_API_KEY=change-me-dev-key
```

## Structure du dépôt

```text
Mastermind_Arena/
├── App_Front/
├── deduction-engine/
├── docs/
├── docker-compose.yml
├── dockerfile
├── pom.xml
├── README.md
└── decision.md
```

## Validation du ticket P-001

Ce socle est considéré valide si :

- le backend compile avec Java 21 ;
- les tests Maven démarrent sans échec de compilation ;
- le frontend peut se lancer localement avec Vite ;
- le dossier de démarrage et les variables sont documentés.

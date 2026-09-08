# ROADMAP

## P-001 — Initialiser le socle projet et le runbook de développement

- objectif : Poser les bases du dépôt avec les modules backend, frontend, conteneurisation et documentation de démarrage pour que le projet soit exécutable et cohérent d'un coup d'œil.
- fichiers concernés : `pom.xml`, `docker-compose.yml`, `dockerfile`, `App_Front/package.json`, `App_Front/README.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE_V2.md`
- critère de validation :
  - le dépôt se construit avec la commande de démarrage backend/frontend documentée ;
  - la structure des modules est claire ;
  - le README et les scripts de lancement permettent de démarrer l'app sans ambiguïté ;
  - le couplage frontend/backend est explicite.
- message de commit : `chore: initialize project skeleton and dev bootstrap`

## P-002 — Définir le contrat runtime du match et le modèle de state

- objectif : Formaliser le `MatchRuntimeState` et les structures de runtime qui représentent l’état actuel d’une partie, afin qu’il soit partagé entre moteur, API et frontend.
- fichiers concernés : `deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/MatchStateHttpResponse.java`, `deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/MatchStateHttpMapper.java`, `deduction-engine/src/main/java/io/mastermindarena/deduction/engine/workflow/*`, `deduction-engine/src/test/java/io/mastermindarena/deduction/api/submitaction/*`
- critère de validation :
  - les champs du match et de la vue publique sont précisément définis ;
  - le state expose le statut du tour, les acteurs, l’historique et les informations autorisées ;
  - les tests de contrat vérifient le mapping API ↔ runtime ;
  - le state est stable même en cas de rechargement ou de redémarrage.
- message de commit : `feat: define match runtime state contract`

## P-003 — Implémenter le moteur de règles Mastermind agnostique

- objectif : Mettre en place le cœur de logique métier autour des actions, résolutions, directives moteur et résultats opaques, afin que le moteur reste générique tout en supportant Mastermind.
- fichiers concernés : `deduction-engine/src/main/java/io/mastermindarena/deduction/engine/workflow/*`, `deduction-engine/src/main/java/io/mastermindarena/deduction/application/submitaction/*`, `deduction-engine/src/test/java/io/mastermindarena/deduction/engine/workflow/*`
- critère de validation :
  - les règles de validation d’action sont testées ;
  - les `EngineDirective` sont appliquées sans interprétation du jeu ;
  - les actions rejetées, acceptées et terminales sont bien distinguées ;
  - une action ne peut pas modifier un match terminé.
- message de commit : `feat: implement generic deduction engine workflow`

## P-004 — Créer la boucle de tour et les transitions de match

- objectif : Définir le cycle complet d’un tour : soumission, validation, changement d’acteur, fin de manche ou annulation, pour garantir la cohérence d’exécution d’un match.
- fichiers concernés : `deduction-engine/src/main/java/io/mastermindarena/deduction/engine/workflow/SubmitActionOrchestrator.java`, `SubmitActionCommand.java`, `SubmitActionResult.java`, `deduction-engine/src/test/java/io/mastermindarena/deduction/engine/workflow/SubmitActionWorkflowTest.java`
- critère de validation :
  - le workflow gère correctement les cas nominal, rejeté, terminal et concurrent ;
  - le `expectedVersion` et l’`idempotencyKey` sont utilisés correctement ;
  - les transitions de tour sont cohérentes entre les joueurs ;
  - le moteur refuse toute action hors cycle valide.
- message de commit : `feat: add turn lifecycle and match transitions`

## P-005 — Exposer l’API locale de soumission et de lecture d’état

- objectif : Rendre le backend exploitable via HTTP local, avec endpoints de soumission d’action et de lecture de l’état d’un match, pour connecter le frontend ou des clients externes.
- fichiers concernés : `deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/*`, `deduction-engine/src/test/java/io/mastermindarena/deduction/api/submitaction/*`, `docs/architecture/p1-application-integration-layer.md`
- critère de validation :
  - `SubmitAction` répond avec code HTTP cohérent ;
  - l’endpoint de lecture de l’état retourne un snapshot conforme ;
  - les erreurs d’authentification et de validation sont bien mappées ;
  - les intégrations HTTP couvrent les cas nominaux et de sécurité.
- message de commit : `feat: expose local submit and match state http api`

## P-006 — Ajouter la persistance et la migration de schéma

- objectif : Sauvegarder les états du match et les résultats d’actions de manière fiable pour supporter le redémarrage, la reprise et l’idempotence.
- fichiers concernés : `deduction-engine/src/main/java/io/mastermindarena/deduction/infrastructure/jdbc/*`, `deduction-engine/src/main/resources/db/migration/V1__submit_action_init.sql`, `deduction-engine/src/main/java/io/mastermindarena/deduction/infrastructure/file/*`, `deduction-engine/src/test/java/io/mastermindarena/deduction/application/persistence/*`
- critère de validation :
  - la base de données est initialisée avec Flyway ;
  - le store JDBC persiste correctement les états ;
  - un redémarrage conserve les données d’un match ;
  - les rééditions idempotentes ne produisent pas de doublons d’effets.
- message de commit : `feat: persist match state with jdbc and migration`

## P-007 — Connecter le front Svelte à la session et aux écrans de match

- objectif : Faire tourner le front Svelte en mode local avec un vrai parcours utilisateur : création de session, observation de l'état du match et affichage des écrans clés.
- fichiers concernés : `App_Front/src/App.svelte`, `App_Front/src/lib/api/*`, `App_Front/src/lib/components/*.svelte`, `App_Front/src/main.js`, `App_Front/package.json`
- critère de validation :
  - les écrans `SessionScreen`, `PartieScreen`, `HistoryScreen`, `SettingsScreen` s’affichent correctement ;
  - le front charge le runtime config et les clients d’API ;
  - le client de match et le client de soumission s’intègrent sans erreur de runtime ;
  - le flux de démarrage Svelte Vite fonctionne en local.
- message de commit : `feat: wire svelte front to match session flow`

## P-008 — Implémenter la logique de jeu Mastermind côté interface

- objectif : Permettre au joueur de jouer une vraie manche Mastermind avec secret, tentative, consensus et validation d’indices, en respectant la règle de jeu manuel de l’application.
- fichiers concernés : `App_Front/src/lib/components/PartieScreen.svelte`, `App_Front/src/lib/utils/symbolSequence.js`, `App_Front/src/lib/utils/symbolVisuals.js`, `docs/GAME_RULES.md`
- critère de validation :
  - l’utilisateur peut choisir une combinaison secrète et la visualiser selon les règles de confidentialité ;
  - une proposition est soumise proprement ;
  - les indices visuels sont affichés et verrouillés correctement ;
  - le tour est clairement visible pour les deux rôle de joueur actif et donneur d’indices.
- message de commit : `feat: implement mastermind gameplay interactions in ui`

## P-009 — Ajouter la synchronisation temps réel avec WebSocket

- objectif : Diffuser les changements de match en temps réel pour que les deux joueurs voient le même état sans rechargement manuel.
- fichiers concernés : `deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/*`, `deduction-engine/src/main/java/io/mastermindarena/deduction/engine/workflow/*`, `App_Front/src/lib/api/*`, `App_Front/src/App.svelte`, `docs/ARCHITECTURE_V2.md`
- critère de validation :
  - un changement d’état du match est visible immédiatement sur les clients connectés ;
  - les messages WebSocket reflètent le `MatchRuntimeState` sans divergence ;
  - les événement de fin de tour et de match sont bien diffusés ;
  - les clients reçoivent un état cohérent après chaque action.
- message de commit : `feat: add realtime websocket synchronization for match state`

## P-010 — Finaliser la robustesse, la validation et la préparation de release

- objectif : Passer la version du projet en état de qualité de release : validation fonctionnelle, couverture de scénarios critiques, nettoyage des points d’API et préparation de la livraison.
- fichiers concernés : `pom.xml`, `deduction-engine/src/test/**`, `App_Front/src/**`, `docker-compose.yml`, `docs/**`, `README.md`
- critère de validation :
  - les tests couvrent les scénarios critique du moteur et du front ;
  - les cas de rejet, idempotence, terminaison et persistance passent ;
  - le projet se lance proprement dans l’environnement local standard ;
  - la documentation de lancement et les tickets de roadmap sont alignés sur le code réel.
- message de commit : `release: harden gameplay flow and prepare stable release`

---

## Priorisation recommandée

1. P-001 à P-006 : fondations backend et moteur de jeu.
2. P-007 à P-009 : front, interaction et synchronisation temps réel.
3. P-010 : validation finale, stabilisation et livraison.

Cette séquence permet de livrer d’abord la base de jeu cohérente, puis d’assembler l’expérience utilisateur sur un backend validé.

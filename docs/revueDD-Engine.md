

| Classe | Rôle | Dépendances | Décision |
| --- | --- | --- | --- |
| `LocalMastermindRuleSet` | Valide structurellement une proposition et maintient le tour en attente des indices manuels. | `RuleSet`, `ActionResolution`, `EngineDirective`, `Rejection`, `RuleEvaluationContext` | Simplifier |
| `LocalMatchStateEndpoint` | Expose la lecture locale de l’état d’une partie. | `MatchStateStore`, `MatchStateHttpMapper`, `MatchStateHttpResponse` | Garder |
| `LocalSubmitActionEndpoint` | Reçoit une soumission d’action et délègue au service applicatif. | `SubmitActionApplicationService`, `SubmitActionHttpMapper`, DTO HTTP | Garder |
| `LocalSubmitActionHttpServer` | Assemble les endpoints, la persistance JDBC, la configuration HTTP et les métriques locales. | JDK HTTP, Jackson, endpoints, stores JDBC | Simplifier |
| `LocalSubmitActionHttpServerMain` | Démarre le serveur HTTP local. | `LocalSubmitActionRuntimeConfig`, `LocalSubmitActionHttpServer` | Garder |
| `LocalSubmitActionRuntimeConfig` | Porte la configuration locale du serveur et de la partie initiale. | Variables d’environnement, JDK | Simplifier |
| `MatchStateHttpMapper` | Transforme l’état interne en vue HTTP et masque les secrets non autorisés. | `MatchRuntimeState`, `MatchActionRecord`, `MatchStateViewActorContext` | Simplifier |
| `MatchStateHttpResponse` | DTO de lecture HTTP d’une partie et de son historique. | Collections JDK | Simplifier |
| `MatchStateViewActorContext` | Identifie le lecteur d’une vue afin de filtrer les données privées. | Types JDK | Garder |
| `SubmitActionHttpMapper` | Convertit les requêtes et réponses entre HTTP et application. | DTO HTTP, DTO applicatifs, `MatchRuntimeState` | Simplifier |
| `SubmitActionHttpRequest` | DTO HTTP de soumission d’une action. | Types JDK | Simplifier |
| `SubmitActionHttpResponse` | DTO HTTP de réponse à une action. | Collections JDK | Simplifier |
| `SubmitActionApplicationRejection` | Représente un rejet applicatif destiné à l’API. | `Rejection`, `RejectionOrigin` | Garder |
| `SubmitActionApplicationRequest` | Transporte une action de l’API vers l’application. | Types JDK | Simplifier |
| `SubmitActionApplicationResponse` | Transporte le résultat du workflow vers l’API. | `SubmitActionResult`, `MatchRuntimeState`, `ActionResolution` | Simplifier |
| `SubmitActionApplicationService` | Adapte une requête applicative en commande de workflow et traduit les erreurs. | `SubmitActionOrchestrator`, DTO applicatifs | Garder |
| `ActionResolution` | Contrat générique de décision contenant directives, rejet et résultats optionnels. | `EngineDirective`, `EvaluationResult`, `MatchOutcome`, `CancellationReason`, `Rejection` | Sommeil |
| `ActionResolutionContractValidator` | Vérifie la cohérence des directives et résultats d’une résolution. | `ActionResolution`, `EngineDirective`, `Rejection` | Sommeil |
| `CancellationReason` | Porte une raison d’annulation structurée. | Types JDK | Sommeil |
| `EngineDirective` | Définit les transitions génériques du moteur. | Types JDK | Sommeil |
| `EvaluationResult` | Transporte un résultat opaque produit par une évaluation automatisée. | Types JDK | Sommeil |
| `LogTarget` | Définit les destinations de journalisation d’un rejet. | Types JDK | Simplifier |
| `MatchOutcome` | Porte le résultat final calculé d’une partie. | `ParticipantResult`, types temporels JDK | Sommeil |
| `ParticipantResult` | Porte le résultat et le rang d’un participant. | Types JDK | Sommeil |
| `Rejection` | Décrit un rejet technique ou métier. | `RejectionOrigin`, `LogTarget` | Garder |
| `RejectionOrigin` | Distingue l’origine moteur et règle métier d’un rejet. | Types JDK | Simplifier |
| `RuleSet` | Contrat d’extension permettant à un jeu de décider une résolution. | `ActionResolution` | Sommeil |
| `Feedback` | Porte les comptes manuels `bienPlaces` et `malPlaces`. | Types JDK | Garder |
| `GameRoom` | Agrège l’état runtime, les deux joueurs et les tours du plateau. | `MatchRuntimeState`, `Player`, `Turn` | Garder |
| `Guess` | Représente une proposition de symboles faite par un joueur. | `Player`, collections JDK | Garder |
| `Player` | Représente l’identité d’un joueur de plateau. | Types JDK | Garder |
| `Turn` | Représente une proposition, son donneur d’indices et son feedback optionnel. | `Player`, `Guess`, `Feedback` | Garder |
| `EventSink` | Port de publication et lecture d’événements de jeu. | Collections JDK | Simplifier |
| `GameEvent` | Vocabulaire fermé des événements de partie diffusables. | Types JDK | Garder |
| `IdempotencyStore` | Port de stockage des résultats par clé d’idempotence. | `SubmitActionResult`, `Optional` | Garder |
| `InMemoryEventSink` | Implémentation mémoire d’un collecteur d’événements. | `EventSink`, collections JDK | Garder |
| `InMemoryIdempotencyStore` | Implémentation mémoire de l’idempotence. | `IdempotencyStore`, collections JDK | Garder |
| `InMemoryMatchStateStore` | Implémentation mémoire du stockage d’état de partie. | `MatchStateStore`, `MatchRuntimeState` | Garder |
| `MatchActionPayloadView` | Déduit type, symboles et résumé depuis un payload non typé. | `Map`, collections JDK | Supprimer MVP |
| `MatchActionRecord` | Historise une action, ses symboles, ses événements et son statut. | `MatchActionPayloadView`, collections JDK | Simplifier |
| `MatchRuntimeState` | Porte l’état du match, le tour actif, l’ordre des joueurs et le journal. | `MatchOutcome`, `CancellationReason`, `MatchActionRecord` | Simplifier |
| `MatchStateStore` | Port de chargement et de sauvegarde de l’état d’une partie. | `MatchRuntimeState`, `Optional` | Garder |
| `RuleEvaluationContext` | Fournit la commande et l’état courant à un `RuleSet`. | `MatchRuntimeState`, `SubmitActionCommand` | Sommeil |
| `SubmitActionCommand` | Commande interne avec joueur, version et clé d’idempotence. | Types JDK | Simplifier |
| `SubmitActionOrchestrator` | Contrôle l’accès, l’idempotence, la version, les transitions et les événements. | Stores, `EventSink`, `RuleSet`, contrats de résolution | Simplifier |
| `SubmitActionResult` | Porte l’état, la résolution et les événements après une action. | `MatchRuntimeState`, `ActionResolution` | Simplifier |
| `FileIdempotencyStore` | Persiste les clés d’idempotence dans des fichiers. | `IdempotencyStore`, `FilePersistenceCodec`, système de fichiers JDK | Sommeil |
| `FileMatchStateStore` | Persiste l’état de partie dans des fichiers. | `MatchStateStore`, `FilePersistenceCodec`, système de fichiers JDK | Sommeil |
| `FilePersistenceCodec` | Sérialise les anciens contrats génériques et l’état dans un format texte. | Workflow, contrats de résolution, encodage JDK | Supprimer MVP |
| `FileWorkflowEventSink` | Persiste les événements de jeu dans un fichier. | `EventSink`, `FilePersistenceCodec`, système de fichiers JDK | Sommeil |
| `JdbcIdempotencyStore` | Persiste l’idempotence en base relationnelle. | `IdempotencyStore`, `JdbcPersistenceContext`, `FilePersistenceCodec`, JDBC | Simplifier |
| `JdbcMatchStateStore` | Charge et sauvegarde l’état de partie en base relationnelle. | `MatchStateStore`, `JdbcPersistenceContext`, `FilePersistenceCodec`, JDBC | Simplifier |
| `JdbcPersistenceContext` | Centralise l’ouverture des connexions et le nommage des tables JDBC. | JDBC, configuration JDK | Garder |
| `JdbcSchemaMigrator` | Applique les migrations de base de données. | `JdbcPersistenceContext`, ressources SQL, JDBC | Garder |
| `JdbcWorkflowEventSink` | Persiste les événements de jeu en base relationnelle. | `EventSink`, `JdbcPersistenceContext`, JDBC | Simplifier |
| `PreferencesIdempotencyStore` | Persiste l’idempotence dans les préférences locales JVM. | `IdempotencyStore`, `FilePersistenceCodec`, `Preferences` JDK | Supprimer MVP |
| `PreferencesMatchStateStore` | Persiste l’état de partie dans les préférences locales JVM. | `MatchStateStore`, `FilePersistenceCodec`, `Preferences` JDK | Supprimer MVP |
| `PreferencesWorkflowEventSink` | Persiste les événements dans les préférences locales JVM. | `EventSink`, `Preferences` JDK | Supprimer MVP |
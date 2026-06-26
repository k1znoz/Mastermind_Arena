Oui. Voici le **résumé consolidé de tout ce qui a été décidé jusqu’ici**, avant de créer le moindre fichier.

---

# 1. Vision du projet

Le projet visible sera :

```text
Mastermind Arena
```

Mais son cœur technique sera conçu comme un moteur générique :

```text
Deduction Engine
```

L’idée est de développer une application centrée sur Mastermind, tout en montrant une architecture extensible capable d’accueillir d’autres jeux de déduction plus tard.

Exemples futurs possibles :

- Bulls and Cows
- Motus
- Wordle-like
- autres jeux de déduction

Mais décision importante :

> En phase 1, on ne développe que Mastermind.  
> On évite le sur-design, mais on évite aussi de coupler le moteur à Mastermind.

---

# 2. Positionnement architectural

Le projet doit démontrer un niveau professionnel Java/Spring :

- Java 21
- Spring Boot 3
- DDD
- Clean Architecture
- Architecture hexagonale
- SOLID
- tests contractuels
- architecture modulaire
- documentation
- CI/CD
- Docker
- PostgreSQL
- Flyway
- WebSocket
- JWT
- frontend SvelteKit

Objectif :

> Ne pas faire un simple projet scolaire, mais un dépôt GitHub capable de montrer une vraie maturité d’architecture logicielle.

---

# 3. Nom du dépôt et structure générale envisagée

Nom recommandé :

```text
mastermind-arena
```

Et non :

```text
deduction-platform
```

Raison :

- `mastermind-arena` est clair, concret, démontrable ;
- `deduction-engine` reste le nom du cœur technique générique ;
- cela donne un meilleur équilibre entre produit visible et architecture avancée.

Structure envisagée :

```text
mastermind-arena/
│
├── deduction-domain/
├── deduction-engine/
├── deduction-application/
├── deduction-api/
├── deduction-persistence/
├── deduction-security/
├── deduction-websocket/
├── deduction-server/
│
├── games/
│   └── mastermind/
│
├── frontend/
├── docs/
├── docker/
├── scripts/
│
├── README.md
├── CHANGELOG.md
├── ROADMAP.md
├── CONTRIBUTING.md
└── LICENSE
```

---

# 4. Responsabilité des modules

## `deduction-domain`

Contient le modèle métier pur.

Règles :

- pas de Spring ;
- pas de JPA ;
- pas de Hibernate ;
- pas de PostgreSQL ;
- pas de REST ;
- pas de WebSocket.

---

## `deduction-engine`

Contient l’orchestration générique :

- cycle de vie d’un Match ;
- gestion des Turn ;
- validation structurelle ;
- application des directives structurelles ;
- propagation d’événements ;
- stockage/transport des résultats opaques.

Le moteur ne contient aucune logique spécifique à Mastermind.

---

## `deduction-application`

Contient les cas d’usage applicatifs :

- CreateMatch
- JoinMatch
- LeaveMatch
- StartMatch
- SubmitAction
- GetMatchState
- GetMatchHistory
- CancelMatch

Il coordonne domaine, moteur, ports, persistance, sécurité, etc.

---

## `deduction-api`

Contient :

- contrôleurs REST ;
- DTO ;
- validation d’entrée ;
- OpenAPI.

Pas de logique métier.

---

## `deduction-persistence`

Contient :

- repositories ;
- JPA ;
- Hibernate ;
- Flyway ;
- mapping persistence/domain.

---

## `deduction-security`

Contient :

- JWT ;
- authentification ;
- autorisation ;
- refresh tokens.

---

## `deduction-websocket`

Contient :

- lobby ;
- synchronisation temps réel ;
- notifications ;
- chat éventuel ;
- diffusion des événements de match.

---

## `deduction-server`

Module d’assemblage Spring Boot.

C’est lui qui démarre l’application.

---

## `games/mastermind`

Contient toute la logique spécifique au Mastermind :

- couleurs ;
- combinaison secrète ;
- validation des propositions ;
- calcul des pions noirs/blancs ;
- modes de jeu Mastermind ;
- IA Mastermind éventuelle ;
- variantes.

Règle essentielle :

> `games/mastermind` peut dépendre du moteur.  
> Le moteur ne dépend jamais de `games/mastermind`.

---

# 5. Décision centrale : moteur agnostique du métier

Décision très importante :

> Le moteur ne doit jamais interpréter les résultats métier produits par un jeu.

Donc on a rejeté l’idée d’un modèle générique du type :

```text
HintToken
Peg
Score générique
Feedback générique
```

Raison :

Cela aurait introduit une pseudo-généralisation prématurée et aurait forcé le moteur à comprendre des concepts qui appartiennent aux jeux.

À la place, on a décidé :

```text
EvaluationResult
```

est un payload opaque.

Le moteur peut :

- le transporter ;
- le stocker ;
- le propager ;
- le diffuser ;
- l’historiser.

Mais il ne peut jamais :

- lire ses champs ;
- en déduire une victoire ;
- en déduire une transition ;
- en comprendre la sémantique.

---

# 6. Ubiquitous Language v1 figé

Le vocabulaire métier/architecture retenu est :

```text
RuleSet
Action
ActionResolution
EngineDirective
EvaluationResult
MatchOutcome
CancellationReason
DomainEvent
Match
Turn
MatchState
ParticipantResult
```

---

## `RuleSet`

Composant spécifique à un jeu ou à un mode de jeu.

Responsable des décisions métier.

Exemple :

```text
MastermindRuleSet
```

C’est lui qui décide :

- si une action métier est valide ;
- si le tour continue ;
- si le match se termine ;
- si le joueur gagne ;
- quel résultat métier produire.

---

## `Action`

Intention soumise par un acteur pendant un tour.

Exemple Mastermind :

```text
SubmitGuessAction
```

Mais le moteur reste générique et ne comprend pas le contenu métier de cette action.

---

## `ActionResolution`

Réponse formelle du `RuleSet` à une `Action`.

Structure décidée :

```text
ActionResolution
├── EngineDirectives
├── EvaluationResult optionnel et opaque
├── DomainEvents
├── MatchOutcome optionnel
└── CancellationReason optionnel
```

---

## `EngineDirective`

Directive structurelle comprise par le moteur.

Liste v1 autorisée :

```text
ACCEPT_ACTION
REJECT_ACTION
CONTINUE_TURN
END_TURN
START_NEXT_TURN
FINISH_MATCH
CANCEL_MATCH
```

Règle importante :

> Les `EngineDirectives` sont strictement structurelles.  
> Elles ne doivent jamais contenir de sémantique propre à un jeu.

Interdit :

```text
CODE_BREAKER_WINS
SECRET_FOUND
WORD_GUESSED
COLOR_MATCHED
SCORE_REACHED
```

Autorisé :

```text
FINISH_MATCH
END_TURN
START_NEXT_TURN
```

---

## `EvaluationResult`

Résultat métier opaque.

Exemple côté Mastermind :

```text
MastermindEvaluationResult
├── exactMatches
├── colorOnlyMatches
├── remainingAttempts
└── secretFound
```

Mais le moteur ne lit jamais ces champs.

---

## `MatchOutcome`

Résultat final structurel d’un match terminé.

Utilisé uniquement avec :

```text
FINISH_MATCH
```

Structure retenue :

```text
MatchOutcome
├── status
├── completionReason
├── participantResults
└── completedAt
```

---

## `ParticipantResult`

Résultat individuel d’un participant dans un `MatchOutcome`.

Structure retenue :

```text
ParticipantResult
├── participantId
├── result
└── rank optionnel
```

On a préféré cette structure à :

```text
winnerIds / loserIds / draw
```

car elle couvre mieux :

- solo ;
- duel ;
- match en équipe ;
- tournoi ;
- égalité ;
- classement.

---

## `CancellationReason`

Raison structurelle d’une annulation.

Utilisée uniquement avec :

```text
CANCEL_MATCH
```

Décision importante :

```text
FINISH_MATCH exige MatchOutcome.
CANCEL_MATCH exige CancellationReason.
```

Et :

```text
Forfeit = FINISH_MATCH + MatchOutcome
Cancellation = CANCEL_MATCH + CancellationReason
```

Donc un forfait sportif n’est pas une simple annulation. C’est une fin de match avec résultat.

---

# 7. Invariants moteur figés

Les invariants suivants sont considérés comme centraux :

1. Le moteur ne calcule jamais la victoire.
2. Le moteur n’interprète jamais `EvaluationResult`.
3. Le moteur applique uniquement les `EngineDirectives`.
4. `FINISH_MATCH` sans `MatchOutcome` est invalide.
5. `CANCEL_MATCH` sans `CancellationReason` est invalide.
6. `FINISH_MATCH` et `CANCEL_MATCH` sont mutuellement exclusifs.
7. Un match terminal n’accepte plus de `SubmitAction`.
8. `expectedVersion` protège contre les écritures concurrentes.
9. `idempotencyKey` garantit qu’une action client ne produit pas deux effets.
10. Ajouter un nouveau jeu ne doit pas nécessiter de modifier le moteur si le contrat `RuleSet` est respecté.

Phrase clé retenue :

> Le moteur ne décide pas du métier ; il garantit l’exécution cohérente des décisions produites par le RuleSet.

---

# 8. Cas d’usage applicatifs v0

Les cas d’usage applicatifs retenus sont :

```text
CreateMatch
JoinMatch
LeaveMatch
StartMatch
SubmitAction
GetMatchState
GetMatchHistory
CancelMatch
```

---

## `CreateMatch`

Objectif :

Créer un match en état `Created` avec un `RuleSet` sélectionné.

Décision corrigée :

> Le créateur peut apparaître au plus une fois dans la liste des participants du match.

On a retiré l’invariant sur `participantResults` à ce stade, car il n’existe pas encore de `MatchOutcome`.

---

## `JoinMatch`

Objectif :

Permettre à un joueur de rejoindre un match ouvert avant démarrage.

Règles :

- impossible si match déjà `InProgress`, `Finished` ou `Cancelled` ;
- impossible de rejoindre deux fois le même match ;
- capacité min/max définie par le `RuleSet`.

---

## `LeaveMatch`

Objectif :

Permettre à un joueur de quitter un match.

Décision importante :

- en état `Created`, quitter peut être structurellement simple ;
- en état `InProgress`, le moteur ne décide pas seul de la conséquence.

Donc :

> En `InProgress`, `LeaveMatch` doit être résolu par le `RuleSet` ou par une `MatchPolicy` fournie par le jeu/mode.

Cela permet de gérer selon les modes :

- abandon ;
- forfait ;
- victoire adverse ;
- attente de reconnexion ;
- remplacement ;
- annulation.

---

## `StartMatch`

Objectif :

Passer un match de `Created` à `InProgress` et créer le premier `Turn`.

Événements attendus :

```text
MatchStarted
TurnStarted
```

---

## `SubmitAction`

Cas d’usage central.

Objectif :

Soumettre une action pendant un tour actif.

Cycle décidé :

```text
1. validation structurelle
2. délégation au RuleSet
3. réception ActionResolution
4. validation du contrat ActionResolution
5. application des EngineDirectives
6. persistance transactionnelle
7. écriture outbox
8. publication post-commit
9. mise à jour du read model
```

---

## `GetMatchState`

Objectif :

Lire l’état courant d’un match.

Décision :

> Ne pas exposer directement l’agrégat d’écriture.  
> Utiliser un read model léger.

---

## `GetMatchHistory`

Objectif :

Lire l’historique fonctionnel d’un match.

Décision :

> Lire depuis un modèle de lecture dédié, pas directement depuis l’agrégat brut.

---

## `CancelMatch`

Objectif :

Annuler un match non terminé.

Décision :

- `Finished` ne peut pas être annulé ;
- `CancelMatch` produit une `CancellationReason` ;
- pas de `MatchOutcome` requis ;
- un forfait doit passer par `FINISH_MATCH`, pas par `CANCEL_MATCH`.

---

# 9. Processus internes moteur

On a distingué les cas d’usage applicatifs des processus internes.

Les processus internes ne sont pas exposés comme use cases.

Liste retenue :

```text
ResolveAction
ApplyEngineDirectives
AdvanceTurn
FinishMatch
PersistEvaluationResult
PublishDomainEvents
```

---

## `ResolveAction`

Délégation au `RuleSet`.

Le moteur transmet :

- contexte structurel ;
- acteur ;
- action ;
- état du match ;
- tour courant.

Le `RuleSet` retourne une `ActionResolution`.

---

## `ApplyEngineDirectives`

Le moteur applique uniquement les transitions structurelles.

Ordre recommandé :

```text
ACCEPT_ACTION ou REJECT_ACTION
CONTINUE_TURN ou END_TURN
START_NEXT_TURN si END_TURN
FINISH_MATCH ou CANCEL_MATCH en terminal
```

---

## `AdvanceTurn`

Conséquence de :

```text
END_TURN + START_NEXT_TURN
```

---

## `FinishMatch`

Conséquence de :

```text
FINISH_MATCH + MatchOutcome
```

---

## `PersistEvaluationResult`

Stockage opaque du résultat produit par le jeu.

Le moteur ne lit pas le contenu.

---

## `PublishDomainEvents`

Publication via outbox après commit.

---

# 10. Rejets : distinction ENGINE vs RULESET

Décision importante :

```text
rejectionOrigin = ENGINE | RULESET
```

---

## Rejet `ENGINE`

Rejet structurel précoce.

Le `RuleSet` n’est pas appelé.

Exemples :

```text
MATCH_NOT_FOUND
MATCH_NOT_IN_PROGRESS
TURN_NOT_ACTIVE
ACTOR_NOT_AUTHORIZED
DUPLICATE_ACTION
VERSION_CONFLICT
```

Règle :

> Un rejet ENGINE ne doit pas polluer le Match History ni produire automatiquement un DomainEvent métier du match.

Il va plutôt vers :

```text
AUDIT_SECURITY_LOG
TECHNICAL_LOG
```

selon la nature.

---

## Rejet `RULESET`

Rejet métier.

Le `RuleSet` est appelé et retourne :

```text
REJECT_ACTION
```

avec :

```text
rejectionOrigin = RULESET
```

Exemples Mastermind :

```text
INVALID_GUESS_LENGTH
UNKNOWN_COLOR
DUPLICATE_COLOR_NOT_ALLOWED
GUESS_NOT_ALLOWED_FOR_ACTOR
```

Mais le moteur ne comprend pas le détail métier.

Selon policy, ce rejet peut apparaître dans :

```text
MATCH_HISTORY
DOMAIN_EVENT_LOG
```

---

# 11. Historisation et logs

Terminologie officielle figée :

```text
MATCH_HISTORY
DOMAIN_EVENT_LOG
AUDIT_SECURITY_LOG
TECHNICAL_LOG
```

---

## `MATCH_HISTORY`

Historique fonctionnel restituable du match.

Exemple :

- action acceptée ;
- action rejetée métier ;
- tour terminé ;
- match terminé.

---

## `DOMAIN_EVENT_LOG`

Journal des événements métier internes/publiables.

Exemples :

```text
ActionResolved
TurnEnded
MatchFinished
MastermindGuessEvaluated
```

---

## `AUDIT_SECURITY_LOG`

Traces liées à :

- autorisations ;
- accès refusés ;
- abus ;
- tentatives suspectes ;
- actions malveillantes.

---

## `TECHNICAL_LOG`

Traces liées à :

- violation de contrat ;
- exception technique ;
- rollback ;
- diagnostic runtime ;
- `RuleSet` défectueux.

---

# 12. Événements moteur retenus

Événements génériques moteur retenus :

```text
ActionSubmitted
ActionAccepted
ActionRejected
ActionResolved
TurnEnded
TurnStarted
MatchFinished
```

Décision importante :

`ActionRejected` n’est pas automatiquement un `DomainEvent` pour tous les rejets.

- rejet ENGINE : plutôt audit/technical log ;
- rejet RULESET : peut devenir événement/historique selon policy.

---

# 13. Événements spécifiques jeu

Les événements spécifiques jeu restent dans :

```text
games/*
```

Exemple Mastermind :

```text
MastermindGuessEvaluated
MastermindCodeBroken
```

Règle :

> Le moteur peut transporter et publier ces événements, mais il ne les interprète jamais.

---

# 14. Read model

Décision :

> Prévoir un read model dédié léger dès v0.

Mais :

```text
pas de CQRS lourd
pas d’event sourcing complet
pas de Kafka obligatoire
```

Objectif :

- éviter d’exposer les agrégats d’écriture ;
- simplifier `GetMatchState` ;
- simplifier `GetMatchHistory` ;
- préparer WebSocket ;
- faciliter pagination et projection API.

Modèles possibles plus tard :

```text
MatchView
MatchHistoryView
TurnView
ActionHistoryView
```

---

# 15. Outbox transactionnelle

Décision :

> Utiliser une outbox transactionnelle pour garantir la cohérence entre écriture métier et publication d’événements.

Pendant `SubmitAction`, dans une transaction unique :

```text
- mise à jour Match / Turn
- ajout historique
- stockage EvaluationResult opaque
- stockage MatchOutcome ou CancellationReason
- écriture DomainEvents dans l’outbox
```

Puis publication après commit.

Invariant :

> Aucun événement ne doit être publié si l’état correspondant n’a pas été persisté.

---

# 16. Idempotence et concurrence

Deux mécanismes sont retenus :

```text
idempotencyKey
expectedVersion
```

---

## `idempotencyKey`

Garantit qu’une même action client rejouée ne produit pas deux effets.

Cas attendu :

```text
Même idempotencyKey
→ même réponse observable
→ aucune nouvelle transition
→ aucun nouvel event
→ aucune double écriture
```

---

## `expectedVersion`

Protège contre les écritures concurrentes.

Cas attendu :

```text
Version obsolète
→ VERSION_CONFLICT
→ rejet ENGINE
→ aucune mutation du match
```

---

# 17. Plan de tests formel v1.1

On a décidé de transformer les règles d’architecture en tests exécutables.

Deux grandes suites :

```text
Contract RuleSet
Workflow SubmitAction
```

---

## Contract RuleSet

Objectif :

Vérifier qu’un `RuleSet` respecte le contrat attendu par le moteur.

Tests P0 :

```text
CR-P0-01 ActionResolution contient au moins une EngineDirective
CR-P0-02 FINISH_MATCH impose MatchOutcome
CR-P0-03 CANCEL_MATCH impose CancellationReason
CR-P0-04 FINISH_MATCH et CANCEL_MATCH exclusifs
CR-P0-05 Directives strictement structurelles seulement
CR-P0-06 Combinaisons de directives cohérentes
CR-P0-07 Rejet métier via REJECT_ACTION avec origin RULESET
```

Tests P1 :

```text
CR-P1-08 MatchOutcome structure générique valide
CR-P1-09 participantResults cohérents
CR-P1-10 EvaluationResult optionnel selon policy
CR-P1-11 DomainEvents spécifiques jeu propagables sans validation sémantique moteur
```

---

## Workflow SubmitAction

Tests P0 :

```text
WF-P0-01 ACCEPT_ACTION + CONTINUE_TURN
WF-P0-02 END_TURN + START_NEXT_TURN
WF-P0-03 FINISH_MATCH avec MatchOutcome
WF-P0-04 CANCEL_MATCH avec CancellationReason
WF-P0-05 MATCH_NOT_FOUND rejet ENGINE
WF-P0-06 MATCH_NOT_IN_PROGRESS rejet ENGINE
WF-P0-07 TURN_NOT_ACTIVE rejet ENGINE
WF-P0-08 ACTOR_NOT_AUTHORIZED rejet ENGINE
WF-P0-09 VERSION_CONFLICT zéro mutation
WF-P0-10 DUPLICATE_ACTION idempotence
WF-P0-11 Résolution sans directive rejetée techniquement
WF-P0-12 FINISH_MATCH sans MatchOutcome rejeté
WF-P0-13 CANCEL_MATCH sans CancellationReason rejeté
WF-P0-14 FINISH_MATCH + CANCEL_MATCH rejetés
WF-P0-15 Match terminal refuse SubmitAction
WF-P0-16 Atomicité write model + outbox
WF-P0-17 Publication post-commit
WF-P0-21 Combinaison incohérente directives rejetée
WF-P0-22 EvaluationResult opaque non interprété
```

Tests P1 :

```text
WF-P1-18 Rejet RULESET historisé selon policy
WF-P1-19 Read model cohérent
WF-P1-20 Tentatives malveillantes vers AUDIT_SECURITY_LOG
```

Test P2 :

```text
WF-P2-23 Comportement stable sous charge modérée multi-actions concurrentes
```

---

# 18. Combinaisons de directives interdites

On a décidé de tester explicitement les combinaisons incohérentes.

Exemples interdits :

```text
REJECT_ACTION + END_TURN
REJECT_ACTION + FINISH_MATCH
CONTINUE_TURN + END_TURN
START_NEXT_TURN sans END_TURN
FINISH_MATCH + START_NEXT_TURN
CANCEL_MATCH + START_NEXT_TURN
```

But :

> Empêcher un `RuleSet` défectueux de produire une résolution structurellement incohérente.

---

# 19. CI gates v1.1

Gates décidés :

## Gate 1 — Critique

```text
100% des tests P0 verts
```

---

## Gate 2 — Fiabilité

```text
Aucun test P0 flaky autorisé
```

---

## Gate 3 — Couverture ciblée

Décision importante :

> On privilégie la couverture des invariants P0 plutôt qu’un pourcentage global artificiel.

La couverture chiffrée est informative, pas bloquante au début.

---

## Gate 4 — Intégrité transactionnelle

Aucun échec autorisé sur :

```text
rollback
atomicité write model + outbox
```

---

## Gate 5 — Observabilité

Les logs doivent aller vers la bonne cible :

```text
MATCH_HISTORY
DOMAIN_EVENT_LOG
AUDIT_SECURITY_LOG
TECHNICAL_LOG
```

---

## Gate 6 — Performance CI

La suite P0 doit rester sous un seuil acceptable pour feedback rapide.

---

# 20. Plan d’exécution par sprints

Décision : avancer par lots de validation architecture.

---

## Sprint A — Fondations contrat P0

Scope :

```text
CR-P0-01 à CR-P0-07
```

Objectif :

Verrouiller le contrat minimal moteur/RuleSet.

---

## Sprint B — Workflows nominaux non terminaux

Scope :

```text
WF-P0-01
WF-P0-02
```

Objectif :

Valider `SubmitAction` sans terminalisation.

---

## Sprint C — Terminalisation nominale

Scope :

```text
WF-P0-03
WF-P0-04
WF-P0-15
```

Objectif :

Séparer strictement `FINISH_MATCH` et `CANCEL_MATCH`.

---

## Sprint D — Erreurs structurelles et idempotence

Scope :

```text
WF-P0-05 à WF-P0-10
```

Objectif :

Valider :

- court-circuit ENGINE ;
- optimistic locking ;
- idempotence ;
- non-pollution du Match History.

---

## Sprint E — Robustesse contrat et rollback

Scope :

```text
WF-P0-11 à WF-P0-14
WF-P0-21
```

Objectif :

Protéger le moteur contre un `RuleSet` incohérent.

---

## Sprint F — Transaction et outbox

Scope :

```text
WF-P0-16
WF-P0-17
```

Objectif :

Garantir atomicité et publication post-commit.

---

## Sprint G — Opacité EvaluationResult

Scope :

```text
WF-P0-22
```

Objectif :

Prouver que le moteur transporte sans interpréter.

---

## Sprint H — P1 lecture, logs, policies, events jeu

Scope :

```text
CR-P1-08 à CR-P1-11
WF-P1-18 à WF-P1-20
```

Objectif :

Valider :

- read model léger ;
- policies d’historisation ;
- propagation neutre des events jeu.

---

## Sprint I — P2 durcissement

Scope :

```text
WF-P2-23
```

Objectif :

Valider une robustesse minimale sous concurrence/charge modérée.

---

# 21. Definition of Done par sprint

Un sprint est terminé si :

1. Le scope du sprint est 100% vert en local et CI.
2. Aucun invariant P0 n’est violé.
3. Logs et historiques vont vers la bonne cible.
4. La documentation de test est mise à jour.
5. Aucun test flaky n’est introduit dans la suite P0.

---

# 22. Roadmap v2 : Architecture Fitness Functions

Décision :

Pas dans le scope immédiat v1, mais prévu ensuite avec ArchUnit.

Tests envisagés :

```text
deduction-domain ne dépend pas de Spring
deduction-domain ne dépend pas de JPA
deduction-engine ne dépend pas de games/mastermind
deduction-engine ne dépend pas de deduction-persistence
games/mastermind peut dépendre de deduction-engine
deduction-api ne contient pas de logique métier
deduction-persistence ne fuite pas ses entités JPA vers le domaine
```

C’est une excellente future étape pour renforcer le côté professionnel du projet.

---

# 23. Dernière recommandation avant création de fichiers

Claude a raison sur ce point :

> Avant de démarrer Sprint A, il faut figer un dictionnaire unique des rejection codes et de rejectionOrigin.

Donc la prochaine micro-décision à prendre avant le premier fichier serait probablement :

```text
Définir la nomenclature des rejection codes.
```

Avec deux familles :

```text
ENGINE rejection codes
RULESET rejection codes
```

Exemples `ENGINE` :

```text
MATCH_NOT_FOUND
MATCH_NOT_IN_PROGRESS
TURN_NOT_ACTIVE
ACTOR_NOT_AUTHORIZED
DUPLICATE_ACTION
VERSION_CONFLICT
INVALID_ACTION_FORMAT
RULESET_CONTRACT_VIOLATION
```

Exemples `RULESET` Mastermind plus tard :

```text
INVALID_GUESS_LENGTH
UNKNOWN_COLOR
DUPLICATE_COLOR_NOT_ALLOWED
GUESS_NOT_ALLOWED_FOR_ACTOR
ATTEMPT_LIMIT_REACHED
```

Mais les codes `RULESET` restent propres au jeu.

---

# Synthèse ultra-courte

On a décidé que :

```text
Mastermind Arena = produit visible
Deduction Engine = cœur générique
Mastermind = premier plugin jeu
```

Le moteur :

```text
orchestre les Match et Turn
applique des EngineDirectives
persiste et propage des payloads opaques
ne connaît jamais la logique Mastermind
ne calcule jamais la victoire
```

Le `RuleSet` :

```text
décide du métier
produit ActionResolution
fournit EvaluationResult
fournit MatchOutcome si FINISH_MATCH
fournit CancellationReason si CANCEL_MATCH
```

Le cœur du système est :

```text
Action
→ RuleSet
→ ActionResolution
→ EngineDirectives
→ transitions moteur
→ outbox
→ read model
```

Et avant de coder, on a déjà figé :

```text
Ubiquitous Language v1
Use cases v0
Cycle SubmitAction
Contrat RuleSet
Plan de tests v1.1
CI gates
Sprints de validation architecture
Roadmap ArchUnit v2
```

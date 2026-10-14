# Mastermind Arena — preuves mobilisables pour le CDA

Source : référentiel TP-01281, millésime 04, fourni par le concepteur, REAC et RE mis à jour le 24 mai 2023. Les pages PDF 54–57 décrivent la présentation et les productions attendues. Ce tableau n'est pas une validation de compétences par le jury.

Le titre permet de présenter plusieurs projets. Ne pas confondre dossier professionnel et dossier de projet : le RE indique un dossier de projet de 40 à 60 pages hors couverture, sommaire et annexes, pour l'ensemble des projets.

| Compétence | Éléments dans ce dépôt | Preuve à préparer / limite |
| --- | --- | --- |
| Installer et configurer l'environnement | Maven, Vite, exemples d'environnement, Docker Compose | Reproduire le démarrage sur une base de développement et expliquer les variables. |
| Développer des interfaces utilisateur | Composants Svelte, parcours simultané, thèmes et mobile | Faire valider les écrans, conserver maquettes/captures et vérifier accessibilité. |
| Développer des composants métier | Workflow, règles, phases, correction et score | Expliquer une transition et son test ; présenter les invariants. |
| Contribuer à la gestion du projet | Décisions, roadmap, évolution du périmètre | Ajouter planning réel, arbitrages, suivi et retour sur difficultés. |
| Analyser les besoins et maquetter | Décisions validées le 13 octobre, règles actuelles | Conserver les échanges de cadrage et valider le parcours/prototype avec le concepteur. |
| Définir l'architecture logicielle | API, application, métier, ports et JDBC | Montrer l'architecture actuelle et un diagramme de séquence réel. |
| Concevoir une base relationnelle | Migrations SQL, rooms, match_state, idempotency, Événements | Produire MCD/MPD et argumenter les contraintes ; un snapshot TEXT n'est pas un modèle relationnel détaillé des tours. |
| Accès aux données SQL et NoSQL | JDBC PostgreSQL et requêtes paramétrées | SQL présent. NoSQL non démontré : JSON/texte stocké dans PostgreSQL ne suffit pas. Compléter par un autre projet si nécessaire. |
| Préparer et exécuter les plans de tests | Tests JUnit, lint, Svelte Check, qualité Maven | Jeu d'essai entrées/attendu/obtenu, test de sécurité, tests PostgreSQL et parcours à deux clients. |
| Préparer et documenter le déploiement | Configurations, runbook, front/backend Vercel, Supabase | Vérifier la procédure réelle et documenter retour arrière, pause DB et versions compatibles. |
| Contribuer à la production DevOps | Déploiements depuis main déclarés par l'auteur ; workflow CI ajouté | Prouver une exécution CI et un déploiement réel ; l'ajout du YAML n'est pas la preuve d'une exécution. |

## Contribution personnelle et assistance

L'auteur a conçu le produit et arbitré ses règles. Il indique que l'écriture initiale du code a été quasi entièrement assistée par Copilot. La finalisation bénéficie Également de Codex. Les explications, les choix et les validations présentés au jury doivent Être maîtrisés par le candidat ; ne pas attribuer une rédaction manuelle ou une exécution personnelle de tests non effectuée.

## Livrables utiles

Expression des besoins, parcours/maquettes validés, diagrammes, modèle de données, extraits représentatifs, cas de test et test de sécurité, journal des validations, procédure de déploiement et captures de la version réellement publiée. Ces preuves sont plus importantes qu'une liste de technologies ambitieuses.

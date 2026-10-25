# Bilan de maintenance et de sécurisation — Mastermind Arena

Réconcilié le **25 octobre 2026**, cible de démonstration le **23 novembre 2026**. Ce document décrit ce qui est livré techniquement et ce qui reste non prouvé. La liste unique des actions restantes et leurs critères est dans [ROADMAP](ROADMAP.md), lots A à E. Les anciens lots 0 à 5 ne constituent plus un second planning à cocher.

Cadre : [AGENTS](../AGENTS.md), [CODE_QUALITY](CODE_QUALITY.md), [decision.md](../decision.md), [GAME_RULES](GAME_RULES.md). Sources de preuve : [VALIDATION](VALIDATION.md), [ARCHITECTURE_V2](ARCHITECTURE_V2.md) et documents spécialisés ci-dessous.

## État technique vérifié

| Ancien lot | Acquis local | Ce que cela ne prouve pas |
|---|---|---|
| 0 — Référence et confinement | Architecture et données privées décrites ; manche de référence ; protections SQL testées sur PostgreSQL isolé | Exposition réelle, Data API et permissions Supabase : B1/B2 |
| 1 — Caractérisation et contrat | Tests des phases simultanées, brouillons, confidentialité, autorisation par place, corrections, replay et réponses tardives ; CI front configurée | Exécution distante du workflow et version candidate : C1 |
| 2 — Découpage du front | Session, admission, polling, synchronisation/soumission, brouillons, vues, diagnostics et défilement séparés ; App compose et coordonne | Accessibilité globale et absence de tout défaut : A |
| 3 — Échanges et backend | Transport partagé, validation à l'exécution, erreurs métier dédiées, handlers séparés, concurrence/rollback, arrêt/redémarrage réel et compatibilité du consommateur actuel testés | Compatibilité des versions réellement publiées et retour arrière candidat : B1/C2 |
| 4 — Données et exploitation | Reconstruction, rôles distincts, formats versionnés, reçus compacts, purge SQL/Java V8, mesure de partie longue et dump/restauration isolés | Reset Supabase, sauvegardes de production, droits effectifs, protections contre les abus et cron actif : B/D |
| 5 — Démonstration et refonte | Principaux écrans intégrés et parcours réel à deux onglets après refonte ; compléments UX contrôlés | Validation par le concepteur, téléphone physique, autres appareils, revue complète et version publiée : A/D/E |

**Dernières commandes consignées :** 71 tests front, lint, Svelte Check avec 0 erreur/avertissement et build réussis ; 57 tests backend dont 25 PostgreSQL/HTTP, aucun ignoré, qualité Maven réussie. La suite backend du 25 octobre n'a pas été répétée pour les seuls compléments frontend suivants. Les commandes détaillées et leurs limites sont dans VALIDATION ; aucun nouveau passage n'est revendiqué par cette réconciliation documentaire.

## Comportements et protections livrés

- Jeu autonome Java/JDBC et Svelte/Vite, polling conservé ; règles simultanées et indices manuels préservés.
- Autorisation serveur par place et code privé, secrets adverses et actions de phase masqués. Clé VITE publique, distincte d'un droit de jouer.
- Validation stricte HTTP et contenus métier : types, compteurs, combinaisons, doublons et JSON supplémentaire ; réponses incompatibles refusées sans inventer de plateau.
- Contexte de partie/acteur/jeton protégé contre les réponses tardives ; requêtes partagées/annulables, retries et conflits de version caractérisés.
- État, événements et idempotence persistés atomiquement ; concurrence et rollback testés sur PostgreSQL isolé.
- Reçus compacts conservant la réponse HTTP d'origine après correction, revanche et restauration. Réduction mesurée de 99,52 % du JSON brut des résultats sur le scénario de 402 actions, pas garantie de capacité distante.
- Migrations V1–V8 et formats persistants explicités ; refus d'une conversion implicite des anciennes données de test. Rôles de migration/exécution et refus des rôles API testés localement.
- Purge après sept jours depuis la dernière action acceptée : événements et reçus en cascade, verrou partagé avec les actions, recontrôle et transaction par partie. Procédure commune Java/futur cron ; exécution quotidienne choisie, non activée.
- Redémarrage réel de Java avec reprise privée, puis sauvegarde/restauration sur base isolée : état, score, replay et revanche vérifiés.

Références : [API_CONTRACT](API_CONTRACT.md), [WORKFLOW_CONTRACT](WORKFLOW_CONTRACT.md), [LOCAL_CONTRACT_CHECK](LOCAL_CONTRACT_CHECK.md), [ERROR_MESSAGES](ERROR_MESSAGES.md), [DATABASE_REBUILD](DATABASE_REBUILD.md), [DATABASE_ROLES](DATABASE_ROLES.md), [DATA_RETENTION](DATA_RETENTION.md), [DATABASE_RECOVERY](DATABASE_RECOVERY.md), [PERFORMANCE_BASELINE](PERFORMANCE_BASELINE.md).

## Refonte et UX : périmètre acquis

Les vues d'entrée/salon/attente/reprise, navigation du duel, joueurs/score, préparation, essai, attentes, indices, correction, abandon, résumés, historique et panneau privé sont intégrés. Les thèmes clair/sombre et les icônes Tabler locales MIT sont conservés.

Parcours réel Java/PostgreSQL après refonte : correction qui rouvre la recherche et invalide les consentements, nouvel essai, indices manuels, fin et revanche commune avec score conservé. Égalité, abandon et reprise également contrôlés. Cela ne vaut pas reprise sur deux appareils physiques.

Compléments sur API synthétique : attente lente/en erreur et salon vide ; historique de 400 actions sur 100 tours ; noms longs de l'attente/historique et huit largeurs sans débordement ; focus des destinations approuvées ; lecture lente, erreur et retry de l'historique sans perte des données.

Déjà réglés : notifications de correction/vérification jusqu'à fermeture, information de conservation près du code privé, indices initiaux et corrections successives via originalFeedback. Ne pas remettre ces décisions dans les questions ouvertes. Restent les notifications de soumission, le message de partie indisponible, les scénarios précis A2/A4/A5 et la revue du concepteur.

Le [suivi UI](UI_REDESIGN.md) et [design-qa](../design-qa.md) précisent les régions comparées et les écarts. Le panneau diagnostic existant et les surfaces d'erreur ne valent pas une refonte intégrale et validée des planches 16/17. Aucun verdict global sur toutes les maquettes ni conformité WCAG complète n'est acquis.

## Limites de production et autorisations

Les versions Vercel réellement publiées, la Data API, les propriétaires et droits Supabase, les sauvegardes disponibles et la configuration de promotion restent à vérifier. Préparer une procédure n'active pas une protection distante.

Le concepteur autorise une reconstruction initiale sans conservation des anciennes parties de test, à effectuer lorsque la version sera prête. Le reset n'est pas réalisé. Cette exception ne dispense pas de protéger les futures parties, sauf expiration selon la règle validée.

Un push sur main peut publier les deux projets. La réconciliation de ces documents n'autorise aucun push, déploiement, changement Supabase ou déclenchement de purge. Les tâches C/D nécessitent leurs autorisations correspondantes. [DEPLOYMENT_COMPATIBILITY](DEPLOYMENT_COMPATIBILITY.md) décrit les conditions de bascule et de repli.

## Champs d'action et critères d'intervention

| Champ | Travail autorisé dans un lot local | Limite |
|---|---|---|
| Front | Composants, brouillons, navigation, transport, tests et styles nécessaires | Aucun changement des règles pendant le refactoring |
| API | Contrat, validation, erreurs, autorisation et confidentialité | Changement de contrat explicité et validé si nécessaire |
| PostgreSQL | Schéma, formats, droits et rétention sur base isolée | Production et données distantes distinctement autorisées |
| Validation | Tests ciblés, qualité, parcours et preuves de rendu | Nommer chaque contrôle impossible ou ignoré |
| Documentation | État courant, décisions, preuves et reste à faire | Aucune proposition présentée comme livraison ou compétence CDA acquise |

Exécuter les contrôles pertinents de CODE_QUALITY. Préserver le travail Git existant et les profils .env ; aucun secret dans les diagnostics. Pas de nouvelle dépendance, abstraction, migration ou découpage sans bénéfice concret démontré.

Les anciens calendriers de lots sont remplacés par les critères de ROADMAP. Le nettoyage n'est pas un chantier indéfini : ne rouvrir un acquis qu'en présence d'une régression, d'un défaut observé ou d'une décision nouvelle.

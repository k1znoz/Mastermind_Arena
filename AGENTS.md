# Cadre des interventions — Mastermind Arena

Ces instructions s'appliquent à tout le dépôt. Les instructions explicites du concepteur prévalent. Le cadre est conservé dans le dépôt, pas dans ses mémoires personnelles.

## Sources à lire

- `decision.md` et `docs/GAME_RULES.md` : décisions et invariants du produit.
- `docs/MAINTENANCE_ROADMAP.md` : lots, champs d'action et critères de sortie.
- `docs/CODE_QUALITY.md` : exigences de conception, sécurité et validation.
- `docs/VALIDATION.md` : preuves obtenues et vérifications encore ouvertes.
- `docs/ARCHITECTURE_V2.md` : architecture actuelle. Les archives décrivent l'historique.

Lire les éventuelles instructions plus spécifiques du dossier concerné. Vérifier l'état Git avant une modification ; préserver le travail existant et ne pas annuler les changements étrangers au lot.

## Périmètre

L'autorisation d'implémenter un lot couvre ses changements locaux nécessaires : source, tests, configuration de développement, documentation et correction des régressions introduites. Elle ne déclenche pas automatiquement tous les autres lots. Une demande de revue ou de documentation ne déclenche pas de changement du comportement de l'application.

Ne pas modifier les règles pendant un refactoring. Ne pas ajouter de calcul automatique des indices, de limite de tours, de comptes ou de plateforme générique. Clarifier une ambiguïté produit tout en poursuivant les travaux indépendants de la réponse.

Ne pas demander de nouvelle confirmation pour les opérations locales réversibles déjà autorisées. Préparer les changements et leurs preuves avant une éventuelle validation finale de publication. Un push sur `main` peut déployer les deux projets Vercel : vérifier l'autorisation dans la conversation avant publication. Toute opération sur Supabase de production, ses permissions ou ses données nécessite une autorisation correspondante. Utiliser une base isolée pour les essais.

## Méthode obligatoire

1. Identifier le lot, annoncer son objectif, lire le code exécuté et ses appelants.
2. Définir le comportement préservé, les risques et les vérifications. Caractériser par des tests les comportements sensibles non couverts avant de les déplacer.
3. Faire un changement cohérent et limité. Séparer refactoring, évolution du contrat et changement visuel lorsque leur mélange rend la revue difficile.
4. Exécuter les contrôles pertinents de `docs/CODE_QUALITY.md`. Corriger les échecs sans assouplir une assertion ou désactiver une règle pour obtenir artificiellement un succès.
5. Relire le diff, vérifier l'absence de secrets et contrôler le rendu si l'interface a changé. Actualiser les documents affectés.
6. Livrer un bilan : changement, raison, preuves, limites et travail restant. Cocher une tâche seulement lorsque ses critères sont satisfaits.

## Contraintes

- Le serveur autorise une place ; une clé `VITE_*` est publique. Le code de reprise est un secret.
- Ne jamais afficher les valeurs des `.env`, mots de passe, jetons ou codes secrets dans les diagnostics. Conserver les profils existants. Ne pas diriger les tests vers Supabase.
- Une réponse asynchrone ne doit pas modifier une autre partie ou identité que celle qui a émis la requête.
- Préserver l'atomicité état/événements/idempotence et la protection contre les écritures concurrentes.
- Ne pas supprimer un lecteur de données historiques ou du code ancien sans vérifier références et compatibilité.
- Exception validée le 14 octobre 2026 : la base actuelle est jetable ; aucune reprise des parties existantes n'est requise. Il est permis de préparer un nouveau schéma/format sans conserver les anciens lecteurs après vérification de leurs usages. Ne pas confondre ce reset initial avec une autorisation de supprimer les futures parties actives.
- Ne pas ajouter de dépendance ou d'abstraction sans bénéfice concret. Aucun changement de framework n'est prévu.
- Ne pas utiliser de sous-agents sans demande explicite ou instruction applicable autorisant la délégation.

## Définition de terminé

Les critères du lot sont satisfaits, les commandes pertinentes ont une sortie capturée, les tests nécessaires ne sont pas ignorés silencieusement et la documentation décrit ce qui est livré. Une compilation locale ne prouve ni une exécution CI distante, ni une validation PostgreSQL, ni un déploiement réussi.

Si une vérification est impossible, nommer la condition manquante et garder le critère ouvert. Ne pas qualifier l'application de totalement sécurisée ou sans bugs.

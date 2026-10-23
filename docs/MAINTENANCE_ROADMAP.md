# Roadmap de maintenance et de sécurisation

Établie le 14 octobre 2026 pour la démonstration du 23 novembre 2026. Suivi réconcilié le 23 octobre avec [les preuves de validation](VALIDATION.md). Complète [la roadmap de finalisation](ROADMAP.md). Les cases cochées correspondent aux travaux réalisés dans le périmètre précisé ; les autres restent à terminer.

État au 23 octobre : lots 1 et 2 réalisés localement ; lot 3 en cours. Transport front partagé, réponses et entrées HTTP contrôlées, handlers séparés, RLS testée sur PostgreSQL isolé et installation JSON propre préparée. Dernière exécution consignée le 18 octobre : 40 tests backend, dont 12 PostgreSQL/HTTP, 57 tests front et parcours combiné des vrais clients front contre Java/JDBC réussis. Ces suites ne sont pas réexécutées par la présente mise à jour documentaire.

| Lot | État | Limite principale |
|---|---|---|
| 0 — Référence et confinement | Référence locale acquise ; confinement distant ouvert | Data API, rôle JDBC et permissions Supabase non vérifiés |
| 1 — Caractérisation et contrat | Réalisé localement | Exécution CI distante non vérifiée |
| 2 — Découpage du front | Réalisé localement | Revue UX/accessibilité complète au lot 5 |
| 3 — Échanges et backend | En cours | Erreurs/messages, métriques, redémarrage réel et compatibilité des déploiements |
| 4 — Données et exploitation | Partiel | Droits séparés, conservation, mesures, restauration et retour arrière |
| 5 — Démonstration | À finaliser | Parcours navigateur final, production, mentions légales et preuves CDA |

Les modifications sont encore présentes dans l'arbre de travail, avec de nombreux fichiers non suivis. Préparer des commits cohérents et vérifiables avant publication ; cette roadmap ne constitue ni un commit ni une preuve de déploiement.

## Objectif et champs d'action

Le concepteur autorise le 14 octobre une base reconstruite de zéro, sans reprise des parties actuelles. Le lot données peut donc remplacer les formats hérités au lieu de maintenir leur compatibilité. Les bases et parties de la future version restent soumises aux règles de sauvegarde et de conservation.

Rendre le code compréhensible, les changements vérifiables et les données mieux protégées. Préserver le jeu autonome, Java/JDBC, Svelte/Vite et le polling tant qu'aucune mesure ne justifie un autre transport.

| Champ | Intervention | Limite |
|---|---|---|
| Front | Session, synchronisation, soumission, brouillons, composants et CSS | Règles préservées ; refonte visuelle majeure discutée séparément |
| API | Contrat, validation, erreurs, autorisation et confidentialité | Pas de réécriture générale ni de changement de framework |
| PostgreSQL | Permissions, migrations, compatibilité, conservation | Tests isolés ; opérations de production distinctement autorisées |
| Tests et CI | Comportements sensibles, intégration et parcours | Contrôle statique distinct de preuve fonctionnelle |
| Exploitation | Profils, déploiement, retour arrière et restauration | Pas de publication automatique des lots locaux |
| Documentation | Décisions, architecture réelle et preuves | Pas de capacité future présentée comme compétence CDA acquise |

## Lots et jalons indicatifs

Les dates sont un planning, pas une garantie. Les critères de sortie priment. En cas de retard, réduire les évolutions facultatives plutôt que supprimer les vérifications essentielles.

### Lot 0 — Référence et confinement (14–18 octobre)

- [x] Documenter l'architecture exécutée, les points d'entrée HTTP et les données privées : [ARCHITECTURE_V2](ARCHITECTURE_V2.md), [API_CONTRACT](API_CONTRACT.md), autorisation par place et masquage vérifiés. Cet inventaire ne constitue pas une revue exhaustive de tout le code historique.
- [x] Capturer une manche de référence à deux clients sur JDBC local : correction du résultat, reprise et revanche incluses. Parcours navigateur consigné le 15 octobre, complété le 18 par le script des vrais clients front contre Java/JDBC ; nouvelle validation visuelle finale au lot 5.
- [ ] Confirmer le statut de la Data API Supabase. Les captures reçues la montrent activée ; sa désactivation n'est pas vérifiée.
- [ ] Identifier rôle JDBC, propriétaires et permissions des cinq tables en production Supabase ; fonctionnement du propriétaire JDBC et refus des rôles API déjà testés sur PostgreSQL isolé.
- [x] Définir les refus d'accès attendus et préparer une migration de protection compatible avec JDBC, puis ses tests (V4, PostgreSQL isolé ; vérification Supabase encore ouverte).

**Sortie :** référence reproductible, échanges et données sensibles identifiés, exposition connue et stratégie de permissions testable. Si la Data API est inutile, préparer sa désactivation ; l'opération en production reste distinctement autorisée. Ne pas attendre le refactoring pour traiter une exposition confirmée.

### Lot 1 — Tests de caractérisation et contrat (18–23 octobre)

- [x] Documenter les routes réelles : admission, reprise, état, soumission et erreurs — [API_CONTRACT](API_CONTRACT.md).
- [x] Ajouter une commande de tests front et son étape CI (`npm test`, runner Node intégré ; exécution CI distante non vérifiée).
- [x] Tester les cases remplies hors ordre, les transitions simultanées, la correction des scores et la revanche : tests front/métier/JDBC et parcours à deux clients consignés les 14 et 15 octobre.
- [x] Tester réponse tardive après changement de partie ou de jeton, conflit de version et réponse perdue après écriture acceptée.
- [x] Compléter les tests HTTP de confidentialité et de corrections non autorisées : secrets, actions cachées même après notification, auteur de correction et usurpation de place vérifiés sur PostgreSQL isolé le 15 octobre.

**Sortie :** scénarios sensibles protégés par des attentes explicites. Une régression pertinente fait échouer un test. Aucun changement des règles pour faciliter les tests ; une correction de bug est distinguée du refactoring.

### Lot 2 — Découpage du front (23 octobre–1er novembre)

- [x] Extraire session/reprise et stockage navigateur (repository injecté).
- [x] Extraire la coordination d'admission (création, entrée, reprise, liste et attente) avec garde de contexte et annulation ; parcours à deux clients PostgreSQL vérifié le 15 octobre.
- [x] Extraire polling, annulation, resynchronisation et soumission (`polling.js`, `gameConnection.js`, cycle de vie piloté par App).
- [x] Extraire les calculs de vue et les brouillons en fonctions testables (`gameView.js`, `boardDraft.js`).
- [x] Réduire `App.svelte` à composition, navigation et coordination des écrans : diagnostics et positionnement après rendu isolés et testés le 15 octobre ; styles de la coque conservés localement.
- [x] Clarifier entrées/sorties de `PartieScreen` : une vue dérivée commune et des événements d'intention ; calculs dupliqués retirés.
- [x] Remplacer les couches CSS historiques par styles locaux et tokens partagés ; rendu comparé avant/après sur plateau mobile/desktop, historique et lobby mobiles le 15 octobre. Accessibilité complète encore ouverte.

**Sortie :** logique réseau/session indépendante du DOM, états métier non dupliqués, tests conservés, rendu mobile/desktop et clair/sombre contrôlé. Évaluer les responsabilités séparées, pas une limite arbitraire de lignes.

### Lot 3 — Échanges et backend (1er–8 novembre)

- [x] Centraliser requêtes, timeout et lecture JSON (`requestJson`) ; configuration des clients du jeu dédupliquée le 18 octobre. Interprétation des rejets conservée par endpoint pour préserver les métadonnées de conflit.
- [ ] Harmoniser les erreurs et messages front : distinguer indisponibilité, timeout, annulation, réponse vide/illisible/incompatible, accès refusé et rejet métier ; conserver le replay exact et ne pas afficher de contenu serveur arbitraire.
- [x] Valider les réponses consommées à l'exécution ; distinguer vide, incompatible et indisponible. Métadonnées/listes, admission, succès, contenus imbriqués et rejets contrôlés le 18 octobre ; parcours des vrais clients front contre HTTP/JDBC réussi via `verify-local-contract.mjs`. Parcours visuel final encore ouvert au lot 5.
- [x] Extraire les handlers HTTP, en préservant l'autorisation côté serveur : `GameHttpHandlers`, `RoomHttpHandlers`, `HttpExchangeSupport` et `HttpObservability` ; protocole caractérisé avant déplacement puis suite qualité réussie.
- [ ] Clarifier états, actions, événements et codes d'erreur.
  - Types des enveloppes POST contrôlés au 18 octobre : pas de coercition scalaire, version obligatoire. Documents HTTP et JSON métier encodés refusent doublons et contenu supplémentaire ; paramètres GET répétés refusés après décodage. Validation métier et codes existants préservés ; harmonisation complète des erreurs et métriques encore ouverte.
- [x] Vérifier replay exact, conflit de version, rollback, concurrence et relecture via de nouvelles instances JDBC ; preuves des 14–18 octobre. Cela ne vaut pas arrêt/redémarrage du processus serveur.
- [ ] Vérifier une reprise après arrêt/redémarrage réel de l'API sur base isolée : même place privée, état/version/score conservés et replay sans double écriture.
- [ ] Préparer la compatibilité du contrat entre deux déploiements séparés.

**Sortie :** fixtures vérifiées des deux côtés, erreurs cohérentes, secrets exclus, intégration/concurrence passées. Aucun contrat incompatible publié sans transition.

Pour clôturer le lot 3, obtenir des preuves ciblées : messages front testés pour les catégories ci-dessus ; codes et métriques cohérents pour les routes utilisées ; scénario reproductible d'arrêt/redémarrage ; stratégie documentée d'ordre de publication et de retour arrière pour les deux projets, avec vérification du comportement en cas de versions incompatibles. Un nouveau parcours navigateur devra confirmer les messages visibles ; les tests HTTP seuls ne le remplacent pas.

### Lot 4 — Données et exploitation (8–15 novembre)

- [x] Appliquer les migrations une fois, avec trace ; refus d'une conversion implicite des anciennes parties.
- [x] Définir un format persistant versionné et reconstruire le schéma local `arena_v1` ; ancienne lecture non requise selon décision du 14 octobre. Bascule Supabase non exécutée.
- [ ] Distinguer les droits de migration et d'exécution.
- [ ] Définir conservation et purge des parties inactives, journaux et clés d'idempotence.
- [ ] Mesurer partie longue et polling ; optimiser un coût démontré.
- [ ] Éprouver restauration sur base isolée et documenter déploiement/retour arrière.

**Sortie :** migration/restauration reproductibles, reprise privée conservée, suppression contrôlée et mesures de référence. Ne pas purger une partie active ou limiter les tours pour résoudre un problème de performance.

### Lot 5 — Stabilisation de la démonstration (16–22 novembre)

- [ ] Revue du code et de l'UX avec le concepteur.
- [ ] Parcours complet PostgreSQL puis production après publication autorisée.
- [ ] Vérifications clavier, contraste, téléphone, coupure réseau et reprise sur un autre appareil.
- [ ] Compléter mentions légales/confidentialité avec des informations confirmées.
- [ ] Capturer les preuves CDA et répéter la présentation avec secours local.

**Sortie :** version identifiée, validations consignées et scénario de secours éprouvé. Privilégier les corrections aux nouvelles fonctionnalités.

## Priorités et suivi

Priorité : exposition ou corruption du résultat, protection contre les régressions, découpage, puis exploitation. Ce plan n'engage pas une conversion TypeScript intégrale, une réécriture relationnelle complète, WebSocket, comptes ou plateforme multi-jeux.

Ordre de travail retenu au 23 octobre : terminer les erreurs/messages du lot 3, puis redémarrage et compatibilité de publication ; vérifier le confinement Supabase dès que l'accès et l'autorisation correspondants sont disponibles ; préparer restauration/retour arrière et protections contre les abus avant ouverture large ; reprendre ensuite la revue UX/accessibilité, les mentions légales et les preuves CDA. Les contrôles distants de sécurité restent prioritaires sur les améliorations facultatives. Les plages de dates des lots sont les jalons initiaux, pas les dates réelles d'exécution.

Pour chaque lot, consigner dans `VALIDATION.md` : date, périmètre, commit ou état local, environnement, commandes, résultats, parcours manuels, limites et prochaines actions. Déplacer explicitement une tâche non terminée ; ne pas la cocher pour tenir une date.

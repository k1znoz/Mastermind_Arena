# Roadmap de maintenance et de sécurisation

Établie le 14 octobre 2026 pour la démonstration du 23 novembre 2026. Suivi réconcilié le 24 octobre avec [les preuves de validation](VALIDATION.md). Complète [la roadmap de finalisation](ROADMAP.md). Les cases cochées correspondent aux travaux réalisés dans le périmètre précisé ; les autres restent à terminer.

État au 24 octobre : lots 1 et 2 réalisés localement ; critères locaux du lot 3 validés, vérification des versions distantes encore ouverte. Dernière preuve backend consignée : 56 tests, dont 25 PostgreSQL/HTTP, qualité Maven réussie ; dernière suite front à 62 tests. Purge commune SQL/Java V8, reçu compact et restauration éprouvés sur bases isolées. Supabase Cron quotidien retenu, activation au déploiement. Refonte locale en cours : accueil, salon, attente, reprise dédiée et navigation responsive du duel intégrés ; bandeau, plateaux et autres écrans restent ouverts. Aucun déploiement de cette refonte. Les preuves et limites sont détaillées dans VALIDATION.

| Lot | État | Limite principale |
|---|---|---|
| 0 — Référence et confinement | Référence locale acquise ; confinement distant ouvert | Data API, rôle JDBC et permissions Supabase non vérifiés |
| 1 — Caractérisation et contrat | Réalisé localement | Exécution CI distante non vérifiée |
| 2 — Découpage du front | Réalisé localement | Revue UX/accessibilité complète au lot 5 |
| 3 — Échanges et backend | Critères locaux réalisés ; vérification distante ouverte | Versions réellement publiées non identifiées/vérifiées ; inventaire courant, messages, redémarrage et compteurs acquis localement |
| 4 — Données et exploitation | Partiel | Configuration Supabase, cron, sauvegardes planifiées, retour arrière des versions candidates et protections contre les abus |
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
- [x] Harmoniser les erreurs et messages front localement : traduction partagée, quinze refus métier vérifiés par tests front et HTTP/JDBC ; annulation et replay préservés. Complément navigateur du 24 octobre : 32 contrôles de synchronisation (huit catégories × téléphone/ordinateur × clair/sombre) et huit captures de soumission longue/inconnue. API synthétique, code de timeout injecté pour son rendu uniquement ; [matrice et limites](screenshots/ERROR_CHECKS.md). Parcours final, accessibilité et production restent au lot 5.
- [x] Valider les réponses consommées à l'exécution ; distinguer vide, incompatible et indisponible. Métadonnées/listes, admission, succès, contenus imbriqués et rejets contrôlés le 18 octobre ; parcours des vrais clients front contre HTTP/JDBC réussi via `verify-local-contract.mjs`. Parcours visuel final encore ouvert au lot 5.
- [x] Extraire les handlers HTTP, en préservant l'autorisation côté serveur : `GameHttpHandlers`, `RoomHttpHandlers`, `HttpExchangeSupport` et `HttpObservability` ; protocole caractérisé avant déplacement puis suite qualité réussie.
- [x] Clarifier les états, actions, événements et codes du chemin exécuté : [WORKFLOW_CONTRACT](WORKFLOW_CONTRACT.md), quatre états, sept commandes, quatre événements effectivement émis et 21 refus métier ; admission/protocole, erreurs locales et noms historiques distingués. Retrait de DECLARE_DISCOVERY approuvé et testé en 400/INVALID_REQUEST, sans écriture. Revue statique du périmètre actif, pas une suppression globale des modules historiques ni une couverture de toutes les branches en production.
  - Défaut reproduit puis corrigé le 24 octobre après accord sur l'exception métier dédiée : pannes techniques en soumission et documents stockés invalides renvoient 500, refus métier préservés. Tests initialement rouges désormais réussis, injection de panne à chaque écriture avec rollback, 52 tests backend dont 21 PostgreSQL/HTTP et qualité Maven réussis. Matrice des erreurs de protocole dans [ERROR_MESSAGES](ERROR_MESSAGES.md), preuves dans [VALIDATION](VALIDATION.md).
  - Revue du 24 octobre : quinze refus publics traduits après accord du concepteur, sans changement de règle, statut ou retry ; [ERROR_MESSAGES](ERROR_MESSAGES.md). Tests front, parcours réel HTTP/JDBC et contrôle des surfaces d'erreur sur téléphone/ordinateur clair/sombre réalisés. Inventaire courant complété séparément dans WORKFLOW_CONTRACT ; ne pas confondre cette revue avec la preuve de rendu.
  - Types des enveloppes POST contrôlés au 18 octobre : pas de coercition scalaire, version obligatoire. Documents HTTP et JSON métier encodés refusent doublons et contenu supplémentaire ; paramètres GET répétés refusés après décodage. Refus métier préservés ; seul le retrait de l'ancienne commande DECLARE_DISCOVERY change son code de refus après accord explicite le 24 octobre.
- [x] Compléter la couverture des compteurs des handlers : lobby, refus de place, attente et OPTIONS comptés une fois. Test HTTP/PostgreSQL le 23 octobre, 41 tests backend dont 13 PostgreSQL/HTTP réussis. Compteurs locaux et durée indicative ; pas de supervision distribuée ni de mesure réseau revendiquée.
- [x] Vérifier replay exact, conflit de version, rollback, concurrence et relecture via de nouvelles instances JDBC ; preuves des 14–18 octobre. Cela ne vaut pas arrêt/redémarrage du processus serveur.
- [x] Vérifier une reprise après arrêt/redémarrage réel de l'API sur base isolée : `verify-local-restart.mjs` PASS le 23 octobre. Deux PID Java successifs, reprise des deux places privées, état/version/score conservés, replay exact sans changement et revanche après reprise ; procédure dans [LOCAL_CONTRACT_CHECK](LOCAL_CONTRACT_CHECK.md). Restauration de sauvegarde et redémarrage distant non couverts.
- [x] Préparer la compatibilité du contrat entre deux déploiements séparés : [DEPLOYMENT_COMPATIBILITY](DEPLOYMENT_COMPATIBILITY.md), ordre de publication, anciennes pages, transition initiale et conditions de retour arrière. Test du consommateur actuel : champ public additif accepté, rupture refusée sans remplacer le plateau, récupération après retour au contrat valide. Versions réellement publiées, promotion Vercel et test avec ces versions restent ouverts avant production au lot 5.

**Sortie :** fixtures vérifiées des deux côtés, erreurs cohérentes, secrets exclus, intégration/concurrence passées. Aucun contrat incompatible publié sans transition.

Messages front, inventaire du workflow courant, compteurs des routes utilisées, arrêt/redémarrage et stratégie locale de compatibilité disposent de preuves ciblées. Le complément navigateur du 24 octobre contrôle les surfaces communes d'erreur sur téléphone/ordinateur et deux thèmes. Pour clôturer le lot 3 dans son ensemble, identifier/vérifier les versions effectivement publiées. La revue UX/accessibilité complète et le parcours final de production restent distincts au lot 5.

### Lot 4 — Données et exploitation (8–15 novembre)

- [x] Appliquer les migrations une fois, avec trace ; refus d'une conversion implicite des anciennes parties.
- [x] Définir un format persistant versionné et reconstruire le schéma local `arena_v1` ; ancienne lecture non requise selon décision du 14 octobre. Bascule Supabase non exécutée.
- [x] Distinguer les droits de migration et d'exécution localement : commande propriétaire `DatabaseMigrationMain`, provisionnement ciblé et API avec `APP_DB_MIGRATE_ON_START=false`, vérification des versions sans DDL. [DATABASE_ROLES](DATABASE_ROLES.md). Rôle restreint testé via HTTP et refus SQLSTATE 42501 ; configuration et permissions Supabase encore à vérifier/appliquer après autorisation.
- [x] Livrer la commande administrative de purge : sept jours depuis la dernière action acceptée, parties non terminées incluses ; lecture/reconnexion/replay sans prolongation. V6 sur installation vide, événements et idempotence rattachés, suppression en cascade par partie, aperçu sans suppression et recontrôle sous le verrou des actions. Tests PostgreSQL de borne, concurrence, rollback, accès et isolation réussis le 23 octobre ; commande réelle et rejeu contrôlés sur base jetable. [DATA_RETENTION](DATA_RETENTION.md).
- [x] Choisir le déclenchement périodique : Supabase Cron quotidien, accord du 24 octobre ; activation au déploiement après reset et vérification du schéma.
- [x] Préparer et tester la procédure SQL commune V8 : Java et futur cron appellent la même logique, verrou commun aux actions, recontrôle d'expiration, cascade et permissions. Huit tests de conservation PostgreSQL ; transaction par partie et arrêt sur erreur préservés, migration depuis V7 sans suppression et rôle API sans EXECUTE vérifiés.
- [ ] Configurer puis vérifier une exécution réelle de Supabase Cron au déploiement, avec procédure de désactivation. Aucune tâche planifiée ni purge en production activée.
- Information des joueurs et messages après expiration transférés au lot 5, à la demande du concepteur le 24 octobre ; aucun texte ni emplacement approuvé pour ajout immédiat. Rétention des journaux d'infrastructure et sauvegardes encore ouverte ; reset initial à exécuter lorsque la version sera prête, sans reprise des anciennes données.
- [x] Mesurer partie longue et polling : 100 tours, 400 actions visibles, 40 lectures par point ; mesures HTTP/JDBC et volumes dans [PERFORMANCE_BASELINE](PERFORMANCE_BASELINE.md). Règles et confidentialité préservées, aucune mesure de capacité distante revendiquée.
- [x] Optimiser le coût démontré de l'idempotence : reçu compact approuvé puis livré localement, format 2 avec V7 sur installation vide. Réponse HTTP initiale préservée après correction/revanche, aucun renouvellement d'activité sur replay. Comparaison sur 402 résultats : JSON brut de 13 003 156 à 61 794 octets, soit environ 99,52 % de moins ; état courant et corps HTTP inchangés. Qualité Maven, tests front, restauration et replay après revanche réussis le 24 octobre ; détails et limites dans [PERFORMANCE_BASELINE](PERFORMANCE_BASELINE.md). Reset et déploiement non effectués.
- [x] Éprouver restauration sur base isolée et documenter déploiement/retour arrière : `verify-local-restart.mjs --restore` PASS le 23 octobre, pg_dump/pg_restore vers une nouvelle base, volumes des cinq tables, places privées, état/score, replay et revanche contrôlés. [DATABASE_RECOVERY](DATABASE_RECOVERY.md) et [DEPLOYMENT_COMPATIBILITY](DEPLOYMENT_COMPATIBILITY.md). Sauvegardes planifiées, objectifs de reprise et restauration Supabase restent à définir/vérifier avant production.

**Sortie :** migration/restauration reproductibles, reprise privée conservée, suppression contrôlée et mesures de référence. Ne pas purger une partie active ou limiter les tours pour résoudre un problème de performance.

### Lot 5 — Stabilisation de la démonstration (16–22 novembre)

- [ ] Intégrer à la refonte UX l'information sur les sept jours d'inactivité et les messages de partie indisponible. Textes et emplacement à définir dans ce lot ; report explicitement demandé le 24 octobre. Ne pas affirmer qu'une partie a été purgée lorsque l'API ne permet pas de le distinguer.

- [ ] Revue du code et de l'UX avec le concepteur.

Étape reprise dédiée livrée localement : planche 13 intégrée dans RecoveryScreen, accès accueil/salon, visibilité du code, retour avec focus, refus et admission fictive vérifiés. 62 tests front réussis ; annulation et réponse tardive après sortie/réouverture couvertes. Conservation des saisies de création au retour approuvée et implémentée en mémoire ; création protégée après retour et effacement lors d'une sortie normale/rechargement contrôlés. Validation avec le vrai backend toujours ouverte ; détails dans UI_REDESIGN et VALIDATION.

Référence de refonte : 17 planches ordinateur/mobile ; source et correspondance dans [UI_REDESIGN](UI_REDESIGN.md). Accueil, salon, attente et reprise dédiée livrés localement. Navigation du duel livrée : barre latérale à partir de 851 px, barre basse en dessous, destinations Plateau/Historique, retour au salon et thème conservés. 62 tests front, lint/check/build réussis ; huit tailles et petit écran avec bouton atteint au clavier contrôlés avec API fictive. Périmètre de comparaison dans [design-qa](../design-qa.md). Prochain travail : bandeau pseudos/score et préparation du secret, puis propositions/indices, historique et panneau privé. Les contenus de jeu actuels ne sont pas encore refondus ; parcours réels et validations distantes restent ouverts.
- [ ] Revoir la durée et la persistance des notifications de soumission : le délai actuel de 2,4 secondes peut gêner la lecture d'un refus long. Constat lors du contrôle des messages, aucune évolution de comportement décidée ou appliquée.

Le concepteur demande le 23 octobre de terminer les travaux techniques avant la revue/refonte visuelle. Cette dernière utilisera le workflow Product Design ; aucune direction graphique nouvelle n'est appliquée pendant les lots d'exploitation.
- [ ] Parcours complet PostgreSQL puis production après publication autorisée.
- [ ] Vérifications clavier, contraste, téléphone, coupure réseau et reprise sur un autre appareil.
- [ ] Compléter mentions légales/confidentialité avec des informations confirmées.
- [ ] Capturer les preuves CDA et répéter la présentation avec secours local.

**Sortie :** version identifiée, validations consignées et scénario de secours éprouvé. Privilégier les corrections aux nouvelles fonctionnalités.

## Priorités et suivi

Priorité : exposition ou corruption du résultat, protection contre les régressions, découpage, puis exploitation. Ce plan n'engage pas une conversion TypeScript intégrale, une réécriture relationnelle complète, WebSocket, comptes ou plateforme multi-jeux.

Ordre de travail retenu au 23 octobre : terminer les erreurs/messages du lot 3, puis redémarrage et compatibilité de publication ; vérifier le confinement Supabase dès que l'accès et l'autorisation correspondants sont disponibles ; préparer restauration/retour arrière et protections contre les abus avant ouverture large ; reprendre ensuite la revue UX/accessibilité, les mentions légales et les preuves CDA. Les contrôles distants de sécurité restent prioritaires sur les améliorations facultatives. Les plages de dates des lots sont les jalons initiaux, pas les dates réelles d'exécution.

Pour chaque lot, consigner dans `VALIDATION.md` : date, périmètre, commit ou état local, environnement, commandes, résultats, parcours manuels, limites et prochaines actions. Déplacer explicitement une tâche non terminée ; ne pas la cocher pour tenir une date.

# Exigences de qualité et de sécurité

Cadre pour les futures implémentations, à lire avec [AGENTS.md](../AGENTS.md) et [la roadmap de maintenance](MAINTENANCE_ROADMAP.md). Ces exigences ne déclarent pas la version actuelle conforme à tous les points.

## Invariants

Préserver les règles de `GAME_RULES.md` : quatre cases, six couleurs, répétitions et essais illimités ; phases simultanées masquées ; indices manuels ; contestation informative ; correction du dernier tour par son auteur avec recalcul des scores et invalidation des accords de revanche ; poursuite sans point supplémentaire ; abandon, revanche commune et reprise privée. Aucun abandon automatique après déconnexion.

Le serveur est la source du résultat métier. Modifier ces règles exige une décision produit distincte du nettoyage.

## Front et état

`App.svelte` compose et coordonne. Session et synchronisation ne manipulent pas le DOM. Les fonctions de plateau ne font ni requête ni écriture navigateur. Les composants reçoivent des données de vue et émettent des intentions.

Éviter plusieurs copies mutables de la même vérité. Distinguer explicitement version d'état, manche et tour ; tester les conversions d'index. Un brouillon garde quatre positions, même rempli hors ordre : ne pas le compacter par suppression des cases vides.

Utiliser tokens communs et styles locaux. Supprimer les règles CSS remplacées après vérification de leurs usages ; ne pas accumuler les surcharges. Ne pas ajouter d'abstractions ou dépendances sans usage concret.

Les variables de thème, couleurs de pions et règles communes d'accessibilité sont dans `app.css`. Les styles d'un écran ou contrôle appartiennent à son composant Svelte. Exprimer une variation partagée par une propriété (par exemple `compact`) plutôt qu'un sélecteur global traversant plusieurs composants. Lors d'un déplacement CSS, comparer le rendu effectif : la spécificité et l'ordre des anciennes surcharges peuvent changer le résultat malgré des déclarations identiques.

## Contrat et requêtes

Documenter les routes utilisées : méthode, chemin, headers, champs obligatoires, types, valeurs, unités, codes HTTP et erreurs stables. Maintenir des exemples nettoyés et fixtures vérifiées par le mapper serveur et le lecteur front. Un support OpenAPI est possible s'il est réellement maintenu.

Valider entrées et réponses à l'exécution. Une réponse incompatible n'est pas une liste vide ni un état plausible. Chaque fallback historique a une justification et une condition de retrait.

- Distinguer échec HTTP, erreur métier, timeout et JSON illisible ; ne pas afficher les détails internes à l'utilisateur.
- Le timeout couvre aussi la lecture du corps. Annuler les requêtes devenues inutiles au changement de contexte ou à la destruction.
- Capturer match, acteur, jeton et version au départ ; vérifier le contexte avant toute mutation ou notification.
- Partager une promesse d'actualisation en cours ou annuler explicitement ; un retour anticipé ne signifie pas que l'état est actualisé.
- Refuser une réponse plus ancienne dans la même partie ; ne pas multiplier les timers ou les requêtes après navigation.

Une clé d'idempotence désigne une opération et son contenu exact. Le retry identique garde sa clé. Une requête reconstruite avec une version ou un contenu différents ne réutilise pas cette clé comme si elle était identique. Après réponse perdue, vérifier l'identité de l'opération appliquée avant renvoi ; un compteur d'actions du même type n'est pas une preuve suffisante.

## Sécurité des échanges

Toutes les entrées navigateur sont non fiables. Valider côté serveur acteur, place, jeton, phase, auteur de correction, compteurs et taille du payload. CORS n'autorise pas une place. Une clé `VITE_*` est publique et ne protège pas seule une partie.

Traiter la reprise comme un secret porteur : pas d'URL, log, export ou capture publique contenant le code. Conserver les empreintes de jetons en base. Documenter le stockage navigateur et prévoir une action pour oublier une place sur appareil partagé, sans empêcher la reprise sur un autre appareil.

Construire les réponses pour l'acteur autorisé. Exclure secret adverse, jetons et actions adverses encore masquées. Tester l'ensemble de l'historique, notamment une notification ou correction pendant une phase masquée. La révélation suit exclusivement les règles du jeu.

Vérifier séparément Data API Supabase et permissions SQL. Préparer une protection reproductible de `rooms`, `match_state`, `idempotency`, `workflow_event`, `schema_version`. Identifier rôle JDBC et propriétaires avant RLS ; pas de politique autorisant tout, ni de `FORCE ROW LEVEL SECURITY` sans test de ses effets. Tester refus des rôles API et fonctionnement du rôle serveur sur une base isolée représentative ; PostgreSQL local n'a pas automatiquement les rôles Supabase.

Ne pas exposer connexion JDBC ou clé Supabase privilégiée au front. Requêtes paramétrées, identifiants SQL validés et TLS distant. Séparer progressivement droits de migration et droits d'exécution.

Définir des contrôles d'abus sur création/jointure et tentatives de codes avant une ouverture large. Une limite mémoire par instance n'est pas une protection globale. Les logs contiennent route, identifiant de requête, statut, code stable et durée ; jamais credentials, jetons ou secrets. Base64 n'est pas du chiffrement.

## Backend et données

Les handlers traduisent HTTP, le service coordonne, le métier décide et les adaptateurs persistent. Les erreurs métier attendues ont un code explicite ; éviter les captures génériques sans traitement adapté.

Les refus attendus du plateau utilisent `ActionRejectedException` avec un code public constant. Ne pas utiliser ce type pour publier un diagnostic JDBC ou de stockage. Le service ne traduit que ces refus ; une panne pendant l'exécution reste un incident 500. La validation de commande à l'entrée reste distincte, et un document persistant invalide n'est pas une erreur de saisie du joueur. Conserver les tests de panne de lecture, de décodage et de rollback d'écriture avant toute modification de cette frontière.

Préserver une transaction commune état/événements/idempotence. Tester rollback et concurrence entre deux instances. Ne pas retirer le `synchronized` global sans examiner les adaptateurs et protéger leurs accès.

Les migrations ont une identité et une trace d'application. Préparer la transition du migrateur actuel ; ne pas rejouer une transformation non idempotente au démarrage ni modifier rétroactivement une migration publiée.

Versionner explicitement le format persistant et tester aller-retour, données invalides et lecture des formats encore nécessaires. Retirer un lecteur historique seulement après preuve de migration ou décision sur la reprise de ces données.

La décision du 14 octobre autorise la reconstruction de la base actuelle sans reprise : pas de test de compatibilité des parties jetables exigé pour cette transition. Tester en revanche reconstruction, nouveau format et redémarrage. Les futurs formats publiés nécessiteront une stratégie d'évolution explicite.

Mesurer le coût d'une partie longue avant optimisation. Distinguer parties actives, archives et logs. Définir les durées de conservation avec le concepteur, puis tester purge et restauration. Ne pas supprimer une partie active ou un secret nécessaire à sa reprise.

## Contrôles requis

| Changement | Vérification |
|---|---|
| Documentation seule | Liens, cohérence, diff et absence de secrets ; pas de suite complète inutile |
| Calcul/état front | Tests ciblés, lint, Svelte Check et build |
| Session/polling/soumission | Identité, réponses tardives, retries, conflit puis parcours à deux clients |
| Composant/CSS | Contrôles front, mobile/desktop, clair/sombre ; clavier des commandes modifiées |
| Métier | Invariants et transitions affectés, qualité Maven |
| API/mapper | Contrat et confidentialité via HTTP réel, contrôles des deux côtés |
| JDBC/permissions | PostgreSQL isolé, autorisation, rollback, concurrence et reprise |
| Migration/codec | Données historiques, application selon contrat, restauration et reprise |
| Publication | CI réellement passée, versions compatibles, smoke test et retour arrière |

```powershell
# Dans App_Front
npm run lint
npm run check
npm run build
# À la racine
mvn -Pquality clean verify
```

Les tests PostgreSQL nécessitent `ARENA_TEST_DB_URL`, `ARENA_TEST_DB_USER`, `ARENA_TEST_DB_PASSWORD` sur une base jetable. Sinon ils sont ignorés : l'annoncer et ne pas revendiquer une validation JDBC. Les tests front sont exécutés par `npm test` (runner Node intégré), également ajouté à la CI. L'exécution distante du workflow reste à vérifier.

Privilégier les comportements qui protègent des erreurs réelles, sans taux de couverture arbitraire ni tests reproduisant simplement l'implémentation. Ne pas ignorer une suite en échec pour livrer.

## Livraison

Fournir périmètre, comportement préservé, résultats capturés, limites et documents actualisés. Vérifier le rendu sur la version effectivement examinée. Retirer commentaires trompeurs, code mort démontré et diagnostics sensibles. Une capacité prévue n'est pas une preuve CDA acquise ; une validation technique n'est pas une certification juridique ni une garantie de sécurité totale.

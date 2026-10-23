# Plan de validation de la finalisation

## Revue documentaire du 23 octobre 2026

- Réconciliation des deux roadmaps avec les preuves des 14–18 octobre et l'état local : référence navigateur/JDBC reconnue, transport partagé séparé de l'harmonisation des erreurs encore ouverte, replay/relecture via de nouvelles instances distingués du redémarrage réel du serveur à éprouver.
- Synthèse d'avancement et critères restants du lot 3 ajoutés ; tâches de confinement Supabase, CI distante, restauration, compatibilité de publication, UX/accessibilité, mentions légales et livrables CDA maintenues ouvertes. Les dates des lots restent les jalons initiaux ; elles ne prouvent pas une exécution.
- Dernières suites consignées : celles du 18 octobre ci-dessous, sans nouvelle exécution le 23. Cette intervention modifie uniquement la documentation ; contrôle des liens locaux, cohérence des statuts et diff. Aucun commit, push, déploiement ou accès Supabase réalisé. Nombreux changements locaux et fichiers non suivis toujours présents ; préparation de commits à faire avant publication.

## Maintenance du 18 octobre 2026 — JSON métier et paramètres GET ambigus

- Deux tests ajoutés avant correction et exécutés en échec : un contenu après une combinaison JSON était ignoré ; deux roomId dans une URL renvoyaient 200 en retenant le dernier. Lecture stricte activée pour les combinaisons, indices et cibles de correction/contestation : doublons de clés et contenu après le document refusés, espaces périphériques et codes métier existants conservés. Aucun calcul d'indices ni règle de résultat modifiés.
- Le lecteur des paramètres GET refuse les noms répétés après décodage URL au lieu d'écraser la première valeur. Test HTTP réel sur roomId, matchId, actorId, valeurs identiques ou différentes et noms encodés ; réponses exactes 400/INVALID_REQUEST. Noms/valeurs encodés une seule fois acceptés, usurpation d'acteur toujours refusée en 403, version inchangée. Les comportements existants des paramètres absents et de la liste sont conservés.
- `mvn -Pquality verify` : 40 tests réussis, dont douze PostgreSQL/HTTP, aucun ignoré ; PMD, SpotBugs (zéro anomalie) et analyse de dépendances réussis. `npm test` : 57 réussis. `verify-local-contract.mjs` PASS avec les vrais clients front contre API :8082 et schéma `json_integration` du PostgreSQL jetable :54330, parcours à deux joueurs incluant correction, contestation, abandon, revanche et reprise.
- Diff vérifié ; API et conteneur jetables arrêtés. Aucun changement des profils, de Supabase ou des déploiements. Interface inchangée, pas de nouveau contrôle visuel. Ce lot traite les ambiguïtés identifiées, sans prétendre à une validation exhaustive de tous les paramètres possibles. Harmonisation des messages, métriques, compatibilité des déploiements et exploitation restent ouvertes.

## Maintenance du 18 octobre 2026 — types des requêtes HTTP entrantes

- Nouveau test PostgreSQL/HTTP exécuté avant correction et en échec : une version mal typée atteignait les traitements métier au lieu d'être refusée comme requête invalide. `RequestBodyContract` contrôle désormais les types de l'enveloppe de soumission, de l'objet action et des champs texte des trois admissions avant conversion Jackson. Les valeurs par défaut implicites, nombres transformés en texte et versions décimales tronquées ne sont plus acceptés sur ces champs.
- Version obligatoire, entière JSON, positive ou nulle et dans l'intervalle long ; identifiants/clé d'idempotence/type d'action obligatoires et non blancs. Payload et feedback restent chaînes ou null/absents. Champs du lobby absents/null et validations métier conservés. Lecture du document HTTP externe stricte : clés dupliquées (y compris dans actionPayload) et contenu après l'objet refusés ; limite de 64 Kio conservée.
- Test réel : types null/booléen/tableau/objet/nombre/chaîne inadéquats, champs manquants, dépassement long, action mal typée, noms/pseudos/codes numériques, doublons et objets concaténés. Réponses exactes 400/INVALID_REQUEST, aucune nouvelle room, version inchangée, puis commande valide acceptée.
- `mvn -Pquality verify` : 38 tests réussis, dont onze PostgreSQL/HTTP, aucun ignoré. PMD, SpotBugs (zéro anomalie) et analyse de dépendances réussis. `npm test` : 57 tests front réussis. Parcours `verify-local-contract.mjs` PASS contre API dédiée :8082 et schéma `input_integration` du PostgreSQL jetable :54330. API et conteneur arrêtés après validation ; profils existants et Supabase inchangés.
- Périmètre limité aux entrées HTTP POST courantes, sans changement des règles ou formats valides. La syntaxe stricte des JSON encodés dans les chaînes métier, les paramètres GET, l'harmonisation des messages front, les métriques et les contrôles d'abus restent ouverts. Aucun changement d'interface, contrôle visuel ou déploiement pour cette étape.

## Maintenance du 18 octobre 2026 — transport front partagé

- Les trois clients utilisent `requestJson` pour la requête bornée par timeout et la lecture JSON. La vérification de configuration des deux clients du jeu est partagée dans `httpClientUtils`. Routes, headers, contenu des commandes, codes d'erreur, métadonnées de conflit et clés d'idempotence conservés. Les erreurs restent interprétées selon leur endpoint ; aucune nouvelle politique de retry.
- Deux tests de caractérisation ajoutés et exécutés avant déplacement : erreurs de lecture (y compris corps vide ou texte interne non JSON) et configuration des clients du jeu. Les tests existants couvrent timeout pendant le corps, annulation, réponses incompatibles, reprises et changements d'identité.
- Après déplacement : 57 tests front réussis, aucun ignoré ; lint, Svelte Check (zéro erreur/avertissement, configuration Vite chargée hors sandbox), build et vérification du diff réussis.
- `verify-local-contract.mjs` : PASS avec les vrais clients front contre Java/JDBC sur 127.0.0.1:8082, schéma `transport_integration` du PostgreSQL jetable :54330. Deux joueurs, replay, conflit, confidentialité, correction, contestation, abandon, revanche et reprise vérifiés. API et conteneur dédiés arrêtés après contrôle.
- Aucun changement métier, composant, CSS ou backend ; suites Maven et parcours visuel non rejoués pour ce déplacement front. Profils existants, Supabase et déploiements non modifiés. L'harmonisation complète des codes/messages et la distinction plus fine des corps vides/illisibles restent ouvertes ; les fallbacks existants sont conservés.

## Maintenance du 18 octobre 2026 — erreurs backend et parcours HTTP combiné

- Nouveau test PostgreSQL/HTTP d'abord en échec : un corps `null` envoyé à certains handlers pouvait fermer la connexion sans réponse exploitable. Lecture des corps bornés conservée, mais objet JSON requis : vide, null, tableau, booléen et JSON mal formé renvoient désormais 400/INVALID_REQUEST sur création, jointure, reprise et soumission. Les erreurs gardent Content-Type JSON et identifiant de requête ; aucune mutation de la partie testée.
- Frontière commune d'erreur pour les handlers du jeu : erreur d'argument en 400/INVALID_REQUEST, exception d'exécution inattendue en 500/SERVER_ERROR. Les handlers de parties distinguent également une requête incorrecte d'un incident interne, avec 500/ROOM_SERVER_ERROR. Codes métier existants conservés ; messages internes inconnus non publiés ni repris comme codes de diagnostic.
- Panne simulée uniquement dans un schéma jetable par renommage de tables : lecture d'état en 500/SERVER_ERROR, liste/statut/reprise en 500/ROOM_SERVER_ERROR, sans détail SQL dans les corps. Le endpoint de santé HTTP reste disponible ; il ne constitue pas un contrôle de disponibilité PostgreSQL.
- `mvn -Pquality verify` : 37 tests réussis, dont dix PostgreSQL/HTTP, aucun ignoré. PMD, SpotBugs (zéro anomalie) et analyse de dépendances réussis. `npm test` : 55 tests front réussis. `git diff --check` réussi.
- Ajout et exécution de `scripts/verify-local-contract.mjs` contre une API Java dédiée sur 127.0.0.1:8082, schéma `contract_integration` du PostgreSQL jetable sur :54330. Les vrais clients front valident liste/statut/admission, replay exact, rejet de version périmée sur une autre intention, secrets/essais/indices, victoire, correction retirant le point, contestation sans révélation prématurée, abandon, revanche et reprise. Résultat PASS. Le premier scénario de conflit a été corrigé pour différencier nouvelle intention et action déjà reconnue comme appliquée ; l'assertion de rejet est conservée.
- Procédure reproductible dans [LOCAL_CONTRACT_CHECK](LOCAL_CONTRACT_CHECK.md). Aucun navigateur connecté n'était disponible dans l'inventaire de l'outil : ce parcours automatisé HTTP/JDBC n'est pas une validation visuelle. Le parcours navigateur reste à rejouer après durcissement. Pas de modification de l'interface ni des profils `.env` ; la base de développement :54329 et Supabase ne sont pas visés. API dédiée et conteneur jetable arrêtés après contrôle.
- Restent ouverts : politique uniforme de métriques, validation détaillée des champs entrants, protections contre les abus, compatibilité entre déploiements, CI distante et exploitation. Aucune revendication de sécurité exhaustive ni de déploiement.

## Maintenance du 18 octobre 2026 — actions imbriquées et rejets

- Les champs JSON imbriqués de l'historique sont validés selon l'action : quatre couleurs A–F pour une proposition ; compteurs entiers positifs ou nuls dont la somme ne dépasse pas quatre pour les indices ; cible de correction/contestation entière positive et antérieure à la notification. READY_SECRET est refusé dans l'historique public. Aucun calcul automatique des indices ajouté.
- Retrait des parsers qui remplaçaient un contenu invalide par un tableau vide, tronquaient les décimaux ou bornaient silencieusement les compteurs. Une réponse incompatible est rejetée en entier ; les indices valides sont convertis exactement en pions BLACK/WHITE pour l'affichage.
- Les rejets de soumission sont contrôlés avant attachement aux erreurs : code stable, origine ENGINE/RULESET, liste d'événements et contexte absent ou lié à la partie demandée. Les erreurs de transport `{code}` restent distinctes. VERSION_CONFLICT n'est exploitable par le client que dans une enveloppe ENGINE valide et avec HTTP 409 ; une erreur incompatible ne reçoit pas ces métadonnées et ne déclenche pas le retry de conflit.
- Fixtures `turn-correction.json` et `action-version-conflict.json` partagées avec Java : record de tour et mapper du rejet moteur, statut 409 vérifié. `npm test` : 55 tests réussis, aucun ignoré. Lint, Svelte Check (zéro erreur/avertissement), build et `git diff --check` réussis. Aucun changement d'interface, route ou dépendance.
- `mvn -pl deduction-engine -Pquality -Dtest=SharedContractTest verify` : trois tests ciblés réussis ; PMD, SpotBugs et analyse de dépendances réussis. Pas de tests PostgreSQL exécutés pour cette étape ; aucun code serveur d'exécution ni SQL modifié.
- Limites : parcours combiné navigateur/PostgreSQL à deux joueurs et erreurs backend encore à vérifier/harmoniser, en particulier les requêtes invalides. La validation reste structurelle : le serveur conserve l'autorité métier. Compatibilité entre déploiements et CI distante non validées. Aucune modification de production ni des profils locaux.

## Maintenance du 18 octobre 2026 — réponses du lobby, admission et soumission

- Validation runtime des métadonnées et listes de parties : identifiant attendu, noms non vides, types, timestamp entier, cohérence présence du deuxième joueur/statut et absence de doublons. Une liste vide valide reste distincte d'une réponse incompatible. Une réponse de statut erronée n'est pas transmise au callback qui remplace la partie.
- Admissions contrôlées selon l'opération : création p1, jointure p2 dans la partie demandée, reprise de la partie/place/jeton contenus dans le code privé. Les réponses restent de même format ; les champs historiques non présents dans les mocks ont été alignés sur le contrat Java réel (locked et createdAt).
- Succès de soumission contrôlé : accepted, matchId, version entière supérieure à la version attendue, statut reconnu, absence de rejet et liste d'événements. Une réponse incompatible reste ambiguë pour une écriture : le mécanisme existant peut rejouer la même opération avec la même clé, sans créer une nouvelle intention.
- Messages distincts pour réponse incompatible, partie absente et code de reprise invalide. Les erreurs inconnues restent génériques ; aucune donnée privée ajoutée aux diagnostics.
- Deux fixtures supplémentaires (admission et action acceptée), avec uniquement des valeurs de test, vérifiées par sérialisation des records Java et consommées via les clients front. `npm test` : 49 réussis, aucun ignoré ; lint, Svelte Check (zéro erreur/avertissement), build et `git diff --check` réussis.
- `mvn -pl deduction-engine -Pquality -Dtest=SharedContractTest verify` : deux tests ciblés réussis, PMD, SpotBugs et analyse de dépendances réussis. Ce n'est pas une nouvelle exécution des neuf tests PostgreSQL : aucun accès base nécessaire pour ces fixtures.
- Limites : pas de parcours navigateur à deux joueurs rejoué pour cette étape. Les tests transport/coordination et fixtures ne remplacent pas cette vérification finale. Harmonisation des erreurs backend, validation des contenus JSON imbriqués des actions et des rejets, CI distante et compatibilité de déploiement restent ouvertes. Aucune règle du jeu, aucun `.env`, aucune donnée ni production modifiés.

## Maintenance du 15 octobre 2026 — handlers HTTP et confidentialité du lot 3

- Serveur principal limité à l'assemblage, l'initialisation et l'enregistrement des routes. Extraction package-local de `GameHttpHandlers`, `RoomHttpHandlers`, `HttpExchangeSupport` et `HttpObservability`. Simplification du try/catch/finally de soumission, fermeture des échanges conservée après réponse. Aucun changement de règle, route, payload, permission SQL ou dépendance.
- Deux tests PostgreSQL/HTTP ajoutés. Avant déplacement : neuf tests PostgreSQL réussis. Après déplacement : 34 tests backend réussis, dont neuf PostgreSQL, aucun ignoré ; `mvn -Pquality clean verify` réussi, puis `mvn -Pquality verify` réussi après simplification finale et extension du scénario de confidentialité. PMD, SpotBugs (zéro anomalie) et analyse de dépendances réussis.
- Test de confidentialité réel : secret propre visible, secret adverse absent, secrets exclus de l'historique et jetons exclus des métadonnées publiques ; essai puis indices adverses cachés avant publication. Une correction et une contestation du tour précédent ne révèlent pas l'essai en cours ; une nouvelle correction ne révèle pas les indices en cours. Refus 403 d'une correction par un autre auteur et d'une usurpation de place, sans modification de l'état ni du journal d'événements.
- Test de protocole réel : OPTIONS autorisé sans clé (204, corps vide), origine et identifiant de requête retournés ; origine non admise refusée sans header CORS, clé manquante refusée, mauvaise méthode refusée, santé publique opérationnelle.
- Base exclusivement jetable : PostgreSQL 16 sur 127.0.0.1:54330, distinct de la base de développement sur :54329. Le conteneur jetable est arrêté après validation. Ni profils `.env`, ni données de développement, ni Supabase modifiés.
- `npm test` : 41 tests front réussis, aucun ignoré, dont la fixture commune au mapper Java. Pas de modification du front dans cette étape ; rendu et compilation front non revérifiés inutilement. `git diff --check` réussi.
- Limites : harmonisation exhaustive des erreurs/métriques, validation complète des réponses, compatibilité entre déploiements, protections contre les abus et exploitation restent ouvertes. Ces tests ne couvrent pas tous les scénarios d'attaque. Aucun déploiement ni exécution CI distante revendiqués ; la prévisualisation locale déjà lancée n'est pas une preuve de redémarrage de la nouvelle version.

## Maintenance du 15 octobre 2026 — diagnostics et défilement du lot 2

- Extraction de `utils/diagnostics.js` et de `state/phaseFocus.js`. App conserve les callbacks de coordination ; DebugDrawer importe son formateur. Suppression de l'option de démasquage, sans effet sur des instantanés déjà masqués, remplacée par une explication. Les instantanés sont vidés lors d'une admission ou d'un oubli local.
- Renforcement explicite du masquage : les champs privés connus (jetons, autorisation, clé API, mot de passe, secret, code d'accès ou de reprise) sont masqués même si leur valeur est numérique, tableau ou objet. La source n'est pas modifiée. Ce filtrage de champs JSON ne prétend pas détecter un secret dans n'importe quel texte libre.
- Le défilement vérifie à nouveau le contexte après rendu. Navigation, nouvelle demande et destruction annulent l'ancien positionnement. Conservation des sélecteurs de phase, de la marge sous la barre fixe et de l'absence de défilement répété dans la même phase. Respect du mouvement réduit ajouté ; le focus clavier n'est pas déplacé.
- `npm test` : 41 tests réussis, aucun ignoré (huit nouveaux : deux diagnostics et six contrôleur de défilement). Svelte Check : zéro erreur et avertissement. Lint, build et `git diff --check` réussis après intégration ; aucune dépendance ajoutée.
- Navigateur avec PostgreSQL local, deux clients : entrée du joueur 2, préparation des secrets, essais et passage aux indices manuels vérifiés. Écran court 390 × 600 : phase d'indices positionnée à 52 px sous une barre de 44 px, soit la marge attendue de 8 px. L'historique activé au clavier reste affiché après les actualisations suivantes. Diagnostic inspecté en clair et sombre, champs `visibleSecretCode` et `opponentSecretCode` masqués dans la réponse réelle. Captures `screenshots/phase-focus-mobile.jpg` et `screenshots/diagnostics-redacted-mobile.jpg`.
- Les courses après navigation et le mouvement réduit sont validés par tests contrôlés ; préférence système et course réseau réelle non simulées dans Chrome. Le parcours victoire/correction/revanche complet conserve sa preuve de l'étape d'admission, sans être revendiqué comme rejoué ici. Backend et suites Maven non modifiés/relancés ; CI distante, accessibilité exhaustive et production restent ouverts. Aucun push ni modification Supabase.

## Maintenance du 15 octobre 2026 — nettoyage des styles du lot 2

- Suppression des anciennes couches globales sans consommateur : plateaux et palettes précédents, éditeur d'identité, anciens statuts et historique. Conservation des trois classes encore référencées par `symbolVisuals.js` (G/H et case vide). Retrait de la grille de fond toujours masquée et du libellé LOBBY toujours masqué ; le nom accessible « Retour au lobby » est conservé.
- Styles déplacés dans TopBar, BottomNav, DebugDrawer et ToastMessage ; surcharges d'accueil/lobby/reprise intégrées à leurs styles locaux. Mise en page, statut et attente sont locaux à App. Variables de thème, palette, base et accessibilité partagées dans `app.css`. Mode compact explicite, retrait des sélecteurs globaux `:has()` et des `!important` du lobby ; ceux du mouvement réduit sont conservés.
- Comparaison DOM des dimensions, positions, couleurs, fontes, espacements et padding avant/après : plateau clair desktop et 390 × 844, historique mobile et lobby mobile vide. Les relevés concordent après correction de deux écarts de cascade ; seul le texte masqué LOBBY a disparu du DOM. Cela ne constitue pas une comparaison exhaustive de tous les pixels ou de toutes les phases.
- Inspection visuelle après modification : plateau sombre mobile et clair desktop, accueil, attente, lobby mobile avec une partie listée, ouverture/fermeture du diagnostic. Activation au clavier de Plateau/Historique, thème, accès au lobby et fermeture du diagnostic vérifiée. Captures `screenshots/cleanup-mobile-dark.jpg`, `cleanup-desktop-light.jpg`, `cleanup-lobby-mobile-light.jpg` et `cleanup-debug-light.jpg`.
- Correction visuelle limitée, distincte du déplacement : le diagnostic avait un fond sombre fixe avec du texte sombre en thème clair et une case à cocher de taille formulaire. Fond lié au thème et case à cocher dimensionnée localement ; résultat inspecté. Aucune règle du jeu modifiée.
- `npm test` : 33 réussis, aucun ignoré. Lint, Svelte Check (zéro erreur/avertissement), build et `git diff --check` réussis. CSS compilé : 22,50 Ko (4,86 Ko gzip), contre 32,39 Ko (6,89 Ko gzip) avant cette étape. Aucune dépendance ajoutée.
- Les parcours métier à deux joueurs de l'étape précédente n'ont pas été intégralement rejoués pour ce déplacement CSS. Backend, Supabase et déploiements non modifiés. La revue complète de l'accessibilité, des états de formulaire et de toutes les phases reste ouverte. App conserve notamment les diagnostics et la coordination du focus ; le lot complet n'est pas déclaré terminé.

## Maintenance du 15 octobre 2026 — admission et parcours après extraction

- `roomConnection.js` extrait la coordination d'admission de App. Réponses tardives ignorées après navigation/changement de place, annulation à la destruction et au changement de contexte, lectures et doubles clics partagés. Une ancienne finalisation ne déverrouille pas une admission plus récente.
- Correction de comportement explicitement ajoutée au refactoring : une admission tardive ne remplace plus une autre session. Les erreurs inconnues ont un message générique, sans réafficher un payload serveur. Les codes de reprise restent dans le corps POST.
- `npm test` : 33 tests réussis, aucun ignoré. Sept tests ajoutés : routes/champs du transport existant caractérisés avant déplacement, propagation d'annulation sur les cinq opérations, cinq scénarios de coordination. Lint, Svelte Check (zéro erreur/avertissement) et build réussis après intégration.
- Navigateur, deux origines locales 127.0.0.1:5173 et :5187 avec PostgreSQL : création « Validation admission », entrée du joueur 2, transition automatique de l'attente, secrets et essais simultanés, indices manuels, victoire avec poursuite, correction de quatre à trois noirs retirant le point et rouvrant les deux plateaux, oubli local puis reprise privée du joueur 2, abandon/révélation et revanche commune avec conservation du score vérifiés. Les codes privés n'ont pas été inclus dans les captures.
- Ce parcours complète la vérification à deux clients restée ouverte dans l'extraction précédente. Les courses réseau sont testées par promesses contrôlées ; elles n'ont pas été simulées par coupure réseau réelle dans Chrome.
- Limites : CSS global, accessibilité complète, reprise d'une admission acceptée dont la réponse serait perdue, exploitation et validations distantes restent ouverts. Aucun changement backend pour cette étape ; suites Maven non relancées. Aucun push ni changement Supabase.

## Maintenance du 15 octobre 2026 — suite du lot 2

- Extraction des brouillons à quatre positions, des calculs de vue partagés par App et PartieScreen et du cycle de polling. Aucun calcul automatique des indices ou du résultat ajouté.
- Le polling programme la prochaine lecture six secondes après la précédente : changement explicite de cadence en cas de réponse lente, sans limite de temps de jeu.
- `npm test` : 26 tests réussis, aucun ignoré ; neuf tests supplémentaires couvrent positions hors ordre, autorisations simultanées, vues après correction et cycle de timer sans accumulation.
- `npm run lint`, `npm run check` (zéro erreur/avertissement, Vite chargé hors sandbox) et `npm run build` réussis après intégration aux composants.
- API et PostgreSQL locaux redémarrés. Reprise de la manche persistée, sélection de la quatrième case avant les autres, maintien du bouton de validation désactivé et effacement vérifiés dans le navigateur. Thèmes mobile clair/sombre inspectés à 390 × 844 ; captures `screenshots/maintenance-mobile-dark.jpg` et `screenshots/maintenance-mobile-light.jpg` remplacées par celles de cette session.
- Le parcours complet à deux joueurs n'a pas encore été rejoué après cette extraction : les preuves du 14 octobre ci-dessous précèdent ce changement. Les contrôles backend du 14 n'ont pas été relancés pour cette modification front. CI distante et production non vérifiées.
- Lot 2 encore partiel : admission et CSS global dans App restent à découper ; validation complète à deux clients et accessibilité à poursuivre.

## Maintenance du 14 octobre 2026

- Front : session/reprise et coordination réseau extraites de `App.svelte` (864 à 703 lignes environ). Promesse de lecture partagée, garde match/acteur/jeton/génération, annulation, prévention des doubles soumissions, replay exact après réponse perdue et nouvelle clé après conflit confirmé dans le même tour/phase.
- Contrat : validations runtime, retrait du faux plateau à partir de `/health`, fixture de préparation commune au mapper Java et au client front ; voir `API_CONTRACT.md`.
- Tests front : 17 réussis via `npm test`, sans dépendance de test supplémentaire. Lint, Svelte Check (zéro erreur/avertissement) et build réussis. CI mise à jour ; exécution distante non vérifiée.
- Backend : 32 tests réussis, dont sept PostgreSQL/HTTP et trois sur le nouveau codec ; `mvn -Pquality verify` réussi avec PMD et SpotBugs. Aucun test PostgreSQL ignoré dans cette exécution : base jetable sur 127.0.0.1:54330, distincte de la base de développement.
- PostgreSQL : RLS sur les cinq tables, retrait des grants PUBLIC/anon/authenticated, propriétaire JDBC opérationnel ; un grant SELECT accidentel à anon ne permet pas de lire les rooms. Migrations déjà appliquées non rejouées.
- Nouvelle installation : schéma local `arena_v1`, JSON `formatVersion:1` pour état et idempotence. Pas de conversion implicite des anciennes parties ; refus des documents anciens, incomplets ou de version inconnue. Correction du score, replay idempotent, abandon et revanche persistés puis relus via de nouvelles instances JDBC dans le test de cycle.
- Navigateur : avant changement de format, sélection hors ordre, victoire, poursuite, correction retirant le point et reprise vérifiées sur JDBC. Après reconstruction, nouvelle admission, secrets/propositions/indices, égalité, revanche commune et reprise après rechargement vérifiées. Le rendu mobile 390 × 844 et le rendu desktop ont été inspectés ; capture `screenshots/maintenance-mobile-dark.jpg`.
- Deux origines explicites pour ce test : 127.0.0.1:5173 et 127.0.0.1:5187. `localhost:5173` peut répondre depuis un autre projet via IPv6 : ne pas utiliser cette adresse pour identifier Mastermind dans cet environnement.
- Supabase, ses permissions effectives et Vercel n'ont pas été modifiés. La remise à zéro distante et la publication restent à effectuer selon `DATABASE_REBUILD.md`. CSS global, handlers HTTP, conservation/purge, contrôles d'abus, restauration et accessibilité complète restent ouverts.

## Résultats locaux du 13 octobre 2026

- Backend : 26 tests JUnit réussis, dont cinq tests PostgreSQL/HTTP réels ; `mvn -Pquality clean verify` réussi, contrôles PMD et SpotBugs inclus.
- Frontend : `npm run lint`, `npm run check` et `npm run build` réussis ; Svelte Check ne signale aucune erreur ni aucun avertissement.
- Navigateur, deux clients : secrets, essais simultanés, indices masqués, victoire avec poursuite, correction de quatre à trois noirs retirant le point et rouvrant le duel, notification, contestation, abandon avec révélation et revanche conservant le score vérifiés.
- Reprise du joueur 2 depuis une autre origine locale avec son code privé vérifiée. Thèmes clair/sombre inspectés à 390 × 844 ; rendu ordinateur inspecté. Captures dans `screenshots/`.
- Ce parcours utilise `UiValidationServer`, un adaptateur mémoire réservé aux tests, avec le moteur et le mapper réels. Il ne prouve pas les transactions JDBC, la persistance après redémarrage ou le fonctionnement des déploiements Vercel.
- PostgreSQL 16 jetable : migrations répétées, reprise avec vérification des jetons, réouverture des composants, idempotence, rollback de l'état et des événements après échec du journal, concurrence entre deux instances et entre deux invités validés.
- Serveur HTTP réel avec PostgreSQL : admission, reprise, refus du mauvais jeton et de l'usurpation de place, JSON invalide et corps trop volumineux validés sans mutation.
- Le parcours visuel complet reste à reproduire sur PostgreSQL puis en production. Accessibilité complète, navigation clavier et coupures réseau restent à vérifier.

Les tests `PostgresWorkflowTest` nécessitent `ARENA_TEST_DB_URL`, `ARENA_TEST_DB_USER` et `ARENA_TEST_DB_PASSWORD`. Utiliser exclusivement une base jetable : chaque test crée un schéma isolé conservé pour inspection jusqu'à la suppression de cette base. Sans ces variables, les cinq tests sont explicitement ignorés. Le workflow CI provisionne son propre PostgreSQL, mais son exécution distante n'a pas encore été vérifiée.

## Métier

- Deux joueurs confirment leur secret, leurs propositions et leurs indices dans des ordres différents.
- Les propositions et indices adverses restent cachés avant publication de la phase ; les secrets restent privés.
- 30 tours sans découverte : la manche continue, les deux secrets et les 122 actions sont conservés.
- Première découverte : un point, recherche du perdant poursuivie seul, aucun point supplémentaire.
- Découverte au même tour : Égalité sans point.
- Correction du dernier tour terminé : bon auteur, compteurs valides, résultat et score réconciliés, ancienne action conservée.
- Correction d'un tour plus ancien ou en cours : rejet sans mutation.
- Correction après une victoire/Égalité : recherche Éventuellement rouverte et consentements de revanche annulés.
- Abandon : résultat cohérent, révélation, puis revanche avec deux consentements et scores conservés.
- Payload invalide, acteur inconnu, version obsolète et clé idempotente conflictuelle : rejet.

## HTTP / PostgreSQL (À exécuter sur une base isolée)

- Migrations de schéma puis création/jointure de room atomiques.
- Même jeton depuis un autre appareil : reprise ; faux jeton et place adverse : refus.
- Deux jointures concurrentes : un seul deuxième joueur admis.
- Deux instances soumettant la même version : un seul effet, l'autre doit se resynchroniser.
- Échec d'écriture d'Événement ou d'idempotence : rollback de l'État.
- JSON invalide ou trop volumineux : erreur structurée, aucune mutation.
- Redémarrage backend : secrets, historique, scores et code de reprise conservés.

## Interface / parcours

Deux clients distincts, téléphone 390×844 et ordinateur : préparation, propositions, indices, victoire, poursuite, correction/contestation, abandon, reprise privée et revanche. Vérifier thème clair/sombre, navigation clavier, couleurs nommées, absence de débordement et erreurs réseau. Les tests avec un adaptateur mémoire ne valident pas JDBC.

## Production

Après publication compatible des deux projets Vercel : répéter la manche à deux clients, tester une reconnexion et vérifier Supabase actif. Ne pas conclure à une validation de production depuis une compilation locale ou un écran de lobby fonctionnel.

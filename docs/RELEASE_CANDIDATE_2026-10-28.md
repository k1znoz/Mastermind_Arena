# Candidate front — préparation du 28 octobre 2026

**Dossier local préparé ; candidate non commitée, non publiée.** Ce document concerne les correctifs front postérieurs à la dernière référence de production fournie par le concepteur : fd500eb pour les deux projets Vercel. Il ne réutilise pas la procédure de reset initial.

## Périmètre vérifié

HEAD local actualisé : 01735e5, incluant les travaux précédents ; polices locales, refus 429 et pages d’information ajoutés ensuite dans le travail courant. Le manifeste comporte désormais 107 fichiers, contre 81 avant ce lot. Comparaison avec la référence de production fournie fd500eb :

| Surface | Constat local | Conséquence pour ce lot |
| --- | --- | --- |
| Backend, pom.xml et migrations | Aucun diff suivi dans deduction-engine et pom.xml ; aucun nouveau fichier backend signalé par Git | Pas de changement serveur ou SQL à livrer |
| Clients HTTP front | roomClient reconnaît désormais le statut 429 et le traduit en ROOM_RATE_LIMITED | Routes, sérialisation et validation des succès inchangées ; code d'erreur interne au front |
| Stockage navigateur | session.js inchangé | Pas de nouvelle clé ni de migration de places |
| Dépendances/build | package.json et package-lock.json inchangés ; vite.config.js déclare les trois entrées HTML | Aucune dépendance ajoutée ; même commande de build, trois documents produits |
| CI et configuration Vercel racine | quality.yml et vercel.json inchangés | Les déclenchements distants restent à confirmer ; aucun nouveau réglage présumé |
| Interface et coordination | Chargements distincts, erreurs contextualisées/persistantes, focus, confirmation de correction, annonces, visibilité du titre et compteurs | Recette front ciblée, sans changement des règles du jeu |

Le manifeste [RELEASE_CANDIDATE_2026-10-28.manifest.json](RELEASE_CANDIDATE_2026-10-28.manifest.json) relève les empreintes SHA-256 de **107 fichiers** source, tests, ressources publiques et configuration front explicitement sélectionnés, dont les polices locales et leurs notices. Aucun .env, profil privé, code de reprise, dump, target, node_modules ou dist n'est inclus. Une modification d'un de ces fichiers exige de réévaluer les preuves concernées et d'actualiser le manifeste ; les modifications des documents sous docs seules ne changent pas cette identité du code.

Ce manifeste identifie le contenu local ; il ne remplace ni commit, ni archive de build, ni preuve de CI. Les variables de production n'ont pas été lues ou attestées par son calcul.

## Contenu à conserver dans la candidate

Le lot comprend les modifications du front et ses tests. Deux fichiers indispensables des travaux précédents, désormais suivis dans 01735e5, doivent rester inclus :

- src/lib/state/submissionFocus.js : suivi du focus utilisé par App.svelte ; son omission casserait le build.
- tests/submissionFocus.test.js : caractérisation des réponses tardives, changements de contexte et déplacements volontaires de focus.

Les nouveaux fichiers fonts.css et public/fonts (binaires, notices et provenance) doivent aussi être inclus avec la modification de app.css. Les rapports UX et leurs captures complètent les preuves. Les scripts audit-legacy-public.sql et remove-legacy-public.sql décrivent un travail déjà effectué ; ils ne sont pas des opérations à réexécuter pour publier le front. Les outils ignorés dans target et App_Front/target ne font pas partie de la livraison. Ne pas ajouter les fichiers privés simplement pour rendre une commande de test reproductible.

## Preuves réutilisées, sans nouvelle campagne UX

- 27 octobre : 83 tests front réussis, aucun ignoré ; lint réussi ; Svelte Check sans erreur/avertissement ; build de 176 modules réussi après restauration de l'encodage UTF-8. Résultat et incident dans [VALIDATION](VALIDATION.md).
- Dernier build avec polices locales et erreur 429 : index-eBB4oJeu.js et index-BRWWkcpR.css ; 85 tests front et lint/check/build réussis. Distribution des 16 polices contrôlée selon [LOCAL_FONTS](LOCAL_FONTS.md), refus 429 selon [ERROR_MESSAGES](ERROR_MESSAGES.md). Ces noms ne prouvent ni la configuration de production ni un déploiement : Vercel doit construire avec les variables du projet cible.
- Parcours avec lenteurs/refus, identité, correction et focus : [UX_FIXES](UX_FIXES_2026-10-27.md). Noms maximaux, zoom réel à 200 %, clavier et correctifs A4-01/A4-02 : [UX_A4](UX_A4_2026-10-27.md).
- Les deux nouveaux tests portent sur l'admission limitée, pas sur une nouvelle règle de jeu/session. La CI de 01735e5 reste à 83 tests ; les 85 tests sont une preuve locale postérieure, pas une nouvelle CI de la candidate.

## CI et préparation de publication

**Preuve distante désormais obtenue pour 01735e5 :** [Quality #4](CI_EVIDENCE_2026-10-28.md) réussie, 83 tests front et 57 backend dont 25 PostgreSQL, aucun ignoré. GitHub indique également deux statuts Vercel success / Deployment has completed. Ce constat ne couvre ni les ajouts locaux non commités, ni les alias canoniques ou un repli réel. La branche main est signalée non protégée dans la réponse GitHub consultée : la réussite CI n'est donc pas présentée comme une condition imposée avant toute publication. Aucun réglage de branche modifié.

1. Relire le lot, conserver les fichiers nouveaux requis et identifier le commit de la candidate complète. 01735e5 identifie les travaux précédents mais n'inclut pas les polices locales ajoutées ensuite ; il n'est donc pas la révision de la candidate complète actuelle.
2. Le workflow Quality exécute deux jobs : backend Java 21 avec PostgreSQL 16 isolé, et front Node 22 avec npm ci, lint, tests, check et build. Il se déclenche sur pull_request ou push sur main. Un simple push de branche ne suffit donc pas à prouver sa réussite.
3. Préférer une validation de la révision via PR avant une publication main ; vérifier les réglages de prévisualisation Vercel avant toute écriture distante. Aucun PR, push ou changement distant n'est effectué par ce dossier.
4. Confirmer les variables de build front destinées au backend canonique et le réglage CORS applicable. Ne pas copier un fichier .env local vers Vercel ni publier le bundle de test sur la seule foi d'une compilation réussie.
5. Dans Vercel, identifier le déploiement front antérieur correspondant à fd500eb et vérifier qu'il est disponible pour repli. Le nom exact et l'action de promotion restent à confirmer avec les accès du projet.

## Recette et publication bornées

Après L2/L3 et la recette L4 de [ROADMAP](ROADMAP.md), obtenir l'autorisation de publier la candidate identifiée. Un push main peut déclencher les deux projets : même si le backend est inchangé dans ce lot, ne pas supposer que seul le front sera reconstruit.

- Aucune migration, reset, modification du rôle SQL ou du cron pour ce lot.
- Une fois publié : vérifier le commit/déploiement réellement servi, les appels au backend canonique, un échange d'essais/indices et la reprise d'une place existante. Vérifier les corrections d'interface concernées dans ce parcours sans recommencer toute la matrice UX.
- Une ancienne page ouverte conserve son ancien front. Le protocole et le backend étant inchangés dans le diff local, aucune rupture de contrat n'est introduite par ce lot ; le comportement effectif d'une ancienne page reste à contrôler lors de la recette finale.
- Consigner la paire réellement en ligne et le résultat dans VALIDATION. Ne pas réutiliser la réussite de production fd500eb comme preuve de publication de ces changements.

## Repli adapté au lot front

Si la nouvelle interface empêche de jouer ou de reprendre une place, suspendre la promotion de la candidate et rétablir le déploiement front antérieur identifié, selon les possibilités vérifiées dans Vercel. Le backend inchangé continue d'utiliser le même schéma ; aucune restauration SQL n'est requise pour annuler ces changements front.

Les requêtes HTTP, les réponses de succès attendues et le stockage de session restent compatibles avec fd500eb : le repli proposé n'exige ni oubli local ni invalidation des codes. L'ancien front pourrait afficher une erreur générique sur 429, sans le nouveau message explicite. Cela constitue une analyse de compatibilité source, pas une exécution du déploiement de repli. Après repli, vérifier lecture et reprise d'une place ; une page conservant le nouveau code devra être rechargée. Ne pas qualifier l'incident de résolu sur la seule réussite d'une promotion Vercel.

## État de sortie du lot préparatoire

Acquis : périmètre, identité du contenu local, fichiers nouveaux requis, CI distante et statuts de déploiement de 01735e5, procédure de publication/repli documentés. Restent : commit de la candidate complète avec les polices et sa CI, paramètres de publication et déploiement front de repli confirmés, fin de L2/L3, [recette publique acquise](FINAL_ACCEPTANCE.md), puis publication autorisée des ajouts locaux. Aucun nouvel obstacle UX générique n'est ajouté.

## Complément local — pages d’information

La candidate inclut aussi les deux HTML, `src/information.js`, `PublicInformation.svelte`, le pied de page d’App et les entrées Vite. Dernier build : 181 modules, `index.html`, `mentions-legales.html`, `confidentialite.html` ; bundles game-CqoULgnX.js, information-C3rCK_-_.js et TopBar-C6aqltNX.js. Les noms du build précédent ci-dessus sont historiques. [85 tests, contrôles HTTP/SSR/Chrome et limites](INFORMATION_PAGES.md). Textes encore partiels : contact et informations juridiques restantes avant publication. L4 public reste acquis, sans être attribué à ces pages locales. Aucun backend, SQL, dépendance ou stockage de place modifié.

Rédaction juridique complétée localement : [analyse](LEGAL_REVIEW_2026-10-28.md), [contrôles actualisés](INFORMATION_PAGES.md). Le manifeste de 107 fichiers est recalculé après modification de PublicInformation. Les références à des builds précédents sont historiques ; la notice conserve des informations à finaliser avant publication.

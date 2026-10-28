# Candidate front — préparation du 28 octobre 2026

**Dossier local préparé ; candidate non commitée, non publiée.** Ce document concerne les correctifs front postérieurs à la dernière référence de production fournie par le concepteur : fd500eb pour les deux projets Vercel. Il ne réutilise pas la procédure de reset initial.

## Périmètre vérifié

HEAD local : fd500eb. Comparaison du travail courant avec cette référence :

| Surface | Constat local | Conséquence pour ce lot |
| --- | --- | --- |
| Backend, pom.xml et migrations | Aucun diff suivi dans deduction-engine et pom.xml ; aucun nouveau fichier backend signalé par Git | Pas de changement serveur ou SQL à livrer |
| Clients HTTP front | Aucun diff dans src/lib/api | Routes, sérialisation et lecteurs inchangés |
| Stockage navigateur | session.js inchangé | Pas de nouvelle clé ni de migration de places |
| Dépendances/build | package.json, package-lock.json et vite.config.js inchangés | Pas de dépendance ni de procédure de build nouvelle |
| CI et configuration Vercel racine | quality.yml et vercel.json inchangés | Les déclenchements distants restent à confirmer ; aucun nouveau réglage présumé |
| Interface et coordination | Chargements distincts, erreurs contextualisées/persistantes, focus, confirmation de correction, annonces, visibilité du titre et compteurs | Recette front ciblée, sans changement des règles du jeu |

Le manifeste [RELEASE_CANDIDATE_2026-10-28.manifest.json](RELEASE_CANDIDATE_2026-10-28.manifest.json) relève les empreintes SHA-256 de **81 fichiers** source, tests, ressources publiques et configuration front explicitement sélectionnés. Aucun .env, profil privé, code de reprise, dump, target, node_modules ou dist n'est inclus. Une modification d'un de ces fichiers exige de réévaluer les preuves concernées et d'actualiser le manifeste ; les modifications documentaires seules ne changent pas cette identité du code.

Ce manifeste identifie le contenu local ; il ne remplace ni commit, ni archive de build, ni preuve de CI. Les variables de production n'ont pas été lues ou attestées par son calcul.

## Contenu à conserver dans la candidate

Le lot comprend les modifications suivies du front et ses tests. Deux fichiers front sont encore non suivis et indispensables :

- src/lib/state/submissionFocus.js : suivi du focus utilisé par App.svelte ; son omission casserait le build.
- tests/submissionFocus.test.js : caractérisation des réponses tardives, changements de contexte et déplacements volontaires de focus.

Les rapports UX et leurs captures complètent les preuves. Les scripts audit-legacy-public.sql et remove-legacy-public.sql décrivent un travail déjà effectué ; ils ne sont pas des opérations à réexécuter pour publier le front. Les outils ignorés dans target et App_Front/target ne font pas partie de la livraison. Ne pas ajouter les fichiers privés simplement pour rendre une commande de test reproductible.

## Preuves réutilisées, sans nouvelle campagne UX

- 27 octobre : 83 tests front réussis, aucun ignoré ; lint réussi ; Svelte Check sans erreur/avertissement ; build de 176 modules réussi après restauration de l'encodage UTF-8. Résultat et incident dans [VALIDATION](VALIDATION.md).
- Dernier build local relevé : index-C7AQeOyX.js et index-pd-AsOAG.css. Ces noms ne prouvent ni la configuration de production ni un déploiement : Vercel doit construire avec les variables du projet cible.
- Parcours avec lenteurs/refus, identité, correction et focus : [UX_FIXES](UX_FIXES_2026-10-27.md). Noms maximaux, zoom réel à 200 %, clavier et correctifs A4-01/A4-02 : [UX_A4](UX_A4_2026-10-27.md).
- Aucun changement applicatif entre ces contrôles et le présent relevé. Les suites complètes ne sont pas relancées pour la seule rédaction de la roadmap.

## CI et préparation de publication

1. Relire le lot, conserver les fichiers nouveaux requis et identifier un commit candidat selon l'autorisation correspondante. Il n'existe pas encore de SHA candidat distinct de fd500eb.
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

Les clients HTTP et le stockage de session sont identiques à fd500eb dans le périmètre inspecté : le repli proposé n'exige ni oubli local ni invalidation des codes. Cela constitue une analyse de compatibilité source, pas une exécution du déploiement de repli. Après repli, vérifier lecture et reprise d'une place ; une page conservant le nouveau code devra être rechargée. Ne pas qualifier l'incident de résolu sur la seule réussite d'une promotion Vercel.

## État de sortie du lot préparatoire

Acquis : périmètre, identité du contenu local, fichiers nouveaux requis, preuves disponibles, CI attendue et procédure de publication/repli documentés. Restent : commit candidat, CI distante, paramètres de publication et déploiement front de repli confirmés, décisions L2/L3, recette concepteur/réseau puis publication autorisée. Aucun nouvel obstacle UX générique n'est ajouté.

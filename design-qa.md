# Accueil, salon, attente et reprise — QA du 24 octobre 2026

final result: passed

## Navigation du duel — périmètre ajouté

Sources : planches 04-code-secret et 12-historique du dossier déjà référencé, chacune de 1642 × 958 pixels. Elles contiennent des cadres de présentation pour desktop et téléphone ; comparaison des régions de navigation, sans métrique de pixel-match avec ces cadres. Captures `docs/screenshots/ui-duel/{plateau,history}-{desktop,mobile}-{dark,light}.jpg` : 1440 × 1024 et 390 × 844 pixels, densité nominale 1. Les variantes source/rendu ont été ouvertes ensemble, d'abord source 04 avec plateau desktop/mobile puis source 12 avec historique desktop/mobile. Les labels et icônes de navigation restent lisibles à cette échelle ; aucun zoom supplémentaire nécessaire pour les régions comparées.

Périmètre livré : sidebar ou barre basse, logo desktop de retour au salon, état actif, icônes, marges de contenu et alignement de TopBar. **Bandeau pseudos/score, outils privés, éditeur du code et contenu de l'historique exclus de cette validation** : ils conservent leur présentation actuelle et présentent encore des différences importantes avec les maquettes. Le panneau privé 14 reste à intégrer. Les fixtures de préparation/historique vide ne représentent pas les événements illustrés sur la planche 12 ; aucune comparaison des lignes n'est revendiquée.

Cinq surfaces contrôlées : Inter et hiérarchie de labels ; sidebar de 216 px et barre basse de 72 px avec marge dédiée ; tokens clair/sombre existants avec état actif cyan ; SVG Tabler circle-dot/chart-bar et logo existant, nets sans asset dessiné ; libellés Plateau/Historique et destinations inchangés. Choix de thème conservé dans TopBar pendant ce lot ; son emplacement final autour du bandeau sera traité avec ce dernier. Différences mineures de version des pictogrammes de bibliothèque et espacement de la sidebar classées en finition P3, sans nouvelle commande ni perte d'information.

Aucun P0/P1/P2 identifié dans les régions de navigation après comparaison initiale ; aucun correctif visuel requis par cette comparaison. Largeurs 320/390/650/768/850/851/1024/1440 sans débordement ; main et TopBar décalés d'au moins 216 px au-dessus du seuil. Boutons 55/56 px. Sur 320 × 640, validation du secret atteinte par Tab : bas à 528,55 px avant navigation à 568 px. Matrices JSON et capture `plateau-small-screen-bottom.jpg` conservées.

Parcours : changement des deux destinations par Entrée, aria-current, brouillon du secret préservé, retour au salon puis reprise ; thèmes testés. Scénario API fictif issu du contrat de préparation, sans envoi de secret ni transaction PostgreSQL. Console interrogée sans erreur retournée. La capture pleine page mobile a expiré une fois ; captures finales limitées au viewport. Override réinitialisé et aperçu laissé ouvert.

## Reprise dédiée — étape courante

La validation visuelle des trois premiers écrans reste acquise. Le périmètre s'étend à la planche `13-reprise.png`, dans le même dossier source, de 1920 × 820 pixels. Elle contient deux cadres dessinés ; comparaison de contenu à l'échelle de présentation, sans pixel-match entre les cadres et les viewports annoncés.

Rendus `recovery-desktop-dark.jpg`, `recovery-desktop-light.jpg`, `recovery-mobile-dark.jpg`, `recovery-mobile-light.jpg` dans `docs/screenshots/ui-entry/`. Viewports 1440 × 1024 et 390 × 844, captures aux mêmes dimensions et densité nominale 1. Source et rendus ouverts ensemble pour examiner titre, champ privé, bouton de visibilité, aide, action principale et notes de confidentialité. Les textes sont lisibles dans les vues ; aucun agrandissement artificiel nécessaire. Aucun P0/P1/P2 visuel identifié sur cette comparaison initiale, aucune correction de présentation demandée par la comparaison.

Les cinq surfaces sont examinées : Inter et hiérarchie des titres ; champ/formulaire centré sur desktop et pleine largeur utile sur mobile ; tokens sombres déjà intégrés et palette claire autorisée ; vrais SVG Tabler locaux de retour/visibilité/cadenas ; texte de la planche conservé. Adaptations attendues : texte sombre sur cyan pour contraste, contrôle clair/sombre toujours disponible, absence de cadre décoratif du catalogue. Dix icônes locales désormais distribuées avec licence MIT.

Parcours vérifiés avec API fictive : accès accueil/salon, focus sur titre puis retour au lanceur, code masqué et visibilité au clavier, refus, admission acceptée vers l'attente. Capture `recovery-desktop-busy.jpg` pendant une réponse retardée de trois secondes ; sortie avant réponse, le salon reste affiché après le délai. Test unitaire complémentaire pour ancienne réponse réussie et refusée après réouverture. Largeurs 320/390/768/1440 sans débordement, matrice `recovery-responsive-checks.json` ; dernière interrogation console sans erreur. Liste vide du salon capturée séparément après scénario contrôlé.

**Choix finalisé :** le concepteur valide la conservation en mémoire du nom et du code d'accès de création pendant l'aller-retour. Deux bindings vers App préservent ces valeurs sans stockage disque. Au navigateur : nom restitué après refus et deux allers-retours ; code masqué restitué puis création fictive confirmée comme protégée. Les sorties normales du salon et le rechargement rendent le formulaire vide. Le lecteur DOM masque les password : aucune comparaison directe de leur valeur ne constitue une preuve ; le contrôle du code repose sur la création protégée et la revue des bindings.

Captures accueil/salon renouvelées après cette finalisation, avec boutons ouvrant la page dédiée. États supplémentaires assumés : une place déjà sauvegardée et une troisième partie fictive ; ils expliquent les actions Reprendre et la hauteur différente du salon par rapport à la planche à deux parties. Sources 01/02 et rendus renouvelés examinés ensemble. [P2] Lien de reprise mobile aligné à gauche relevé pendant cette comparaison, corrigé par text-align:center ; capture reprise et comparée à nouveau avec la source 01 : alignement centré confirmé. Aucun P0/P1/P2 restant identifié sur ce périmètre. Aucune validation réelle entre appareils ou PostgreSQL revendiquée pour la reprise.

## Périmètre précédent — accueil, salon et attente

## Sources et preuves

Source visuelle : dossier `C:/Users/alois/.codex/visualizations/2026/10/24/01a16d0e-cc35-76b0-a365-7622aa825f7b/mastermind-maquettes/images/`, planches `01-accueil.png`, `02-lobby.png` et `03-attente-joueur.png`. Planches de 1642 × 958 pixels contenant deux cadres illustrés, annoncés à 1440 × 1024 et 390 × 844 CSS px. Les proportions des cadres rasterisés ne correspondent pas exactement aux viewports annoncés : comparaison des régions de contenu, sans promesse de correspondance au pixel ni copie des cadres de présentation.

Implémentation : `http://127.0.0.1:5178/`, API fictive sur loopback 8084, données synthétiques uniquement. Captures dans `docs/screenshots/ui-entry/` : préfixes `welcome`, `lobby` et `waiting`, variantes `desktop-dark`, `desktop-light`, `mobile-dark`, `mobile-light`. Desktop : 1440 × 1024 pixels ; accueil et attente mobile : 390 × 844. À l'étape de compactage, salon à deux parties de 390 × 1048, ou largeur de contenu 375 px avec barre de défilement Chrome ; les captures renouvelées montrent trois parties et une place sauvegardée, avec une hauteur plus grande. Viewport mobile 390 × 844 CSS px ; comparaison à la densité capturée, sans métrique pixel avec les cadres du catalogue. Captures supplémentaires pour noms longs, refus du code, reprise invalide et attente protégée.

Les sources et captures correspondantes ont été ouvertes ensemble dans les mêmes entrées de comparaison. Titres, boutons et champs restent lisibles dans ces vues : aucun agrandissement supplémentaire requis pour ces régions.

## Historique de comparaison

1. [P2] Libellés monospace et titres en majuscules hérités ; paragraphe mobile sur trois lignes ; bouton Rejoindre redondant avec le formulaire protégé ; marges de labels excessives. Corrigés par styles Inter ciblés, paragraphe sur deux lignes, affichage conditionnel du bouton et marges neutralisées dans le salon.
2. [P2] Gemme au lieu du losange et avatar devant le titre du salon. Corrigés avec Tabler square-rotated et avatars dans les lignes de parties. Captures finales reprises après rechargement.
3. Comparaison intermédiaire : [P2] salon mobile trop haut, mesuré à 1 217 px. Corrigé par réduction des espacements, padding, en-têtes et hauteurs excessives. Contrôles d'au moins 44 px et champs à 16 px conservés. Nouveau rendu à 1 048 px, comparé avec la planche 02 dans la même entrée : aucune différence P0/P1/P2 restante identifiée sur ce périmètre. Le défilement résiduel est attendu pour des contrôles utilisables, les noms longs et le volet de reprise conservé ; ne pas compresser toute la page artificiellement dans 844 px.
4. Contrôle clavier : perte de focus à la disparition de Rejoindre lors de l'ouverture protégée. Corrigée par une action Svelte ciblée sur le nouveau champ. Après rechargement : champ password actif, puis Tab vers Entrer et contour visible ; refus synthétique présenté.
5. Attente : planche 03 et quatre captures clair/sombre, desktop/mobile ouvertes ensemble. Composition centrée, deux places en ligne puis empilées, distinction de la place libre et actions vérifiées. Variantes protégée et nom long contrôlées. Aucun nouveau P0/P1/P2 visuel identifié ; aucune correction visuelle requise après cette comparaison.

## Surfaces requises

- **Typographie :** Inter, casse normale, hiérarchie et retours à la ligne examinés ; famille exacte du dessin rasterisé non certifiée.
- **Espacement :** deux cartes desktop, une colonne mobile ; densité corrigée et hiérarchie conservée, sans suppression des contrôles ni chevauchement observé.
- **Couleurs :** charbon/cyan limités aux vues avant partie ; palette claire existante selon accord. Texte sombre sur cyan pour lisibilité. Aucune référence claire fournie.
- **Assets :** sept SVG Tabler v3.35.0 locaux à cette étape précédente, dix après la page de reprise ; licence MIT dans les sources et le build. Avatars de bibliothèque, sans données de profil ajoutées. Différences mineures acceptées : silhouettes de bibliothèque en contour et absence du petit séparateur décoratif entre les places ; aucune information de jeu perdue.
- **Contenu :** pseudo, créateur et statut réels des composants ; données des captures fictives. Captures accueil/salon rafraîchies après remplacement du volet par le bouton de page dédiée ; états supplémentaires décrits en tête.

## Parcours et limites

Chrome utilisé après indisponibilité du navigateur intégré. Plusieurs redémarrages de l'aperçu ont été nécessaires. Saisie du pseudo, accès au salon par Entrée, actualisation, ouverture du champ protégé avec focus, mots de passe masqués et thèmes contrôlés. Refus synthétique 403 affiché « Code d’accès incorrect. ». Volet de reprise ouvert au clavier, Tab vers le champ puis le bouton et reprise invalide vérifiés. Création fictive puis attente, actualisation et retour au salon effectués au clavier ; aucune admission contre le vrai backend revendiquée.

Largeurs 320, 390, 768 et 1440 : aucun débordement horizontal mesuré sur salon et attente. Pseudo de 24 caractères et partie de 60 caractères sans espace : contenu visible avec retour à la ligne. Matrices enregistrées dans les deux JSON responsive-checks. Aucune erreur console retournée par les dernières interrogations. Override Chrome réinitialisé. Les captures ne prouvent ni un appareil physique ni une conformité WCAG complète.

## À terminer

- [x] Réduire la densité mobile et comparer à nouveau.
- [x] Contrôler les commandes modifiées au clavier, le focus du champ protégé, les noms longs et les thèmes.
- [x] Intégrer et comparer l'attente d'adversaire.
- [ ] Compléter les variantes vides/chargement et erreur d'attente, les transitions de focus entre écrans et après opérations asynchrones. Le focus sur les boutons momentanément désactivés n'est pas revendiqué comme préservé.
- [x] Finaliser le choix de retour de la reprise dédiée et rafraîchir les captures accueil/salon.
- [ ] Intégrer navigation et plateaux, puis éprouver les parcours réels.
- [ ] Parcours réels à deux clients sur PostgreSQL isolé.

Validation locale passée pour le périmètre accueil/salon/attente/reprise décrit ici. Le premier lot complet et la refonte des 17 planches restent ouverts pour les autres éléments listés, notamment parcours réels et états complémentaires. Aucun déploiement ni opération Supabase.

# Refonte UI/UX — référence du 24 octobre 2026

## Source retenue

Le concepteur demande d'utiliser les 17 planches de ce dossier pour la refonte de l'application existante :

`C:/Users/alois/.codex/visualizations/2026/10/24/01a16d0e-cc35-76b0-a365-7622aa825f7b/mastermind-maquettes/index.html`

Le HTML est un catalogue de PNG, pas un prototype interactif. Les 17 images ont été ouvertes et examinées ; aucune source de ce dossier n'est modifiée. Cette référence est extérieure au dépôt et devra être jointe à une livraison destinée à un autre poste. Son texte précise que les dimensions des contrôles et la typographie restent à vérifier lors de l'intégration ; ne pas présenter ces planches comme une spécification exacte au pixel.

Direction visible : fond charbon, accents cyan, surfaces sobres, jetons ronds, contenu principal centré. Navigation latérale sur ordinateur, navigation basse sur téléphone ; bandeau du duel avec pseudos et score. Référence de travail annoncée sur les planches : ordinateur 1440 × 1024 et téléphone 390 × 844. Les bordures des planches, titres de présentation, cadres de téléphone et barres système ne font pas partie de l'interface web à intégrer.

## Correspondance avec l'application

| Planches | Parcours | Composants et coordination concernés |
|---|---|---|
| 01–02 | Pseudo, liste, création et entrée | WelcomeScreen, LobbyScreen, TopBar ; roomConnection conservé |
| 03 | Attente de l'adversaire | WaitingScreen ; coordination et métadonnées conservées dans App.svelte |
| 04–05 | Choix du secret et attente | PartieScreen, CodePegs, brouillon de préparation |
| 06–08 | Proposition, attente et indices | PartieScreen, vue dérivée gameView et brouillons existants |
| 09 | Contestation et correction | Contrôles actuels ciblant feedbackVersion ; conditions d'auteur conservées |
| 10–11 | Résultat, poursuite et revanche | PartieScreen ; résultat distinct de fin de recherche |
| 12 | Historique | HistoryScreen ; uniquement les actions autorisées reçues |
| 13–14 | Reprise privée et oubli local | RecoveryScreen pour la saisie ; outils privés du duel et coordination session/reprise existante |
| 15 | Confirmation d'abandon | Confirmation actuelle à représenter en dialogue accessible |
| 16 | Erreur de connexion | Erreurs de synchronisation existantes et dernier plateau valide |
| 17 | Diagnostic | DebugDrawer et sanitizeDebug ; accès de développement préservé |

L'intégration vise l'application Svelte/Java existante, ses vrais clients HTTP et ses règles. Elle ne crée ni nouveau framework ni application de démonstration déconnectée du backend. Réutiliser les composants et tokens, conserver les gardes de contexte, la confidentialité, les retries et le polling. Ne pas recopier les couleurs, noms ou scores fictifs comme données fixes du jeu.

## Décisions confirmées pendant l'intégration

- **Thème clair :** adapter la même structure avec la palette claire existante, selon la réponse du concepteur.
- **Victoire et poursuite :** le concepteur précise que la planche qu'il désigne comme 11 représente sa victoire pendant que l'adversaire cherche encore. Cette intention prévaut sur l'interprétation initiale du catalogue. Distinguer résultat attribué et fin des recherches à partir de l'état serveur ; aucune nouvelle révélation de secrets n'est autorisée.
- **Pictogrammes :** Tabler local sous licence MIT, accepté après explication de la licence. Vingt SVG v3.35.0, licence conservée avec les fichiers et copiée dans les fichiers publics pour distribution. Aucun paquet npm ni chargement externe supplémentaire.

## Invariants et détails à compléter

- Les indices restent manuels ; au plus quatre au total. Les nombres et combinaisons des illustrations ne constituent pas des résultats calculés ni de nouveaux formats.
- Afficher la phase simultanée réelle, notamment l'attente des indices adverses ; une seule planche d'attente ne représente pas tous les états possibles.
- Prévoir match nul, poursuite du perdant, refus, chargement et listes vides à partir de l'état existant. Le gagnant continue à donner les indices tant que l'autre cherche.
- Les demandes de correction restent informatives et les corrections peuvent changer résultat et score ; aucune restriction supplémentaire inspirée du dessin.
- Un échec réseau local n'est pas la preuve que l'adversaire est déconnecté. Garder la nuance entre refus connu et action non confirmée.
- Code de reprise masqué par défaut ; copie et oubli local sans suppression de la partie. Ne jamais inscrire un secret réel dans une capture ou un diagnostic.
- Conserver le choix clair/sombre accessible pendant le jeu, même si son emplacement varie entre les planches. Les formulaires et dialogues doivent rester accessibles au clavier, avec focus géré et libellés des couleurs.
- Décisions du 25 octobre : notifications de correction/vérification visibles jusqu'à fermeture ; note près du code privé « Partie supprimée après 7 jours sans action de jeu. ». Le cron quotidien reste à activer au déploiement. Contrat HTTP complété, avec accord du concepteur, pour distinguer indices initiaux et corrections.

## Ordre d'intégration et critères de sortie

1. Coque, tokens, navigation, accueil/lobby/attente et reprise : premier lot visuel cohérent, sans déplacement de la logique réseau.
2. Préparation, propositions, indices, correction et états d'attente : vrais plateaux et contrôles existants, données adaptées à l'état courant.
3. Résultat, historique, abandon, reprise privée et diagnostic ; informations de conservation et présentation des erreurs selon choix confirmés.
4. Contrôles front pertinents, parcours à deux clients sur PostgreSQL isolé, clavier et comparaison avec les références à mêmes tailles/états. Tester aussi des tailles intermédiaires, noms longs, erreurs et historique long. Documenter les écarts nécessaires aux règles/accessibilité et les validations distantes restant ouvertes.

## Avancement local de la refonte

Les actions restantes et leurs critères sont désormais centralisés dans [ROADMAP, lot A](ROADMAP.md#a--revue-de-la-candidate-locale-et-finitions-ciblées). Ce fichier décrit la correspondance des maquettes et le périmètre livré ; il ne constitue pas un second planning.

Navigation complétée le 25 octobre : destinations de focus approuvées pour accueil/salon, changement du pseudo et lien vers l'historique ; focus des onglets conservé. Historique corrigé pour distinguer lecture lente et panne, avec données préservées et retry vérifié. Dernière suite : **71 tests front**, lint/check/build réussis. Le focus après les autres opérations asynchrones et la revue globale restent ouverts.


- [x] Intégrer la structure accueil et salon, les tokens sombres limités à ces vues et l'adaptation claire existante.
- [x] Ajouter les icônes locales et conserver leurs libellés accessibles ; supprimer le bouton Rejoindre redondant lorsque le code d'accès est demandé.
- [x] Conserver les événements des composants, la coordination HTTP et les règles ; dernière suite frontend : 71 tests, lint, vérification Svelte et build réussis le 25 octobre.
- [x] Comparer et vérifier accueil, salon et attente en clair/sombre et mobile/desktop ; navigation clavier, refus synthétiques et noms longs contrôlés. Le salon mobile passe de 1 217 à 1 048 px ; focus vers le champ du code à son ouverture. Validation visuelle de ce périmètre dans [design-qa](../design-qa.md), sans validation multijoueur réelle.
- [x] Refaire l'attente d'adversaire : composant WaitingScreen, créateur réel des métadonnées, place disponible, accès ouvert/protégé et commandes existantes ; polling inchangé.
- [x] Livrer la page de reprise dédiée suivant la planche 13 : code masqué, visibilité explicite, refus, reprise fictive acceptée, retour avec focus et annulation en cours vérifiés. Conservation en mémoire du nom et du code d'accès de création lors de l'aller-retour validée par le concepteur puis contrôlée au navigateur. Aucune persistance du brouillon sur disque ; validation avec le vrai backend toujours distincte.
- [x] Contrôler l'attente lente/en erreur et le retour au salon vide avec une API synthétique maîtrisée ; nom accessible, Tab pendant lecture et focus du titre après retour vérifiés. Les transitions des autres écrans restent ouvertes ci-dessous.
- [x] Intégrer la navigation du duel : DuelNav remplace BottomNav après vérification de son unique usage ; destinations inchangées, sidebar à partir de 851 px et barre basse en dessous. TopBar réserve l'espace de la sidebar ; contenu et défilement conservent une marge pour les contrôles fixes. Navigation clavier, sélection aria-current, retour au salon et brouillon du secret préservé contrôlés sur fixture de préparation.
- [x] Intégrer les cartes pseudos/score et la préparation du secret : quatre cases sélectionnables, palette nommée sur six colonnes desktop/trois mobile, confidentialité, confirmation et attente du code adverse. Données, événements, brouillons et gardes existants conservés ; contrôle local avec API fictive, sans soumission réelle du secret.
- [x] Finaliser la reprise privée : panneau modal latéral desktop et pleine largeur mobile, masquage initial, visibilité explicite, copie et oubli local. Fermeture native et retour du focus vérifiés ; oubli puis reprise de la même partie sur Java/PostgreSQL local réussis. La commande de thème reste disponible dans TopBar.
- [x] Intégrer l'éditeur d'essai, l'attente d'un essai déjà envoyé et la saisie manuelle des indices (planches 06–08). Éditeur partagé visuellement avec la préparation, comparaison responsive essai/secret, compteurs limités à quatre au total, onglets et lignes d'essais avec indices déclarés lisibles. Parcours de saisie contrôlés avec API fictive ; aucune soumission réelle validée dans ce lot.
- [x] Présenter la correction du dernier tour terminé dans un panneau dédié avec essai concerné, indices actuels, compteurs et total. Demande de vérification affichée uniquement lorsqu'elle existe ; focus à l'ouverture et retour au bouton après annulation vérifiés. Commande et cible de version conservées ; correction réelle qui rouvre la recherche vérifiée à deux onglets le 25 octobre.
- [x] Remplacer la confirmation d'abandon en ligne par un dialogue natif : centré sur ordinateur, volet bas sur téléphone, arrière-plan modal, focus initial sur Continuer, Échap et annulation avec retour au déclencheur. Aucune confirmation d'abandon activée pendant les essais fictifs.
- [x] Ajouter les résumés des recherches fondés sur les joueurs déclarés trouvés par le serveur et leurs essais déjà autorisés. Victoire avec poursuite, fin des recherches, égalité et abandon contrôlés sur données réelles isolées ; aucune nouvelle révélation hors abandon. Essais consultables après la fin et correction toujours disponible.
- [x] Livrer les notifications de vérification/correction avec fermeture explicite, conservées lors de l'aller-retour Plateau/Historique. Historique chronologique avec auteurs, essais, indices initiaux et corrections successives ; cible du tour corrigé retrouvée par sa version. Le nouveau champ HTTP originalFeedback est validé et testé sans modifier les indices effectifs du plateau.
- [x] Éprouver deux onglets sur Java/PostgreSQL isolé : correction qui rouvre la recherche et annule les consentements, nouvel essai, indices manuels, fin et revanche commune, score conservé. Reprise privée, égalité et abandon également contrôlés ; preuves dans VALIDATION.
- [x] Contrôler l'historique de 400 actions jusqu'au tour 100 et les noms longs de l'attente/historique sur huit largeurs ; lecture lente, erreur et retry de l'historique vérifiés sans perte des données. Destinations de focus accueil/salon, changement du pseudo et lien vers l'historique vérifiées ; onglets inchangés.

Restent précisément : revue du concepteur (A1), chargement de la liste du salon/admissions et focus après les soumissions restantes (A2), message de partie indisponible et notifications de soumission (A3), contraste/clavier complémentaires et noms longs des résultats/panneau privé (A4), téléphone physique/coupure réseau/reprise sur un autre appareil (A5). L'historique long n'est plus une tâche ouverte. Le diagnostic et la présentation complète des surfaces d'erreur ne sont pas déclarés conformes aux planches 16/17 par les contrôles partiels existants.

Les parcours locaux décrits sont validés, y compris le duel réel du 25 octobre. La refonte complète reste ouverte pour les états et contrôles complémentaires listés ; aucune validation de production ni conformité WCAG complète revendiquée. Le socle technique reste décrit dans [WORKFLOW_CONTRACT](WORKFLOW_CONTRACT.md), la progression dans [MAINTENANCE_ROADMAP](MAINTENANCE_ROADMAP.md), les preuves dans [VALIDATION](VALIDATION.md).

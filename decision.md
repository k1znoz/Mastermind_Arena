# Décisions actuelles à Mastermind Arena

Validées avec le concepteur le 13 octobre 2026. Les réflexions initiales sur une plateforme générique sont conservées dans [les archives](docs/archive/decision-initiale.md).

## Produit et livraison

Décisions du 25 octobre : afficher les notifications de correction/vérification jusqu'à fermeture manuelle, sans disparition automatique. Ajouter près du code de reprise uniquement « Partie supprimée après 7 jours sans action de jeu. » ; la seconde phrase proposée est retirée à la demande du concepteur. Compléter le contrat HTTP pour conserver les indices initiaux dans l'historique, distinctement des indices effectifs des plateaux et des événements de correction. Aucun changement des règles ni activation distante de purge n'est autorisé par ces décisions.

Décision du 24 octobre — retour de reprise : conserver le nom de partie et le code d'accès saisis dans le salon pendant l'aller-retour vers la page de reprise, uniquement en mémoire. Aucun enregistrement de ce brouillon sur disque. Les autres sorties du salon conservent leur comportement de remise à zéro ; la sauvegarde de la place privée reste indépendante.

Décisions UI du 24 octobre : utiliser les maquettes fournies, adapter leur structure au thème clair avec la palette claire existante. L'écran désigné comme planche 11 par le concepteur doit représenter une victoire pendant que l'adversaire continue sa recherche ; le résultat attribué ne vaut pas fin des recherches ni autorisation de révéler un secret. Icônes Tabler locales sous licence MIT approuvées après explication ; conserver attribution et licence, sans dépendance npm ni service externe supplémentaire.

Décision du 14 octobre 2026 : les données actuelles sont jetables. La remise à zéro des parties est autorisée ; aucune conservation de leur historique ni compatibilité des anciens codes de partie n'est nécessaire pour la nouvelle version. Préparer et valider la reconstruction sur une base isolée avant l'opération distante. Cette décision ne dispense pas de protéger les parties créées après la nouvelle publication.

- Jeu Mastermind indépendant ; une plateforme de jeux reste un projet futur.
- Présentation au portfolio / diplôme CDA le 23 novembre 2026, avec une démonstration en ligne.
- Deux projets Vercel distincts (front / backend), déployés depuis `main`. PostgreSQL est hébergé sur Supabase.
- Conception et décisions par l'auteur ; rédaction initiale du code largement assistée par Copilot. Les nouvelles modifications doivent Être relues et comprises par le concepteur.

## Règles validées

- Deux plateaux simultanés : secrets à propositions cachées jusqu’à la validation des deux à indices manuels cachés jusqu’à la validation des deux à tour suivant.
- Quatre couleurs, répétitions autorisées, six choix : bleu, vert, jaune, rouge, noir, blanc.
- Pas de limite de durée ou de nombre de tours ; possibilité d'abandon.
- Quatre indices noirs déclarés Établissent une découverte. Découvertes au même tour : match nul, sans point.
- Premier joueur à trouver : un point. L'autre peut continuer seul avec les indices du gagnant, sans gagner un point supplémentaire.
- Abandon : fin de recherche et révélation des codes. Avant une victoire, le point revient à l'adversaire ; après une victoire, le résultat existant est conservé.
- Confiance par défaut. Contestation informative et correction possible des indices du dernier tour terminé.
- Une correction peut modifier le résultat et le score, rouvrir la recherche et annuler les consentements à la revanche. Elle est historisée et signalée à l'autre joueur.
- Revanche avec consentement des deux joueurs ; scores conservés ; nouveaux secrets.
- Pas de délai de déconnexion ; reprise depuis un autre onglet ou appareil à l'aide d'un code privé propre au joueur.

## Conservation

Décision de conservation du 23 octobre : supprimer les parties après sept jours depuis leur dernière action acceptée, y compris celles non terminées ; création comme référence en l'absence d'action. Lecture, polling et reconnexion ne prolongent pas ce délai. Commande administrative implémentée localement selon [DATA_RETENTION](docs/DATA_RETENTION.md), aucune purge encore activée. Elle concerne la conservation/reprise, sans ajout d'une limite de tours ou d'un abandon automatique dans le moteur.

Précision du concepteur : toutes les données actuelles, journal technique inclus, sont des tests jetables. Ne pas prévoir de reprise des données des versions précédentes. Préparer le nouveau schéma et sa validation ; effectuer le reset initial ultérieurement, sans effacement implicite au démarrage ou à la migration. Les nouveaux événements seront rattachés à leur partie et supprimés avec elle.

Planification de la conservation, précisée le 24 octobre : retenir Supabase Cron avec une exécution quotidienne. Préparer et tester la logique avant publication ; configurer et activer la tâche au déploiement, après le reset initial et la vérification du nouveau schéma. Aucun accès administratif à fournir à GitHub pour cette purge. La suppression est définitive dans la base active ; les sauvegardes suivent une politique distincte. Le seuil de sept jours rend une partie éligible : l'exécution quotidienne implique normalement une suppression entre sept et huit jours d'inactivité, sans garantie de délai en cas d'incident du planificateur. Aucune tâche SQL planifiée n'est encore livrée ou activée.

Organisation validée le 24 octobre : procédure PostgreSQL commune à la commande Java et au futur cron, avec une transaction par partie. En cas d'erreur, les suppressions précédemment validées restent effectives, la partie en erreur conserve toutes ses données et le passage s'arrête. V8 implémente cette organisation ; la planification distante reste différée.

## Commandes et découvertes

Décision du 24 octobre sur le contrat de commande : retirer DECLARE_DISCOVERY, inutilisée par le front et toujours rejetée par le moteur. Elle reçoit désormais 400/INVALID_REQUEST comme une commande inconnue, après les contrôles préalables d'accès et d'admission. Aucun changement des conditions de victoire : seuls les indices manuels effectifs déterminent la découverte.

## Reçu de soumission

Décision technique validée : remplacer les instantanés complets du journal idempotent par des reçus compacts. Un retry identique doit restituer exactement la réponse HTTP initiale, même après une correction ou une revanche. Le plateau complet reste lu séparément ; le résultat interne distingue l'état courant du reçu historique. Le reset initial reste différé, sans reprise des anciens jeux de test.

## Interface

Complément du 25 octobre : afficher « Actualisation… » pendant la lecture du statut d'attente, avec annonce accessible, bouton conservé au clavier et retour au salon disponible ; polling inchangé. Au retour « Voir les parties » depuis cette attente, placer le focus sur le titre du salon sans l'ajouter à l'ordre de tabulation.

Complément navigation approuvé le 25 octobre : focus sur le titre du salon après « Voir les parties » depuis l'accueil ou retour depuis le duel ; champ du pseudo après « Changer de pseudo » ; titre de l'historique après « Voir l'historique ». Les onglets Plateau/Historique conservent leur focus. Le texte « Connexion en cours » de l'historique est réservé à une lecture effectivement en cours ; après échec, conserver l'erreur précise existante et les dernières données reçues.

Décision du 24 octobre : reporter l'information des joueurs sur la conservation et les messages associés aux parties indisponibles à la refonte UX. Les formulations et l'emplacement proposés ne sont pas validés pour implémentation. Aucun ajout immédiat près du code de reprise ; la règle de conservation et le choix du cron quotidien restent inchangés.

- Téléphone prioritaire, présentation Également utilisable sur ordinateur.
- Une action principale par phase ; contrôles secondaires discrets ; univers sobre, accents cyan.
- Modes clair et sombre. Les couleurs sont nommées dans la palette et les commandes accessibles.
- Les joueurs connaissent le Mastermind ; l'accueil explique la variante simultanée et les indices manuels.

Les règles détaillées sont dans [GAME_RULES](docs/GAME_RULES.md). L'architecture réellement utilisée est dans [ARCHITECTURE_V2](docs/ARCHITECTURE_V2.md).

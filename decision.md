# Décisions actuelles à Mastermind Arena

## Diffusion et perte de données — 28 octobre 2026

Première protection contre les abus approuvée ensuite : règle WAF Vercel pour les POST d'admission (création, jointure et reprise), budget partagé de 20 requêtes par fenêtre fixe de 60 secondes et par IP ; compteurs régionaux et partage du budget entre joueurs d'un même réseau explicités. Lectures et actions du duel hors de cette règle. Vérifier l'offre et les conditions d'activation avant toute modification distante ; aucun coût ou changement d'abonnement accepté par cette décision. Activation et preuve encore ouvertes dans ROADMAP.

Le concepteur retient un portfolio public avec lien librement diffusé. Pour les futures parties et scores, il accepte la perte en cas d'incident : aucune sauvegarde récupérable à organiser comme exigence de cette livraison. Ce choix ne désactive pas d'éventuelles sauvegardes du fournisseur, n'affirme pas leur présence et n'autorise aucune suppression supplémentaire. Les contrôles d'atomicité, de concurrence et de confidentialité restent requis. La purge après sept jours depuis la dernière action acceptée est inchangée.

## État d'exécution au 27 octobre 2026

Les résultats fournis confirment arena_runtime activé avec les droits limités prévus, un parcours déployé avec reprise sur téléphone et le premier appel automatique réussi de la purge le 27 octobre à 03 h UTC. Les mentions contraires ci-dessous décrivent les décisions avant leur exécution. Nombre supprimé non établi par « 1 row ». Nettoyage public engagé par inventaire, sans suppression effectuée. Preuves dans [VALIDATION](docs/VALIDATION.md).

Validées avec le concepteur le 13 octobre 2026. Les réflexions initiales sur une plateforme générique sont conservées dans [les archives](docs/archive/decision-initiale.md).

## Produit et livraison

Message de partie indisponible approuvé le 27 octobre : « Cette partie est introuvable ou n’est plus disponible. Les parties sont supprimées après 7 jours sans action de jeu. » À utiliser pour ROOM_NOT_FOUND et MATCH_NOT_FOUND, sans changer les codes HTTP ni attribuer une expiration précise à une partie introuvable.

Décisions UX du 27 octobre après audit : séparer erreurs de liste et d'admission, contextualiser création/jointure/reprise sans conclure à un refus lorsque la réponse est incertaine ; conserver les refus métier précis. Garder le bouton d'envoi focalisé et empêcher les doubles actions pendant la requête ; conserver le repère au refus et viser le titre de la phase suivante après succès uniquement si le joueur n'a pas déplacé son focus. Un polling ne déplace pas le focus clavier.

Annoncer les envois du code, de l'essai, des indices, de la correction et de la revanche ; distinguer envoi de revanche et attente de l'accord adverse. Garder l'éditeur et le brouillon de correction pendant l'enregistrement et au refus tant que la cible reste modifiable ; fermer après confirmation. Erreurs de notification visibles jusqu'à fermeture, succès temporaires 2,4 secondes, notifications de correction/vérification persistantes inchangées. Complément validé après essai mobile : erreurs dans un bandeau dans le flux avant le contenu, pour ne pas masquer l'action ; succès restant flottants.

Décision du 27 octobre — états du salon : distinguer « Création en cours… », « Connexion à la partie… » sur la ligne concernée et « Actualisation… », avec annonce accessible et requêtes actuelles conservées. Une jointure ne doit pas afficher un état de création. Les règles, routes et mécanismes de partage/annulation des requêtes restent inchangés.

Complément responsive approuvé le 27 octobre : entre 851 et 1 100 pixels, placer le bouton Rejoindre sous l'identité et le badge pour que son état long ne comprime pas le nom de la partie. Les dispositions mobile et grand écran restent celles prévues.

Décision du 26 octobre — rôle JDBC : créer arena_runtime, avec uniquement les lectures/écritures nécessaires aux cinq tables arena_v1, sans propriété, privilège administratif, suppression ou exécution de purge. Le concepteur valide ce nom et cette séparation. Il choisit le SQL Editor pour définir le mot de passe en privé après information sur sa présence possible dans l'historique/logs ; aucun mot de passe n'est transmis à l'agent. La création distante, l'activation du login et la bascule Vercel restent à effectuer, puis à vérifier.

Décision du 26 octobre : laisser la Data API Supabase désactivée pour Mastermind, qui accède à PostgreSQL via le backend Java/JDBC. Le concepteur confirme l'avoir remise sur OFF après une réactivation temporaire. Conserver pour le moment les logs connus PostgREST « pg_pgrst_no_exposed_schemas » ; ne pas appliquer le contournement de schéma vide/configuration authenticator proposé. Cette décision ne règle ni la séparation du rôle JDBC ni l'activation du cron.

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

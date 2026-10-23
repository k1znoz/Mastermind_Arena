# Décisions actuelles à Mastermind Arena

Validées avec le concepteur le 13 octobre 2026. Les réflexions initiales sur une plateforme générique sont conservées dans [les archives](docs/archive/decision-initiale.md).

## Produit et livraison

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

## Interface

- Téléphone prioritaire, présentation Également utilisable sur ordinateur.
- Une action principale par phase ; contrôles secondaires discrets ; univers sobre, accents cyan.
- Modes clair et sombre. Les couleurs sont nommées dans la palette et les commandes accessibles.
- Les joueurs connaissent le Mastermind ; l'accueil explique la variante simultanée et les indices manuels.

Les règles détaillées sont dans [GAME_RULES](docs/GAME_RULES.md). L'architecture réellement utilisée est dans [ARCHITECTURE_V2](docs/ARCHITECTURE_V2.md).

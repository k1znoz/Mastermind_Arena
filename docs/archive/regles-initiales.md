# Règles de jeu - Mastermind Arena

## 1. Objet du document

Ce document est la source de vérité du gameplay de **Mastermind Arena** dans sa version jeu de plateau en ligne. Il décrit les règles observables par les joueurs, la représentation du plateau et les responsabilités du système.

L'objectif est de reproduire l'expérience d'un plateau de Mastermind physique : deux joueurs se font face, définissent chacun une combinaison secrète, proposent des combinaisons et placent eux-mêmes les pions d'indice.

## 2. Principes fondamentaux

| Principe | Règle |
| --- | --- |
| Nombre de joueurs | Une partie oppose exactement deux joueurs. |
| Combinaisons secrètes | Chaque joueur choisit sa propre combinaison secrète avant le début de la manche. |
| Plateau | Chaque joueur possède un plateau de propositions et une zone d'indices, comme sur un jeu physique. |
| Indices | Les joueurs saisissent manuellement les pions noirs et blancs qu'ils attribuent à la proposition adverse. |
| Calcul des indices | Le serveur ne calcule jamais le nombre de pions noirs ou blancs et ne valide pas leur cohérence avec une combinaison secrète. |
| Alternance | Les rôles de joueur actif et de donneur d'indices alternent à chaque tour. |
| Fin de manche | Une manche se termine lorsqu'un joueur annonce avoir trouvé la combinaison adverse. |

## 3. Matériel virtuel

La partie met à disposition l'équivalent des éléments d'un plateau physique. Les paramètres exacts de la combinaison (longueur, symboles autorisés et nombre de lignes) sont fournis par la configuration de la partie.

| Élément | Description | Propriétaire / visibilité |
| --- | --- | --- |
| Palette de symboles | Ensemble des couleurs ou symboles pouvant être placés dans une combinaison. | Visible par les deux joueurs. |
| Combinaison secrète | Suite de symboles choisie au début de la manche. | Visible uniquement par son propriétaire jusqu'à la fin de la manche. |
| Lignes de proposition | Lignes ordonnées où sont posées les tentatives contre la combinaison adverse. | Visibles par les deux joueurs. |
| Zone d'indices | Emplacements permettant de poser des pions noirs, blancs ou de laisser des emplacements vides. | Renseignée manuellement par le donneur d'indices, puis visible par les deux joueurs. |
| Indicateur de tour | Signale le joueur qui doit agir et le rôle qu'il exerce. | Visible par les deux joueurs. |

## 4. Rôles

Les rôles sont contextuels : chaque joueur est tour à tour auteur de proposition et donneur d'indices.

| Rôle | Responsabilités | Action autorisée pendant le tour |
| --- | --- | --- |
| Joueur actif | Proposer une combinaison pour deviner la combinaison secrète adverse. | Remplir une ligne de proposition et la soumettre. |
| Donneur d'indices | Observer la proposition reçue et donner les indices correspondants à sa discrétion. | Poser manuellement les pions noirs et blancs, puis valider les indices. |

Après la validation des indices, les rôles sont échangés. Ainsi, les deux joueurs avancent simultanément sur leur propre plateau, un échange complet correspondant à une proposition de chaque joueur.

## 5. Déroulement d'une manche

### 5.1 Préparation

1. La partie réunit deux joueurs.
2. La configuration du plateau est affichée aux deux joueurs.
3. Chaque joueur compose et confirme sa combinaison secrète.
4. Tant que les deux combinaisons ne sont pas confirmées, aucun tour ne commence.
5. Une fois les deux combinaisons prêtes, le premier joueur actif est déterminé par la partie. Son adversaire devient donneur d'indices.

Une combinaison confirmée est verrouillée pour la manche. Son propriétaire peut la consulter ; l'adversaire ne peut pas la voir avant la révélation de fin de manche.

### 5.2 Tour de jeu

Un tour se déroule strictement dans l'ordre suivant :

| Étape | Acteur | Action | Résultat |
| --- | --- | --- | --- |
| 1 | Joueur actif | Compose une proposition complète avec les symboles autorisés. | La proposition occupe la prochaine ligne libre de son plateau. |
| 2 | Joueur actif | Soumet la proposition. | La proposition est verrouillée et transmise au donneur d'indices. |
| 3 | Donneur d'indices | Pose zéro ou plusieurs pions noirs et blancs dans la zone d'indices de cette ligne. | Les indices restent modifiables jusqu'à leur validation. |
| 4 | Donneur d'indices | Valide les indices. | Les indices sont verrouillés et deviennent communs aux deux joueurs. |
| 5 | Système | Change le joueur actif. | L'ancien donneur d'indices devient joueur actif pour son propre plateau. |

Un joueur ne peut ni modifier une proposition soumise, ni modifier des indices validés. Une ligne suivante ne peut pas être jouée avant la validation des indices de la ligne précédente.

### 5.3 Signification conventionnelle des pions

Les pions conservent la convention du Mastermind physique. Leur ordre dans la zone d'indices n'a pas de signification : il n'associe jamais un indice à une position précise de la proposition.

| Pion | Signification conventionnelle |
| --- | --- |
| Noir | Un symbole de la proposition est présent dans la combinaison secrète et à la bonne position. |
| Blanc | Un symbole de la proposition est présent dans la combinaison secrète, mais à une autre position. |
| Vide | Aucun indice supplémentaire n'est donné. |

Le donneur d'indices est seul responsable de l'application de cette convention. La partie ne peut pas déduire ni corriger un indice à partir de la combinaison secrète.

## 6. Annonce de découverte et fin de manche

À son tour, un joueur peut annoncer qu'il a trouvé la combinaison secrète adverse. Cette annonce clôt immédiatement la manche ; elle ne nécessite pas de soumission d'une nouvelle ligne ni de confirmation automatique par le serveur.

| Événement | Conséquence |
| --- | --- |
| Un joueur annonce avoir trouvé | La manche passe à l'état terminée. |
| Manche terminée | Les deux combinaisons secrètes sont révélées aux deux joueurs. |
| Manche terminée | Aucune proposition ou aucun indice supplémentaire ne peut être ajouté ou modifié. |
| Nouvelle manche | Une nouvelle préparation est nécessaire, avec deux nouvelles combinaisons secrètes. |

L'annonce est une déclaration de joueur. La V1 n'établit pas automatiquement si la combinaison annoncée est exacte et ne désigne pas de vainqueur calculé par le serveur.

## 7. Responsabilités du système

| Le système gère | Le système ne gère pas |
| --- | --- |
| La présence de deux joueurs dans une partie. | Le calcul des pions noirs et blancs. |
| La configuration et l'affichage du plateau. | La comparaison d'une proposition avec une combinaison secrète. |
| La confidentialité des combinaisons avant la fin de manche. | La vérification de la sincérité ou de l'exactitude des indices. |
| L'ordre des tours et l'alternance des rôles. | La validation automatique d'une annonce de découverte. |
| Le verrouillage des propositions et des indices validés. | L'attribution automatique d'un gagnant. |
| La révélation des combinaisons à la fin de manche. | |

## 8. États de la manche

| État | Condition d'entrée | Actions possibles |
| --- | --- | --- |
| Préparation | Partie créée ou nouvelle manche lancée. | Chaque joueur définit et confirme sa combinaison secrète. |
| Attente de proposition | Les deux combinaisons sont confirmées, ou les indices du tour précédent sont validés. | Seul le joueur actif soumet une proposition ou annonce avoir trouvé. |
| Attente d'indices | Le joueur actif a soumis une proposition. | Seul le donneur d'indices renseigne et valide les indices. |
| Terminée | Un joueur annonce avoir trouvé. | Consultation du plateau et des combinaisons révélées. |

## 9. Périmètre V1 et pistes V2

La V1 privilégie la fidélité au jeu de plateau et l'autonomie des joueurs. Les éléments ci-dessous sont explicitement hors périmètre, mais restent des évolutions possibles.

| Évolution V2 envisagée | Statut V1 |
| --- | --- |
| Classement, historique de victoires et statistiques | Non implémenté. |
| Mode contre une IA | Non implémenté. |
| Vérification automatique des indices ou de la découverte | Non implémentée. |
| Attribution automatique des scores et des vainqueurs | Non implémentée. |
| Variantes de règles, formats de plateau ou modes compétitifs | Non implémentés. |

Toute évolution V2 qui automatise une décision aujourd'hui manuelle devra compléter ce document avec des règles explicites, sans modifier rétroactivement le comportement de la V1.

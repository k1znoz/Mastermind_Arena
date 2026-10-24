# Règles actuelles à Mastermind Arena

Source de référence validée le 13 octobre 2026. Le serveur organise la manche ; il ne compare jamais les secrets aux essais pour calculer les indices.

## Matériel et confidentialité

Deux joueurs composent chacun un secret de quatre couleurs parmi bleu, vert, jaune, rouge, noir et blanc. Les répétitions sont autorisées. Le code reste visible uniquement par son propriétaire pendant la recherche. Les anciennes parties à huit symboles sont des données de l'ancienne version : commencer une nouvelle manche pour utiliser les six couleurs actuelles.

## Cycle simultané

1. Chaque joueur confirme son secret. Quand les deux secrets sont prêts, les propositions sont ouvertes.
2. Chacun envoie un essai. Il reste visible uniquement par son auteur jusqu’à ce que les deux aient envoyé leur essai.
3. Les essais sont révélés. Chaque joueur compare l'essai adverse à son propre secret et saisit les indices manuellement.
4. Noir = couleur à la bonne place. Blanc = couleur à une autre place. La somme des deux compteurs ne dépasse pas quatre ; leur ordre ne désigne aucune case.
5. Les indices restent privés jusqu’à la validation des deux joueurs. Le serveur utilise les indices déclarés pour constater une découverte, sans vérifier leur sincérité.
6. Sans découverte, un nouveau tour commence. Aucune limite temporelle ou de nombre d'essais n'est imposée.

## Résultat du duel et poursuite

- Un joueur reçoit quatre indices noirs : il a trouvé. Le premier tour de découverte détermine le gagnant et donne un point.
- Les deux trouvent au même tour : Égalité, aucun point.
- Un seul trouve : son plateau de recherche s'arrête, mais il continue à donner les indices à l'autre. L'autre joueur peut continuer aussi longtemps qu'il le souhaite ; sa découverte ultérieure ne rapporte pas de point.
- Le résultat du duel est distinct de la fin de recherche. Une manche finit lorsque les deux ont trouvé ou qu'un joueur abandonne.
- En cas d'abandon avant un résultat, l'adversaire reçoit un point. Un résultat déjà attribué est conservé. Les codes sont révélés après l'abandon.
- Une annonce libre de découverte n'est pas une action autorisée : les indices manuels constituent la déclaration observable.

## Confiance, contestation et correction

Les indices sont acceptés par confiance, sans action supplémentaire. Le receveur peut demander une vérification des indices du dernier tour terminé. La demande informe l'autre joueur et ne bloque pas le jeu.

L'auteur des indices peut corriger ceux du dernier tour terminé, y compris après une fin de manche tant qu'une revanche n'a pas démarré. Une correction n'est pas possible sur un tour en cours ou un tour plus ancien. Elle conserve l'action initiale dans le journal, applique les indices effectifs au plateau et informe l'adversaire.

Le résultat est recalculé à partir des indices effectifs : un point peut Être retiré, transféré ou remplacé par une Égalité. Une manche peut reprendre si une découverte est annulée. Les consentements à la revanche sont annulés après une correction : chacun doit accepter à nouveau le résultat actuel. Les propositions déjà validées restent enregistrées.

## Revanche et reconnexion

Après la fin de recherche, chacun peut proposer ou accepter une revanche. Deux consentements sont nécessaires. Les scores restent conservés et les secrets de la nouvelle manche doivent Être choisis à nouveau.

Une déconnexion ne crée ni abandon automatique, ni délai. Le joueur peut reprendre avec son code privé. Ce code est distinct du code d'accès partagé qui protège l'entrée dans une partie ; il donne accès à une place précise, doit rester privé et peut Être utilisé sur un autre appareil.

Conservation validée le 23 octobre ; commande administrative implémentée localement, aucune planification activée : les données d'une partie, même non terminée, seront supprimées après sept jours sans nouvelle action acceptée (création comme référence initiale). Lecture et reconnexion ne prolongeront pas ce délai ; le code privé ne permettra plus une reprise après suppression. Il s'agit d'une expiration des données, pas d'un abandon automatique attribuant un point. Voir [DATA_RETENTION](DATA_RETENTION.md).

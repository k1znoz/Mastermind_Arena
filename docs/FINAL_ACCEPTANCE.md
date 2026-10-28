# Recette finale bornée — Mastermind Arena

Protocole préparé le 28 octobre 2026. **Recette publique déclarée réussie par le concepteur ; L4 clôturé dans le périmètre ci-dessous.** Ne pas le relancer intégralement après chaque correction ciblée. Les résultats détaillés restent dans [VALIDATION](VALIDATION.md).

## Retour du concepteur — 28 octobre 2026

Le concepteur indique avoir effectué les tests et ne signale aucun défaut. Il précise ensuite avoir utilisé **https://mastermind-arena.vercel.app/** et confirme la réussite de la coupure réelle/reconnexion, sans perte de place, doublon ni révélation indue selon le scénario demandé.

Observation exacte fournie : l'écran d'essai disparaît pendant le problème de connexion, ce qui empêche d'envoyer un essai. Le scénario effectivement éprouvé est donc perte de connexion, soumission indisponible puis reconnexion ; aucun envoi hors ligne n'a pu être réalisé. Il ne démontre pas une requête perdue en cours de vol après commit, couverte séparément par les tests d'idempotence.

**L4 clôturé pour ce périmètre public déclaré.** Révision exacte servie et clients précis non fournis ; ne pas attribuer cette recette aux polices locales encore non publiées. Leur rendu reste un contrôle ciblé de livraison, sans nouveau parcours général. Aucun défaut n'est inventé à partir de la seule disparition de l'éditeur.

## Préparer le relevé

Noter la révision testée, l'URL et les deux clients employés. La version locale avec polices locales et la version déployée peuvent différer : ne pas mélanger leurs résultats. Utiliser uniquement une partie de démonstration dédiée ; garder les codes de reprise hors captures, journaux ou dossier public.

## Parcours fonctionnel — deux clients, une partie

Les combinaisons ci-dessous sont des données de test proposées, jamais les secrets d'une partie réelle. Alice et Bob peuvent être deux navigateurs ou un ordinateur et un téléphone.

| Étape | Action de test | Résultat à relever |
| --- | --- | --- |
| 1. Admission | Alice crée une partie dédiée ; Bob la rejoint depuis le second client. Si code d'accès utilisé, essayer une fois un code incorrect puis le bon. | Refus compréhensible, jointure correcte et identité de chacun préservée. Le parcours normal déjà confirmé avec le pare-feu est réutilisable si la même version est testée. |
| 2. Secrets | Alice choisit quatre bleus ; Bob quatre verts. Chacun confirme. | Avant les deux confirmations, aucune composition adverse révélée. Ensuite chacun peut proposer. |
| 3. Tour simultané | Alice propose quatre verts ; Bob quatre jaunes. Alice donne 0 noir/0 blanc à Bob ; Bob donne 4 noirs/0 blanc à Alice. | Actions adverses masquées jusqu'aux conditions prévues. Alice gagne un point, Bob peut poursuivre. |
| 4. Correction du résultat | Sans nouvel essai entre-temps, Bob corrige ses indices de 4 noirs à 3, puis les remet à 4. Il s'agit volontairement d'une modification manuelle pour éprouver le recalcul. | Résultat/score recalculés et notification visible ; le point n'est pas cumulé à chaque correction. Alice finit avec un seul point. |
| 5. Poursuite | Bob propose quatre bleus ; Alice donne 4 noirs. | Bob termine sa recherche, sans point supplémentaire ; historique cohérent. |
| 6. Revanche | Un joueur propose, l'autre accepte. | Pas de nouvelle manche avant l'accord commun ; scores conservés après acceptation. |
| 7. Reprise | Sur la nouvelle manche, retrouver une place avec son code privé depuis un autre client. | Même partie/place et score, pas d'accès à l'identité adverse. La reprise Firefox → téléphone déjà confirmée n'est pas à refaire sans changement concerné. |
| 8. Présentation | Pendant ces actions, observer clair/sombre, texte et commandes dans les deux clients. Sur la candidate locale, confirmer le chargement effectif des polices locales. | Aucun texte indispensable masqué ni commande essentielle inaccessible ; ne pas ouvrir une nouvelle campagne générale de variantes. |

Un abandon confirmé peut remplacer la fin d'une manche dédiée de démonstration si le concepteur souhaite le revoir : il révèle les codes selon les règles. Ouverture, annulation et Échap ont déjà leurs preuves clavier ; ils ne sont pas à refaire comme nouveaux travaux systématiques.

## Réseau physique — scénario encore non vérifié

Sur une manche active dédiée, avec un essai complet préparé :

1. Couper réellement le réseau d'un client de test, sans fermer sa page. Garder le second client connecté.
2. Tenter l'envoi de l'essai sur le client hors ligne ; relever l'erreur et la conservation du plateau/brouillon.
3. Rétablir le réseau. Attendre la synchronisation ; si l'essai n'est pas confirmé, réessayer avec le bouton normal de l'application, sans recréer une partie.
4. Relever une seule action correspondante dans l'historique une fois publiée et la même place privée. Aucun secret adverse ne doit apparaître pendant les phases masquées.

Ce test ne valide pas à lui seul tous les cas de réponse perdue après commit : leur idempotence est couverte séparément par les tests. Il établit le comportement utilisateur après coupure réelle, jusqu'ici absent des preuves. Ne pas demander de code privé ou d'activation du réseau global à l'agent ; le concepteur effectue lui-même la coupure sur son appareil de test.

## Clôture

Consigner pour chaque défaut : étape, attendu, obtenu, gravité et décision corrigé/reporté. Un succès vaut pour la version et les clients relevés ; un scénario non tenté reste non vérifié. L4 est clos lorsque le parcours convenu et le réseau physique sont renseignés et qu'aucun défaut bloquant connu ne reste sans traitement.

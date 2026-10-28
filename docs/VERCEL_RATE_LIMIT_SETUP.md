# Limitation des admissions — règle retenue, activation à vérifier

## État courant — 28 octobre 2026

Le concepteur effectue lui-même le réglage. Sa capture montre arena-admissions avec POST ET Request Path Starts with /local/rooms, fenêtre fixe 60 secondes, 20 requêtes, clé IP Address et réponse 429 : paramètres conformes à la décision. Le 28 octobre, après les instructions d'ajout/publication, il confirme le test normal de création puis jointure depuis un second client. Aucun blocage du parcours habituel déclaré.

Cette réussite ne prouve pas le refus au dépassement. L'état Active après publication, l'offre/règles préexistantes et le retour après expiration de fenêtre n'ont pas été relevés par l'agent. La preuve complète de limitation reste ouverte. Les mentions d'absence de configuration ci-dessous décrivent la préparation antérieure à ces captures et ne sont plus l'état courant.

Préparée et approuvée le 28 octobre 2026 pour le portfolio public : règle Vercel, budget partagé de 20 admissions POST par fenêtre fixe de 60 secondes et par IP, limites régionales et réseau partagé acceptées. **Aucune règle créée ou activée, aucun prix accepté.** L'offre, les règles existantes et les conditions d'activation restent à vérifier sur le projet.

Accès du 28 octobre : tentative d'ouverture de la seule page Firewall du backend ; Chrome non connecté à la session. Reconnexion de cette page demandée au concepteur. Aucun recours au contrôle global du navigateur, aucune modification distante.

## Configuration proposée

| Champ | Proposition |
| --- | --- |
| Projet | Backend mastermind-arena-backend, pas le projet front |
| Nom de règle | arena-admissions |
| Condition | Méthode POST **ET** chemin commençant par /local/rooms |
| Couverture | Création /local/rooms, jointure /local/rooms/join, reprise /local/rooms/resume ; budget commun |
| Clé de comptage | Adresse IP |
| Algorithme | Fenêtre fixe de 60 secondes |
| Seuil | 20 requêtes par fenêtre, point de départ validé à ajuster après observation et accord |
| Action | Réponse 429 ; pas de challenge interactif sur ces appels XHR |
| Hors règle | OPTIONS, lectures GET et actions du duel /local/submit-action |

Le préfixe couvre également les suffixes acceptés par le routage des handlers. Ne pas restreindre la règle au seul domaine canonique en laissant les autres domaines du même projet hors couverture. Vérifier le chemin réellement observé dans les événements Vercel avant publication de la règle ; une autre réécriture d'entrée doit être prise en compte si elle existe.

Le seuil de 20 est un choix de produit/exploitation approuvé, pas un chiffre établi par une mesure. Les créations, tentatives de code et reprises partagent le budget, y compris les échecs. Deux joueurs sur le même réseau partagent aussi ce budget. Une fenêtre fixe n'est pas une limite glissante sur toute période de 60 secondes.

## Pourquoi ce premier dispositif

Filtrer avant Java réduit les appels d'admission et les dérivations de codes arrivant au serveur. Cette option évite d'ajouter un stockage de quotas ou un service au backend pour cette première protection. Une limite en mémoire Java serait propre à chaque instance ; un quota PostgreSQL demanderait une évolution backend/SQL et recevrait quand même les requêtes jusqu'à l'application.

Limites de la règle retenue : contournement possible par plusieurs IP, compteurs régionaux, pas de plafond mondial de salons ni de protection ciblée des lectures/actions du duel par cette règle. Elle complète les contrôles existants sans garantir l'absence d'abus. Une insuffisance observée devra conduire à un lot distinct, pas à une garantie inventée.

## Conditions Vercel vérifiées dans la documentation

La [documentation de limitation WAF](https://vercel.com/docs/vercel-firewall/vercel-waf/rate-limiting), consultée le 28 octobre, annonce la fonction sur tous les plans, une règle de limitation sur Hobby, fenêtre fixe disponible, réponse 429 et compteurs par région. Tarification/volume inclus et règle disponible doivent être contrôlés dans le projet avant toute activation ; ce dossier n'autorise ni frais ni changement d'abonnement.

Les [conditions de règles](https://vercel.com/docs/vercel-firewall/vercel-waf/rule-configuration) peuvent porter sur méthode et chemin. Il faut bien combiner ces deux conditions avec ET. Les trois routes ne sont pas trois conditions de chemin cumulées avec ET : la proposition utilise leur préfixe commun.

## Activation et preuve à préparer après accord

1. Vérifier dans Firewall le plan, la règle disponible et les règles existantes ; ne pas les remplacer silencieusement. Relever la configuration précédente pour annulation.
2. Configurer le brouillon correspondant au tableau. Vérifier l'étendue Preview/Production dans les conditions réelles proposées par Vercel. Ne pas publier une règle sur la production comme simple test.
3. Sur un déploiement isolé couvert par la règle approuvée, utiliser des requêtes invalides sans données privées ni création de partie : elles doivent atteindre le refus habituel avant le seuil, puis être arrêtées par 429 au dépassement. Vérifier retour après renouvellement de fenêtre. Aucun test de charge ou rafale contre la production.
4. Vérifier qu'OPTIONS, GET et une partie déjà active ne sont pas bloqués par cette règle. Tester aussi l'accès depuis le front : front/back étant sur deux origines, un blocage avant Java peut être masqué au JavaScript si la réponse n'a pas les bons en-têtes CORS. Ne pas promettre un message de quota exact avant cette observation. La [documentation CORS Vercel](https://vercel.com/kb/guide/how-to-enable-cors) rappelle la nécessité des en-têtes sur les réponses d'erreur.
5. Après preuve et autorisation d'activation, publier la règle sur la portée voulue ; relever son identifiant, ses paramètres et les observations. En cas de faux positifs, restaurer la version précédente ou désactiver uniquement cette règle via le tableau de bord, selon autorisation. Une simple sauvegarde de brouillon n'active pas la protection.

Critère de sortie : règle choisie et réellement active dans le périmètre convenu, refus au dépassement et rétablissement observés, parcours normal conservé, limites et éventuel message générique CORS documentés. La préparation de ce fichier ne clôture pas la protection L2.

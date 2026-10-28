# Protection contre les abus — inventaire ciblé du 28 octobre 2026

Lecture du code local des admissions et de leurs appelants, sans changement applicatif, requête de charge ou accès à Supabase. Ce document prépare L2 de [ROADMAP](ROADMAP.md) ; il ne constitue pas un audit de sécurité exhaustif ou une vérification du pare-feu Vercel.

## Protections présentes et limites

| Contrôle | Source | Ce qu'il couvre et ce qu'il ne couvre pas |
| --- | --- | --- |
| CORS et clé API à l'entrée des routes salon | [RoomHttpHandlers](../deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/RoomHttpHandlers.java) | Origines navigateur et clé attendue contrôlées. Une clé VITE est publique ; ces contrôles ne limitent pas la fréquence d'un client qui connaît la clé. |
| Corps JSON limité à 65 536 octets | [HttpExchangeSupport](../deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/HttpExchangeSupport.java) | Borne la taille d'un corps lu ; ne borne ni le nombre d'appels ni leur coût cumulé. |
| Noms/pseudos/codes bornés | [JdbcRoomDirectory](../deduction-engine/src/main/java/io/mastermindarena/deduction/infrastructure/jdbc/JdbcRoomDirectory.java) | Nom 60, pseudo 24, code d'accès 64 caractères ; ne limite pas les tentatives. |
| Liste limitée à 100 salons en attente | JdbcRoomDirectory.waitingRooms | Borne le résultat d'une lecture ; ne plafonne pas la création de salons ou leur nombre total. |
| Jointure transactionnelle avec FOR UPDATE | JdbcRoomDirectory.join | Empêche deux invités d'occuper la même place ; ce verrou n'est pas un quota de tentatives. |
| Code d'accès dérivé avec sel et PBKDF2-HMAC-SHA256 | [RoomCredentials](../deduction-engine/src/main/java/io/mastermindarena/deduction/infrastructure/jdbc/RoomCredentials.java) | 120 000 itérations, résultat de 256 bits. Vérification coûteuse à chaque tentative sur salon protégé ; aucune temporisation ou limitation de fréquence ajoutée par cette fonction. |
| Jeton de place aléatoire de 32 octets, empreinte SHA-256 | RoomCredentials ; JdbcRoomDirectory.authorize/resume | Autorisation d'une place et reprise privée, comparaison d'empreintes ; aucun plafond d'essais de reprise dans ces fonctions. |
| Purge quotidienne des parties inactives | [DATA_RETENTION](DATA_RETENTION.md) | Conservation bornée à partir de l'inactivité ; ne protège pas d'un pic de création ou de tentatives pendant la journée. |

## Limitation de fréquence : état constaté

Complément distant déclaré le 28 octobre : paramètres WAF conformes sur la capture du concepteur, puis création/jointure normales confirmées. Pas de preuve de dépassement ou de rétablissement ; ne pas déduire l'efficacité du quota de ce seul parcours réussi. Voir [VERCEL_RATE_LIMIT_SETUP](VERCEL_RATE_LIMIT_SETUP.md).

Dans les handlers salon inspectés, aucun compteur/quota n'est appelé avant création, jointure ou reprise. Recherche complémentaire dans deduction-engine/src/main, .github et vercel.json : aucune occurrence de rate limit, Retry-After, TOO_MANY_REQUESTS, 429 ou throttling. Ce relevé établit l'absence de mécanisme identifié dans ce périmètre source ; il ne prouve pas l'absence de règles configurées dans le tableau de bord Vercel, non consulté ici.

Conséquences fonctionnelles : créations répétées susceptibles de consommer stockage/connexions, essais répétés sur codes d'accès, coût de dérivation des codes et appels de reprise répétés. Aucun incident réel ni exploitation n'est affirmé par cette lecture.

## Décisions et sortie attendue

Diffusion fixée le 28 octobre : portfolio public avec lien librement diffusé. La [règle WAF Vercel](VERCEL_RATE_LIMIT_SETUP.md) est approuvée : 20 admissions POST par fenêtre fixe de 60 secondes et par IP, sans limiter le polling, budget partagé et compteurs régionaux. Restent la vérification de l'offre/règles existantes, la configuration et sa preuve ; aucune protection nouvelle n'est encore active. Page Chrome non connectée lors de la tentative d'accès, reconnexion demandée.

Avant implémentation, fixer : routes couvertes, seuils et fenêtre de temps, emplacement du contrôle, portée entre instances, traitement des utilisateurs partageant une connexion, réponse HTTP et affichage front. Une mesure par instance doit être décrite comme telle. Une mesure qui exige un nouveau service, des droits distants ou une dépendance reste à approuver.

Critère de fin de L2 pour cette partie : dispositif retenu explicitement, comportement sous/seuil et après expiration vérifié sur environnement isolé, erreurs compréhensibles et parties existantes toujours accessibles. Pas de test de charge sur la production. Aucun seuil, stockage ou fournisseur n'est choisi par ce document.

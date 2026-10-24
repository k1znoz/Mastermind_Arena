# Roadmap de finalisation — présentation du 23 novembre 2026

Le chantier de qualité du code est détaillé dans [MAINTENANCE_ROADMAP.md](MAINTENANCE_ROADMAP.md), avec ses exigences dans [CODE_QUALITY.md](CODE_QUALITY.md) et le cadre des interventions dans [AGENTS.md](../AGENTS.md). Les deux roadmaps partagent la même cible de démonstration.

État réconcilié le 24 octobre 2026 : critères locaux du lot 3 validés, compatibilité distante et exploitation de production encore ouvertes. Dernière preuve backend consignée : 56 tests, dont 25 PostgreSQL/HTTP, aucun ignoré, qualité Maven réussie ; restauration/reprise et purge éprouvées sur bases isolées. Dernière suite front : 62 tests, lint/check/build réussis. Refonte en cours : accueil, salon, attente, reprise dédiée et navigation du duel intégrés localement. Bandeau, plateaux, historique et panneau privé encore à refondre ; parcours à deux clients à refaire après intégration. Voir [VALIDATION](VALIDATION.md) et [UI_REDESIGN](UI_REDESIGN.md). Aucune validation de production revendiquée.

## Décisions figées

Voir [decision.md](../decision.md) et [GAME_RULES.md](GAME_RULES.md). Jeu autonome, phases simultanées, six couleurs, essais illimités, indices manuels, poursuite après victoire, correction du dernier tour, reprise privée et revanche commune.

## Lot 1 — Implémentation locale

- [x] Valider côté serveur les combinaisons et les compteurs d'indices.
- [x] Conserver les secrets et tout l'historique au-delà de 50 actions.
- [x] Séparer victoire et fin de recherche ; enlever la limite de dix tours.
- [x] Ajouter abandon, contestation et correction avec recalcul du point.
- [x] Permettre la reprise d'une place avec un code privé.
- [x] Donner priorité à l'action courante sur téléphone ; thèmes clair/sombre.
- [x] Aligner la documentation sur Svelte/Vite, HTTP JDK et JDBC.

## Lot 2 — Validation avant publication

- [x] Vérifier migrations, reprise, rollback et concurrence sur PostgreSQL isolé ; refus d'accès et payloads invalides via le serveur HTTP réel.
- [x] Vérifier le parcours à deux clients avec le moteur réel : mémoire de test le 13 octobre, navigateur/PostgreSQL le 15, puis vrais clients front contre HTTP/JDBC le 18 ; correction du résultat, abandon, revanche et reprise inclus. Voir [VALIDATION.md](VALIDATION.md). Le parcours navigateur final après les derniers durcissements reste ouvert.
- [x] Vérifier les critères locaux du lot 3 : messages, inventaire du workflow, compteurs, reprise après redémarrage réel et stratégie de compatibilité entre les deux déploiements. [WORKFLOW_CONTRACT](WORKFLOW_CONTRACT.md) et [VALIDATION](VALIDATION.md).
- [ ] Clôturer la vérification distante du lot 3 : identifier les versions front/back effectivement publiées et éprouver leur compatibilité avec la candidate avant promotion.
- [ ] Vérifier Data API et permissions effectives Supabase ; préparer les droits d'exécution, la conservation des données et les protections contre les abus.
- [x] Préparer et tester la purge administrative après sept jours d'inactivité : nouveau schéma vide, associations et cascade, aperçu, concurrence et rollback. Aucune reprise des anciennes données de test ; reset initial différé.
- [x] Remplacer les instantanés idempotents par des reçus compacts : réponse HTTP inchangée, replay après correction/revanche/restauration vérifié ; JSON des résultats réduit de 99,52 % sur 402 actions.
- [x] Définir le déclenchement périodique : Supabase Cron quotidien ; procédure SQL commune V8 validée localement avec la commande Java.
- [ ] Configurer, activer et vérifier le cron au déploiement après autorisation ; information après expiration reportée à la refonte UX.
- [x] Éprouver la restauration sur base isolée : pg_dump/pg_restore, procédure V8 restaurée, reprise privée, état/score, replay et revanche vérifiés le 24 octobre.
- [ ] Vérifier le retour arrière des versions candidates selon leur compatibilité ; préparer des commits cohérents, puis vérifier la CI distante.
- [ ] Vérifier téléphone et ordinateur, clavier, contraste, thème clair/sombre et erreurs réseau.

Contrôle ciblé des messages réalisé le 24 octobre sur téléphone/ordinateur et deux thèmes : [40 captures et limites](screenshots/ERROR_CHECKS.md). Ce contrôle ne clôt pas la revue complète clavier/contraste ni le parcours final.
- [ ] Relire les modifications avec le concepteur et ajuster l'UX à son retour.
- [ ] Publier les deux projets selon l'ordre de compatibilité préparé et vérifier une manche sur les URL de production, après autorisation de publication.

La procédure préparée est dans [DEPLOYMENT_COMPATIBILITY](DEPLOYMENT_COMPATIBILITY.md). Avant publication, identifier les versions réellement en ligne et vérifier leur compatibilité avec la candidate ; les tests des lecteurs actuels ne valident pas ces versions historiques.

- [ ] Compléter les mentions légales et la confidentialité avec des informations confirmées.

## Lot 3 — Preuves CDA et démonstration

- [ ] Maquettes/parcours validés et captures correspondant à la version publiée.
- [ ] Modèle relationnel et choix de conception explicités ; scripts SQL et accès JDBC documentés.
- [ ] Jeu d'essai avec entrées, attendu, obtenu et analyse des écarts.
- [ ] Test de sécurité représentatif et trace de résolution d'un problème.
- [ ] Procédure de déploiement, retour arrière et démonstration de secours locale.
- [ ] Répétition de la démonstration ; contrôle Vercel et Supabase avant l'Épreuve.

La plateforme générique et le transport WebSocket ne sont pas nécessaires à la finalisation de ce jeu. La couverture NoSQL doit Être démontrée par une réalisation distincte ou une évolution utile et explicitement décidée, pas par le simple stockage de JSON en PostgreSQL.

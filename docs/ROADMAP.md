# Roadmap de finalisation — présentation du 23 novembre 2026

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
- [x] Vérifier le parcours à deux clients avec le moteur réel et un stockage mémoire de test ; voir [VALIDATION.md](VALIDATION.md). Reproduction PostgreSQL encore nécessaire.
- [ ] Vérifier téléphone et ordinateur, clavier, contraste, thème clair/sombre et erreurs réseau.
- [ ] Relire les modifications avec le concepteur et ajuster l'UX à son retour.
- [ ] Publier les deux projets ensemble et vérifier une manche sur les URL de production.

## Lot 3 — Preuves CDA et démonstration

- [ ] Maquettes/parcours validés et captures correspondant à la version publiée.
- [ ] Modèle relationnel et choix de conception explicités ; scripts SQL et accès JDBC documentés.
- [ ] Jeu d'essai avec entrées, attendu, obtenu et analyse des écarts.
- [ ] Test de sécurité représentatif et trace de résolution d'un problème.
- [ ] Procédure de déploiement, retour arrière et démonstration de secours locale.
- [ ] Répétition de la démonstration ; contrôle Vercel et Supabase avant l'Épreuve.

La plateforme générique et le transport WebSocket ne sont pas nécessaires à la finalisation de ce jeu. La couverture NoSQL doit Être démontrée par une réalisation distincte ou une évolution utile et explicitement décidée, pas par le simple stockage de JSON en PostgreSQL.

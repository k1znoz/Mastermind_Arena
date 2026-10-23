# Roadmap de finalisation — présentation du 23 novembre 2026

Le chantier de qualité du code est détaillé dans [MAINTENANCE_ROADMAP.md](MAINTENANCE_ROADMAP.md), avec ses exigences dans [CODE_QUALITY.md](CODE_QUALITY.md) et le cadre des interventions dans [AGENTS.md](../AGENTS.md). Les deux roadmaps partagent la même cible de démonstration.

État réconcilié le 23 octobre 2026 : implémentation et découpage réalisés localement, durcissement HTTP largement avancé ; finalisation des échanges, exploitation, revue UX et publication encore ouvertes. Les dernières suites consignées datent du 18 octobre (40 tests backend, dont 12 PostgreSQL/HTTP ; 57 tests front ; parcours combiné réussi). Voir [VALIDATION](VALIDATION.md) pour l'environnement et les limites. Aucune nouvelle exécution ni validation de production n'est revendiquée au 23 octobre.

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
- [ ] Clôturer le lot 3 de maintenance : messages d'erreur, métriques, reprise après redémarrage réel et compatibilité entre les deux déploiements.
- [ ] Vérifier Data API et permissions effectives Supabase ; préparer les droits d'exécution, la conservation des données et les protections contre les abus.
- [ ] Éprouver restauration et retour arrière sur base isolée ; préparer des commits cohérents, puis vérifier la CI distante sur la version candidate.
- [ ] Vérifier téléphone et ordinateur, clavier, contraste, thème clair/sombre et erreurs réseau.
- [ ] Relire les modifications avec le concepteur et ajuster l'UX à son retour.
- [ ] Publier les deux projets selon l'ordre de compatibilité préparé et vérifier une manche sur les URL de production, après autorisation de publication.
- [ ] Compléter les mentions légales et la confidentialité avec des informations confirmées.

## Lot 3 — Preuves CDA et démonstration

- [ ] Maquettes/parcours validés et captures correspondant à la version publiée.
- [ ] Modèle relationnel et choix de conception explicités ; scripts SQL et accès JDBC documentés.
- [ ] Jeu d'essai avec entrées, attendu, obtenu et analyse des écarts.
- [ ] Test de sécurité représentatif et trace de résolution d'un problème.
- [ ] Procédure de déploiement, retour arrière et démonstration de secours locale.
- [ ] Répétition de la démonstration ; contrôle Vercel et Supabase avant l'Épreuve.

La plateforme générique et le transport WebSocket ne sont pas nécessaires à la finalisation de ce jeu. La couverture NoSQL doit Être démontrée par une réalisation distincte ou une évolution utile et explicitement décidée, pas par le simple stockage de JSON en PostgreSQL.

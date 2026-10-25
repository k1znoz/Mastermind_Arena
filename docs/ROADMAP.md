# Roadmap de finalisation — démonstration du 23 novembre 2026

État réconcilié le **25 octobre 2026** à partir de [VALIDATION](VALIDATION.md), [UI_REDESIGN](UI_REDESIGN.md) et des décisions du concepteur. Ce fichier est la liste de référence des travaux restants. [MAINTENANCE_ROADMAP](MAINTENANCE_ROADMAP.md) décrit le bilan technique et les limites ; les preuves datées restent dans VALIDATION.

Une case cochée indique une preuve obtenue dans le périmètre nommé. Une préparation locale ne vaut ni validation distante ni publication. Les changements locaux sont encore dans l'arbre de travail ; aucune version candidate publiée n'est attestée.

## Ce qui est déjà acquis

| Périmètre | Preuve disponible | Limite |
|---|---|---|
| Jeu autonome et règles actuelles | Phases simultanées, six couleurs, indices manuels, correction du résultat, poursuite, abandon, revanche et reprise privée implémentés et testés | Aucun changement de règle à prévoir pour finir |
| Front découpé et échanges contrôlés | Session, polling, admission, soumission, brouillons et vues séparés ; **71 tests front**, lint/check/build réussis | Accessibilité complète non attestée |
| Backend et PostgreSQL | **57 tests backend dont 25 PostgreSQL/HTTP**, aucun ignoré ; qualité Maven réussie | Dernière suite backend du 25 octobre ; non répétée pour les seuls compléments UI suivants |
| Parcours réel après refonte | Deux onglets contre Java/JDBC/PostgreSQL isolé : correction rouvrant la recherche, poursuite, fin et revanche ; reprise, égalité et abandon également contrôlés | Deux onglets sur un poste, pas deux appareils physiques |
| Refonte principale | Entrée/salon/attente/reprise, duel, correction, abandon, résultats, historique avant/après et panneau privé intégrés ; clair/sombre | Rendu des cas et régions décrits dans design-qa, pas approbation globale des 17 planches |
| Compléments UX | Attente lente/en erreur, historique lent/en erreur avec retry, destinations de focus approuvées, historique de **400 actions**, noms longs de l'attente/historique et huit largeurs contrôlés | Scénarios complémentaires sur API synthétique ; pas panne réseau physique |
| Exploitation préparée | Reconstruction, rôles SQL, reprise après redémarrage, dump/restauration, purge V8 et reçus compacts éprouvés sur bases isolées | Supabase et déploiements actuels non vérifiés ; cron inactif |

Les règles sont dans [GAME_RULES](GAME_RULES.md), les choix dans [decision.md](../decision.md). Aucun compte, plateforme générique, WebSocket, calcul automatique des indices ou limite de tours n'est nécessaire à cette livraison.

## A — Revue de la candidate locale et finitions ciblées

**Prochain jalon recommandé :** te faire essayer la version locale actuelle, consigner les défauts concrets et corriger ceux qui gênent la démonstration. Ne pas recommencer une refonte générale.

- [ ] **A1 — Revue avec le concepteur.** Parcourir accueil, création/jointure, duel simultané, correction, poursuite, abandon, revanche et reprise. Dresser une liste de défauts avec écran, action, résultat attendu et gravité ; corriger ou reporter explicitement chaque point.
- [ ] **A2 — Chargements et focus encore ouverts.** Vérifier lecture lente de la liste du salon, admissions lentes et résultat des soumissions de secret/essai/indices/correction/revanche. Contrôler le focus après succès, refus et disparition du déclencheur. Réutiliser les contrôles déjà acquis de l'attente et de l'historique.
- [ ] **A3 — Messages à décider.** Valider puis traiter le message de partie indisponible et la durée/persistance des notifications de soumission actuellement affichées 2,4 secondes. Les notifications de correction/vérification sont déjà persistantes ; leur comportement n'est pas à redécider. Sans trace de suppression, ne pas affirmer qu'une partie introuvable a expiré.
- [ ] **A4 — Accessibilité et petits écrans.** Terminer la revue clavier des commandes restantes, mesurer le contraste en clair/sombre, vérifier zoom et lisibilité des résultats/panneau privé avec noms longs. Documenter les défauts et leur résolution. Les huit largeurs et l'historique long déjà contrôlés ne sont pas à reprendre intégralement sans régression.
- [ ] **A5 — Appareils et réseau réels.** Vérifier sur téléphone et ordinateur, deux identités sur deux appareils, coupure puis retour réseau et reprise privée sur l'autre appareil. Confirmer absence de perte de place, d'action doublée et de révélation indue.

**Sortie :** défauts bloquants de la revue corrigés, choix A3 tranchés, contrôles restants documentés. Ne pas déclarer une conformité WCAG complète sur la seule base de contrôles DOM ou de captures.

## B — Préparation de production et informations publiques

Les lectures de configuration distante peuvent être engagées en parallèle de A dès que les accès sont disponibles. Toute exposition confirmée est prioritaire sur une finition visuelle.

- [ ] **B1 — Inventaire distant et confinement.** Identifier les versions front/back en ligne, les réglages de publication des deux projets Vercel, le statut de la Data API et les propriétaires/droits SQL Supabase. Consigner les constats sans secrets ; distinguer état observé et modifications proposées.
- [ ] **B2 — Configuration cible et abus.** Vérifier ou préparer les rôles de migration/exécution, CORS, secrets serveur et variables publiques. Définir puis tester les protections nécessaires contre créations/jointures abusives et essais de codes ; ne pas présenter une limite mémoire par instance comme une protection globale. Faire approuver les choix non encore fixés.
- [ ] **B3 — Sauvegardes et reprise.** Décider fréquence, conservation, perte de données et délai de reprise acceptables ; vérifier ce que fournit Supabase et préparer les accès nécessaires. La restauration locale a déjà sa preuve ; il reste la configuration et la procédure adaptées à la production.
- [ ] **B4 — Mentions légales et confidentialité.** Réunir les informations confirmées sur l'éditeur, les hébergeurs, les contacts et les traitements effectifs, puis compléter les pages et liens. Vérifier les exigences applicables lors de ce travail ; ne pas inventer d'identité, d'adresse ou de politique de sauvegarde.

**Sortie :** configuration cible et points de sécurité connus, décisions d'exploitation fixées, informations publiques complètes. Appliquer les changements Supabase uniquement avec l'autorisation correspondante.

## C — Version candidate identifiée et bascule préparée

- [ ] **C1 — Version et CI.** Relire le diff, séparer le travail en commits cohérents selon autorisation, inclure les fichiers nécessaires et identifier une révision candidate. Vérifier les contrôles locaux pertinents puis la CI sur cette révision. Un push sur main peut publier les deux projets : ne pas l'utiliser comme simple sauvegarde.
- [ ] **C2 — Compatibilité et retour arrière.** Vérifier les anciennes pages, la nouvelle paire front/back, V1–V8 et les formats persistants. Tester les binaires réellement retenus pour le retour arrière ou documenter une correction en avant. Ne pas supposer qu'un ancien backend lit les reçus compacts.
- [ ] **C3 — Répétition de bascule.** Exécuter sur environnement isolé la reconstruction initiale et l'ordre de publication retenu ; préparer la gestion des admissions pendant une éventuelle interruption, les contrôles après migration et les critères d'arrêt. Le mécanisme de maintenance reste à choisir si la compatibilité impose une interruption.

**Sortie :** révision candidate identifiée, CI vérifiée, procédure applicable et répétée avec solution de repli. Référence : [DEPLOYMENT_COMPATIBILITY](DEPLOYMENT_COMPATIBILITY.md). Pas de nouvelle migration ou abstraction sans besoin constaté.

## D — Publication et vérification en ligne

À exécuter après validation de la candidate et autorisation de publication. L'accord sur les règles ou sur les travaux locaux ne vaut pas publication.

- [ ] **D1 — Reset initial et configuration.** Effectuer la remise à zéro des anciennes données de test au moment convenu, installer le schéma et les rôles vérifiés ; contrôler accès serveur et refus des accès non autorisés. Aucune reprise des anciens historiques n'est requise. Cette exception ne concerne pas les futures parties.
- [ ] **D2 — Déployer et contrôler les deux projets.** Publier dans l'ordre préparé ; relever les révisions effectives. Jouer sur les URL de production avec deux places et vérifier confidentialité, correction, poursuite, revanche et reprise ; vérifier aussi l'absence de diagnostic ou secret exposé.
- [ ] **D3 — Activer Supabase Cron.** Programmer la procédure commune une fois par jour, contrôler une exécution et sa désactivation possible. Suppression après sept jours depuis la dernière action acceptée, parties non terminées incluses ; aucun renouvellement par consultation/reconnexion/replay. Ne pas promettre une suppression à l'heure exacte du septième jour.

**Sortie :** version publiée identifiée, manche et reprise en ligne vérifiées, droits et cron contrôlés. Une compilation ou un PASS local ne coche aucune de ces tâches.

## E — Preuves CDA et préparation de la présentation

Travail de sélection, d'explication et de mise en forme des preuves existantes, pas reconstruction arbitraire de fonctionnalités.

- [ ] **E1 — Couverture du référentiel.** Réconcilier [CDA_COVERAGE](CDA_COVERAGE.md) avec les exigences fournies ; distinguer preuve acquise, pièce à produire et compétence à démontrer ailleurs. Décrire honnêtement conception personnelle et écriture du code largement assistée.
- [ ] **E2 — Dossier de preuves.** Rassembler modèle relationnel/SQL/JDBC, choix d'architecture, jeu d'essai attendu/obtenu, sécurité et résolution d'un défaut, déploiement/repli ; lier chaque affirmation à un fichier ou une exécution. Les tests existent déjà ; leur présentation CDA reste à finaliser.
- [ ] **E3 — Captures et démonstration.** Capturer la version effectivement publiée, préparer un scénario court et un secours local reproductible, répéter et vérifier Vercel/Supabase avant le 23 novembre.

**Sortie :** démonstration répétée, dossier explicable et preuves rattachées à une version. JSON dans PostgreSQL ne constitue pas à lui seul une réalisation NoSQL.

## Ordre de travail et entretien du suivi

Ordre recommandé : **A → C → D**, avec **B en parallèle de A et achevé avant D**, et **E préparé en parallèle puis finalisé sur la version publiée**. Les constats de sécurité B1 peuvent avancer immédiatement ; un risque confirmé passe avant A.

Prochaine action concrète : préparer la revue A1 et une liste bornée des vérifications A2/A4, puis te laisser essayer la candidate. Les choix de messages, de protections, de sauvegardes ou de publication non réglés seront soumis au concepteur avant le travail dépendant.

Ne pas ajouter une nouvelle tâche générale « vérifier toute l'UX » après chaque correctif. Nommer le scénario manquant, réutiliser les preuves encore pertinentes et clôturer le point lorsque son critère est atteint. Actualiser ce fichier après chaque lot ; conserver commandes, résultats et incidents dans VALIDATION. Les dates des anciens lots étaient indicatives et ne sont plus utilisées comme statut d'avancement.

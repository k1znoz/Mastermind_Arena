# Roadmap de livraison — présentation du 23 novembre 2026

Réconciliée le **28 octobre 2026**. Cette liste remplace l'ancien enchaînement A → B → C → D. Les preuves détaillées restent dans [VALIDATION](VALIDATION.md) ; le bilan technique reste dans [MAINTENANCE_ROADMAP](MAINTENANCE_ROADMAP.md). Les identifiants A–E ne servent plus qu'à retrouver les anciennes références.

## 1. Acquis — ne pas recommencer

| Travail | État et preuve | Limite |
| --- | --- | --- |
| Jeu et backend | Règles simultanées, six couleurs, indices manuels, correction du résultat, poursuite, abandon, revanche et reprise privée livrés | Aucun nouveau mécanisme de jeu requis |
| Front et refonte | Écrans intégrés, clair/sombre, découpage et échanges contrôlés | Recette finale du concepteur à recueillir |
| Chargements, erreurs et focus — A2/A3 | Terminés localement ; 83 tests front, lint/check/build réussis le 27 octobre | Correctifs non publiés |
| Contrôles A4 exécutés | Clavier, contrastes mesurés, petits écrans, noms maximaux et zoom réel dans les cas du [relevé A4](UX_A4_2026-10-27.md) ; A4-01/A4-02 corrigés et contre-vérifiés | Pas de conformité globale ni de test audio revendiqué |
| PostgreSQL et exploitation — D1/D3, partie de B1/B2 | arena_v1, rôle restreint arena_runtime, RLS/droits, nettoyage initial public et purge quotidienne 03 h UTC contrôlés selon les résultats fournis | Ne pas refaire le reset ; futures parties à préserver |
| Version publiée — D2 | Deux projets Ready sur main/fd500eb d'après les captures fournies ; partie et reprise mobile déclarées réussies | Ne couvre pas la candidate locale suivante |
| Backend et restauration | 57 tests backend dont 25 PostgreSQL/HTTP, qualité Maven ; reconstruction/restauration isolées documentées | Tests backend du 25 octobre ; sauvegarde de production non décidée |

**Périmètre constaté le 28 octobre :** HEAD est fd500eb, identique à la référence publiée déclarée. Les changements applicatifs locaux concernent le front ; backend, migrations, clients HTTP front et stockage de session sont inchangés dans le périmètre inspecté. Pas de nouveau reset ni de migration pour ce lot. [Préparation de la candidate](RELEASE_CANDIDATE_2026-10-28.md).

## 2. Nécessaire pour terminer la livraison

### L1 — Candidate front et préparation de publication (ancien C1/C2/C3)

- [x] Délimiter le lot depuis fd500eb : front et documentation ; backend/migrations/protocole/stockage navigateur inchangés.
- [x] Préparer le dossier de publication : fichiers nouveaux indispensables, preuves réutilisées, CI attendue, procédure et repli adaptés à ce lot sans migration.
- [ ] Identifier une révision candidate incluant les fichiers nouveaux, selon autorisation de commit ; vérifier la CI distante sur cette révision.
- [ ] Confirmer le déploiement front de repli et son accessibilité dans Vercel avant publication ; vérifier le repli selon le dossier de candidate.

**Sortie :** candidate identifiée, contrôles rattachés, CI connue et repli front praticable. La préparation locale n'autorise aucun push sur main. Pas de répétition de bascule SQL pour ce lot front.

### L2 — Exploitation restante (restes de B1/B2/B3)

- [x] Diffusion fixée le 28 octobre : portfolio public, lien librement diffusé.
- [x] Inventorier les protections présentes : [relevé ciblé](ABUSE_PROTECTION.md). Autorisation, taille et champs bornés présents ; aucune limitation de fréquence identifiée dans les handlers d'admission inspectés. Pare-feu distant non vérifié.
- [x] Première protection approuvée le 28 octobre : [règle WAF Vercel](VERCEL_RATE_LIMIT_SETUP.md), budget commun de 20 POST d'admission/60 s/IP, compteurs régionaux, sans migration. Lectures et actions du duel hors règle ; limites de réseau partagé acceptées.
- [x] Paramètres de la règle vérifiés sur capture du concepteur : POST ET préfixe /local/rooms, 20/60 s/IP, fenêtre fixe et 429. Création puis jointure normales confirmées par le concepteur le 28 octobre ; pas de blocage habituel déclaré.
- [ ] Compléter la preuve de protection : état publié/actif, offre/règles existantes, refus au dépassement puis retour après fenêtre et comportement cross-origin. Le réglage est effectué par le concepteur ; aucune rafale de production autorisée. Pas de nouvelle décision de seuil à redemander.
- [x] Perte des futures parties/scores en cas d'incident acceptée le 28 octobre, décision consignée dans decision.md et DATABASE_RECOVERY. Pas de sauvegarde récupérable à mettre en place comme exigence de livraison ; aucune promesse de restauration. Purge après sept jours d'inactivité inchangée.
- [ ] Vérifier les réglages distants encore non consignés pour la candidate : variables publiques/serveur, CORS nécessaire et déclenchement/promotion des deux projets Vercel.

**Sortie :** choix d'exploitation explicites et protections correspondantes vérifiées. Droits SQL et cron acquis ne sont pas réouverts sans changement ou incident.

### L3 — Informations publiques (ancien B4)

- [ ] Réunir les informations d'éditeur/contact/hébergeurs et les traitements effectifs ; vérifier les exigences applicables à la diffusion retenue.
- [ ] Compléter pages et liens avec les informations confirmées ; ne pas inventer identité, adresse, sauvegarde ou conformité.

**Sortie :** informations adaptées au site et à son éditeur. Les décisions personnelles sont demandées avant rédaction définitive.

### L4 — Recette finale bornée avec le concepteur (ancien A1/A5)

- [ ] Essayer une fois la candidate sur un parcours complet : création/jointure, essais/indices, correction, résultat/poursuite, revanche et reprise. Consigner les seuls défauts constatés ; corriger ou reporter explicitement.
- [ ] Exécuter une coupure puis un retour réseau physiques : place retrouvée, pas d'action doublée ni de révélation indue. Réutiliser la preuve Firefox → téléphone pour la reprise normale.

**Sortie :** aucun défaut bloquant connu pour la démonstration ; scénario réseau renseigné. Pas de nouvelle matrice générale après chaque correction.

### L5 — Publier les correctifs (nouvelle publication après D2)

- [ ] Obtenir l'autorisation de publication de la candidate identifiée ; vérifier L1–L4 et les décisions de report éventuelles.
- [ ] Publier sans opération SQL pour ce lot ; contrôler la paire réellement en ligne et la reprise d'une place existante.
- [ ] Consigner révisions/déploiements et recette. En cas d'échec, appliquer le repli front prévu ; ne pas restaurer la base pour un défaut front.

**Sortie :** correctifs locaux réellement en ligne, référence de version et résultat de recette.

### L6 — Preuves CDA et démonstration (ancien E1–E3)

- [ ] Relier [CDA_COVERAGE](CDA_COVERAGE.md) aux preuves existantes ; nommer les compétences à démontrer ailleurs.
- [ ] Préparer les pièces SQL/JDBC, architecture, sécurité, tests, incident résolu et déploiement. Distinguer conception personnelle et écriture largement assistée par Copilot.
- [ ] Préparer un scénario court, ses captures sur version publiée, un secours local et une répétition avant le 23 novembre.

**Sortie :** démonstration répétée et dossier explicable, preuves rattachées à une version.

## 3. Compléments reportables — aucun lancement automatique

- Test audio au lecteur d'écran : non réalisé ; aucun prêt côté concepteur. Aucune installation autorisée. Ce manque reste visible et interdit de revendiquer une validation complète de l'accessibilité ; il n'empêche pas les autres lots d'avancer.
- Extension des matrices clavier/zoom/contraste : uniquement sur défaut concret, changement concerné ou demande explicite. Les cas non testés ne deviennent pas des cas réussis.
- Finitions visuelles sans défaut bloquant, plateforme multijeux, comptes et autres extensions : hors livraison actuelle.

Tout défaut empêchant de jouer, exposant un secret ou perdant une place remonte dans les nécessaires. Cette séparation n'est ni une dispense juridique ni une certification d'accessibilité.

## Ordre et point d'arrêt

**En cours : L2, choix et préparation de la limitation des abus pour le portfolio public.** Diffusion et perte acceptable sont fixées. Le dossier local L1 est préparé ; commit/CI/repli distant restent ouverts. L3 et L6 peuvent avancer indépendamment lorsque leurs informations sont disponibles. L4 est une seule recette bornée ; L5 intervient après ses prérequis et autorisation.

À la fin de chaque lot : livrable, preuve, limite et prochaine action. Actualiser cette liste sans y recopier le journal chronologique. Quand le lot est terminé ou attend une décision externe, passer au travail indépendant autorisé au lieu de relancer une revue UX générale.

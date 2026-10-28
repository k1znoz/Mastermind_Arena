# Roadmap de livraison — présentation du 23 novembre 2026

Réconciliée le **28 octobre 2026**. Cette liste remplace l'ancien enchaînement A → B → C → D. Les preuves détaillées restent dans [VALIDATION](VALIDATION.md) ; le bilan technique reste dans [MAINTENANCE_ROADMAP](MAINTENANCE_ROADMAP.md). Les identifiants A–E ne servent plus qu'à retrouver les anciennes références.

## 1. Acquis — ne pas recommencer

| Travail | État et preuve | Limite |
| --- | --- | --- |
| Jeu et backend | Règles simultanées, six couleurs, indices manuels, correction du résultat, poursuite, abandon, revanche et reprise privée livrés | Aucun nouveau mécanisme de jeu requis |
| Front et refonte | Écrans intégrés, clair/sombre, découpage et échanges contrôlés | L4 clôturé sur le site public selon le concepteur ; ajouts locaux hors de cette preuve |
| Chargements, erreurs et focus — A2/A3 | Terminés localement ; 83 tests front, lint/check/build réussis le 27 octobre | Inclus dans 01735e5 ; ajouts locaux postérieurs distincts |
| Contrôles A4 exécutés | Clavier, contrastes mesurés, petits écrans, noms maximaux et zoom réel dans les cas du [relevé A4](UX_A4_2026-10-27.md) ; A4-01/A4-02 corrigés et contre-vérifiés | Pas de conformité globale ni de test audio revendiqué |
| PostgreSQL et exploitation — D1/D3, partie de B1/B2 | arena_v1, rôle restreint arena_runtime, RLS/droits, nettoyage initial public et purge quotidienne 03 h UTC contrôlés selon les résultats fournis | Ne pas refaire le reset ; futures parties à préserver |
| Version publiée — D2 | Deux projets Ready sur main/fd500eb d'après les captures fournies ; partie et reprise mobile déclarées réussies | Ne couvre pas la candidate locale suivante |
| Backend et restauration | 57 tests backend dont 25 PostgreSQL/HTTP, qualité Maven ; reconstruction/restauration isolées documentées | Nouvelle preuve CI du 28 octobre sur 01735e5 ; perte des parties acceptée, pas de restauration promise |

**Périmètre actualisé le 28 octobre :** HEAD est 01735e5, intégrant le travail précédent ; polices locales, message d'admission 429 et pages d’information restent dans le travail courant. Dernière recette publique déclarée réussie, sans SHA exact relevé ; GitHub signale les déploiements de 01735e5 terminés sans prouver leurs alias. Les différences applicatives depuis fd500eb concernent le front ; backend/migrations/contrat serveur et stockage de session inchangés. roomClient ajoute uniquement la traduction locale du statut 429. Pas de reset ni de migration. [Candidate et manifeste de 107 fichiers](RELEASE_CANDIDATE_2026-10-28.md).

## 2. Nécessaire pour terminer la livraison

### L1 — Candidate front et préparation de publication (ancien C1/C2/C3)

- [x] Délimiter le lot depuis fd500eb : front et documentation ; backend/migrations/protocole/stockage navigateur inchangés, gestion locale du refus HTTP 429 ajoutée côté admissions.
- [x] Préparer le dossier de publication : fichiers nouveaux indispensables, preuves réutilisées, CI attendue, procédure et repli adaptés à ce lot sans migration.
- [x] Vérifier la CI distante de la révision intermédiaire 01735e5 : Quality #4, 83 tests front et 57 backend dont 25 PostgreSQL, aucun ignoré ; deux statuts Vercel success. [Preuve distante](CI_EVIDENCE_2026-10-28.md). Ne couvre pas les polices locales non commitées ni les alias de production.
- [ ] Identifier une révision candidate incluant les fichiers nouveaux, selon autorisation de commit ; vérifier la CI distante sur cette révision.
- [ ] Confirmer le déploiement front de repli et son accessibilité dans Vercel avant publication ; vérifier le repli selon le dossier de candidate.

**Sortie :** candidate identifiée, contrôles rattachés, CI connue et repli front praticable. La préparation locale n'autorise aucun push sur main. Pas de répétition de bascule SQL pour ce lot front.

### L2 — Exploitation restante (restes de B1/B2/B3)

- [x] Diffusion fixée le 28 octobre : portfolio public, lien librement diffusé.
- [x] Inventorier les protections présentes : [relevé ciblé](ABUSE_PROTECTION.md). Autorisation, taille et champs bornés présents ; aucune limitation de fréquence identifiée dans les handlers d'admission inspectés. Pare-feu distant non vérifié.
- [x] Première protection approuvée le 28 octobre : [règle WAF Vercel](VERCEL_RATE_LIMIT_SETUP.md), budget commun de 20 POST d'admission/60 s/IP, compteurs régionaux, sans migration. Lectures et actions du duel hors règle ; limites de réseau partagé acceptées.
- [x] Paramètres de la règle vérifiés sur capture du concepteur : POST ET préfixe /local/rooms, 20/60 s/IP, fenêtre fixe et 429. Création puis jointure normales confirmées par le concepteur le 28 octobre ; pas de blocage habituel déclaré.
- [x] Traiter un 429 lisible côté admissions avec le message approuvé, sans nouvelle admission ni retry automatique ; tests HTML/vide/JSON et trois opérations, 85 tests front et lint/check/build réussis. Le fallback réseau/CORS reste prudent. [Détails](ERROR_MESSAGES.md).
- [ ] Compléter la preuve de protection : état publié/actif, offre/règles existantes, refus au dépassement puis retour après fenêtre et comportement cross-origin. Le réglage est effectué par le concepteur ; aucune rafale de production autorisée. Pas de nouvelle décision de seuil à redemander.
- [x] Perte des futures parties/scores en cas d'incident acceptée le 28 octobre, décision consignée dans decision.md et DATABASE_RECOVERY. Pas de sauvegarde récupérable à mettre en place comme exigence de livraison ; aucune promesse de restauration. Purge après sept jours d'inactivité inchangée.
- [ ] Vérifier les réglages distants encore non consignés pour la candidate : variables publiques/serveur, CORS nécessaire et déclenchement/promotion des deux projets Vercel.

**Sortie :** choix d'exploitation explicites et protections correspondantes vérifiées. Droits SQL et cron acquis ne sont pas réouverts sans changement ou incident.

### L3 — Informations publiques (ancien B4)

- [x] Relever les traitements dans le code et les services utilisés : [préparation L3](PUBLIC_INFORMATION_PREPARATION.md). Partie/stockage navigateur/logs distingués, chargement Google Fonts identifié, sources CNIL officielles consultées.
- [x] Cadre/identité confirmés : projet personnel en France, sans monétisation, éditeur Aloïs Sautet. [Textes de travail](PUBLIC_INFORMATION_DRAFT.md) préparés ; contact à compléter plus tard selon sa demande, pas de valeur fictive publiée.
- [x] Remplacer Google Fonts par les mêmes polices locales avec licences ; lint/check/build et distribution HTTP/empreintes contrôlés. [Preuves](LOCAL_FONTS.md). Rendu des pages d’information contrôlé dans Chrome ; contrôle ciblé après livraison, sans rouvrir L4.
- [ ] Compléter le contact public et les coordonnées/modalités des hébergeurs ; finaliser les fondements, durées des journaux et informations de droits/transferts pertinentes.
- [x] Coder les deux pages locales et les liens de pied de page en nouvel onglet avec indication accessible ; conserver les saisies du jeu. Trois entrées construites, rendu et navigation vérifiés dans Chrome. [Preuve locale](INFORMATION_PAGES.md).
- [x] Rechercher les obligations applicables et compléter les clauses connues : [analyse juridique sourcée](LEGAL_REVIEW_2026-10-28.md), droits et recours CNIL, données techniques et transferts. Le concepteur maintient deux pages et son nom ; coordonnées à finaliser avant publication. Lint/check/build et rendu HTML contrôlés après ces ajouts ; aucune conformité complète revendiquée.
- [ ] Compléter les textes publics après réception du contact et vérification des mentions restantes ; aucun champ fictif ni conformité revendiquée.

**Sortie :** informations adaptées au site et à son éditeur. Les décisions personnelles sont demandées avant rédaction définitive.

### L4 — Recette finale bornée avec le concepteur (ancien A1/A5)

**L4 clôturé le 28 octobre dans le périmètre déclaré du site public https://mastermind-arena.vercel.app/.** Le concepteur confirme la recette et la coupure réelle sans perte de place, doublon ni révélation. Pendant le défaut de connexion, l'éditeur d'essai disparaît et interdit la soumission. Aucun envoi hors ligne effectivement effectué, aucun cas de réponse perdue après commit déduit de cet essai. Révision exacte servie non relevée ; les polices locales non publiées restent à contrôler lors de leur livraison, sans rouvrir tout L4.

- [x] Préparer un seul [protocole de recette](FINAL_ACCEPTANCE.md), avec version/clients relevés, parcours simultané/correction/poursuite/revanche et coupure physique. Ce document ne vaut pas exécution.
- [x] Parcours FINAL_ACCEPTANCE déclaré réussi par le concepteur sur le site public : aucun défaut signalé. Ce résultat ne couvre pas des assets ou changements locaux non publiés.
- [x] Coupure puis retour réseau physiques confirmés sur ce site : place retrouvée, aucun doublon ni révélation déclaré ; éditeur masqué pendant la déconnexion, donc soumission empêchée. Ne pas présenter ce scénario comme un envoi hors ligne ou une réponse perdue après commit.

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

**État courant :** code local L3 livré et vérifié ; contact et informations juridiques restantes ouverts. L2 attend la preuve distante de la règle déjà choisie/configurée. L1 attend la révision complète, sa CI et le repli. L4 public est clôturé. Suite : finaliser ces prérequis, publication autorisée L5, puis pièces et répétition CDA L6 ; aucun nouvel audit UX général.

À la fin de chaque lot : livrable, preuve, limite et prochaine action. Actualiser cette liste sans y recopier le journal chronologique. Quand le lot est terminé ou attend une décision externe, passer au travail indépendant autorisé au lieu de relancer une revue UX générale.

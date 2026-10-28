# Informations publiques — préparation L3 du 28 octobre 2026

Inventaire fondé sur le code courant, pour préparer mentions légales et confidentialité du portfolio public. **Document de travail, pas une page publiée ni une attestation de conformité.** Les pages partielles sont désormais implémentées localement ; [code et preuves](INFORMATION_PAGES.md).

## Informations demandées au concepteur

- Complément juridique sourcé : [analyse du 28 octobre](LEGAL_REVIEW_2026-10-28.md). Deux pages maintenues, sans troisième page CGU ni case d’acceptation. Le nom Aloïs Sautet reste affiché ; le concepteur reporte la finalisation des coordonnées après explication des exigences de la LCEN. Droits, recours CNIL et traitements techniques sont maintenant rédigés et intégrés ; contact, fondements définitifs et réglages des prestataires restent ouverts.
- Pays et cadre confirmés : projet personnel en France, sans entreprise ni monétisation du jeu. Ce cadre déclaré ne constitue pas une qualification juridique de conformité.
- Identité publique fournie explicitement : Aloïs Sautet. Contact à compléter plus tard selon sa demande ; aucune adresse inventée ni déduite d'un fichier.
- Polices locales approuvées et implémentées : mêmes familles/graisses, notices et licences conservées. [Preuves](LOCAL_FONTS.md).
- [Textes de travail](PUBLIC_INFORMATION_DRAFT.md) préparés ; pages locales intégrées avec les seuls faits confirmés ; publication après finalisation des informations restantes.

## Données et finalités relevées dans le code

| Ensemble | Données / usage | Conservation et accès constatés |
| --- | --- | --- |
| Salon | Nom de partie, pseudos, identifiant et date de création ; création/jointure et présentation des parties en attente | PostgreSQL. La liste des salons en attente expose nom et pseudo du créateur aux clients qui consultent cette liste. Aucun compte ou e-mail demandé par le formulaire de jeu. |
| Place privée | Jeton d'accès à une place, identifiant d'acteur et de partie ; autorisation et reprise | Jeton de 32 octets généré côté serveur ; empreinte SHA-256 en base, jeton utilisable transmis au navigateur. Ce n'est pas une donnée anonyme. |
| Code d'accès partagé | Accès facultatif au salon | Sel et dérivé PBKDF2 en base ; ne pas le confondre avec la reprise privée. |
| Partie et historique | Codes de jeu, essais, indices manuels/corrections, résultat/scores, versions et événements ; jeu simultané, affichage et reprise fiable | PostgreSQL ; réponses filtrées selon la place et la phase. Les données du jeu sont partagées avec l'adversaire selon les règles, pas déclarées publiques intégralement. |
| Idempotence | Identité de l'action, reçu et événements associés ; éviter les doubles actions et restituer une réponse perdue | Suppression avec la partie selon la procédure commune, pas une conservation indépendante illimitée. |
| Mémoire locale de place | Partie, acteur, pseudo, nom de salon et jeton dans sessionStorage ; copie dans localStorage sous arenaSavedRoom | Aucune date d'expiration applicative dans session.js. Oublier ma place supprime les clés de place ; la purge du serveur n'efface pas les stockages des appareils. |
| Préférence de thème | arenaTheme dans localStorage ; conserver le choix clair/sombre | Pas de durée d'expiration codée ; distinct de l'oubli de place. Suppression par les fonctions de gestion des données du navigateur. |
| Logs applicatifs | Route, méthode, statut, identifiant de requête, durées/compteurs ; diagnostic et exploitation | HttpObservability écrit vers stdout ; pas de règle locale de purge de ces logs avec la partie. Son format n'inclut pas pseudo, jeton ou corps de requête. |

Sources : [session.js](../App_Front/src/lib/state/session.js), [App.svelte](../App_Front/src/App.svelte), [JdbcRoomDirectory](../deduction-engine/src/main/java/io/mastermindarena/deduction/infrastructure/jdbc/JdbcRoomDirectory.java), [RoomCredentials](../deduction-engine/src/main/java/io/mastermindarena/deduction/infrastructure/jdbc/RoomCredentials.java), [HttpObservability](../deduction-engine/src/main/java/io/mastermindarena/deduction/api/submitaction/HttpObservability.java), [règles du jeu](GAME_RULES.md).

Le nom de salon et les pseudos sont du texte libre : ne pas les qualifier systématiquement de données non personnelles ni prétendre que le jeu est anonyme parce qu'il n'a pas de compte.

## Conservation : formulation de travail

« Les parties sont supprimées après sept jours sans action de jeu. La suppression est exécutée par une tâche quotidienne ; un incident peut la retarder. Les données enregistrées dans votre navigateur restent gérées séparément. Vous pouvez oublier votre place sur cet appareil ; cela ne supprime pas immédiatement la partie sur le serveur. »

Cette formulation décrit la [rétention décidée](DATA_RETENTION.md). Le concepteur accepte la perte de parties/scores en cas d'incident ; aucune restauration n'est promise. L'absence de sauvegarde organisée par le projet ne signifie pas absence de copies, logs ou traitements propres aux fournisseurs.

## Services et appels externes

- Vercel héberge le front et le backend selon la configuration déclarée. Sa [notice de confidentialité](https://vercel.com/legal/privacy-notice) décrit notamment des données de trafic des utilisateurs finaux. Durées effectives des logs et réglages du projet non relevés : ne pas appliquer automatiquement le délai de purge du jeu à ces journaux.
- Supabase héberge PostgreSQL. Sa [politique](https://supabase.com/privacy) distingue les données du compte client des données confiées par les applications ; pour ces dernières, il renvoie à son accord de traitement. Région/garanties/transferts applicables au projet à confirmer avant texte définitif. Ne pas prétendre à une conservation uniquement en France ou dans l'UE à partir du seul hôte JDBC.
- Le chargement initial de Space Grotesk, JetBrains Mono et Inter depuis Google a été remplacé par [fonts.css](../App_Front/src/fonts.css) et les fichiers locaux, après accord. La [documentation Google](https://developers.google.com/fonts/docs/technical_considerations) décrit le mécanisme précédent ; les sources/licences sont conservées, aucune URL de chargement Google ne reste dans la CSS construite. [Preuves locales](LOCAL_FONTS.md).
- Recherche dans src, index.html et package.json : aucun SDK de mesure d'audience ou publicité identifié. Cela ne constitue pas une inspection réseau de la production ni une preuve d'absence de cookies/traceurs de plateforme. Les réglages Vercel ne sont pas déduits du seul package.json.

## Exigences à appliquer après confirmation du cadre

Sources officielles consultées le 28 octobre :

- [CNIL — mentions légales](https://www.cnil.fr/fr/cnil-direct/question/les-mentions-legales-sur-un-site-web-est-ce-obligatoire) : distinguer identification du responsable/hébergeur et information sur les données.
- [Service public — site professionnel](https://entreprendre.service-public.gouv.fr/vosdroits/F37351?lang=fr) : les informations dépendent de l'éditeur ; ne pas appliquer une fiche entreprise comme réponse automatique à un projet personnel.
- [CNIL — information des personnes](https://www.cnil.fr/fr/conformite-rgpd-information-des-personnes-et-transparence) : identité/contact, finalités, fondement, destinataires, conservation et exercice des droits, avec les compléments pertinents pour les transferts. Le fondement et les informations propres à l'éditeur ne sont pas encore arrêtés.
- [CNIL — cookies et traceurs](https://cnil.fr/fr/cookies-et-autres-traceurs/que-dit-la-loi) : l'exigence de consentement dépend des finalités et des exemptions applicables, pas du seul emploi du mot cookie. Ne pas ajouter un bandeau générique avant qualification des usages réels.

## Texte public encore à terminer

Mentions légales : identification adaptée au cadre confirmé, contact, responsable de publication le cas échéant et coordonnées vérifiées des hébergeurs. Les noms commerciaux Vercel/Supabase ne suffisent pas à remplir tous les champs.

Confidentialité : présentation simple des finalités du jeu et de la reprise, visibilité des pseudos, données locales et leur suppression, conservation serveur/logs distincte, services destinataires, fondement et droits/contact. Définir comment traiter une demande sans réclamer un code privé dans un e-mail ordinaire ; aucun nouveau formulaire ni mécanisme de suppression distante n'est choisi ici.

Identité/cadre et polices sont réglés ; le contact est volontairement reporté. Les textes de travail sont préparés sans valeur fictive installée dans l'interface. Restent la finalisation des informations d'hébergement/traitement et du contact, avant publication des pages et liens déjà codés localement.

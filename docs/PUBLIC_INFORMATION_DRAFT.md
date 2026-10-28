# Mentions et confidentialité — textes de travail

Actualisation du 28 octobre : deux pages locales et pied de page intégrés ; [preuves et limites](INFORMATION_PAGES.md). 85 tests front réussis, lint/check sans erreur, build de 181 modules et trois documents HTML. Contrôles Chrome bornés ; aucun déploiement. Contact et mentions restantes ouverts.

Préparés le 28 octobre 2026. **Non publiables en l'état** : contact laissé volontairement à compléter selon le choix du concepteur, coordonnées légales des hébergeurs et modalités/fondement des traitements encore à finaliser. Aucun de ces champs ne sera remplacé par une valeur inventée dans l'interface.

## 1. Mentions légales — rédaction proposée

### Éditeur

Mastermind Arena est édité par **Aloïs Sautet**, en France. Ce jeu gratuit est un projet personnel présenté dans un portfolio et dans le cadre d'une formation. Il n'est pas monétisé.

**Contact : à compléter avant publication.**

### Hébergement

L'interface et le serveur du jeu sont hébergés par Vercel. La base PostgreSQL est hébergée par Supabase.

**Coordonnées légales complètes des hébergeurs : à vérifier et compléter avant publication.** Les services utilisés et leurs notices sont recensés dans [la préparation L3](PUBLIC_INFORMATION_PREPARATION.md).

### Ressources utilisées

Les icônes Tabler sont distribuées sous licence MIT. Les polices Inter, Space Grotesk et JetBrains Mono sont servies depuis le site, sous licence SIL Open Font License 1.1. Leurs notices et licences sont conservées avec les ressources du projet.

Le code et les ressources du projet restent soumis à leurs licences respectives. Ce texte ne crée pas de licence générale sur le jeu.

## 2. Confidentialité — rédaction proposée

### Jouer sans compte

Mastermind Arena ne demande ni création de compte ni adresse e-mail pour jouer. Vous choisissez un pseudo et pouvez créer ou rejoindre une partie. Le nom de la partie et le pseudo de son créateur sont visibles dans la liste des parties en attente. Évitez d'y saisir des informations personnelles.

Les données de jeu comprennent les identifiants de la partie et des joueurs, les codes de jeu, les essais, les indices et leurs corrections, l'historique et les scores. Elles servent à faire fonctionner le duel, afficher son résultat et permettre sa reprise. Les informations visibles par l'adversaire dépendent de la phase et des règles du jeu.

### Votre place et votre code de reprise

Une place est protégée par un jeton privé. La base conserve une empreinte de ce jeton ; votre navigateur conserve les informations nécessaires pour retrouver votre place. Le code de reprise donne accès à cette place : gardez-le privé et ne l'envoyez pas dans une demande de contact ordinaire.

Le code d'accès facultatif d'un salon est différent du code de reprise. Il sert à rejoindre la partie et est conservé sous forme dérivée en base.

### Conservation des parties

Les parties sont supprimées après sept jours sans action de jeu. Une tâche quotidienne effectue cette suppression ; un incident peut la retarder. La suppression concerne la partie, ses places, son historique et ses reçus d'action.

Une défaillance du service peut aussi entraîner la perte des parties ou des scores. Aucune récupération n'est promise pour cette version de démonstration.

### Informations conservées dans votre navigateur

Votre navigateur conserve votre place privée et votre préférence de thème pour permettre la reprise et mémoriser votre choix clair/sombre. Ces informations n'ont pas de délai d'expiration ajouté par l'application.

L'action « Oublier ma place sur cet appareil » retire les informations locales de cette place. Elle ne supprime pas immédiatement la partie sur le serveur. La préférence de thème reste distincte et peut être supprimée avec les données du site dans votre navigateur. La purge d'une partie sur le serveur n'efface pas automatiquement les données des appareils.

### Hébergement et fonctionnement technique

Vercel assure l'hébergement de l'interface et du serveur ; Supabase assure celui de la base. Les services d'hébergement et de sécurité traitent également des informations techniques nécessaires à leur fonctionnement. Les journaux techniques ne suivent pas automatiquement la durée de conservation des parties.

Les polices sont distribuées depuis le site : leur chargement ne nécessite pas de requête vers Google Fonts.

L’adresse IP est traitée au niveau du réseau et des protections contre les abus. Les dates, routes, statuts et informations de navigateur, selon les services, permettent le diagnostic et la sécurité. L’éditeur et les prestataires ont accès aux informations nécessaires à l’exploitation ; les joueurs ne voient que les informations autorisées par le jeu.

Les prestataires peuvent faire intervenir des équipes ou sous-traitants hors de l’Union européenne. Leurs documents de traitement décrivent les garanties prévues, notamment les clauses contractuelles types selon les cas. La région de la base ne suffit pas à déterminer tous les lieux de traitement.

**À finaliser avant publication : durées réelles des journaux, entités contractuelles et mécanismes applicables au projet.** Les [recherches et limites](LEGAL_REVIEW_2026-10-28.md) distinguent les informations vérifiées et les réglages du projet encore inconnus.

### Vos droits et le contact

Dans les conditions du RGPD, vous pouvez demander l’accès à vos données, leur rectification, leur effacement ou la limitation de leur traitement. L’opposition et la portabilité dépendent du fondement et des conditions du traitement concerné. Ces droits ne donnent pas accès aux données privées de l’adversaire.

Une réponse est apportée en principe sous un mois. Ce délai peut être prolongé de deux mois en cas de complexité ou de demandes nombreuses, avec information dans le premier mois. Vous pouvez adresser une réclamation à la CNIL, notamment sur [son site](https://www.cnil.fr/fr/adresser-une-plainte).

N’envoyez pas votre code de reprise par message ordinaire. L’identifiant de partie et le pseudo peuvent aider à situer une demande ; une vérification proportionnée peut être nécessaire avant toute communication ou suppression.

**À compléter avant publication :** coordonnées et régime d’identification du responsable, contact réel, qualification des fondements par finalité et procédure sûre d’exercice des droits. La présence de ce texte ne crée pas un formulaire ni une procédure de vérification déjà implémentée.

## 3. Conditions de sortie L3

Le cadre personnel en France et l’autorisation d’afficher Aloïs Sautet sont confirmés ; le régime d’identification complet reste à finaliser au regard de la LCEN. Le contact est reporté à sa demande. Deux pages seulement sont retenues, sans page CGU ni case d’acceptation ajoutée. Les clauses de confidentialité connues sont intégrées localement ; les champs non vérifiés restent uniquement signalés dans la documentation. Voir [l’analyse sourcée](LEGAL_REVIEW_2026-10-28.md).

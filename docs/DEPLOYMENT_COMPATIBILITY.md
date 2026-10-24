# Compatibilité des publications front et backend

Procédure préparée le 23 octobre 2026. Les deux projets Vercel suivent `main` d'après le concepteur ; aucun réglage distant ni déploiement n'a été vérifié ici. Un push peut déclencher les deux projets, sans garantir leur ordre ou leur simultanéité. Une ancienne page peut rester ouverte après une publication du front.

## Contrats à préserver

- HTTP : routes, méthodes, headers, formes JSON et codes décrits dans [API_CONTRACT](API_CONTRACT.md). Les fixtures Java sont consommées par le front. Elles ne décrivent pas toutes les anciennes versions déjà publiées.
- Navigateur : code de reprise et identité de place. Un déploiement ordinaire ne doit pas invalider les places existantes ; ne pas demander un oubli local pour masquer un défaut de compatibilité.
- PostgreSQL : état et idempotence JSON `formatVersion:1`, journal et migrations. Le numéro de format persistant n'est pas un numéro de version du protocole HTTP.

Le lecteur front actuel accepte un champ public supplémentaire dans une réponse connue. Supprimer/renommer un champ requis, changer son type ou ajouter une phase/action inconnue est incompatible pour ce lecteur. Les requêtes JSON ne bénéficient pas de cette tolérance : ne pas envoyer de nouveau champ tant que tous les serveurs concernés ne savent pas le lire. Un champ ajouté ne doit jamais révéler secret, jeton ou action masquée.

Le test `contract.test.js` vérifie une réponse additive simulée, puis une phase inconnue, un champ requis absent et un type incompatible via le vrai client de lecture et son coordinateur. Les réponses incompatibles déclenchent une erreur sans remplacer le dernier plateau valide ; le retour à un contrat valide permet de reprendre. C'est une vérification des consommateurs actuels, pas un test entre deux déploiements historiques. Une erreur de contrat sur une réponse d'écriture peut survenir après commit : conserver la clé et le replay exact, sans créer une nouvelle intention.

## Publication compatible

1. Identifier les commits et déploiements exacts du front et du backend actuels, les paramètres d'environnement concernés et le schéma utilisé. Préparer les versions de retour arrière. Si ces éléments sont inconnus, garder la publication ouverte.
2. Faire passer les contrôles locaux puis la CI distante de la version candidate. Exécuter le parcours [HTTP/JDBC](LOCAL_CONTRACT_CHECK.md), y compris correction, abandon, revanche et reprise. Tester une copie isolée du schéma si les données changent.
3. Préparer un backend acceptant les commandes du front actuellement publié et celles du nouveau front. Garder les champs/valeurs attendus par les anciennes pages. Pour un changement de requête, faire évoluer le lecteur avant le nouvel émetteur ; pour une réponse, garder l'ancienne forme pendant la transition.
4. Préparer les deux déploiements sans promotion automatique en production lorsque le changement exige un ordre. Vérifier dans Vercel le mécanisme réellement disponible et le régler avec l'autorisation correspondante ; aucun réglage n'est présumé ici. Si les deux publications peuvent partir ensemble, la paire doit fonctionner dans les deux ordres avant le push.
5. Après autorisation de publication, promouvoir le backend compatible, tester le front existant contre lui, puis promouvoir le nouveau front. Vérifier aussi une ancienne page restée ouverte, les deux places privées et la reprise. Contrôler URL backend, CORS et configuration de build du front ; ne pas consigner leurs secrets dans les preuves.
6. En cas de réussite, identifier la paire publiée et consigner le parcours en production dans VALIDATION. Retirer une ancienne forme seulement lors d'un lot distinct, après vérification des consommateurs encore actifs et des places à préserver.

## Rupture ou transition initiale

La base actuelle peut être reconstruite selon la décision du 14 octobre ; suivre [DATABASE_REBUILD](DATABASE_REBUILD.md). Cette exception ne garantit pas que l'ancien front fonctionne avec le nouveau schéma/backend et n'autorise pas les resets futurs.

La candidate à reçus compacts exige V1–V8, état format 1 et reçu format 2. V7 refuse une installation remplie pour cette transition initiale : le reset des jeux de test reste une opération distincte à effectuer lorsque la version sera prête. Les anciens binaires exigeant V1–V6 ou lisant des résultats complets ne constituent pas un retour arrière compatible avec les nouveaux reçus. Ne pas rétablir un ancien backend sur ce schéma sans preuve de compatibilité ; le contrat HTTP inchangé ne garantit pas la compatibilité du stockage.

Si la nouvelle paire est incompatible avec les anciennes pages, préparer une interruption annoncée et empêcher les nouvelles admissions/actions pendant la bascule, avec un mécanisme effectivement disponible et vérifié. Ne pas simuler cette protection par CORS ou par une clé publique. Le mécanisme de maintenance n'est pas livré par cette procédure : il reste à choisir et à tester avant une telle publication. Vérifier la nouvelle paire avant réouverture ; informer de la nécessité de recharger et, pour le seul reset initial, de l'invalidité des anciens codes.

## Retour arrière

V8 ajoute la procédure de purge commune sans changer les formats d'état/reçu ni HTTP. Elle n'efface pas les données V7. En revanche, le binaire actuel exige exactement V1–V8 : un ancien binaire qui refuse les versions supérieures à 7 ne redémarrera pas sur ce schéma. Ne pas retirer la trace de migration pour contourner ce refus ; vérifier explicitement la compatibilité du binaire de retour arrière. La tâche cron n'est pas activée par la migration et doit rester inactive pendant une bascule non validée.

- Incident front seul : revenir au déploiement front précédent seulement s'il est compatible avec le backend et les données présents ; vérifier reprise et lecture avant réouverture.
- Incident backend : revenir au backend précédent seulement s'il lit les données écrites depuis la bascule et accepte les commandes des pages encore ouvertes. Sinon, maintenir l'interruption et corriger en avant ; ne pas rejouer un ancien binaire sur un format inconnu.
- Changement de schéma/format : pas de rollback SQL automatique ni de suppression de parties actives. Restaurer uniquement selon une procédure éprouvée sur base isolée, avec identification des écritures qui seraient perdues et autorisation correspondante. La restauration reste à valider au lot 4.

## Preuves encore ouvertes avant production

Versions exactes actuellement publiées, réglages de promotion Vercel, test entre ces versions et la candidate, CI distante, permissions Supabase, sauvegarde/restauration et parcours production restent non vérifiés. Aucun déploiement n'est autorisé ou effectué par ce document.

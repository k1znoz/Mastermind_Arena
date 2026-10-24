# Sauvegarde et restauration — preuve locale

Scénario validé le 23 octobre 2026. Il complète [la transition initiale](DATABASE_REBUILD.md) et [la compatibilité de publication](DEPLOYMENT_COMPATIBILITY.md). Il ne configure ni sauvegarde planifiée ni restauration Supabase.

## Exécuter la preuve

Utiliser un conteneur PostgreSQL 16 jetable nommé exactement `mastermind-restore-validation`, avec le port 5432 publié uniquement sur `127.0.0.1:54330`. Préparer une base vide `arena_test` et un utilisateur de test propriétaire. Le script vérifie que le port du conteneur correspond à l'URL JDBC explicite. Le port HTTP 8082 doit être libre. Ne pas charger les profils de développement ou de production.

```powershell
mvn -pl deduction-engine compile dependency:build-classpath '-Dmdep.outputFile=target/runtime-classpath.txt'
$env:ARENA_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:54330/arena_test'
$env:ARENA_TEST_DB_USER = '<utilisateur propriétaire de test>'
$env:ARENA_TEST_DB_PASSWORD = '<mot de passe de test>'
node scripts/verify-local-restart.mjs --restore
```

Le script crée un schéma unique, deux places, des secrets et une manche terminée avec un score non nul. Il arrête son API, puis exécute les outils PostgreSQL dans le conteneur :

1. `pg_dump --format=custom --schema <schéma de test>` sauvegarde les cinq tables, leurs contraintes, index, politiques et métadonnées de migration. L'archive reste dans `/tmp` du conteneur jetable ; aucun dump n'est imprimé ou ajouté au dépôt.
2. `createdb` crée une nouvelle base au nom unique ; aucune base existante n'est supprimée ou remplacée.
3. `pg_restore --no-owner --exit-on-error` restaure l'archive dans cette base, sous le même utilisateur propriétaire de test. Les volumes de lignes des cinq tables sont comparés à la source.
4. Un nouveau processus Java démarre sur la base restaurée. Les deux places privées sont reprises, la vue est comparée exactement (version, historique, scores et secrets révélés), et la commande d'abandon acceptée est rejouée. Même réponse idempotente, aucun changement de la vue ni du score. Une revanche commune vérifie ensuite que les écritures restent possibles.

Les processus Java sont arrêtés par le script. Après inspection, arrêter uniquement le conteneur jetable dédié ; son option Docker `--rm` supprime les bases de test et l'archive avec le conteneur. Conserver une trace du PASS, jamais les jetons ou le contenu du dump.

## Ce qui est prouvé

Restauration logique du schéma de test dans une autre base PostgreSQL de même version majeure, fonctionnement des migrations déjà appliquées au redémarrage, reprise privée, état métier et résultat idempotent conservés. La base source reste intacte jusqu'à la suppression de l'environnement jetable.

Le scénario n'éprouve pas une copie de fichiers ou de volume, les sauvegardes/PITR Supabase, un rôle d'exécution restreint différent du propriétaire, une autre version PostgreSQL, une archive endommagée ou des écritures concurrentes pendant le dump. Les permissions effectives doivent être retestées avec les rôles cibles avant toute restauration distante.

## Préparer une restauration réelle

- Identifier version du serveur, schéma, propriétaire, rôles/grants et format persistant. Vérifier la compatibilité du binaire destiné à relire la sauvegarde ; ne pas rétrograder un format inconnu.
- Définir avec le concepteur la fréquence et la durée de conservation des sauvegardes, la perte maximale de données acceptable et le délai de reprise visé. Ces décisions restent ouvertes ; aucun historique actif n'est purgé implicitement.
- Une archive contient les secrets de jeu et les empreintes de jetons : accès réservé, stockage et transfert chiffrés, aucun dépôt Git, diagnostic ou capture publique. Ne pas confondre le fichier de test jetable avec une stratégie de sauvegarde durable.
- Restaurer d'abord dans une cible isolée neuve. Contrôler cinq tables/migrations, permissions, reprise, confidentialité, correction, idempotence et nouvelles écritures avec les versions candidates.
- Avant une bascule, empêcher les écritures sur l'ancienne cible avec un mécanisme de maintenance effectivement testé. Comparer l'heure de la sauvegarde aux dernières actions et identifier les écritures qui seraient perdues. L'interruption et toute perte de données nécessitent l'autorisation correspondante ; le mécanisme de maintenance n'est pas livré ici.
- Garder l'ancienne cible disponible pour diagnostic, changer la configuration seulement après validation, puis rejouer les contrôles sur la nouvelle cible. Consigner les versions et résultats sans credentials. Ne supprimer aucune cible de production sans autorisation distincte.

Les détails d'accès Supabase, les sauvegardes disponibles et les objectifs de reprise restent à vérifier avant de transformer cette procédure préparée en opération de production.

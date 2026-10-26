# Bascule Supabase vers arena_runtime

Nom et droits approuvés le 26 octobre 2026. Le concepteur choisit le SQL Editor pour définir le mot de passe en privé, après information sur l'historique des requêtes. Ce document ne prouve aucune création ou bascule distante.

## 1. Préparer puis créer le rôle

Depuis la racine :

```powershell
node scripts/prepare-runtime-role.mjs
```

Copier `target/supabase-arena-runtime.sql` dans Supabase SQL Editor et exécuter comme propriétaire de arena_v1. Le fichier réutilise `db/runtime_access.sql` ; aucun mot de passe ni donnée de partie n'y figure. Il exige exactement V1–V8 et refuse un rôle arena_runtime déjà présent, sans modifier ses droits ou son mot de passe.

Résultat attendu : rôle arena_runtime, tous les indicateurs retournés à false, y compris rolcanlogin. La connexion est volontairement désactivée jusqu'à l'étape 2. RLS reste activée ; des politiques réservées à ce rôle sont ajoutées. Aucun accès supplémentaire pour anon/authenticated/PUBLIC, aucun droit de propriété, DELETE, TRUNCATE, DDL ou exécution de purge. Droits précis dans [DATABASE_ROLES](DATABASE_ROLES.md).

Le backend continue à utiliser sa configuration actuelle pendant cette préparation. Ce script ne supprime ni table ni partie, ne lance aucune migration et n'active pas le cron.

## 2. Définir le mot de passe en privé

Générer un mot de passe unique dans un gestionnaire, par exemple 32 caractères aléatoires mêlant lettres et chiffres. Copier le modèle `target/supabase-arena-runtime-password.sql` dans une nouvelle requête privée du SQL Editor ; remplacer uniquement la valeur assignée à private_password dans cet éditeur, puis exécuter. Le modèle refuse le placeholder et les valeurs de moins de 24 caractères. Un mot de passe contenant une apostrophe nécessite son échappement SQL par deux apostrophes ; ne pas improviser une interpolation de commandes.

Ne pas enregistrer le mot de passe réel dans les fichiers locaux, les documents du dépôt ou le chat. La requête SQL contenant ce secret peut être conservée dans l'historique ou les logs du service : ce compromis du SQL Editor a été accepté par le concepteur. Une alternative psql avec commande interactive `\password arena_runtime` évite la commande en clair, mais n'est pas la méthode choisie ici.

Résultat attendu : rolcanlogin true. Conserver le mot de passe pour Vercel sans le communiquer à l'agent.

## 3. Configurer uniquement le backend Vercel

Relever les variables déjà présentes avant toute modification, en gardant leurs valeurs privées. Conserver les variables POSTGRES_* gérées par l'intégration ; créer les overrides APP_DB_* explicites ci-dessous pour le backend, dans l'environnement effectivement utilisé.

| Variable | Valeur |
|---|---|
| APP_DB_SCHEMA | arena_v1 |
| APP_DB_MIGRATE_ON_START | false |
| APP_DB_PASSWORD | Nouveau mot de passe, valeur brute dans le champ secret Vercel |
| APP_DB_USER | arena_runtime pour connexion directe ; arena_runtime.PROJECT_REF pour le pooler partagé Supavisor |
| APP_DB_URL | URL JDBC de la connexion choisie, sans utilisateur ni mot de passe dans l'URL |

Exemple pour le pooler partagé en transaction :

```text
jdbc:postgresql://HOTE_COPIE_DE_CONNECT:6543/postgres?sslmode=require&prepareThreshold=0
```

Copier l'hôte, le port et le project ref depuis Supabase Connect ; ne pas les déduire de la région. Si le mode est session, conserver son port fourni, habituellement 5432. Ne pas confondre rôle PostgreSQL arena_runtime et nom de connexion au pooler avec suffixe de projet. Une connexion directe n'a pas ce suffixe et peut nécessiter un accès IPv6.

Vérification du 26 octobre : l'hôte direct communiqué par le concepteur ne publie qu'une adresse IPv6 lors du contrôle DNS, sans enregistrement IPv4 observé. Ne pas remplacer automatiquement une connexion Vercel fonctionnelle par cet endpoint. Pooler partagé en mode transaction recommandé et son hôte exact demandé dans Connect ; la configuration concrète reste en attente de cette donnée. Le champ user=postgres affiché par défaut dans Connect décrit l'administrateur : le rôle approuvé doit être arena_runtime, avec suffixe de projet pour le pooler partagé.

Hôte du Transaction pooler fourni ensuite par le concepteur : aws-0-eu-west-1.pooler.supabase.com. Après création et activation réussies du rôle, la configuration cible du backend de ce projet est :

```env
APP_DB_URL=jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
APP_DB_USER=arena_runtime.lzxkzivarminevytyhfa
APP_DB_SCHEMA=arena_v1
APP_DB_MIGRATE_ON_START=false
```

Ajouter APP_DB_PASSWORD avec le mot de passe du rôle arena_runtime dans le champ secret Vercel, sans le copier dans ce document. Paramètres publics de connexion fournis, aucun mot de passe reçu. Ce bloc est une cible préparée : il n'atteste ni création du rôle, ni activation, ni modification des variables, ni redéploiement réussi. Conserver les variables POSTGRES_* de l'intégration ; les overrides APP_DB_* sont prioritaires pour le backend.

Le code privilégie APP_DB_USER sur l'utilisateur embarqué dans POSTGRES_URL et APP_DB_PASSWORD sur POSTGRES_PASSWORD. APP_DB_URL évite de dépendre d'une URL d'intégration masquée. Le mot de passe passé séparément reste brut : pas de percent-encoding dans APP_DB_PASSWORD. Ces overrides ne concernent pas le frontend et ne doivent pas porter un préfixe VITE_.

Sources : [connexion Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres), [rôles](https://supabase.com/docs/guides/database/postgres/roles), [psql et mot de passe](https://www.postgresql.org/docs/17/app-psql.html).

## 4. Vérifier la bascule et prévoir le repli

Redéployer le backend après l'installation et l'activation du rôle. Vérifier démarrage sans migration, liste, création/jointure, tour avec événements/retry et reprise ; vérifier ensuite correction, abandon et revanche. L'administrateur conserve la purge quotidienne, le rôle de jeu ne l'exécute pas.

Relever dans les logs de connexion une nouvelle session sous arena_runtime et les éventuels refus SQL, sans copier de credentials. Une requête du SQL Editor sous postgres ne prouve pas l'identité du backend. La réussite locale ne clôt pas ces vérifications Supabase/Vercel.

En cas d'échec, rétablir les variables du backend selon leur état relevé avant la bascule, puis redéployer. Ne pas modifier le mot de passe du rôle postgres. Après confirmation que le backend utilise de nouveau l'ancienne configuration, `ALTER ROLE arena_runtime NOLOGIN;` désactive les nouvelles connexions de ce rôle ; les sessions déjà ouvertes doivent être gérées séparément. Ne pas supprimer de tables, de données ou de politiques pour contourner un refus. La suppression complète du rôle, si souhaitée, demande une revue de ses dépendances.

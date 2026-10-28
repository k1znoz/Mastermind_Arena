# CI distante — vérification du 28 octobre 2026

Lecture via le connecteur GitHub, sans relance de job, push ou modification distante. Dépôt k1znoz/Mastermind_Arena ; branche main et HEAD local observés sur **01735e58ecdb07f75c334d45f0345eb5aa70fa86**.

## Exécution vérifiée

[Quality #4](https://github.com/k1znoz/Mastermind_Arena/actions/runs/40670370594), déclenchée par push sur cette révision : completed / success. Exécution du 28 octobre de 10:13:14 à 10:14:52 UTC, soit 11:13:14–11:14:52 à Paris.

| Job | Résultat des étapes et journaux |
| --- | --- |
| Frontend, job 122755304768 | npm ci, lint, tests, check et build réussis. Journal TAP : 83 tests, 83 pass, 0 fail/cancelled/skipped. Svelte Check : 0 erreur/avertissement. Build réussi. |
| Backend, job 122755304245 | Conteneur PostgreSQL initialisé ; mvn -B -pl deduction-engine -Pquality clean verify réussi. Journal Maven : 57 tests, 0 échec/erreur/ignoré ; BUILD SUCCESS. |
| Périmètre PostgreSQL | PostgresWorkflowTest : 17 tests ; JdbcMatchRetentionTest : 8 tests ; 0 ignoré dans les deux suites. Ces 25 tests ont donc réellement tourné, pas seulement été découverts. |
| Rapports | Artefact java-test-reports présent, non expiré au relevé, 37 825 octets. Contenu de l'archive non téléchargé : les nombres ci-dessus proviennent des journaux consultés. |

Les journaux bruts et métadonnées de compte ne sont pas copiés dans le dépôt ; seuls les identifiants, étapes et résumés utiles sont consignés ici.

## Statuts Vercel associés au commit

GitHub signale success / Deployment has completed pour :

- [mastermind-arena-backend](https://vercel.com/k1znozs-projects/mastermind-arena-backend/GDYwhhBcoSYJcCjuLu78rYaiQtc5)
- [mastermind-arena](https://vercel.com/k1znozs-projects/mastermind-arena/8UpcybN7YKX64gHJ3wdu9ij9nbov)

Ces statuts prouvent que les publications associées ont été signalées terminées à GitHub. Ils ne prouvent pas une recette utilisateur, la santé de PostgreSQL ni le rattachement actuel des domaines canoniques à ces déploiements. Le déploiement de repli reste à contrôler dans Vercel ; aucune promotion ou restauration n'a été exécutée.

## Limite de révision

Les polices locales et leurs notices, le traitement d'admission HTTP 429 et les documents courants non commités sont postérieurs à cette révision et ne bénéficient pas de cette CI distante. Contrôles locaux dans [LOCAL_FONTS](LOCAL_FONTS.md) et [ERROR_MESSAGES](ERROR_MESSAGES.md) : la suite locale compte désormais 85 tests, la CI ici décrite reste à 83. La candidate complète de 103 fichiers reste à figer dans une nouvelle révision avant de lui attribuer une CI. Ne pas présenter le success de 01735e5 comme celui d'un contenu local non poussé.

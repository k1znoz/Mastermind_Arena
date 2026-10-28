# Polices locales — preuve du 28 octobre 2026

Décision du concepteur : servir depuis le projet les mêmes polices, sans changer le design. Modification limitée à l'import de app.css, une feuille fonts.css et les ressources publiques/licences associées. Aucun paquet npm ajouté, aucun changement de jeu, d'API ou de stockage.

## Ressources

La requête Google Fonts auparavant présente dans app.css a fourni les faces WOFF2 de Space Grotesk 400/600/700, JetBrains Mono 400/500/700 et Inter 400/500/600. Les 48 déclarations @font-face conservent familles, graisses, styles, plages Unicode et font-display: swap ; seules les URL sont remplacées par /fonts/.

16 binaires distincts, 332 744 octets au total. Notice d'auteur et licence SIL OFL 1.1 originales conservées pour chaque famille depuis le dépôt officiel google/fonts. Aucune transformation du binaire. [Relevé de provenance](../App_Front/public/fonts/README.md) et [empreintes](../App_Front/public/fonts/SOURCES.json).

## Vérifications capturées

- Téléchargement initial bloqué par le réseau du bac à sable ; même script relancé avec l'accès autorisé aux sources officielles. Aucun contournement de permissions.
- npm run lint réussi ; npm run check : 0 erreur, 0 avertissement ; npm run build : 176 modules, JS index-BK6yjSka.js, CSS index-BRWWkcpR.css.
- Les 16 polices sont présentes dans dist/fonts avec les empreintes SHA-256 attendues ; servies en HTTP 200 depuis un serveur statique lié à 127.0.0.1:5187, longueurs conformes au manifeste. Serveur arrêté après contrôle.
- Aucune URL fonts.googleapis.com ou fonts.gstatic.com dans la CSS construite. Les liens de provenance restent dans les documents, sans chargement de police externe à l'exécution.

Les tests métier/front de logique ne sont pas répétés pour ces seuls fichiers CSS et binaires. Aucun contrôle de rendu dans un navigateur connecté n'est revendiqué : Chrome n'est plus disponible dans la session et le concepteur a choisi de gérer lui-même la page Vercel. La recette visuelle finale L4 devra confirmer les trois familles effectivement chargées et l'absence de régression de texte ; il ne s'agit pas d'une nouvelle campagne UX générale.

Manifeste de candidate actualisé : 103 entrées, source de base 01735e5 et référence de production fournie fd500eb. Aucun commit, push ou déploiement effectué par ce lot.

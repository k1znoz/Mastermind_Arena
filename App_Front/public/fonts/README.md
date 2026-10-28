# Polices servies localement

Ajout du 28 octobre 2026, selon la décision du concepteur. Les fichiers WOFF2 proviennent de la feuille Google Fonts précédemment importée par app.css. Les déclarations de famille, graisse, style, unicode-range et font-display: swap sont conservées dans src/fonts.css ; seules les URL des fichiers sont remplacées par /fonts/.

- Space Grotesk : 400, 600, 700.
- JetBrains Mono : 400, 500, 700.
- Inter : 400, 500, 600.

Les 16 fichiers représentent 332 744 octets. Le nom de chaque fichier contient le début de son SHA-256. SOURCES.json conserve l'URL d'origine, l'empreinte complète et la taille de chaque ressource, ainsi que la requête CSS d'origine. Les sous-ensembles Unicode fournis sont conservés ; aucune conversion ou modification du binaire n'est effectuée.

Licences et notices d'auteur originales SIL Open Font License 1.1, conservées par famille :

- inter-OFL.txt — https://github.com/google/fonts/tree/main/ofl/inter
- jetbrainsmono-OFL.txt — https://github.com/google/fonts/tree/main/ofl/jetbrainsmono
- spacegrotesk-OFL.txt — https://github.com/google/fonts/tree/main/ofl/spacegrotesk

Ne pas rétablir un chargement Google Fonts dans app.css. Pour une mise à jour, récupérer les faces et notices officielles, actualiser ensemble src/fonts.css, les fichiers, SOURCES.json et ce relevé, puis vérifier le build et le chargement depuis l'origine du site. Les téléchargements ne nécessitent ni paquet npm ni appel réseau à l'exécution de l'application.

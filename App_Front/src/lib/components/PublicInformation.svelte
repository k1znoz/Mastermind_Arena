<script>
  import { onMount } from 'svelte'
  import TopBar from './TopBar.svelte'

  export let page = 'mentions'
  let theme = 'dark'

  function toggleTheme() {
    theme = theme === 'dark' ? 'light' : 'dark'
    document.documentElement.dataset.theme = theme
    try { localStorage.setItem('arenaTheme', theme) } catch { /* Reading remains available without storage. */ }
  }

  onMount(() => {
    const preferred = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
    try { theme = localStorage.getItem('arenaTheme') || preferred } catch { theme = preferred }
    if (!['dark', 'light'].includes(theme)) theme = preferred
    document.documentElement.dataset.theme = theme
  })
</script>

<div class="information-page">
  <TopBar arrival {theme} on:theme={toggleTheme} />
  <main>
    <a class="back" href="/">Aller au jeu</a>
    <nav aria-label="Informations du site">
      <a href="/mentions-legales.html" aria-current={page === 'mentions' ? 'page' : undefined}>Mentions légales</a>
      <a href="/confidentialite.html" aria-current={page === 'confidentialite' ? 'page' : undefined}>Confidentialité</a>
    </nav>

    {#if page === 'confidentialite'}
      <h1>Confidentialité</h1>
      <p class="intro">Les informations utilisées pour jouer, retrouver votre place et faire fonctionner Mastermind Arena.</p>

      <section aria-labelledby="playing-data">
        <h2 id="playing-data">Jouer sans compte</h2>
        <p>Vous n’avez pas besoin de créer un compte ni de fournir une adresse e-mail pour jouer. Vous choisissez un pseudo et pouvez créer ou rejoindre une partie.</p>
        <p>Le pseudo et les informations nécessaires à votre place et au duel permettent de fournir le jeu demandé. Sans ces informations, il n’est pas possible de participer. Le code d’accès partagé d’un salon est facultatif.</p>
        <p>Le nom d’une partie et le pseudo de son créateur sont visibles dans la liste des parties en attente. Évitez d’y saisir des informations personnelles.</p>
        <p>Les données du duel comprennent les identifiants des joueurs et de la partie, les codes de jeu, les essais, les indices et leurs corrections, l’historique et les scores. Elles permettent de jouer, d’afficher le résultat et de reprendre la partie. Leur visibilité pour l’adversaire suit les règles du jeu.</p>
      </section>
      <section aria-labelledby="private-seat">
        <h2 id="private-seat">Votre place privée</h2>
        <p>Un jeton privé protège votre place. La base en conserve une empreinte ; votre navigateur conserve les informations nécessaires pour retrouver cette place.</p>
        <p>Votre code de reprise donne accès à votre plateau. Gardez-le privé et ne l’envoyez pas dans une demande de contact ordinaire. Le code d’accès facultatif d’un salon est différent : il permet de rejoindre la partie et est conservé sous forme dérivée en base.</p>
      </section>
      <section aria-labelledby="game-retention">
        <h2 id="game-retention">Conservation des parties</h2>
        <p>Les parties sont supprimées après sept jours sans action de jeu. Une tâche quotidienne effectue cette suppression ; un incident peut la retarder. La partie, ses places, son historique et ses reçus d’action sont concernés.</p>
        <p>Une défaillance du service peut entraîner la perte des parties ou des scores. Aucune récupération n’est promise pour cette version de démonstration.</p>
      </section>
      <section aria-labelledby="browser-data">
        <h2 id="browser-data">Sur votre appareil</h2>
        <p>Votre navigateur conserve votre place privée et votre préférence de thème pour permettre la reprise et mémoriser votre choix clair ou sombre. L’application n’ajoute pas de date d’expiration à ces informations locales.</p>
        <p>« Oublier ma place sur cet appareil » retire les informations locales de cette place. Cela ne supprime pas immédiatement la partie sur le serveur. La préférence de thème est distincte et peut être supprimée avec les données du site dans votre navigateur.</p>
        <p>La suppression d’une partie sur le serveur n’efface pas automatiquement les données des appareils.</p>
      </section>
      <section aria-labelledby="providers">
        <h2 id="providers">Hébergement et fonctionnement</h2>
        <p>Vercel héberge l’interface et le serveur ; Supabase héberge la base PostgreSQL. Les services d’hébergement et de sécurité traitent également des informations techniques pour leur fonctionnement. Les journaux techniques ne suivent pas automatiquement la durée de conservation des parties.</p>
        <p>L’adresse IP est utilisée au niveau du réseau et des protections contre les abus. Les informations de requête, comme la date, la route, le statut et les informations de navigateur selon le service, peuvent servir à détecter une panne ou une tentative d’accès abusive. Les données du jeu sont accessibles à l’éditeur et aux prestataires dans la mesure nécessaire à l’exploitation du service.</p>
        <p>Les prestataires peuvent faire intervenir des équipes ou des sous-traitants hors de l’Union européenne. Leurs documents de traitement décrivent les mécanismes prévus pour encadrer ces transferts ; la région de la base ne détermine pas à elle seule tous les lieux de traitement.</p>
        <p>Les polices sont servies depuis le site : leur chargement ne nécessite pas de requête vers Google Fonts.</p>
        <ul>
          <li><a href="https://vercel.com/legal/privacy-notice">Notice de confidentialité Vercel</a></li>
          <li><a href="https://vercel.com/legal/dpa">Traitement des données par Vercel</a></li>
          <li><a href="https://supabase.com/legal/customer-resources/data-processing-addendum">Traitement des données par Supabase</a></li>
        </ul>
      </section>
      <section aria-labelledby="privacy-rights">
        <h2 id="privacy-rights">Vos droits</h2>
        <p>Dans les conditions prévues par le RGPD, vous pouvez demander l’accès à vos données, leur rectification, leur effacement ou la limitation de leur traitement. Le droit d’opposition et le droit à la portabilité s’appliquent selon le fondement et les conditions du traitement concerné.</p>
        <p>Une demande ne donne pas accès aux données privées de l’adversaire. N’envoyez pas votre code de reprise dans un message ordinaire : il donne accès à votre place.</p>
        <p>Le délai de réponse est en principe d’un mois. En cas de complexité ou de demandes nombreuses, il peut être prolongé de deux mois, avec information dans le premier mois.</p>
        <p>Vous pouvez également <a href="https://www.cnil.fr/fr/adresser-une-plainte">adresser une réclamation à la CNIL</a>.</p>
      </section>
    {:else}
      <h1>Mentions légales</h1>
      <p class="intro">Informations sur l’éditeur et les ressources de Mastermind Arena.</p>
      <section aria-labelledby="publisher">
        <h2 id="publisher">Éditeur</h2>
        <p>Mastermind Arena est édité par <strong>Aloïs Sautet</strong>, en France. Ce jeu gratuit est un projet personnel présenté dans un portfolio et dans le cadre d’une formation. Il n’est pas monétisé.</p>
      </section>
      <section aria-labelledby="hosting">
        <h2 id="hosting">Hébergement</h2>
        <p>L’interface et le serveur sont hébergés par <a href="https://vercel.com/">Vercel</a>. La base PostgreSQL est hébergée par <a href="https://supabase.com/">Supabase</a>.</p>
      </section>
      <section aria-labelledby="credits">
        <h2 id="credits">Polices et icônes</h2>
        <p>Les icônes Tabler sont distribuées sous licence MIT. Inter, Space Grotesk et JetBrains Mono sont servies localement sous licence SIL Open Font License 1.1.</p>
        <ul>
          <li><a href="/licenses/tabler-icons.txt">Licence des icônes Tabler</a></li>
          <li><a href="/fonts/inter-OFL.txt">Licence Inter</a></li>
          <li><a href="/fonts/spacegrotesk-OFL.txt">Licence Space Grotesk</a></li>
          <li><a href="/fonts/jetbrainsmono-OFL.txt">Licence JetBrains Mono</a></li>
        </ul>
        <p>Le code et les ressources restent soumis à leurs licences respectives.</p>
      </section>
    {/if}
  </main>
</div>

<style>
  .information-page { min-height:100vh; padding-top:104px; }
  main { width:100%; max-width:800px; margin:auto; padding:28px 24px 56px; }
  a { color:var(--primary); text-underline-offset:4px; overflow-wrap:anywhere; }
  a:focus-visible { outline:3px solid var(--primary); outline-offset:4px; border-radius:3px; }
  .back { display:inline-flex; align-items:center; min-height:44px; margin-bottom:16px; }
  nav { display:flex; flex-wrap:wrap; gap:8px 24px; border-bottom:1px solid var(--line); padding-bottom:16px; }
  nav a { display:inline-flex; align-items:center; min-height:44px; color:var(--muted); }
  nav a[aria-current="page"] { color:var(--text); font-weight:600; text-decoration-thickness:2px; }
  h1 { font:600 clamp(30px,6vw,42px)/1.2 Inter,sans-serif; margin:32px 0 16px; letter-spacing:-.03em; }
  .intro { font-size:18px; color:var(--muted); }
  section { margin-top:32px; padding-top:24px; border-top:1px solid var(--line); }
  h2 { margin:0 0 16px; font-size:22px; text-transform:none; }
  p,li { font-size:16px; line-height:1.75; }
  p { margin:0 0 16px; }
  ul { margin:16px 0; padding-left:24px; }
  li { margin:10px 0; }
  @media(max-width:600px) { .information-page { padding-top:80px; } main { padding:20px 24px 40px; } }
</style>

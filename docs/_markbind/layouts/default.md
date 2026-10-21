<head-bottom>
  <link rel="stylesheet" href="{{baseUrl}}/stylesheets/main.css">
</head-bottom>

<div><a class="skip-link" href="#content-wrapper">Skip to content</a></div>

<header sticky>
  <navbar type="dark">
    <a slot="brand" href="{{baseUrl}}/index.html" title="Home" class="navbar-brand">TrackCall</a>
    <li><a href="{{baseUrl}}/index.html" class="nav-link">Home</a></li>
    <li><a href="{{baseUrl}}/UserGuide.html" class="nav-link">User Guide</a></li>
    <li><a href="{{baseUrl}}/DeveloperGuide.html" class="nav-link">Developer Guide</a></li>
    <li><a href="{{baseUrl}}/AboutUs.html" class="nav-link">About Us</a></li>
    <li><a href="https://github.com/AY2627S1-CS2103T-W12-1/tp" target="_blank" rel="noopener" class="nav-link" aria-label="TrackCall on GitHub"><md>:fab-github:</md></a>
    </li>
    <li slot="right">
      <form class="navbar-form">
        <searchbar :data="searchData" placeholder="Search" :on-hit="searchCallback" menu-align-right></searchbar>
      </form>
    </li>
  </navbar>
</header>

<details class="compact-site-nav">
  <summary>Site Map</summary>
  <nav aria-label="Compact site map">
    <ul>
      <li><a href="{{baseUrl}}/index.html">Home</a></li>
      <li><a href="{{baseUrl}}/UserGuide.html">User Guide</a></li>
      <li><a href="{{baseUrl}}/DeveloperGuide.html">Developer Guide</a></li>
      <li><a href="{{baseUrl}}/AboutUs.html">About Us</a></li>
      <li><a href="{{baseUrl}}/SettingUp.html">Setting up</a></li>
      <li><a href="{{baseUrl}}/Testing.html">Testing</a></li>
      <li><a href="{{baseUrl}}/Documentation.html">Documentation</a></li>
      <li><a href="{{baseUrl}}/Logging.html">Logging</a></li>
      <li><a href="{{baseUrl}}/DevOps.html">DevOps</a></li>
      <li><a href="{{baseUrl}}/iterations/v1.4.html">v1.4 iteration</a></li>
      <li><a href="{{baseUrl}}/releases/v1.3/README.html">v1.3 release</a></li>
      <li><a href="{{baseUrl}}/releases/v1.3/RELEASE_NOTES.html">v1.3 release notes</a></li>
      <li><a href="{{baseUrl}}/releases/v1.4/RELEASE_NOTES.html">v1.4 release notes</a></li>
    </ul>
  </nav>
</details>

<div id="flex-body">
  <nav id="site-nav">
    <div class="site-nav-top">
      <div class="fw-bold mb-2" style="font-size: 1.25rem;">Site Map</div>
    </div>
    <div class="nav-component slim-scroll">
      <site-nav>
* [Home]({{ baseUrl }}/index.html)
* [User Guide]({{ baseUrl }}/UserGuide.html) :expanded:
  * [Quick Start]({{ baseUrl }}/UserGuide.html#quick-start)
  * [Features]({{ baseUrl }}/UserGuide.html#commands)
  * [FAQ]({{ baseUrl }}/UserGuide.html#faq)
  * [Command Summary]({{ baseUrl }}/UserGuide.html#command-summary)
* [Developer Guide]({{ baseUrl }}/DeveloperGuide.html) :expanded:
  * [Acknowledgements]({{ baseUrl }}/DeveloperGuide.html#acknowledgements)
  * [Setting Up]({{ baseUrl }}/DeveloperGuide.html#setting-up-getting-started)
  * [Design]({{ baseUrl }}/DeveloperGuide.html#design)
  * [Implementation]({{ baseUrl }}/DeveloperGuide.html#implementation)
  * [Documentation, logging, testing, dev-ops]({{ baseUrl }}/DeveloperGuide.html#documentation-logging-testing-dev-ops)
  * [Appendix: Requirements]({{ baseUrl }}/DeveloperGuide.html#appendix-requirements)
  * [Appendix: Instructions for manual testing]({{ baseUrl }}/DeveloperGuide.html#appendix-instructions-for-manual-testing)
* [About Us]({{ baseUrl }}/AboutUs.html)
* [Development resources]({{ baseUrl }}/SettingUp.html)
  * [Setting up]({{ baseUrl }}/SettingUp.html)
  * [Testing]({{ baseUrl }}/Testing.html)
  * [Documentation]({{ baseUrl }}/Documentation.html)
  * [Logging]({{ baseUrl }}/Logging.html)
  * [DevOps]({{ baseUrl }}/DevOps.html)
  * [v1.4 iteration]({{ baseUrl }}/iterations/v1.4.html)
  * [v1.3 release]({{ baseUrl }}/releases/v1.3/README.html)
  * [v1.4 release notes]({{ baseUrl }}/releases/v1.4/RELEASE_NOTES.html)
      </site-nav>
    </div>
  </nav>
  <main id="content-wrapper" tabindex="-1">
    <span id="overview"></span>
    {{ content }}
  </main>
  <details id="page-nav">
    <summary>On this page</summary>
    <div class="nav-component slim-scroll">
      <page-nav />
    </div>
  </details>
  <scroll-top-button></scroll-top-button>
</div>

<script src="{{baseUrl}}/javascripts/responsive-navigation.js" defer></script>

<footer>
  <!-- Support MarkBind by including a link to us on your landing page! -->
  <div class="text-center">
    <small>[<md>**Powered by**</md> <img src="https://markbind.org/favicon.ico" width="30"> {{MarkBind}}, generated on {{timestamp}}]</small>
  </div>
</footer>

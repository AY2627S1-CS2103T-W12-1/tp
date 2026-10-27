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
    <li><a href="{{baseUrl}}/index.html#releases" class="nav-link">Releases</a></li>
    <li><a href="https://github.com/AY2627S1-CS2103T-W12-1/tp" target="_blank" rel="noopener" class="nav-link" aria-label="TrackCall on GitHub"><md>:fab-github:</md></a>
    </li>
    <li slot="right">
      <form class="navbar-form">
        <searchbar :data="searchData" placeholder="Search" :on-hit="searchCallback" menu-align-right></searchbar>
      </form>
    </li>
  </navbar>
</header>

<div id="flex-body">
  <nav id="site-nav">
    <div class="site-nav-top">
      <div class="navigation-label">Site map</div>
    </div>
    <div class="nav-component slim-scroll">
      <site-nav>
* [Home]({{ baseUrl }}/index.html)
* [User Guide]({{ baseUrl }}/UserGuide.html) :expanded:
  * [Quick Start]({{ baseUrl }}/UserGuide.html#quick-start)
  * [Features]({{ baseUrl }}/UserGuide.html#commands)
    * [Browse and find members]({{ baseUrl }}/UserGuide.html#browse-members)
    * [Add and update members]({{ baseUrl }}/UserGuide.html#manage-members)
    * [Organise member groups]({{ baseUrl }}/UserGuide.html#manage-tags)
    * [Exchange rosters with CSV]({{ baseUrl }}/UserGuide.html#csv-files)
    * [Remove member records]({{ baseUrl }}/UserGuide.html#remove-records)
    * [Help and session]({{ baseUrl }}/UserGuide.html#help-and-session)
  * [Correcting Errors]({{ baseUrl }}/UserGuide.html#correcting-errors)
  * [Your Data]({{ baseUrl }}/UserGuide.html#your-data)
  * [FAQ]({{ baseUrl }}/UserGuide.html#faq)
  * [Command Summary]({{ baseUrl }}/UserGuide.html#command-summary)
* [Developer Guide]({{ baseUrl }}/DeveloperGuide.html) :expanded:
  * [Acknowledgements]({{ baseUrl }}/DeveloperGuide.html#acknowledgements)
  * [Feature Map]({{ baseUrl }}/DeveloperGuide.html#feature-map)
  * [Setting Up]({{ baseUrl }}/DeveloperGuide.html#setting-up-getting-started)
  * [Design]({{ baseUrl }}/DeveloperGuide.html#design)
  * [Implementation]({{ baseUrl }}/DeveloperGuide.html#implementation)
  * [Development workflow]({{ baseUrl }}/DeveloperGuide.html#documentation-logging-testing-dev-ops)
  * [Appendix: Requirements]({{ baseUrl }}/DeveloperGuide.html#appendix-requirements)
  * [Appendix: Manual Testing]({{ baseUrl }}/DeveloperGuide.html#appendix-instructions-for-manual-testing)
* [About Us]({{ baseUrl }}/AboutUs.html)
* [Releases]({{ baseUrl }}/index.html#releases)
  * [v1.4 - TrackCall]({{ baseUrl }}/releases/v1.4/RELEASE_NOTES.html)
  * [v1.4 project history]({{ baseUrl }}/iterations/v1.4.html)
  * [v1.3 - TrackCall]({{ baseUrl }}/releases/v1.3/RELEASE_NOTES.html)
  * [v1.3 package guide]({{ baseUrl }}/releases/v1.3/README.html)
* [Development resources]({{ baseUrl }}/SettingUp.html)
  * [Setting up]({{ baseUrl }}/SettingUp.html)
  * [Testing]({{ baseUrl }}/Testing.html)
  * [Documentation]({{ baseUrl }}/Documentation.html)
  * [Logging]({{ baseUrl }}/Logging.html)
  * [DevOps]({{ baseUrl }}/DevOps.html)
      </site-nav>
    </div>
  </nav>
  <main id="content-wrapper" tabindex="-1">
    <span id="overview"></span>
    {{ content }}
  </main>
  <nav id="page-nav" aria-label="On this page">
    <div class="nav-component slim-scroll">
      <page-nav />
    </div>
  </nav>
  <scroll-top-button></scroll-top-button>
</div>

<script src="{{baseUrl}}/javascripts/navigation-accessibility.js" defer></script>

<footer>
  <!-- Support MarkBind by including a link to us on your landing page! -->
  <div class="text-center">
    <small>[<md>**Powered by**</md> <img src="https://markbind.org/favicon.ico" width="30"> {{MarkBind}}, generated on {{timestamp}}]</small>
  </div>
</footer>

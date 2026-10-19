---
title: Documentation guide
pageNav: 3
---

# Documentation guide

TrackCall uses [MarkBind](https://markbind.org/) to build the project website from the Markdown files in `docs/`.
Edit those source files and preview the result before submitting a documentation change.
The [course guide to forked MarkBind sites](https://se-education.org/guides/tutorials/markbind-forked-sites.html) and
[MarkBind user guide](https://markbind.org/userGuide/gettingStarted.html) explain the available features.

## Previewing documentation locally

Install Node.js 22.13 or later, then run the following commands from the project root:

```bash
cd docs
npm ci
npm run serve
```

`npm ci` installs the exact dependency versions recorded in `package-lock.json`.
`npm run serve` starts a local preview on port 8080 and refreshes it when documentation changes.
Open the address printed in the terminal, normally `http://localhost:8080/`. Stop the preview with `Ctrl+C`.
If a shared layout or site configuration change does not appear, restart the preview.

`npm run build` starts from a clean output directory and verifies every documentation page,
local link, and section target. A missing page or broken internal link fails the build.
Published release-note Markdown files keep their original checksums; legacy heading aliases
for those files are configured in `site.json` and added by `_markbind/plugins/legacyAnchors.js`.
For other pages, preserve explicit heading IDs when editing headings so existing bookmarks keep working.

Before submitting changes, run this command from `docs/`:

```bash
npm run build
```

This generates the production site in `docs/_site/` using the `/tp` base URL used by GitHub Pages.
Following the course setup, keep `baseUrl` in `docs/site.json` empty for local previews;
the production build supplies `/tp` separately.
The generated site and `node_modules/` are build outputs; keep them out of version control.
Keep `package.json` and `package-lock.json` together when updating dependencies.

## Maintaining the site

* Edit the existing Markdown pages to update their content. Their published `.html` URLs remain the same.
* Keep images in `docs/images/` and diagram sources in `docs/diagrams/`.
* Update `docs/site.json` for site settings and page configuration.
* Update `docs/_markbind/layouts/default.md` for the shared layout and navigation.
* Update `docs/stylesheets/main.css` for styling; it starts from the course-provided MarkBind stylesheet.
* Keep section headings stable when possible: changing a heading can break links to its section.
* Preview the User Guide, Developer Guide, and navigation at both desktop and phone widths after layout changes.
* IntelliJ users can enable [soft wrapping for Markdown files](https://se-education.org/guides/tutorials/intellijUsefulSettings.html#enabling-soft-wrapping)
  to make long paragraphs easier to edit.

## Publishing the site

The **Documentation** workflow in `.github/workflows/docs.yml` builds documentation changes in pull requests.
A failed documentation build fails that check. After documentation changes are merged into the team repository's
`master` branch, the workflow publishes the generated site to
[TrackCall's project website](https://ay2627s1-cs2103t-w12-1.github.io/tp/).
Forks and pull requests build the site without publishing to the team website.

The workflow follows the [course's branch-based Pages setup](https://se-education.org/guides/tutorials/markbind-forked-sites.html#initiating-the-site):
it publishes the generated website to the team repository's `gh-pages` branch using GitHub's built-in token.
Only the deployment job receives `contents: write`; the build job and pull requests have read access.
The locked MarkBind 7.2.1 build uses Node.js 22 instead of the starter action's older Node.js 18 runtime,
then publishes the verified output using the same `gh-pages` publishing action as the starter.

For the first deployment after migrating from Jekyll:

1. After the migration is merged, run **Actions → Documentation → Run workflow** on the team repository's `master`
   branch if its workflow has not already run. The deployment creates `gh-pages`.
2. A repository administrator must open the **team repository's Settings → Pages → Build and deployment**.
3. Set **Source** to **Deploy from a branch**, select **gh-pages** and **/(root)**, then click **Save**.
4. Wait for the Pages deployment to finish and check the published guides. If the first site does not appear
   after selecting the branch, rerun the **Documentation** workflow on `master`.

The website address stays the same. No personal access token or organization-wide permission change is needed
when the repository allows the deployment job's explicit `contents: write` permission.

## Style guidance

* Follow the [Google developer documentation style guide](https://developers.google.com/style).
* Also follow the [SE-EDU Markdown coding standard](https://se-education.org/guides/conventions/markdown.html).

## Diagrams

See the [SE-EDU PlantUML guide](https://se-education.org/guides/tutorials/plantUml.html).
Use Java 25 when running Java-based diagram tools or application build tasks.
On macOS, select the project's Java version with `sdk use java 25.0.3.fx-zulu` if needed.
Keep diagram sources and their exported images together so the diagrams can be updated later.

## Converting documentation to PDF

Open the generated page and use the browser's print preview to check page breaks, tables, and diagrams before saving.
See the [SE-EDU guide to saving web documents as PDF files](https://se-education.org/guides/tutorials/savingPdf.html).

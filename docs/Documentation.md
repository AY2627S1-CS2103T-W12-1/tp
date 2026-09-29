---
layout: page
title: Documentation guide
---

Use this guide to update the website, diagrams, and project portfolios.

* Table of Contents
{:toc}

## Update the website

The site uses [Jekyll](https://jekyllrb.com/). Documentation lives in `docs/`.
See [Using Jekyll for project documentation](https://se-education.org/guides/tutorials/jekyll.html)
for setup and publishing instructions.

| Change | File or folder |
| --- | --- |
| Guide content | Markdown files in `docs/` |
| Site name, description, and URL | `docs/_config.yml` |
| Left-sidebar sections | `docs/_data/navigation.yml` |
| Shared page structure | `docs/_layouts/` and `docs/_includes/` |
| Colours, text sizing, and spacing | `docs/_sass/minima/custom-styles.scss` |
| Search and section navigation | `docs/assets/js/documentation.js` |

The right-hand outline is generated from each page's headings.
When changing a heading, check links to its section, including the left-sidebar entry.
Use the shared styles to keep pages consistent.

For easier Markdown editing in IntelliJ, enable
[soft wrapping](https://se-education.org/guides/tutorials/intellijUsefulSettings.html#enabling-soft-wrapping).
Soft wrapping changes the editor view without adding line breaks to the published text.

## Write readable content

* Use short paragraphs and direct sentences.
* Use headings for sections, numbered lists for steps, and bullets for related points.
* Keep command syntax and exact application messages in code formatting.
* Clearly distinguish available features from planned behaviour.
* Follow the [Google developer documentation style guide](https://developers.google.com/style)
  and [SE-EDU Markdown standard](https://se-education.org/guides/conventions/markdown.html).

Before publishing, check section links, search results, narrow screens, and the rendered page.

## Maintain diagrams

Keep PlantUML sources in `docs/diagrams/` and generated PNGs in `docs/images/`.
See [Using PlantUML](https://se-education.org/guides/tutorials/plantUml.html) for authoring instructions.

`style.puml` defines the shared beige, ivory, sage, and dark-text palette.
The published diagrams were rendered with PlantUML 1.2026.8 and Java 25, using the Smetana layout engine.
For example, run this from the repository root:

```shell
java -jar /path/to/plantuml.jar -Playout=smetana -tpng -o ../images docs/diagrams/UiClassDiagram.puml
```

Review arrows, labels, clipping, and diagram warnings before committing the image.
Keep the intended-interface mockup at `docs/images/Ui.png` in its original aspect ratio.

UI mockups and the automatic-saving illustration were prepared with AI assistance.
They are labelled design illustrations, not evidence that proposed features are implemented.

## Prepare project portfolios

The starter portfolio template is kept in
[`docs/_templates/project-portfolio.md`](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/master/docs/_templates/project-portfolio.md).
It is excluded from the public website and search results.

Create a team member's page in `docs/team/` and link it from About Us when ready.
Replace all examples with verified contributions and working links before publishing.

## Export a PDF

Follow [Saving web documents as PDF files](https://se-education.org/guides/tutorials/savingPdf.html).
Check page breaks, tables, and diagrams in the exported file.

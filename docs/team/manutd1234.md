---
layout: default.md
title: Ian Abiel Wangsa - Project Portfolio
pageNav: 3
---

# Ian Abiel Wangsa - Project Portfolio

## Overview {id="project-overview"}

TrackCall is a desktop membership roster for club secretaries. It combines typed commands with a JavaFX interface
for maintaining contact details, finding members, organising overlapping groups, and exchanging CSV rosters.

My role is **Team Lead**, with primary feature responsibilities for **`list`, `clear`, and `sort`**. I also implemented
email search and contributed to data protection, input validation, testing, documentation, and release preparation.
My work spans both user-facing features and the safeguards needed to keep those features reliable together.

This portfolio highlights my most significant contributions first, followed by the wider project work.
The linked PRs provide implementation and validation evidence. Shared components, preserved teammate commits,
and defect reports are identified separately from work I implemented.

## Most significant contributions

| Contribution | Why it matters | Main evidence |
| --- | --- | --- |
| **Safe, scoped roster clearing** | Lets secretaries remove the displayed group or the whole roster deliberately, with confirmation and recovery when saving fails. | [#43](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/43), [#322](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/322) |
| **Persistent, stable name sorting** | Gives the complete roster a predictable saved order while retaining active search and tag conditions. | [#222](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/222), [#319](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/319) |
| **Cumulative email search** | Helps secretaries narrow an existing selection and remove individual conditions without rebuilding the search. | [#174](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/174), [#288](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/288) |
| **Protection against destructive file operations** | Prevents exports from replacing application data and preserves recoverable files during failed writes or outside edits. | [#110](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/110), [#219](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/219), [#294](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/294) |
| **Integrated guides and documentation website** | Makes command behaviour, developer explanations, diagrams, and release evidence easier to find and maintain together. | [#163](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/163), [#202](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/202), [#252](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/252) |

## Code contributed

- [My code on the course RepoSense dashboard](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=manutd1234&breakdown=true)
- [My merged pull requests](https://github.com/AY2627S1-CS2103T-W12-1/tp/pulls?q=is%3Apr+is%3Amerged+author%3AManutd1234)
- [My GitHub profile](https://github.com/Manutd1234)

## Core feature contributions

### 1. Listing and clearing members

**Purpose:** support everyday roster navigation and deliberate bulk deletion without losing unrelated members.

- **Listing:** implemented view reset, empty/singular/plural result counts, and rejection of extra arguments.
  `list` is read-only and does not rewrite the roster file.
- **Scoped clearing:** distinguished `clear`, which removes displayed members, from `clear /all`, which removes
  the complete roster. This allows a secretary to clear a filtered group without deleting hidden members.
- **Confirmation:** capture the intended members before deletion and require confirmation. Cancelling preserves
  the roster; the command does not silently change its target set while confirmation is pending.
- **Failure handling:** coordinate command execution, model state, filtering, and storage. A failed save restores
  the affected state and reports the storage failure; successful clearing refreshes the displayed counts.
- **Validation:** covered empty rosters, filtered clearing, full-roster clearing, cancellation, saved results,
  and rollback. The later integration retained active conditions rather than discarding the user's search context.

The initial implementation is in [#43](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/43).
The scoped design and its integrated tests are in
[#322](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/322).

### 2. Persistent and stable name sorting

**Purpose:** make the roster's order predictable across searches, edits, and application restarts.

- **Comparison:** introduced case-insensitive name sorting and improved handling of repeated name spacing.
- **Stability:** preserved the relative order of equal-name members after updates. This avoids unexpected movement
  when members have equivalent sort keys but different contact details.
- **Persistence:** evolved the original display-only sort into a sort of the complete saved roster.
  Active conditions remain applied; `list` and restart retain the saved order.
- **Subsequent changes:** additions and imports append, while edits keep the member's position until another sort.
  This makes the effect of `sort` explicit instead of continuously moving records during editing.
- **Validation:** added equal-name update regressions and model/command tests for persistence and filtering.
  In #222, six of the nine new regression tests failed before the fix; a separate randomised reference-order check
  exercised repeated updates.

The progression is recorded in [#68](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/68),
[#88](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/88),
[#222](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/222), and
[#319](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/319).
The final v1.5 behaviour is the persistent design; the earlier display-only design is historical.

### 3. Email search and removable conditions

**Purpose:** find members by email fragments and combine that search with an existing selection.

- **Command implementation:** added `findemail`, its parser, case-insensitive literal matching, predicate,
  removable email conditions, and offline command help.
- **Condition composition:** keywords within one email query provide alternatives. Separate active conditions
  narrow the result together, so another email search can refine the current selection.
- **Query identity:** recognise equivalent repeated queries despite changes in keyword order, case, or repetition.
  A different query that happens to match the same members remains a distinct, removable condition.
- **Read-only behaviour:** searching changes the view without rewriting saved data.
- **Validation:** tested parser boundaries, repeated and combined searches, condition removal, feedback,
  and preservation of saved-file contents and timestamps.

The original feature is in [#174](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/174);
the cumulative search design is in [#288](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/288).

## Reliability and data protection

### Protecting exports and application files

- **Safe replacement:** changed CSV export to finish and close a sibling temporary file before replacing its
  destination. Failed output must preserve the previous complete export.
- **Protected destinations:** rejected paths that target roster storage, preferences, the running application,
  or reserved aliases. Checks include normalised paths and existing file aliases.
- **Usable exports:** rejected literal double quotes in export paths because import could not reopen those paths.
  Tests retained ordinary, space-containing, apostrophe, Unicode, and mixed-case-extension filenames.
- **Validation:** reproduced destructive export failures before fixing them. Regression scenarios include partial
  output followed by failure, encoding errors, missing parent directories, protected files, and CSV round trips.

Evidence: [#110](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/110),
[#219](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/219), and
[#303](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/303).

### Recovery, outside edits, and privacy

- **Strict loading:** rejected malformed UTF-8 and JSON containing trailing values or junk, preserving the invalid
  file instead of accepting an unintended roster that a later save could overwrite
  ([#109](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/109)).
- **Recovery baseline:** registered recovered data as the session's storage baseline so the existing outside-edit
  detection could protect it before the first command save
  ([#287](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/287)).
- **Outside-file preservation:** handled a file appearing after startup had observed no file. The first refused save
  preserves active and backup bytes; an intentional retry preserves the outside file's exact contents in backup,
  including when its members match the in-memory roster
  ([#294](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/294)).
- **Privacy and concurrency:** contributed session locking, backup retention, retry guidance, and removal of member
  values from diagnostic logs in [#212](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/212).
  This integrated work preserved Eron's original contribution; outside-edit protection also builds on Glory's work.

The difficult part was preserving a recoverable state across multiple failures: saving the active file, retaining
the promised backup, restoring the view, and explaining whether the user should retry or reload.

## Testing and other functional improvements

- **CSV validation:** rejected unescaped quotes in unquoted fields while retaining valid quoted commas and doubled
  quotes. An invalid later row cannot partially import earlier rows or reset the existing view
  ([#114](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/114)).
- **Input and identity boundaries:** fixed integer index overflow and non-ASCII index acceptance
  ([#111](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/111)); aligned trimmed duplicate checks
  ([#113](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/113)); handled Unicode spaces in addresses and search
  keywords ([#296](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/296)); and prevented Unicode name case variants
  from bypassing duplicate checks ([#302](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/302)).
- **Consistent feedback:** aligned command validation, offline help, singular/plural member feedback, duplicate-prefix
  errors, and equality/hash contracts in [#218](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/218).
  Shared tag constraint text keeps six offline help entries consistent in
  [#304](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/304).
- **Readable controls:** added wrapping for filters and member tags in narrow windows, with layout checks
  ([#220](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/220)). Restored offline help and exercised its real FXML,
  reflow, and keyboard navigation in [#44](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/44).
- **Targeted integration tests:** added keyboard-focus, command-draft, last-filter-removal, and combined
  active-file/backup rollback regressions without changing production code
  ([#330](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/330)). On the same production baseline, these tests raised
  measured line coverage from 97.76% to 98.06% and branch coverage from 95.40% to 95.80%.
- **v1.6 confirmation fix:** accepted surrounding Unicode spaces consistently during pending confirmations, with a
  regression that failed before the fix and passed afterward
  ([#341](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/341)). This merged change is not in the published v1.5 JAR.

The linked PRs record Java 25 test commands, failure reproduction, and validation limits.
Coverage figures above describe the #330 comparison, not a claim about every later version or absence of bugs.

## Documentation, diagrams, and website

### User and Developer Guides

- **User Guide:** documented list, clearing, sorting, email search, validation, and recovery behaviour.
  Reorganised commands into task groups, shortened instructions, and aligned examples with the integrated application.
- **Developer Guide:** explained feature interactions, design choices, storage failure paths, and manual test cases.
  Updated the feature map and implementation descriptions as behaviour changed.
- **UML:** revised the corresponding PlantUML sources and rendered images. Split long CSV sequences into smaller
  diagrams with `sd` and `ref` frames and simplified shared architecture/model views.
- **Readability:** standardised diagram presentation with proportional sizing and fine black borders, and used
  concise bullets and clearer headings across the guides.
- **Traceability:** retained existing anchors, corrected stale behaviour descriptions, and recorded library and
  layout reuse. Examples include [#112](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/112) and
  [#164](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/164).

The larger documentation updates are in [#202](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/202) and
[#252](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/252). The latter preserved Glory's existing commits while
integrating the guide, diagram, and release-page changes.

### Website and development tooling

- **MarkBind migration:** moved the documentation website to the course layout, preserving the documentation content
  and improving shared navigation ([#163](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/163)).
- **Site maintenance:** improved guide navigation, image presentation, development-resource instructions, and
  release pages. Build verification checks generated pages, local links, and section targets.
- **Packaging:** contributed application-folder path boundaries and packaging of both macOS JavaFX native
  architectures, selected at startup ([#88](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/88)).
- **CI efficiency:** helped avoid duplicate fork-push and PR test runs and cancel superseded runs while retaining
  the three-platform Java checks ([#164](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/164)).

## Team contributions and release work

- **Integration:** addressed review feedback and integration conflicts across feature and documentation work,
  retaining teammate commits and adapting tests and guides to the combined behaviour.
- **Release preparation:** prepared release records, download information, and validation evidence for
  [v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/138),
  [v1.4](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/194), and
  [v1.5](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/252).
  Recorded local checks separately from hosted CI and unperformed manual platform acceptance.
- **Reviewing tests:** identified temporary-path handling that failed when the parent folder contained spaces in
  [Noah's acceptance-test PR](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/230#pullrequestreview-5656127110).
- **Reviewing UI changes:** flagged a test that changed the user's clipboard without restoring it in
  [Eron's address-copy PR](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/241#pullrequestreview-5656128159).
- **Bug triage:** reported reproducible beta findings with expected/actual behaviour, impact, and evidence, and
  directed them to relevant feature owners. Examples include
  [common member-name punctuation](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/353) and
  [email-domain duplicate detection](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/355).
  These links credit reporting and triage, not implementation of the resulting fixes.

## Reuse and AI assistance

TrackCall extends SE-EDU AddressBook Level 3. My work also uses the project's JavaFX interface, Jackson storage,
Apache Commons CSV handling, and MarkBind documentation tooling. The
[Developer Guide acknowledgements](../DeveloperGuide.html#acknowledgements) record sources and reuse details.

I used OpenAI Codex extensively for implementation suggestions, code and test generation, debugging, audits,
review assistance, documentation, diagrams, and release preparation, including drafting this portfolio.
The linked PRs document validation and limitations. These contributions include AI-generated and adapted work;
automated checks do not replace contributor responsibility for understanding and reviewing it.

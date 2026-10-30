---
layout: default.md
title: Ian Abiel Wangsa - Project Portfolio
pageNav: 3
---

# Ian Abiel Wangsa - Project Portfolio

## Overview {id="project-overview"}

TrackCall is a desktop membership roster for club secretaries. It combines typed commands with a JavaFX interface
for maintaining contact details, finding members, organising overlapping groups, and exchanging CSV rosters.

My role is Team Lead. My primary feature responsibilities are `list`, `clear`, and `sort`.
I also contributed email search, data-protection fixes, regression tests, and the documentation website.
The examples below link to merged work; they do not claim sole ownership of shared components or teammate commits.

## Code contributed

- [My code on the course RepoSense dashboard](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=manutd1234&breakdown=true)
- [My merged pull requests](https://github.com/AY2627S1-CS2103T-W12-1/tp/pulls?q=is%3Apr+is%3Amerged+author%3AManutd1234)
- [My GitHub profile](https://github.com/Manutd1234)

## Enhancements implemented

- **List and clear the roster.** Implemented list feedback, view reset, argument validation, and rollback when saving
  a clear fails in [#43](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/43).
  Later, [#322](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/322) distinguished `clear` for displayed members from
  `clear /all` for the whole roster. Confirmation captures the intended members before deletion; cancelling preserves
  them, and a failed save restores records and selections. This required coordinating command state, filtering,
  persistence, and feedback rather than only emptying a list.
- **Sort members by name.** Added case-insensitive sorting in
  [#68](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/68), improved name-spacing comparisons in
  [#88](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/88), and stabilised equal-name ordering in
  [#222](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/222).
  The final v1.5 design in [#319](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/319) saves the complete roster
  in name order while preserving active conditions. List and restart retain that order; subsequent additions append
  and edits keep their position until another sort.
- **Search by email.** Added `findemail`, its parser and predicate, removable email conditions, offline help, and
  regression tests in [#174](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/174).
  [#288](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/288) made searches cumulative with other active conditions
  and distinguished an equivalent repeated query from a different query producing the same results.
- **Improve member readability.** Kept filter controls and member tags usable in narrow windows through
  [#220](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/220), with wrapping and layout checks.

## Quality and data protection

- **Protect local files.** Added packaged file-boundary handling in
  [#88](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/88) and protected reserved export destinations and aliases in
  [#110](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/110) and
  [#219](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/219).
- **Preserve recovery safeguards.** [#287](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/287) established the
  recovered roster as the session's storage baseline, allowing existing outside-edit detection to work before the
  first command save. Tests cover refusal of the first save and an explicit retry that preserves the outside version.
- **Reduce privacy exposure.** Contributed session locking, backup handling, safe retry guidance, and removal of
  member values from diagnostic logs in [#212](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/212).
  That PR preserved Eron's original contribution and depended on the team's related integration work.
- **Add targeted regressions.** [#330](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/330) covers keyboard focus,
  draft preservation, and combined active-file/backup rollback failures. It adds tests without changing production code.
- **Contribute to v1.6.** [#341](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/341) fixes surrounding Unicode spaces
  in pending confirmations. Its regression failed before the fix and passed afterward. This is merged v1.6 work;
  it is not part of the published v1.5 JAR.

## Documentation and website

- **User Guide:** documented list, clearing, sorting, email search, validation, and recovery behaviour. In
  [#252](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/252), reorganised command sections into task groups,
  shortened instructions, retained existing anchors, and aligned examples with the integrated implementation.
- **Developer Guide:** explained the list/clear, sorting, email-search, storage and recovery paths; updated manual
  tests and the corresponding PlantUML diagrams. Shared architecture, model, and CSV diagrams were also reorganised
  into smaller, more readable views. Evidence is in
  [#202](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/202) and
  [#252](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/252).
- **Website:** migrated the documentation to the course's MarkBind layout in
  [#163](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/163), then improved navigation, guide formatting, image
  presentation and release pages. Documentation builds check generated pages, local links and section targets.

## Team contributions and reviews

- Prepared release records and validation evidence for
  [v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/138),
  [v1.4](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/194), and
  [v1.5](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/252).
  Recorded local checks separately from cross-platform CI and unperformed manual acceptance.
- Reviewed Noah's acceptance-test PR and identified temporary-path handling that failed when the parent folder
  contained spaces: [review of #230](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/230#pullrequestreview-5656127110).
- Reviewed Eron's address-copy fix and flagged a test that did not restore the user's clipboard:
  [review of #241](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/241#pullrequestreview-5656128159).
- Reported reproducible beta findings and linked them to feature owners, for example
  [common member-name punctuation](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/353) and
  [email-domain duplicate detection](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/355).
  Reporting a defect is distinct from implementing or merging its fix.

## Reuse and AI assistance

TrackCall extends SE-EDU AddressBook Level 3. My work also uses the project's JavaFX interface, Jackson storage,
Apache Commons CSV handling, and MarkBind documentation tooling. The
[Developer Guide acknowledgements](../DeveloperGuide.html#acknowledgements) record sources and reuse details.

I used OpenAI Codex extensively for implementation suggestions, code and test generation, debugging, audits,
review assistance, documentation, diagrams, and release preparation, including drafting this portfolio.
The linked PRs document validation and limitations. These contributions include AI-generated and adapted work;
automated checks do not replace contributor responsibility for understanding and reviewing it.

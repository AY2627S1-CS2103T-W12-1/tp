---
layout: default.md
title: Ian Abiel Wangsa — contributions
pageNav: 3
---

# Ian Abiel Wangsa

[GitHub: Manutd1234](https://github.com/Manutd1234) · [Project team](../AboutUs.html)

My initial assigned commands were `list` and `clear`. The merged contributions below also include
sorting, email search, offline-help implementation, and shared reliability work. TrackCall builds on
AB3 and includes changes by the whole team; these links identify specific contributions, not exclusive
ownership of every class touched.

## Feature contributions

| Contribution | Implementation notes | Merged evidence |
| --- | --- | --- |
| List and clear | Restore stored order without saving; clear all records with save-failure rollback. | [#43](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/43), [DG](../DeveloperGuide.html#list-and-clear-commands) |
| Offline help | Local command catalogue, topic navigation, keyboard support and readable feedback. | [#44](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/44), [DG](../DeveloperGuide.html#offline-command-help) |
| Name sorting | Sort the current view without changing stored order; normalize repeated spaces for comparison. | [#68](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/68), [#88](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/88), [DG](../DeveloperGuide.html#name-sorting) |
| Email search | Literal, case-insensitive matching; replace prior conditions while retaining sorting. | [#174](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/174), [DG](../DeveloperGuide.html#find-members-by-email) |

## Shared reliability and documentation work

* [#88](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/88): packaged file-location restrictions and Mac architecture support.
* [#109](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/109), [#110](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/110), and [#114](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/114): corrupt JSON, protected exports and CSV quoting fixes.
* [#212](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/212): single-instance protection, useful-backup preservation on unchanged saves, optional-address clearing, import-save guidance and privacy corrections. This PR also retains a teammate's original logging commit.
* [#163](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/163), [#164](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/164), and [#202](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/202): MarkBind migration, navigation, guide corrections and UML updates.

## Pending v1.5 fixes

[Validation and feedback #218](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/218),
[export aliases #219](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/219),
[narrow-window layout #220](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/220), and
[equal-name sorting #222](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/222) are separate contributions
awaiting integration. Their PRs contain reproduction steps and validation evidence.

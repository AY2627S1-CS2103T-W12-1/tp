---
layout: default.md
title: "v1.3 release records"
pageNav: 6
---

# v1.3 release records

This directory records the published TrackCall v1.3 MVP deliverables, including
Eron’s active-filter feedback change in [PR #137](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/137).
The release was published and updated on 15 October 2026.

- [Published GitHub release](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/tag/v1.3)
- [Download trackcall.jar](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/trackcall.jar)
- [Release checklist #135](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/135)
- [v1.3 milestone](https://github.com/AY2627S1-CS2103T-W12-1/tp/milestone/3)
- [Week 9 requirements](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week9/project.html)

## Recorded deliverables

| File | Purpose |
| --- | --- |
| [RELEASE_NOTES.md](RELEASE_NOTES.md) | Published changes compared with AB3, screenshots, download instructions, and limitations. |
| [VALIDATION.txt](VALIDATION.txt) | Build and test results, screenshot provenance, and verification limits. |
| [SHA256SUMS.txt](SHA256SUMS.txt) | Published SHA-256 manifest for the release assets, including the JAR. |
| [interface.jpg](interface.jpg) | Fresh capture of the rebuilt app’s interface and welcome message. |
| [filter-single.jpg](filter-single.jpg) | Eron’s supplied single-tag filter screenshot. |
| [filter-multiple.jpg](filter-multiple.jpg) | Eron’s supplied screenshot showing every active filter. |
| [tag-group.jpg](tag-group.jpg) | Fresh capture of bulk tagging in the rebuilt app. |
| [validation.jpg](validation.jpg) | Fresh capture of invalid phone-number feedback in the rebuilt app. |
| [backup-recovery.png](backup-recovery.png) | Startup recovery choices for an unreadable sample roster. |
| [bulk-untag.png](bulk-untag.png) | Removing a tag from the displayed group. |
| [csv-export.png](csv-export.png) | Exporting the complete roster as CSV. |
| [csv-import.png](csv-import.png) | Import results after validating a CSV file. |
| [edit-filtered-view.png](edit-filtered-view.png) | Updating a member while retaining the filtered view. |
| [offline-help.png](offline-help.png) | Built-in CSV import instructions. |
| [protected-export.png](protected-export.png) | Preventing CSV export from overwriting the roster. |
| [sort-names.png](sort-names.png) | Alphabetical sorting of displayed members. |

The original captures, notes, validation record and checksum manifest mirror the published release assets. The gallery also includes thirteen consistently framed PNG presentations added on 23 October 2026.
All thirteen original captures and the executable are unchanged. The notes and checksum manifest include the complete feature catalogue and the framed screenshot gallery; the validation record distinguishes the original captures from their presentation derivatives. The notes retain their public
release-image URLs; the table above links to the copies stored in Git.

The executable is distributed through GitHub Releases. Its source is commit
[`e3872268dd2bdd8967ab2778c15ca8bb37430beb`](https://github.com/AY2627S1-CS2103T-W12-1/tp/commit/e3872268dd2bdd8967ab2778c15ca8bb37430beb).
This documentation archive was added after publication and does not change the
release tag or executable.

## Framed gallery images

All thirteen gallery images use a common white canvas, centered windows, proportional scaling, and soft shadows. The originals in the table above remain available.

* [framed-interface.png](framed-interface.png)
* [framed-filter-single.png](framed-filter-single.png)
* [framed-filter-multiple.png](framed-filter-multiple.png)
* [framed-tag-group.png](framed-tag-group.png)
* [framed-bulk-untag.png](framed-bulk-untag.png)
* [framed-sort-names.png](framed-sort-names.png)
* [framed-csv-export.png](framed-csv-export.png)
* [framed-csv-import.png](framed-csv-import.png)
* [framed-offline-help.png](framed-offline-help.png)
* [framed-edit-filtered-view.png](framed-edit-filtered-view.png)
* [framed-validation.png](framed-validation.png)
* [framed-protected-export.png](framed-protected-export.png)
* [framed-backup-recovery.png](framed-backup-recovery.png)

## Verify the published downloads

Download all thirty release assets into a new directory, then run from that directory:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

The manifest covers the other twenty-nine assets and does not include itself. The JAR
must be downloaded alongside the files to check the complete manifest. The
archive-only README is not a published asset and is not part of the manifest.

The v1.3 JAR is 13,893,874 bytes and has SHA-256:

```text
52a6d36bcd33010740f32bdbe8184d8cc3996f422be32866dfa92fe725b80d46
```

The release validation recorded 505 passing tests and packaged checks for all
fourteen command types. Consult [VALIDATION.txt](VALIDATION.txt) for the exact
scope and remaining limitations. This documentation-only archive introduces no
executable lines, so changed-line test coverage does not apply to this PR.

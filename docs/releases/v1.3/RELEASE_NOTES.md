# v1.3 - TrackCall

The executable includes the 15 October 2026 filter-feedback update ([#137](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/137)). If you downloaded an earlier v1.3 build, download `trackcall.jar` again.

TrackCall's first MVP helps club membership secretaries maintain contact details and organise overlapping groups using typed commands.

## What's New Compared with AB3

* **Keep a club roster easy to read.** A redesigned light interface adds labelled contact fields, visible group tags, first-launch guidance, and clearer feedback.
* **Find overlapping groups.** `filter t/TAG` narrows the displayed members by an exact, case-sensitive tag. Successive filters find the intersection and name every active tag; `list` restores everyone.
* **Update a whole group's tags.** `tagall` and `untagall` change one tag for every displayed member, preserve their other tags, leave hidden members unchanged, and report changed and skipped counts.
* **Put names in order.** `sort` displays the current results in case-insensitive A–Z order without rewriting the saved roster order. It stays active through searches and member changes.
* **Exchange rosters through CSV.** Export every member, including hidden records, or import a validated UTF-8 CSV and append unique members. Both commands share the `name,phone,email,address,tags` format.
* **Get help without internet access.** `help`, `help COMMAND`, and F1 open examples, syntax, expected results, and common errors for all fourteen commands. Escape closes help.
* **Edit without losing your place.** Editing preserves name searches, tag filters, and sorting. Duplicate checks compare trimmed name, phone, email, and address, allowing different people to share a name.
* **Correct mistakes with clearer feedback.** Stronger member-index and field validation, clearer list/find/clear counts, and atomic CSV validation help prevent unintended changes.
* **Protect and recover local data.** CSV operations stay within the JAR folder and protect application, roster, backup, and recovery files. Failed exports preserve existing files; automatic backups and explicit startup recovery protect an unreadable roster. A failed `clear` save restores the previous roster and view.

The familiar `add`, `edit`, `delete`, `find`, `list`, `clear`, and `exit` commands remain available. Download **[trackcall.jar](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/trackcall.jar)** and use **Java 25** to get started.

## Complete feature set

This release includes **fourteen commands**. Alongside the additions above, TrackCall retains and adapts
AB3's core `add`, `edit`, `delete`, `clear`, `list`, `find`, `help`, and `exit` commands.

| Task | Commands | Available in v1.3 |
| --- | --- | --- |
| Maintain member records | `add`, `edit` | Record a name, phone, email, required address, and optional tags; update contact details or replace a member's tags. |
| Browse and find members | `list`, `find`, `filter`, `sort` | Show the full roster, search complete name words, narrow by tags, and sort displayed names from A to Z. |
| Update a displayed group's tags | `tagall`, `untagall` | Add or remove one tag for every displayed member while preserving other tags and hidden records. |
| Exchange rosters | `export`, `import` | Export every member to CSV or validate a CSV and append unique member records. |
| Remove records | `delete`, `clear` | Delete one displayed member or clear the whole roster. Both are immediate, with no confirmation or undo. |
| Get help and finish working | `help`, `exit` | Read built-in help and command examples offline, then close the application. |

Member records retain their contact fields and tag assignments. Successful data changes are saved locally;
automatic backups keep the previous saved roster, and startup recovery offers choices when a roster is unreadable.
See the installation and limitations below for file locations and save-error guidance.

## TrackCall in Action

All thirteen screenshots use a consistent centered frame, white margins, and soft shadow. Windows are scaled proportionally, and dialogs keep their natural proportions. The original captures remain available in the release assets; `VALIDATION.txt` records their provenance. Sample data is shown, and the status-bar path depends on the launch folder.

### 1. A clearer membership workspace

First launch shows the command box, welcome message, member cards, group tags, and data-file status bar.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-interface.png"><img width="960" alt="TrackCall v1.3 first-launch interface with sample member records and group tags" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-interface.png"></a>
</p>

### 2. Filter a single group

`filter t/committee` shows Bob and Daniel and names the active tag in the feedback.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-filter-single.png"><img width="960" alt="TrackCall v1.3 showing committee members and single-tag filter feedback" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-filter-single.png"></a>
</p>

### 3. See every active tag filter

After `filter t/committee`, run `filter t/year2` to keep members in both groups. The feedback names both active tags.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-filter-multiple.png"><img width="960" alt="TrackCall v1.3 showing the intersection of committee and year2 with both active tags named" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-filter-multiple.png"></a>
</p>

### 4. Add a tag to a displayed group

`filter t/friends` followed by `tagall t/event2026` adds the event tag to Alex and Bernice while keeping their existing tags.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-tag-group.png"><img width="960" alt="TrackCall v1.3 adding event2026 to two displayed members" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-tag-group.png"></a>
</p>

### 5. Remove a tag from a displayed group

`untagall t/event2026` removes the event tag from the two displayed friends while retaining their other tags. Members outside the current view are unchanged.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-bulk-untag.png"><img width="960" alt="TrackCall v1.3 reporting a bulk tag removal while retaining other member tags" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-bulk-untag.png"></a>
</p>

### 6. Sort names from A to Z

`sort` puts the displayed members in alphabetical order. Use `list` to restore the complete roster in stored order.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-sort-names.png"><img width="960" alt="TrackCall v1.3 showing members sorted alphabetically by name" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-sort-names.png"></a>
</p>

### 7. Export a complete roster

`export members.csv` writes every member to a UTF-8 CSV, including members hidden by the current view. The result reports the resolved destination.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-csv-export.png"><img width="960" alt="TrackCall v1.3 reporting a successful complete-roster CSV export" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-csv-export.png"></a>
</p>

### 8. Import validated member records

`import p/import-demo.csv` validates the whole CSV before appending unique members, then reports imported and skipped duplicate counts.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-csv-import.png"><img width="960" alt="TrackCall v1.3 reporting imported and skipped member counts after CSV validation" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-csv-import.png"></a>
</p>

### 9. Read command help offline

`help import` opens the import syntax, example, expected result, and common errors. Select All commands to return to the fourteen-command catalogue.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-offline-help.png"><img width="960" alt="TrackCall v1.3 built-in offline help showing CSV import instructions" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-offline-help.png"></a>
</p>

### 10. Keep the filtered view after editing

Use `filter t/friends`, then `edit 1 p/98765432` to update Alex while keeping the two displayed friends.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-edit-filtered-view.png"><img width="960" alt="TrackCall v1.3 retaining its displayed group after a member edit" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-edit-filtered-view.png"></a>
</p>

### 11. Reject invalid input

`edit 1 p/abc` explains the phone-number rule and leaves the member unchanged.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-validation.png"><img width="960" alt="TrackCall v1.3 rejecting an invalid phone number with corrective feedback" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-validation.png"></a>
</p>

### 12. Protect the active roster from CSV export

`export data/addressbook.json` is rejected because CSV export cannot overwrite the active roster file.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-protected-export.png"><img width="960" alt="TrackCall v1.3 rejecting a CSV export to the protected roster file" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-protected-export.png"></a>
</p>

### 13. Recover from an unreadable roster

If a roster cannot be loaded, TrackCall preserves its damaged contents and offers recovery choices before opening an editable roster.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-backup-recovery.png"><img width="960" alt="TrackCall v1.3 startup recovery dialog for an unreadable roster" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/framed-backup-recovery.png"></a>
</p>

<details>
<summary>Installation, validation and known limitations</summary>

## Download and run

1. Install **Java 25**. On macOS, follow the [course Java installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).
2. Download **[trackcall.jar](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/trackcall.jar)** from this release's assets. The source-code ZIP/TAR downloads are not the executable.
3. Put the JAR in an empty, writable folder, open a terminal there, and run:

   ```text
   java -jar trackcall.jar
   ```

4. Try `help`, `list`, and `sort`. Your first launch displays sample members. Successful changes are saved automatically to `data/addressbook.json` beside the JAR.

The executable is 13,893,874 bytes (about 13.9 MB). `SHA256SUMS.txt` is attached so downloads can be checked. This release also includes downloadable screenshot assets.

- [User Guide for v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/v1.3/docs/UserGuide.md)
- [Developer Guide for v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/v1.3/docs/DeveloperGuide.md)
- [Project website](https://ay2627s1-cs2103t-w12-1.github.io/tp/)

## Scope and known limitations

- Deletion and clearing are immediate, with no confirmation or undo. Keep backups of important data.
- `find` matches complete name words; tags are case-sensitive. Names currently accept ASCII letters, digits, and spaces.
- For `edit`, an unsupported prefix-like token inside an address can become literal address text; use lowercase `t/` for tags. `add` rejects unsupported single-letter prefixes.
- Manual JSON editing must use the current `persons` / `tags` structure. The Developer Guide's proposed stricter `tagged` schema is not implemented. Unknown member fields and repeated fields are not yet rejected strictly.
- CSV commands in the packaged app accept files only within the JAR folder or its subfolders. Copy an incoming CSV there before importing it.
- If a save fails after add/edit/delete/tagall/untagall/import, the change remains in memory and must be saved successfully before exiting. Consult the User Guide's recovery instructions. A failed `clear` rolls back.
- This MVP has no cloud synchronisation, accounts, payment processing, event attendance, or messaging features.

## Build and validation

Built without source modifications from team commit [`e3872268dd2bdd8967ab2778c15ca8bb37430beb`](https://github.com/AY2627S1-CS2103T-W12-1/tp/commit/e3872268dd2bdd8967ab2778c15ca8bb37430beb).

- Clean Java 25 validation of PR #137: `./gradlew clean check coverage shadowJar`. The merge commit has the identical source tree; `./gradlew shadowJar javadoc` then built the final release artifact.
- **505 tests passed**, with zero failures, errors, or skipped tests. These include command-box integration for all fourteen commands, CSV round trips, and startup/recovery tests.
- Checkstyle and repository-wide checks passed. Javadoc generation succeeded with existing warnings.
- The packaged JAR was launched successfully on macOS ARM64 with Java 25. It bundles Windows, Linux, Intel Mac, and ARM Mac native libraries; separate Windows/Linux manual execution was not performed in this release session.
- A separate smoke check loaded production classes from the release JAR and exercised all fourteen command types, CSV round trips, saved-record reload, protected export paths, and backup recovery while running from a different working directory.
- For PR #137, all **16 changed executable lines** were fully covered locally in JaCoCo, with no missed branches on those lines. Its Windows, macOS, and Linux GitHub CI jobs passed. This is a local coverage result, not a claimed Codecov percentage.
- Coverage across all historical PRs is not claimed to be 100%: the published report for merged PR #134 is **98.97959% patch coverage** (one partially covered changed line). Passing tests or CI does not establish 100% patch coverage.

Release tracking: #135. Milestone: [v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/milestone/3).

</details>

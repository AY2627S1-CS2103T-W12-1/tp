# TrackCall v1.3: first MVP release

**Updated on 15 October 2026 to include filter-feedback change ([#137](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/137)). If you downloaded v1.3 earlier today, download `trackcall.jar` again to get this update.**

TrackCall helps club membership secretaries maintain contact details and organise overlapping groups using typed commands. This first product release builds on AddressBook Level 3 (AB3) with group operations, CSV exchange, name sorting, offline help, and safer local storage.

## What's new compared with AB3

- **A club-focused interface.** TrackCall has a redesigned light interface, labelled contact fields, visible group tags, a welcome message at startup, and clearer success/error feedback.
- **Filter groups with `filter t/TAG`.** Narrow the displayed members by an exact, case-sensitive tag. Apply successive filters to find members belonging to several groups, or filter name-search results. Feedback now lists every distinct active tag in the order applied, so successive filters clearly show the complete intersection. Repeating a tag does not duplicate it in the message. `list` shows everyone again and resets the active filters.
- **Update a group's tags with `tagall` and `untagall`.** Add or remove one tag across every currently displayed member while preserving other tags and leaving hidden members unchanged. Feedback reports changed and skipped members. Tags support 1–30 ASCII letters or digits.
- **Sort names with `sort`.** Display the current results in case-insensitive A–Z order. Sorting remains active through searches and member changes; `list`, a successful import, or restarting returns to stored order. Sorting does not rewrite the saved roster order.
- **Exchange rosters through CSV.** `export [FILEPATH]` exports the complete roster, including hidden members. `import p/FILE_PATH` validates the entire UTF-8 CSV before appending unique members, reports skipped duplicates, and then shows the full roster. Exported files use the same five-column format accepted by import: `name,phone,email,address,tags`.
- **Read help offline.** `help`, `help COMMAND`, and F1 open the built-in command guide with examples, syntax, expected results, and common errors for all fourteen commands. Escape closes help.
- **Keep the current view after editing.** Editing preserves active name searches, tag filters, and sorting. A member who no longer matches disappears from the view but remains in the roster.
- **Know when the view resets.** `add` shows the full roster and clears active name searches and tag filters while preserving name sorting. A new `find` searches the full roster and replaces previous searches and tag filters.
- **Use more precise duplicate checks and feedback.** Duplicate detection compares the trimmed name, phone, email, and address; different people may share a name. Tags do not determine identity. Member indices and list/find/clear feedback have stronger validation and clearer counts.
- **Protect local files.** Packaged builds keep application files and CSV operations within the JAR folder. Export protects the active roster, preferences, running JAR, backup, and recovery filenames. Failed exports preserve an existing destination file. CSV import rejects malformed quoting and invalid UTF-8 without partially adding records.
- **Recover from an unreadable roster.** Before replacing a valid roster, TrackCall keeps its previous saved contents in `data/addressbook.json.bak`. Startup preserves an unreadable roster in a uniquely named recovery copy, then offers a valid backup, an explicit empty roster, or closing without replacement. A failed `clear` save restores the previous roster and view.

The familiar `add`, `edit`, `delete`, `find`, `list`, `clear`, and `exit` commands remain available. Together with the additions above, v1.3 provides fourteen commands for the membership workflow.

## Product screenshots

The filter screenshots below are original captures supplied for this release. The interface, bulk-tagging, and validation screenshots were captured directly from the rebuilt release JAR on macOS. All five images are uploaded unchanged. The interface, bulk-tagging, and validation screenshots use the built-in sample roster; the filter screenshots use illustrative member records. The data-file path depends on the launch location. `VALIDATION.txt` records their provenance and the commands checked against the updated JAR.

### Redesigned interface and first-launch guidance

![TrackCall’s command box, welcome message, sample member cards, group tags and data-file status bar](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/interface.jpg)

### Filter a single group

`filter t/committee` lists Bob and Daniel and names the active tag in the feedback.

![Eron’s screenshot of two committee members and single-tag filter feedback](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/filter-single.jpg)

### Show every active filter

Applying `filter t/year2` after `filter t/committee` leaves Bob and reports both active tags: `1 member(s) listed with tags "committee", "year2".`

![Eron’s screenshot of Bob matching committee and year2, with both active filters named](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/filter-multiple.jpg)

### Bulk tagging a displayed group

After `filter t/friends` and `tagall t/event2026`, Alex and Bernice retain their existing tags and gain `event2026`.

![The rebuilt TrackCall JAR reports adding event2026 to two displayed members](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/tag-group.jpg)

### Correcting invalid input

An invalid phone number produces feedback and leaves the member unchanged.

![The rebuilt TrackCall JAR rejects edit 1 p/abc and explains the phone-number rule](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/validation.jpg)

## Download and run

1. Install **Java 25**. On macOS, follow the [course Java installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).
2. Download **[trackcall.jar](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.3/trackcall.jar)** from this release's assets. The source-code ZIP/TAR downloads are not the executable.
3. Put the JAR in an empty, writable folder, open a terminal there, and run:

   ```text
   java -jar trackcall.jar
   ```

4. Try `help`, `list`, and `sort`. Your first launch displays sample members. Successful changes are saved automatically to `data/addressbook.json` beside the JAR.

The executable is 13,893,874 bytes (about 13.9 MB). `SHA256SUMS.txt` is attached so downloads can be checked. This release also includes downloadable copies of the five screenshots.

- [User Guide for v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/v1.3/docs/UserGuide.md)
- [Developer Guide for v1.3](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/v1.3/docs/DeveloperGuide.md)
- [Project website](https://ay2627s1-cs2103t-w12-1.github.io/tp/)

## Scope and known limitations

- Deletion and clearing are immediate, with no confirmation or undo. Keep backups of important data.
- `find` matches complete name words; tags are case-sensitive. Names currently accept ASCII letters, digits, and spaces.
- For `edit`, an unsupported prefix-like token inside an address can become literal address text; use lowercase `t/` for tags. `add` rejects unsupported single-letter prefixes.
- Manual JSON editing must use the current `persons` / `tags` structure. Unknown member fields and repeated fields are not yet rejected strictly.
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

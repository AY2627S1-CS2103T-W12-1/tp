# TrackCall v1.4 alpha

TrackCall v1.4 alpha brings the membership workflow to nineteen commands. This release includes
all features merged into the team repository for v1.4, including the selected-member panel and
optional addresses. It requires Java 25.

## Changes since v1.3

- Find members by a full email address or any literal email fragment with
  `findemail KEYWORD [MORE_KEYWORDS]...`. Matching ignores case and accepts any supplied keyword;
  a new email search replaces earlier searches, tag filters, and show selections.
- Find partial name words with `find`, rather than having to type a complete word.
- Select a subset of displayed members with `show INDEX [MORE_INDICES]...`.
- Add or remove one displayed member's tag with `tag INDEX t/TAG` and `untag INDEX t/TAG`.
  Other tags and contact fields are retained. Repeating an operation reports that no change is needed.
- Rename a tag across the whole roster with `renametag t/CURRENT_TAG t/NEW_TAG`.
- Add a member without an address by omitting `a/ADDRESS`. Missing addresses appear as `(none)`
  and survive saving, loading, and CSV export/import. A supplied blank `a/` is still rejected.
- Confirm a deletion with `y` as the next input. Other submitted input cancels deletion and is not
  executed as a command. Clearing the entire roster remains immediate.
- Read names and tags in a compact roster and select a member to see and copy contact fields.
  F2 focuses the roster; Escape hides the selected details and returns to the command box.
- See active name, email, tag, and show conditions above the roster. Remove an individual condition
  with its X button. The Show indicator reports active show selections; clearing it retains other
  filters. The roster heading shows the displayed member count, including zero results.
- Reject unknown or uppercase single-letter field prefixes in `edit` before changing records or
  files. Valid address text containing standalone `c/o`, URLs, numeric slashes, or embedded slashes
  remains supported. Add and edit share prefix validation.
- Use the course MarkBind website layout, shared Site Map, page outlines, and documentation search.
  Existing page paths and legacy section anchors are retained. Long desktop page outlines scroll.
- Updated guides, UML diagrams, command help, filtering diagnostics, and regression tests accompany
  the implemented features. Gradle application and test tasks explicitly enable Java assertions.
- CI runs the complete Linux, macOS, and Windows test matrix. Feature branches use PR checks;
  superseded runs are cancelled automatically to avoid duplicate work.

## Product screenshots

These captures show the compact roster, visible search and tag conditions, and selected-member
details. They use sample data. The data-file path in the status bar depends on where the app runs.

### Compact roster

The roster lists member names and tags while keeping contact details in a separate panel.

![TrackCall roster with member names and tags](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/roster-overview.png)

### Active search and tag conditions

The name search and family tag filter appear above the roster, with a separate X button for each.

![TrackCall showing the active family tag filter and Alex name search](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/active-filters.png)

### Selected-member details

Selecting Andy reveals contact fields and copy buttons alongside the filtered roster.

![TrackCall showing Andy selected with phone, email, address, tags, and copy buttons](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/selected-member.png)

## Install and run

Download `trackcall.jar` from this release, put it in an empty writable folder, and use Java 25:

```text
java -ea -jar trackcall.jar
```

The application opens with sample members on first launch. Use `help` for the offline command
catalogue or `help COMMAND` for one command. The nineteen commands are `add`, `edit`, `delete`,
`clear`, `list`, `show`, `find`, `findemail`, `filter`, `sort`, `tag`, `untag`, `tagall`, `untagall`,
`renametag`, `import`, `export`, `help`, and `exit`.

Data is saved beside the JAR in `data/addressbook.json`. CSV paths must remain inside the JAR's
folder or its subfolders. Keep a backup before replacing an existing installation's data.

## Validation and provenance

The Java 25 build passes `clean check coverage shadowJar`: 632 tests across 84 suites, with zero
failures, errors, or skipped tests, plus both Checkstyle tasks. The MarkBind build verifies all
13 pages and 1,195 local references. The release includes a separate validation record and
SHA-256 checksums for the exact uploaded artifacts.

See `VALIDATION.txt` for the final source, packaged checks, platform coverage, and limitations.
The October 16 preparation record is superseded by these final release records.

## Remaining limitations

- `clear` has no confirmation or undo; `delete` has confirmation but no undo.
- Tags remain case-sensitive, and member names accept ASCII letters, digits, and spaces.
- JSON uses `persons` and per-member `tags`; the proposed `tagged` schema is not implemented.
  Unknown and repeated JSON fields are not yet rejected strictly.
- A save failure after most data-changing commands can leave changes in memory. Follow the
  User Guide's recovery steps before exiting. A failed clear restores the previous roster and view.
- Successful `untag` feedback can include contact fields even when the selected-member panel is hidden.
- Automated Windows and Linux CI is distinct from manual execution of the downloaded JAR on those
  platforms. Separate manual Windows/Linux acceptance is not claimed by this release.

Release source: [`e00be2a94386967185d5a1b308b3317c37ca7221`](https://github.com/AY2627S1-CS2103T-W12-1/tp/commit/e00be2a94386967185d5a1b308b3317c37ca7221). The lightweight `v1.4` tag points to this verified merge commit.

Final build checks: [master Java CI](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions/runs/39634782333), [v1.4 tag Java CI](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions/runs/39634952042), [documentation build and publication](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions/runs/39634782431).

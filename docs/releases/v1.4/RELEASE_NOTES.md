# v1.4 - TrackCall

TrackCall v1.4 alpha expands the membership workflow to nineteen commands, with more flexible searches, finer tag control, and a compact roster beside selected-member details.

## What's New in v1.4

* **Find members with less typing.** `find` accepts parts of name words. `findemail` searches full email addresses or literal fragments, ignores case, and accepts any supplied keyword; a new email search replaces earlier searches, filters, and show selections.
* **Focus on the members you need.** `show INDEX [MORE_INDICES]...` selects a subset of the displayed list. Visible name, email, tag, and show conditions have individual X controls, and the roster heading reports the displayed count.
* **Read and copy contact details when needed.** The compact roster shows names and tags. Select a member to reveal contact fields and copy buttons; F2 focuses the roster and Escape hides the selected details and returns to the command box.
* **Adjust one member's tags without replacing the rest.** `tag INDEX t/TAG` and `untag INDEX t/TAG` retain other tags and contact fields. Repeated operations report when no change is needed.
* **Rename a group across the whole roster.** `renametag t/CURRENT_TAG t/NEW_TAG` updates every matching member, including hidden records, while keeping their other tags.
* **Add members before their address is known.** Omit `a/ADDRESS` when adding a member. The missing address appears as `(none)` and survives saving, reloading, and CSV export/import. A supplied blank `a/` is rejected.
* **Check before deleting a member.** `delete INDEX` asks for `y` as the next input. Any other submitted input cancels and is not executed as a command. Clearing the whole roster remains immediate.
* **Catch mistyped field prefixes before saving.** `add` and `edit` share validation for unknown or uppercase single-letter prefixes. Legitimate address text containing `c/o`, URLs, numeric slashes, and embedded slashes remains supported.
* **Navigate updated guides more easily.** The MarkBind website adds a shared Site Map, page outlines, and documentation search while retaining existing paths and legacy section anchors. Long desktop outlines scroll. Offline help covers all nineteen commands.
* **Keep maintenance and diagnostics current.** Updated UML diagrams, filtering diagnostics, regression tests, explicit Java assertions, and the Linux/macOS/Windows CI matrix accompany the implemented features. Superseded CI runs are cancelled automatically.

Download **[trackcall.jar](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/trackcall.jar)** and use **Java 25**. See the [User Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/UserGuide.html) for complete command instructions.

## Complete feature set

This release includes **nineteen commands**. It carries forward the v1.3 workflow and retains and adapts
AB3's core `add`, `edit`, `delete`, `clear`, `list`, `find`, `help`, and `exit` commands.

| Task | Commands | Available in v1.4 |
| --- | --- | --- |
| Maintain member records | `add`, `edit` | Record a name, phone, email, optional address, and tags; update contact details or replace a member's tags. Select a member to read and copy contact fields. |
| Browse and find members | `list`, `show`, `find`, `findemail`, `filter`, `sort` | Show all or selected members, search partial names or emails, combine tag filters, and sort displayed names from A to Z. Active conditions can be removed individually. |
| Organise member groups | `tag`, `untag`, `tagall`, `untagall`, `renametag` | Change one member's tag, update every displayed member, or rename a tag across the full roster while preserving other tags. |
| Exchange rosters | `export`, `import` | Export every member to CSV or validate a CSV and append unique member records, including records without an address. |
| Remove records | `delete`, `clear` | Confirm before deleting one displayed member, or clear the whole roster immediately. Neither operation has undo. |
| Get help and finish working | `help`, `exit` | Read built-in help for all nineteen commands offline, then close the application. |

Member records retain their contact fields and tag assignments. Successful data changes are saved locally;
automatic backups keep the previous saved roster, and startup recovery offers choices when a roster is unreadable.
See the installation and limitations below for file locations and save-error guidance.

## TrackCall in Action

All thirteen screenshots use a consistent centered frame, white margins, and soft shadow. Windows are scaled proportionally, and dialogs keep their natural proportions. The original captures remain available in the release assets; `VALIDATION.txt` records their provenance. Sample data is shown, and the status-bar path depends on the launch folder. Successful commands clear the command box; each caption identifies the relevant command.

### 1. A compact membership roster

Names and tags stay in the roster, with contact details available in a separate panel when a member is selected.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-roster-overview.png"><img width="960" alt="TrackCall v1.4 compact roster showing member names and tags" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-roster-overview.png"></a>
</p>

### 2. See and remove active conditions

Name and tag conditions appear above the roster. Use an individual condition's X button to remove that condition.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-active-filters.png"><img width="960" alt="TrackCall v1.4 showing active family tag and Alex name conditions with separate removal controls" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-active-filters.png"></a>
</p>

### 3. Select a member to read and copy details

Selecting Andy reveals phone, email, address, tags, and adjacent copy buttons. Use F2 to focus the roster and Escape to hide the details.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-selected-member.png"><img width="960" alt="TrackCall v1.4 selected-member panel showing Andy and contact-field copy buttons" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-selected-member.png"></a>
</p>

### 4. Search using part of a name

`find lex` matches Alex Yeoh without requiring the complete name word.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-partial-name.png"><img width="960" alt="TrackCall v1.4 finding Alex Yeoh with the partial name lex" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-partial-name.png"></a>
</p>

### 5. Search email addresses with multiple keywords

`findemail berniceyu roy` returns members whose email contains either keyword, ignoring case. The active Email condition stays visible.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-email-search.png"><img width="960" alt="TrackCall v1.4 email search returning Bernice Yu and Roy Balakrishnan" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-email-search.png"></a>
</p>

### 6. Show selected members only

`show 1 3 5` keeps the selected members from the current list. The Show indicator and roster count make the restricted view visible.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-show-selection.png"><img width="960" alt="TrackCall v1.4 showing three selected members with an active Show indicator and roster count" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-show-selection.png"></a>
</p>

### 7. Add one member's tag

`tag 1 t/committee` adds committee to Alex while retaining friends.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-tag-member.png"><img width="960" alt="TrackCall v1.4 adding committee to Alex while retaining the friends tag" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-tag-member.png"></a>
</p>

### 8. Remove one member's tag

`untag 1 t/committee` removes committee from Alex while keeping friends and the contact fields.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-untag-member.png"><img width="960" alt="TrackCall v1.4 removing committee from Alex and retaining friends" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-untag-member.png"></a>
</p>

### 9. Rename a tag throughout the roster

`renametag t/colleagues t/committee` replaces colleagues for every matching member and reports how many changed.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-rename-tag.png"><img width="960" alt="TrackCall v1.4 reporting that colleagues was renamed to committee for two members" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-rename-tag.png"></a>
</p>

### 10. Record an unknown address

`add n/Mei Tan p/91234567 e/mei@example.com t/committee` creates Mei without an address. Selecting the record shows `(none)` in the address field.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-optional-address.png"><img width="960" alt="TrackCall v1.4 showing Mei Tan with no address supplied" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-optional-address.png"></a>
</p>

### 11. Confirm a member deletion

`delete 1` names Alex and asks for confirmation. Type `y` to delete or submit any other input to cancel. No record is removed while this prompt is pending.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-delete-confirmation.png"><img width="960" alt="TrackCall v1.4 pending deletion prompt with a highlighted confirmation command box" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-delete-confirmation.png"></a>
</p>

### 12. Reject a mistyped field prefix

`edit 1 a/Main Road T/committee` is rejected because the tag prefix must be lowercase. The roster is unchanged.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-prefix-validation.png"><img width="960" alt="TrackCall v1.4 rejecting an uppercase T field prefix before applying an edit" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-prefix-validation.png"></a>
</p>

### 13. Read help for the expanded workflow

`help findemail` opens the email-search instructions, syntax, example, and common errors. Select All commands to return to the nineteen-command offline catalogue.

<p align="center">
  <a href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-offline-help.png"><img width="960" alt="TrackCall v1.4 offline help showing the new email-search command" src="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/download/v1.4/framed-offline-help.png"></a>
</p>

<details>
<summary>Installation, validation and known limitations</summary>

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

</details>

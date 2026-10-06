---
layout: page
title: User Guide
---

TrackCall helps a club's membership secretary maintain member contact details using typed commands.

This guide covers the **v1.2 development build** and its twelve available commands.
Tag filtering, CSV import/export, and name sorting are available. Bulk tag editing remains planned.

Start with [Quick start](#quick-start), or use the [command summary](#command-summary) to look up a command.

* Table of Contents
{:toc}

## Quick start

### Install and launch

1. Install Java **25**. On macOS, use the compatible JDK+FX distribution in the
   [course installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).
2. Follow the [setup guide](SettingUp.md) to prepare your project checkout.
3. Run `./gradlew run` from the checkout. On Windows, use `gradlew.bat run`.
   Published packages will appear on the [team releases page](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases).

### Try your first commands

1. On first launch, check that sample members and a welcome hint appear.
2. Type `help` and press **Enter** to open the offline command guide.
3. Close help with **Escape**. Type `list` to see the roster.
4. Add a member:

   ```text
   add n/Alice Tan p/91234567 e/alice@example.com a/12 Orchard Road t/committee
   ```

After saving, TrackCall reports `Added member`. Name, Phone, Email, Address, and Tags appear on separate lines.
Alice also appears in the member list. `Tags: (none)` means the member has no tags.

Type `export` to create a CSV copy of the full roster, or use `import p/FILE_PATH` to append members
from an existing CSV roster.

### Read the interface

* **Command box:** enter a command above the result area.
* **Result area:** read feedback and errors. Scroll through long feedback or drag the divider to give it more space.
* **Member list:** check each member's current list number, name, contact details, and tags. Long details wrap.
* **Window position:** TrackCall restores its saved position within the available screens at startup.
  It also handles a previously connected monitor being unavailable.

## Commands

### Read command formats

* Use lowercase keywords without a leading slash: `help`, not `/help`.
* Replace `UPPER_CASE` placeholders with your own values.
* Square brackets mark optional input. `[t/TAG]...` means zero or more tags.
* Do not type the brackets or ellipsis.
* Submit each command on one line.

Use the [command summary](#command-summary) for a compact reference.

### Offline help: `help`

Open help by typing `help`, pressing **F1**, or choosing **Help → Help**.
The guide works without internet access and does not change or save the roster.

The overview groups all twelve commands into four categories:
Browse members, Manage members, Remove records, and Help & session.
Each command has a short description and a runnable example.
The overview uses two columns in a wide window and one in a narrow window.

* **Open details:** select a command-name button or type `help COMMAND`, such as `help edit`.
  Details include syntax, an example, the expected result, usage notes, and common errors.
* **Return to the overview:** select **All commands**, or close help and type `help`.
* **Scroll:** use **Up/Down**, **Page Up/Page Down**, or **Home/End**.
* **Close:** press **Escape**.

Help accepts at most one lowercase topic.
`help ADD`, `help unknown`, and `help add edit` report errors.
`tagall` and `untagall` are not available help topics in this build.

### Add a member: `add`

Provide a name, phone, email, and address.
Required prefixes may appear in any order, but each must appear exactly once.
Separate each prefix from the preceding value with whitespace; pasted tabs also work.
Use a separate `t/` for each optional tag.

| Field | Current rule |
| --- | --- |
| Name | Letters, digits, and spaces. Cannot be blank. |
| Phone | At least three digits. No spaces or punctuation. |
| Email | A local part and domain separated by `@`, without spaces. See the email rules below. |
| Address | Non-blank text. |
| Tag | 1 to 30 ASCII letters or digits, without spaces. Tags are case-sensitive. |

**Email rules:**

* The local part contains letters or digits, optionally separated by single `+`, `_`, `.`, or `-` characters.
* Domain labels contain letters or digits, with hyphens allowed internally.
* The final domain label needs at least two characters.

For example:

```text
add n/Bob Lee p/92345678 e/bob@example.com a/20 College Road t/committee t/year1
```

This adds Bob with two tags, saves the roster, and shows the complete member list.
Repeated identical tags appear once.

A member is a duplicate only when its trimmed name, phone, email, and address all exactly match an
existing member, including case and internal spaces. Tags do not affect identity. A same-name member
with different contact details is allowed.

### Show all members: `list`

`list` removes name-search and tag-filter restrictions and shows the full roster in its existing order.
It does not change or save records.

The result is `Showing 1 member.`, `Showing N members.`, or `No members in the address book.`
Extra arguments produce `Invalid command format. Usage: list` without changing the current view.

### Sort members by name: `sort`

`sort` orders the currently displayed members by full name from A to Z, ignoring letter case.
It preserves the current search restriction and does not change or save the stored roster order.
Sorting stays active as members are added, edited, deleted, or searched. Use `list` to restore
the complete roster in stored order, or restart the app to return to stored order.
Check the displayed member numbers again before using `edit` or `delete`.
Extra arguments such as `sort date` produce `Invalid command format. Usage: sort`.

### Update a member: `edit`

1. Check the member's current displayed number and contact details.
2. Supply that number and at least one field:

   ```text
   edit 1 p/98765432 e/alice.new@example.com
   ```

This changes the first displayed member's phone and email. Omitted fields stay unchanged.
After saving, TrackCall shows the updated details and restores the complete list.
An edit that would make all four contact fields match another member is rejected.

**Supplied tags replace all existing tags.**

* Keep both tags: `edit 1 t/committee t/year1`.
* Remove all tags: `edit 1 t/`.

Check the member number again after any command that changes the list.

### Find members: `find`

`find Alice Tan` searches the complete roster for names containing **Alice or Tan**.

* Matching ignores letter case and uses complete words.
  `Alice` matches `Alice Tan`, but `Ali` does not.
* Each new search replaces the previous search.
* Phone numbers and tags are not searched.
* Supply at least one keyword.

The result reports `Members found: N`.
No matches produce `Members found: 0` and an empty list. No records are deleted.
Use `list` to show everyone again. Searching does not save the data file.

### Filter displayed members by tag: `filter`

Use `filter t/TAG` to show only currently displayed members with that exact tag.
For example, `filter t/committee` shows members tagged `committee`. Matching is case-sensitive,
so `Committee` is a different tag. The result keeps roster order, renumbers the visible members,
and reports `N member(s) listed with tag "TAG".` Zero matches are successful and show an empty list.

Run `filter` again to narrow the results by another tag. Run `list` first to filter the whole roster.
For example, run `filter t/committee` and then `filter t/year1` as two separate commands.
Only members with **both** tags remain. A single `filter` command accepts only one `t/TAG`.
`find` starts a new name search over the full roster and replaces previous filters.
Subsequent index-based commands use the currently displayed numbers. A successful `edit` currently
restores the full roster. Filtering does not change or save member data.

For example, run `filter t/family`, check the displayed members, then use `edit 1 p/98765432`
to update the first displayed family member. A successful edit shows everyone again. To search
all members named Tan after filtering, run `find Tan`.

Supply exactly one tag of 1 to 30 letters or digits with no internal spaces. Spaces around the tag
are ignored. Missing or extra parameters and invalid tags are rejected without changing the view.

### Delete a member: `delete`

`delete INDEX` removes one member using a positive number from the **currently displayed list**.

1. Run `find Alice` and check the results.
2. Use `delete 1` to remove the first match.

Other records remain, and the remaining matches are renumbered.
The result shows the removed member's details.

**Deletion is immediate, with no confirmation or undo.** Back up records you may need later.

### Clear all members: `clear`

`clear` removes the entire roster, including members hidden by a search.
It saves the empty roster and resets the search after a successful save.

**Clearing is immediate, with no confirmation or undo.**

| Situation | Feedback after saving |
| --- | --- |
| One member removed | `Cleared 1 member. The address book is now empty.` |
| Multiple members removed | `Cleared N members. The address book is now empty.` |
| Roster already empty | `The address book is already empty. No changes were made.` |

The count includes hidden members.
Extra arguments produce `Invalid command format. Usage: clear. This command removes all members.`

If saving fails, TrackCall restores the roster and previous view. It reports:
`Unable to clear the address book because the changes could not be saved. No members were removed.`
See [Correcting errors](#correcting-errors) for recovery steps.

### Export members to CSV: `export`

`export [FILEPATH]` writes every member to a UTF-8 CSV file, including members hidden by a search.
The columns are `name`, `phone`, `email`, `address`, and `tags`, so the exported file can be imported directly.
Tags for each member are sorted alphabetically and joined with semicolons.

* Without `FILEPATH`, the file is written to `addressbook.csv` in the application's current directory.
* The path may be relative or absolute. The parent directory must already exist and be writable.
* An existing destination file is overwritten.
* Export leaves the roster, current search, and automatic JSON data file unchanged.
* An empty roster produces a file containing only the column headers.

Examples:

* `export` writes `addressbook.csv` in the current directory.
* `export backups/members.csv` writes `members.csv` in the existing `backups` directory.

### Import members from CSV: `import`

`import p/FILE_PATH` validates a local UTF-8 CSV file, appends unique members in file order, saves the
complete roster, and restores the full list in stored order. The import always checks the full roster,
including members hidden by `find` or `filter`.

Use double quotes around a path containing whitespace:

```text
import p/"data/member list.csv"
```

The first row must be exactly:

```text
name,phone,email,address,tags
```

Every later non-blank row must have exactly five fields. CSV quoting supports commas and doubled double
quotes. Embedded line breaks are not supported. Separate tags with semicolons; leave the tags field empty
for no tags. An optional UTF-8 byte-order mark is accepted.

```text
name,phone,email,address,tags
John Doe,98765432,johnd@example.com,"John Street, Block 123",year1;committee
```

The whole file is validated before any member is added. Exact contact-detail duplicates against existing
members or earlier rows are skipped; tags are not merged. The result reports imported and skipped counts.
A header-only or duplicate-only file succeeds with zero imports and still retries automatic saving.

If a row, header, CSV structure, or UTF-8 encoding is invalid, no records or view state change. The first
error identifies the row where applicable. The source CSV is never modified.

### Finish your session: `exit`

`exit` closes TrackCall and its help window. It accepts no arguments.
Successful data changes have already been saved.
Exiting does not retry a failed save. Resolve storage errors first to keep unsaved changes.

## Correcting errors

### Invalid input

An unknown command points you to `help`.
A known command with missing or invalid input shows the problem and its rules.
Rejected input leaves the roster and current list unchanged.

* `add n/Alice` is incomplete. Use `help add`, then enter all four required fields.
* `delete 0` is invalid. Use `list`, check the member, and enter its displayed number.

### A save fails after add, edit, delete, or import

The change remains visible in this session, but the previous saved file stays unchanged.
The command box clears the input to avoid repeating an already-applied change.

1. Fix the reported file or folder problem.
2. Check the current list.
3. Use `edit` to reapply an existing field value. This retries saving without changing anything else.
   If the roster is empty, use `clear` to retry saving it.

For import, the complete imported batch remains in memory and TrackCall reports
`Could not save data to file: DETAILS`. Exiting before a successful retry loses unsaved changes.

### A save fails after clear

TrackCall restores every member and the previous view. It reports `No members were removed.`
The clear input stays in the command box.

Fix the storage problem. Retry only if you still want to remove the entire roster.

## Your data

TrackCall automatically saves after successful `add`, `edit`, `delete`, `clear`, and `import` commands.
`help`, `list`, `sort`, `find`, `filter`, `export`, and `exit` do not save the roster.

### Locate or back up your roster

The default file is `data/addressbook.json`, relative to the folder where you start TrackCall.
The status bar shows its location. Start from the same folder to keep using the same roster.

1. Close TrackCall.
2. Copy the data file to create a backup.
3. To move it to another computer you own, place the copy at the corresponding data-file location.
4. Start TrackCall and check the roster.

### Editing the file manually

Close TrackCall and make a backup first. Keep the existing JSON structure:

* A `persons` array contains the members.
* Each member has `name`, `phone`, `email`, `address`, and `tags` fields.
* This build uses `tags`. The Developer Guide's planned `tagged` field is not the current file format.

### Missing or invalid files

| File state | Startup behaviour |
| --- | --- |
| Missing | Sample members are loaded. |
| Unreadable or invalid | An empty roster is loaded. The loading error is recorded in the log. |

Startup and read-only commands leave the member file unchanged.
If an expected roster is missing, close the app and repair or restore the file first.
A successful data change replaces the file.

## Current limitations

* Bulk tag changes are planned. Use `edit` for an individual member's tags today.
* An edit restores the full list instead of retaining an active name search or tag filter.
* An unrecognised prefix-like token inside an address can be stored as literal address text.
  For example, `a/Main Road T/committee` does not assign a tag.
  Use lowercase `t/` and check the displayed details after adding or editing.

The [Developer Guide](DeveloperGuide.md#appendix-requirements) records the intended MVP and later ideas.

## FAQ

### How do I move my roster to another computer?

Close TrackCall and copy `data/addressbook.json` to the corresponding location on your other computer.
See [Your data](#your-data) for working-folder and backup instructions.

### Why did a member disappear after a search?

A search changes the displayed list, not the saved roster. Run `list` to show everyone.
Check the current displayed number before editing or deleting a member.

### Can I update a whole group's tags?

Use `filter t/TAG` to narrow the displayed members. Bulk tag changes remain planned;
use `add` and `edit` to manage individual members' tags in this build.
See [Current limitations](#current-limitations).

### What should I do after a save error?

Follow [Correcting errors](#correcting-errors) before exiting.
A failed `clear` save restores the roster.
Unsaved changes from `add`, `edit`, `delete`, and `import` remain visible in memory.

## Command summary

| Task | Format | Example and result |
| --- | --- | --- |
| Open help | `help [COMMAND]` | `help add` opens the add instructions. |
| Add a member | `add n/NAME p/PHONE e/EMAIL a/ADDRESS [t/TAG]...` | The quick-start example adds Alice. |
| Show everyone | `list` | `list` restores the complete roster. |
| Sort by name | `sort` | Sorts the current view from A to Z, ignoring letter case. |
| Update a member | `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...` | `edit 1 p/98765432` changes member 1's phone. |
| Find by name | `find KEYWORD [MORE_KEYWORDS]` | `find Alice Tan` shows names containing Alice or Tan. |
| Filter by tag | `filter t/TAG` | `filter t/committee` narrows the displayed members. |
| Delete one member | `delete INDEX` | `delete 1` removes the first currently displayed member. |
| Clear the roster | `clear` | `clear` removes all members, including those hidden by a search. |
| Export members | `export [FILEPATH]` | `export members.csv` writes the full roster to CSV. |
| Import members | `import p/FILE_PATH` | `import p/members.csv` appends unique valid records. |
| Close the app | `exit` | `exit` ends the session. |

`INDEX` always refers to the currently displayed list. Check the member details before using `edit` or `delete`.
See [Commands](#commands) for parameter rules and expected results.

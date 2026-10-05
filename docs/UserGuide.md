---
layout: page
title: User Guide
---

TrackCall helps a club's membership secretary maintain member contact details using typed commands.

This guide covers the **v1.2 development build** and its eight available commands.
Tag filtering and bulk tag editing remain planned.

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

The overview groups all nine commands into four categories:
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
`filter`, `tagall`, and `untagall` are not available help topics in this build.

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
| Tag | Letters and digits, without spaces. Tags are case-sensitive. |

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

A member with **exactly the same name** as an existing member is rejected, even if other details differ.
The message is `A member with this name already exists. Use edit to update that member.`

### Show all members: `list`

`list` removes the name-search restriction and shows the full roster in its existing order.
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
Renaming a member to another member's exact name is rejected.

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

### A save fails after add, edit, or delete

The change remains visible in this session, but the previous saved file stays unchanged.
The command box clears the input to avoid repeating an already-applied change.

1. Fix the reported file or folder problem.
2. Check the current list.
3. Use `edit` to reapply an existing field value. This retries saving without changing anything else.
   If the roster is empty, use `clear` to retry saving it.

Exiting before a successful retry loses those unsaved changes.

### A save fails after clear

TrackCall restores every member and the previous view. It reports `No members were removed.`
The clear input stays in the command box.

Fix the storage problem. Retry only if you still want to remove the entire roster.

## Your data

TrackCall automatically saves after successful `add`, `edit`, `delete`, and `clear` commands.
`help`, `list`, `sort`, `find`, and `exit` do not save the roster.

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

* Tag filtering and bulk tag changes are planned. Use `edit` for an individual member's tags today.
* Names determine duplicates in this build. The planned MVP will compare all four contact fields.
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

### Can I filter by tag or update a whole group's tags?

Those commands are planned. Use `add` and `edit` to manage individual members' tags in this build.
See [Current limitations](#current-limitations).

### What should I do after a save error?

Follow [Correcting errors](#correcting-errors) before exiting.
A failed `clear` save restores the roster.
Unsaved changes from `add`, `edit`, and `delete` remain visible in memory.

## Command summary

| Task | Format | Example and result |
| --- | --- | --- |
| Open help | `help [COMMAND]` | `help add` opens the add instructions. |
| Add a member | `add n/NAME p/PHONE e/EMAIL a/ADDRESS [t/TAG]...` | The quick-start example adds Alice. |
| Show everyone | `list` | `list` restores the complete roster. |
| Sort by name | `sort` | Sorts the current view from A to Z, ignoring letter case. |
| Update a member | `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...` | `edit 1 p/98765432` changes member 1's phone. |
| Find by name | `find KEYWORD [MORE_KEYWORDS]` | `find Alice Tan` shows names containing Alice or Tan. |
| Delete one member | `delete INDEX` | `delete 1` removes the first currently displayed member. |
| Clear the roster | `clear` | `clear` removes all members, including those hidden by a search. |
| Close the app | `exit` | `exit` ends the session. |

`INDEX` always refers to the currently displayed list. Check the member details before using `edit` or `delete`.
See [Commands](#commands) for parameter rules and expected results.

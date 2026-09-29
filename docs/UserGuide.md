---
layout: page
title: User Guide
---

TrackCall helps a club's membership secretary maintain member contact details using typed commands.
This guide describes the **v1.2 development build**. Tag filtering and bulk tag editing are still planned;
the eight commands below are available now.

* Table of Contents
{:toc}

## Quick start

1. Install Java **25**. On macOS, use the compatible JDK+FX distribution in the
   [course installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).
2. For this development build, follow the [setup guide](SettingUp.md), then run `./gradlew run`
   from your project checkout (`gradlew.bat run` on Windows).
   Published packages will appear on the [team releases page](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases).
3. On first launch, sample members appear with a welcome hint. Type `help` and press **Enter**
   to explore the offline command guide.
4. Close help with **Escape**. Type `list` to see the roster, then try:

   ```text
   add n/Alice Tan p/91234567 e/alice@example.com a/12 Orchard Road t/committee
   ```

   After saving, TrackCall reports `Added member` with Name, Phone, Email, Address, and Tags on separate lines.
   Alice also appears in the member list. In feedback, `Tags: (none)` means no tags are assigned.

The light beige interface places the command box above the result area. Each member card shows
its current list number, name,
labeled contact details, and tags. Long details wrap. Scroll to read more, or drag the divider between
the result area and member list to give feedback more space. At startup, TrackCall restores its
saved window position within the available screens, including after a monitor has been disconnected.

## Commands

Command keywords are lowercase, with no leading slash: type `help`, not `/help`. Replace `UPPER_CASE` placeholders with your values.
Square brackets mark optional input; `[t/TAG]...` means zero or more tags.
Do not type these brackets or the ellipsis. Submit each command on one line.

| Task | Format | Example and result |
| --- | --- | --- |
| Open help | `help [COMMAND]` | `help add` opens the add instructions. |
| Add a member | `add n/NAME p/PHONE e/EMAIL a/ADDRESS [t/TAG]...` | The quick-start example adds Alice. |
| Show everyone | `list` | `list` restores the complete roster. |
| Update a member | `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...` | `edit 1 p/98765432` changes member 1's phone. |
| Find by name | `find KEYWORD [MORE_KEYWORDS]` | `find Alice Tan` shows names containing Alice or Tan. |
| Delete one member | `delete INDEX` | `delete 1` removes the first currently displayed member. |
| Clear the roster | `clear` | `clear` removes all members, including those hidden by a search. |
| Close the app | `exit` | `exit` ends the session. |

### Offline help: `help`

Type `help`, press **F1**, or choose **Help → Help** to see all eight commands in four compact
groups: Browse members, Manage members, Remove records, and Help & session. Each command has
a short description and a runnable example. The overview uses two columns in a wide window
and one column when narrowed. The guide works without internet access.

Select a command-name button, or type `help COMMAND`, such as `help edit`, to open its full
syntax, example, expected result, usage notes, and common errors.
Select **All commands** to return to the full guide, or close it and type `help`.
Use **Up/Down**, **Page Up/Page Down**, **Home/End** to scroll, and **Escape** to close it.

Help accepts at most one lowercase topic. For example, `help ADD`, `help unknown`, and
`help add edit` report errors. `filter`, `tagall`, and `untagall` are not help topics in this build.
Help does not change or save the roster.

### Add a member: `add`

Provide a name, phone, email, and address. Prefixes may appear in any order; each required prefix
must appear exactly once. Separate prefixes from the preceding value with whitespace; pasted tabs
also work. Use a separate `t/` for each optional tag.

| Field | Current rule |
| --- | --- |
| Name | Letters, digits, and spaces; cannot be blank. |
| Phone | At least three digits, with no spaces or punctuation. |
| Email | A local part and domain separated by `@`, without spaces. The local part allows letters/digits separated by single `+`, `_`, `.`, or `-` characters. Domain labels use letters/digits and internal hyphens; the final label needs at least two characters. |
| Address | Non-blank text. |
| Tag | Letters and digits, with no spaces. Tags are case-sensitive. |

For example:

```text
add n/Bob Lee p/92345678 e/bob@example.com a/20 College Road t/committee t/year1
```

This adds Bob with two tags, saves the roster, and shows the complete member list.
Repeated identical tags appear once. A member with **exactly the same name** as an existing member
is rejected, even if their other details differ. The message is
`A member with this name already exists. Use edit to update that member.`

### Show all members: `list`

`list` removes the name-search restriction and shows the full roster in its existing order.
The result is `Showing 1 member.`, `Showing N members.`, or `No members in the address book.`
`list` does not change or save records. Extra arguments are rejected with
`Invalid command format. Usage: list`, without changing the current view.

### Update a member: `edit`

First check the current list number and contact details. Then supply that number and at least one field:

```text
edit 1 p/98765432 e/alice.new@example.com
```

This changes the first displayed member's phone and email. Omitted fields stay unchanged.
After saving, TrackCall shows the updated details and restores the complete list.
Renaming a member to another member's exact name is rejected.

**Supplied tags replace all existing tags.** To keep both `committee` and `year1`, enter
`edit 1 t/committee t/year1`. To remove all tags, enter `edit 1 t/`.
Check the member number again after commands that change the list.

### Find members: `find`

`find Alice Tan` searches the complete roster for names containing **Alice or Tan**.
Matching ignores letter case and uses complete words: `Alice` matches `Alice Tan`, but `Ali` does not.
Each new search replaces the previous one. Phone numbers and tags are not searched.

The result reports `Members found: N`. No matches produce `Members found: 0` and an empty list,
without deleting anything. Use `list` to show everyone again.
Supply at least one keyword. Searching does not save the data file.

### Delete a member: `delete`

`delete INDEX` removes one member using a positive number from the **currently displayed list**.
For example, run `find Alice`, check the results, then use `delete 1` to remove the first match.
Other records remain, and the remaining matches are renumbered. The result shows the removed member's details.

Deletion is immediate, with no confirmation or undo. Back up records you may need later.

### Clear all members: `clear`

`clear` removes the entire roster, including members hidden by a search, and saves the empty roster.
After saving, the result is `Cleared 1 member. The address book is now empty.` or
`Cleared N members. The address book is now empty.`, counting hidden members too.
An empty roster reports `The address book is already empty. No changes were made.`
A successful clear also resets the search. It is immediate, with no confirmation or undo.
Extra arguments are rejected with `Invalid command format. Usage: clear. This command removes all members.`
If saving fails, TrackCall restores the roster and previous view and reports
`Unable to clear the address book because the changes could not be saved. No members were removed.`

### Finish your session: `exit`

`exit` closes TrackCall and its help window. It accepts no arguments.
Successful data changes have already been saved. Exiting does not retry a failed save;
resolve a storage error first if you need to keep unsaved changes.

## Correcting errors

An unknown command points you to `help`. A known command with missing or invalid input shows the
problem and its rules. For example, `add n/Alice` is incomplete: use `help add`, then enter all four
required fields. `delete 0` is invalid: use `list`, check the member, and enter its displayed number.
Rejected input leaves the roster and current list unchanged.

If saving an **add, edit, or deletion fails**, the change remains visible in this session while the
previous saved file stays unchanged. The command box clears the input to avoid repeating an
already-applied change. Fix the reported file/folder problem, check the current list, then reapply
an existing field value with `edit` to save without changing anything else. If the roster is empty,
use `clear` to retry saving it. Exiting before a successful retry loses those unsaved changes.

If saving **clear fails**, TrackCall restores every member and the previous view and reports
`No members were removed.` The clear input stays in the command box. Fix the storage problem,
then retry only if you still want to remove the entire roster.

## Your data

TrackCall automatically saves after successful `add`, `edit`, `delete`, and `clear` commands.
`help`, `list`, `find`, and `exit` do not save the roster.

The default file is `data/addressbook.json`, relative to the folder from which you start TrackCall.
The status bar shows its location. Start from the same folder to keep using the same roster.
To back up or move your data, close TrackCall and copy this file. On your own other computer,
place the copy at the corresponding data-file location before starting the app.

### Editing the file manually

Close TrackCall and make a backup first. Keep the existing JSON structure: a `persons` array,
with `name`, `phone`, `email`, `address`, and `tags` for each member. The current file uses `tags`,
not the planned `tagged` field described in the Developer Guide.

An unreadable or invalid member file starts an empty roster; the loading error is recorded in the log.
A missing file starts sample records instead. Neither startup nor read-only commands replace the
member file. If an expected roster is missing, close the app and repair or restore the file before
adding or clearing records, because a successful data change replaces the file.

## Current limitations

* Tag filtering and bulk tag changes are planned; use `edit` for an individual member's tags today.
* Names determine duplicates in this build. The planned MVP will compare all four contact fields.
* An unrecognised prefix-like token inside an address can be stored as literal address text.
  For example, `a/Main Road T/committee` does not assign a tag. Use lowercase `t/` and check the
  displayed member details after adding or editing.

The [Developer Guide](DeveloperGuide.md#appendix-requirements) records the intended MVP and later ideas.

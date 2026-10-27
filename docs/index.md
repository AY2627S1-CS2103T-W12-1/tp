---
layout: default.md
title: ""
pageNav: 2
---

# TrackCall

**Keep your club's member records organised, one command at a time.**

TrackCall helps membership secretaries maintain contact details, find the right members, and manage
overlapping groups using tags. A compact roster and a separate details panel keep the workspace easy to scan.

<p class="d-flex flex-wrap gap-2 my-3">
  <a class="btn btn-primary" href="https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/tag/v1.4">Download v1.4</a>
  <a class="btn btn-outline-primary" href="UserGuide.html#quick-start">Get started</a>
  <a class="btn btn-outline-secondary" href="UserGuide.html#commands">Explore features</a>
</p>

Requires **Java 25**. TrackCall works with local files and includes offline help for all nineteen commands.

[![CI Status](https://github.com/AY2627S1-CS2103T-W12-1/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W12-1/tp/branch/master/graph/badge.svg)](https://app.codecov.io/gh/AY2627S1-CS2103T-W12-1/tp)

This website describes the **v1.5 preparation build (unreleased)**. Its single-instance protection,
address clearing, backup preservation, import-save guidance, and diagnostic logging improvements are
not in the v1.4 download. Use the [v1.4 User Guide](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/v1.4/docs/UserGuide.md)
with that release.

## Interface

![TrackCall showing a compact roster and one selected member's details](images/Ui.png)

Type a command, read the result, then select a member to view and copy their contact details.
Names and tags stay in the roster; the details panel appears when you select a member.
See [Read the interface](UserGuide.html#read-the-interface) for keyboard shortcuts and a tour of the workspace.

## Available features {id="features-in-v14-alpha"}

TrackCall has **nineteen commands**. Start with a task below; the
[User Guide command summary](UserGuide.html#command-summary) contains the full syntax.

### Browse and find members

Use `list` to restore the whole roster, `show` to select displayed members, `find` or `findemail`
to search, `filter` to narrow by tag, and `sort` for A–Z order.
[Browse commands →](UserGuide.html#browse-members)

### Add and update members

Use `add` and `edit` to maintain contact details. Select one member to view and copy their details.
[Member commands →](UserGuide.html#manage-members)

### Organise member groups

Use `tag` and `untag` for one member, `tagall` and `untagall` for the displayed group,
and `renametag` to update a tag throughout the roster.
[Group commands →](UserGuide.html#manage-tags)

### Exchange rosters with CSV

Use `import` to validate and append unique records, or `export` to copy the entire roster,
including hidden members.
[CSV commands →](UserGuide.html#csv-files)

### Remove member records

Use `delete` to remove one member after confirmation. `clear` removes the entire roster immediately,
including hidden members, with no confirmation or undo.
[Removal commands →](UserGuide.html#remove-records)

### Help and session

Use `help` or `help COMMAND` for offline instructions, and `exit` to close TrackCall.
[Help and session commands →](UserGuide.html#help-and-session)

### Local data and recovery

Valid changes save locally. Read-only commands leave the roster file unchanged.
A backup and recovery dialog help recover from an unreadable roster.
The performance target is **500 members**, not a hard record limit.
[Saving and recovery →](UserGuide.html#your-data)

## Releases

See what changed and browse screenshots of each release:

* [v1.4 - TrackCall](releases/v1.4/RELEASE_NOTES.html)
* [v1.3 - TrackCall](releases/v1.3/RELEASE_NOTES.html)

## Find the right guide

| I want to… | Start here |
| --- | --- |
| Install TrackCall and look up commands | [User Guide](UserGuide.html) |
| Understand the design and implementation | [Developer Guide](DeveloperGuide.html) |
| Run the application from source | [Setup instructions](SettingUp.html) |
| Meet the people building TrackCall | [About Us](AboutUs.html) |

## Acknowledgements

Based on [AddressBook Level 3](https://se-education.org/addressbook-level3)
from the [SE-EDU initiative](https://se-education.org).

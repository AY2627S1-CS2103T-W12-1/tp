---
layout: page
title: TrackCall
---

TrackCall helps membership secretaries keep their organisation's member records up to date.
Use typed commands to manage contact details, find members, and update a group's tags at once.

The performance target is 500 members. This is not a hard record limit.

## Interface

![TrackCall showing a compact roster and one selected member's details](images/Ui.png)

In the current source build, the command box is followed by the result display and a compact roster beside
the selected member's details. Contact details stay hidden until a member is selected. This UI update is
newer than the v1.4 alpha release.

## Features in v1.4 alpha

* Add, edit, list, find, delete, and clear member records.
* Filter the displayed members by tag, and sort them by name.
* Add or remove one tag from a displayed member with `tag INDEX t/TAG` or `untag INDEX t/TAG`.
* Add or remove a tag for every displayed member with `tagall` and `untagall`.
* Rename a tag for every member with `renametag`.
* Import and export member records as CSV files.
* Offline help for all eighteen commands.
* Local saving after successful data changes, with recovery guidance for save errors.
  A failed clear restores the roster.

Read-only commands leave the roster file unchanged.

## Find the right guide

* [User Guide](UserGuide.html): install TrackCall and look up commands.
* [Developer Guide](DeveloperGuide.html): understand the implementation and planned requirements.
* [Setup instructions](SettingUp.html): run and develop the application.
* [About Us](AboutUs.html): meet the project team.

## Acknowledgements

Based on [AddressBook Level 3](https://se-education.org/addressbook-level3)
from the [SE-EDU initiative](https://se-education.org).

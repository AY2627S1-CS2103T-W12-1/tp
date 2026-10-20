# TrackCall

[![CI Status](https://github.com/AY2627S1-CS2103T-W12-1/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W12-1/tp/branch/master/graph/badge.svg)](https://app.codecov.io/gh/AY2627S1-CS2103T-W12-1/tp)

TrackCall helps membership secretaries keep their organisation's member records up to date.
It is a desktop app designed for around 500 members, although this is not a hard limit.
Type commands to find members and update their contact details quickly.

## Features in v1.4 alpha

* Add, edit, list, show selected members by index, find, delete, and clear member records.
* Open offline instructions for all available commands with `help`, or one command with `help COMMAND`.
* Read member cards and multiline feedback in a light beige interface, with wrapping and scrolling.
* Save data changes locally, with clear recovery guidance if saving fails and rollback for a failed clear.
* Keep the previous valid saved roster as an automatic backup, and recover damaged files at startup.
* Leave the roster file unchanged when running read-only commands.
* Narrow the displayed members by an exact tag using `filter t/TAG`, then use their displayed numbers to edit or delete.
* Add or remove a tag for every displayed member with `tagall t/TAG` or `untagall t/TAG`.
* Rename a tag for every member with `renametag t/CURRENT_TAG t/NEW_TAG`.
* Import and export CSV member records, and sort the displayed list by name.

## Interface

The current source build shows names and tags in the roster. Select a member to reveal their contact details
in a separate panel, where each field can be copied. Press **F2** to focus the roster and **Escape** to hide
the selected details and return to the command box. This UI update is newer than the v1.4 alpha release.

![TrackCall showing a compact roster and one selected member's details](docs/images/Ui.png)

## Documentation

* [Project website](https://ay2627s1-cs2103t-w12-1.github.io/tp/)
* [User Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/UserGuide.html)
* [Developer Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/DeveloperGuide.html)
* [About Us](https://ay2627s1-cs2103t-w12-1.github.io/tp/AboutUs.html)

The User Guide covers the current source build. The Developer Guide documents the current implementation,
its limitations, and requirements for later versions. Published documentation follows the team's
merged default branch.

## Local development

Use Java 25. On macOS, select the course-compatible JDK+FX distribution. Start the application with:

```shell
./gradlew run
```

Run the automated tests with:

```shell
./gradlew test
```

See the [setup instructions](docs/SettingUp.md) for the full development setup.

## Acknowledgements

This project is based on [AddressBook Level 3](https://se-education.org/addressbook-level3),
created by the [SE-EDU initiative](https://se-education.org).
The starter provides the application structure, contact-management code, tests, and documentation.

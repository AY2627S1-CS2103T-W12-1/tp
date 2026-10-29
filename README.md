# TrackCall

[![CI Status](https://github.com/AY2627S1-CS2103T-W12-1/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W12-1/tp/branch/master/graph/badge.svg)](https://app.codecov.io/gh/AY2627S1-CS2103T-W12-1/tp)

TrackCall helps membership secretaries keep their organisation's member records up to date.
It is a desktop app designed for around 500 members, although this is not a hard limit.
Type commands to find members and update their contact details quickly.

## Features in v1.5

* Add, edit, list, and find member records by name with `find`, by email with `findemail`, and show selected members by displayed number with `show`.
* Narrow the displayed members by tag with `filter t/TAG [t/MORE_TAGS]...`. Tags in one command mean *or*, separate commands mean *and*, and each condition appears as a box under the command field that you can remove.
* Sort the whole roster by name with `sort`. The order is saved and used by `export`.
* Add or remove one member's tag with `tag INDEX t/TAG` or `untag INDEX t/TAG`, retaining their other tags.
* Add or remove a tag for every displayed member with `tagall t/TAG` or `untagall t/TAG`, and rename a tag for them with `renametag t/CURRENT_TAG t/NEW_TAG`.
* Confirm before removing members: `delete INDEX` asks for `yes`, `clear` removes the displayed members, and `clear /all` removes everyone.
* Export the displayed members to CSV with `export`, and append members from a CSV file with `import p/FILE_PATH`.
* Open offline instructions for all commands with `help`, or one command with `help COMMAND`.
* Save data changes locally, with clear recovery guidance if saving fails and rollback for a failed clear.
* Keep the previous valid saved roster as an automatic backup, and recover damaged files at startup.
* Leave the roster file unchanged when running read-only commands.

## Interface

Names and tags appear in the roster. Select a member to reveal their contact details in a separate panel,
where each field can be copied. Press **F2** to focus the roster and **Escape** to hide the selected details
and return to the command box. Search, filter, and `show` conditions appear as removable boxes under the command field.

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

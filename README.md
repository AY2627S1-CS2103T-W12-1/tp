# TrackCall

[![CI Status](https://github.com/AY2627S1-CS2103T-W12-1/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W12-1/tp/branch/master/graph/badge.svg)](https://app.codecov.io/gh/AY2627S1-CS2103T-W12-1/tp)

TrackCall helps membership secretaries keep their organisation's member records up to date.
It is a desktop app with a planned performance target of 500 members, not a hard record limit.
Type commands to find members and update their contact details.

## Current development build

The working branch begins v1.2; v1.1 was the documentation practice iteration.

* Add, edit, list, find, delete, and clear member records.
* Open offline instructions for all eight available commands with `help`, or one command with `help COMMAND`.
* Read labeled member cards and multiline feedback in a light beige interface, with wrapping and scrolling.
* Save data changes locally, with clear recovery guidance if saving fails and rollback for a failed clear.
* Leave the roster file unchanged when running read-only commands.

Tag filtering and bulk tag changes are planned for the MVP and are not available in this build.

## Planned interface

![TrackCall mockup showing committee members after filtering by tag](docs/images/Ui.png)

This AI-assisted mockup shows the intended product, including planned tag filtering. It is not a
screenshot of the current development build.

## Documentation

* [Project website](https://ay2627s1-cs2103t-w12-1.github.io/tp/)
* [User Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/UserGuide.html)
* [Developer Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/DeveloperGuide.html)
* [About Us](https://ay2627s1-cs2103t-w12-1.github.io/tp/AboutUs.html)

The User Guide describes the working build. The Developer Guide separates the current implementation
from planned MVP requirements. Published documentation follows the team's merged default branch.

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

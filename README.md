# TrackCall

[![CI Status](https://github.com/AY2627S1-CS2103T-W12-1/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-W12-1/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W12-1/tp/branch/master/graph/badge.svg)](https://app.codecov.io/gh/AY2627S1-CS2103T-W12-1/tp)

TrackCall helps membership secretaries keep their organisation's member records up to date.
It is a desktop app with a planned performance target of 500 members, not a hard record limit.
Type commands to find members, update contact details, and organise groups without editing each record one at a time.

## Planned features

* Add and edit member records with `add` and `edit`, and view or remove records as needed.
* Find members by name and narrow the results by group tags.
* Add or remove a tag for everyone in the displayed list with one command.
* Look up command syntax with `help` or `help COMMAND`, and save changes to a local data file automatically.

## Planned interface

![TrackCall mockup showing committee members after filtering by tag](docs/images/Ui.png)

The intended interface mockup was prepared with AI assistance and includes planned tag filtering.

This is a mockup of the intended product. It keeps AB3's command box, result display,
member list, and data-file status bar. The TrackCall features above are planned;
the current application still contains the AB3 starter behaviour.

## Documentation

* [Project website](https://ay2627s1-cs2103t-w12-1.github.io/tp/)
* [User Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/UserGuide.html)
* [Developer Guide](https://ay2627s1-cs2103t-w12-1.github.io/tp/DeveloperGuide.html)
* [About Us](https://ay2627s1-cs2103t-w12-1.github.io/tp/AboutUs.html)

The Developer Guide describes the planned requirements. The User Guide still describes
the starter application and will be updated as TrackCall features are implemented.

## Local development

Use Java 25. Start the application with:

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

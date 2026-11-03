# v1.6 - TrackCall

TrackCall v1.6 is the final public release for managing a student club or society's membership roster.
It keeps the nineteen-command workflow and improves validation, CSV sharing, keyboard use, and recovery.

Download **trackcall.jar** from the [v1.6 release page](https://github.com/AY2627S1-CS2103T-W12-1/tp/releases/tag/v1.6).
Use **Java 25** and follow the [Quick start](../../UserGuide.html#quick-start).

## Changes since v1.5

- **More consistent member names:** periods, slashes, commas and parentheses are accepted after the initial
  letter or digit. Canonically equivalent Unicode spellings match during duplicate checks, name search,
  and sorting; entered spelling is preserved.
- **Bounded contact fields:** names, phone numbers, emails and addresses have documented length limits.
  Addresses reject bidirectional control characters. The same value rules apply to commands, CSV imports
  and saved JSON. Back up existing data before upgrading; an invalid saved field enters recovery rather
  than loading a partial roster.
- **Clearer command validation:** numeric invalid indices produce index errors; malformed index text
  produces command-usage guidance. Slash unit labels such as `Block A/12` and `Block B/12` are rejected
  consistently in add/edit; use `Block A-12` and `Block B-12`. Existing slash addresses remain loadable
  from saved JSON and CSV.
- **Safer pasted input and keyboard use:** unsupported pasted controls are rejected without silently
  changing the command. Single-member selection, contact-field copying and narrow-window layouts have
  regression coverage.
- **Safer CSV round trips:** exports mark their spreadsheet-safe encoding so imports remove only the
  exporter's protection prefix. Unmarked legacy CSV values remain literal. Header names ignore case,
  duplicate headers remain invalid, and malformed files are rejected as a whole.
- **Stronger persistence safeguards:** recovery preserves damaged files and useful backups; failed writes,
  file permissions, outside edits and concurrent launches have additional checks.
- **Expanded documentation and tests:** the User Guide includes consistent screenshots for command
  workflows. The Developer Guide, offline help and regression suites describe the integrated behavior.

## Install and upgrade

1. Back up your existing application folder, including its `data` folder.
2. Put `trackcall.jar` in a writable folder. Keep the JAR and data together when moving an installation.
3. Check `java -version` reports Java 25, then run `java -jar trackcall.jar` from a terminal.
4. Use `help` or `help COMMAND` for offline instructions. If recovery opens, follow its choices;
   do not discard the preserved original merely to bypass validation.

The GitHub release contains the executable, its SHA-256 checksum and a validation record identifying
its exact source revision. Verify the downloaded file against that checksum.

## Screenshot refresh — 3 November 2026

All 23 User Guide screenshots and the website interface image were recaptured from the published
v1.6 JAR at source `3b33b8c306b00959ccd13ba60cceb7eaeb5463b9` using Java 25.0.4 on macOS.
The executable is unchanged: SHA-256 `cfb659e640ac3a3cb3c14dc2eec08c2eb324631674e3cf1fed95c9e56a5a602c`.
The [capture manifest](../../images/user-guide/features/capture-manifest.json) records the original PNGs,
JPEG derivatives and scenarios. Deletion and clearing images show pending prompts that were cancelled;
the exit image shows `help exit`, rather than execution of `exit`.

## Guides and remaining limits

- [User Guide](../../UserGuide.html): all commands, field limits, screenshots and recovery instructions.
- [Developer Guide](../../DeveloperGuide.html): implementation, UML and manual test scenarios.
- [Testing guide](../../Testing.html): automated suites and release checks.
- Confirmed deletion and clearing have no in-app undo. Retain an independent backup.
- CSV paths stay inside the application folder or its subfolders; import limits remain 10 MiB and
  10,000 nonblank member rows. Embedded line breaks are unsupported.
- After a failed save, follow the displayed retry or recovery instructions before exiting. `exit`
  does not retry saving. A failed clear restores the previous roster and view.
- Sorting is explicit: run `sort` again after adding, importing or changing names when needed.
- The validation record distinguishes automated checks from operating systems actually exercised
  with the packaged application; passing tests do not establish that every possible defect is absent.

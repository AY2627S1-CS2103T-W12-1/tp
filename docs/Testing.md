---
layout: default.md
title: Testing guide
pageNav: 6
---

# Testing guide

## Running tests

You can run tests in two ways.

Use Java 25. `build.gradle` explicitly enables Java assertions for tests and Java execution tasks,
including `run` and `runShadow`. Generated launch scripts also use `-ea` through
`applicationDefaultJvmArgs`. For a direct packaged run, use `java -ea -jar trackcall.jar`.
In an IntelliJ Application run configuration, add `-ea` to the VM options.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose `Run 'ABC'`
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)

<box type="info">:link: **Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</box>

## Types of tests

The automated suite includes these types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `seedu.address.commons.util.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `seedu.address.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `seedu.address.logic.LogicManagerTest`

1. *UI tests* exercise JavaFX controls and their interaction with logic and storage.<br>
   For example: `seedu.address.ui.MainWindowTest` checks command feedback, selection, and persisted changes.

## Randomised CSV and recovery checks

The normal test suite includes generated CSV round trips and storage failure scenarios.
Run just these checks with:

```shell
./gradlew test --tests '*CsvRoundTripRandomizedTest' --tests '*RosterRecoveryRandomizedTest'
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.
Gradle may reuse a previous result; add `--rerun-tasks` to force a fresh run.

* CSV checks use 40 fixed seeds, including empty and 500-member rosters, and repeat each round trip three times.
  Generated fields include quotes, commas, tabs, Unicode, empty addresses, leading-zero phones, and mixed-case tags.
  Another 20 seeds check that an encoding failure after valid rows preserves the previous export and removes temporary files.
* Recovery checks use 30 fixed seeds to truncate saved rosters at different positions, reject unsafe saves,
  preserve damaged bytes, restore the previous backup, and verify saving resumes after recovery.
  A separate check blocks backup replacement with a non-empty directory, repairs it, and retries the save.

Failures report the seed and, for round trips, the iteration. All files live in JUnit temporary directories;
these checks do not use your real roster. They run automatically in the existing CI test jobs on Windows, macOS, and Linux.
They simulate specific write and corruption failures; they do not simulate a full disk or a power outage.
## Release verification

Run `./gradlew check shadowJar` with Java 25 to check code style, run the automated suite, and build
`build/libs/trackcall.jar`. Run `npm ci` and `npm run build` from `docs/` to verify the production website.
Tests and the documentation build passing are necessary checks; also exercise the packaged JAR and rendered guides.

Use the [manual test instructions](DeveloperGuide.html#appendix-instructions-for-manual-testing) for the
complete membership workflows, invalid inputs, persistence, and recovery scenarios. Start with a temporary,
writable folder and synthetic members so destructive commands cannot remove real data. Check the User Guide's
examples against the same JAR intended for release, including filtered member numbers and CSV round trips.

Record the source revision, Java version, operating system, JAR checksum, and completed scenarios with the release.
Repeat the relevant regression checks after a fix and the full release checks after integration.
The website's navigation, images, tables, and section links should also be inspected at desktop and phone widths.

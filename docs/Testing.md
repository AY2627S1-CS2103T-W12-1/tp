---
layout: default.md
title: Testing guide
pageNav: 6
---

# Testing guide

Use this guide to choose tests, run them locally, and check a release.
For step-by-step application scenarios, see the [manual tests](DeveloperGuide.html#appendix-instructions-for-manual-testing).

## Running tests

- Use **Java 25**. Run the commands below from the project root.
- On Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.
- Gradle enables Java assertions for tests, `run`, and `runShadow`.
  For a direct JAR run, use `java -ea -jar trackcall.jar`.
- In an IntelliJ **Application** run configuration, add `-ea` to **VM options**.

**Using Gradle**

- All tests: `./gradlew test`.
- One suite: `./gradlew test --tests '*LogicManagerTest'`.
- Force a fresh run: add `--rerun-tasks`.
- Start with a clean build: `./gradlew clean test`.
- Read results in `build/reports/tests/test/index.html`; failures include the test name and assertion.

**Using IntelliJ**

- All tests: right-click `src/test/java` and choose **Run 'All Tests'**.
- A subset: right-click a package, class, or test method and choose **Run**.

See the [SE-EDU Gradle tutorial](https://se-education.org/guides/tutorials/gradle.html) for task basics.

## Types of tests

These categories describe what a test exercises; a suite can cover more than one category.

- **Unit tests:** check one class or operation in isolation.
  Example: `StringUtilTest` checks string helpers.
- **Integration tests:** check interactions between components.
  Example: `StorageManagerTest` checks the storage manager and file storage implementations together.
- **Hybrid tests:** combine focused checks with component interactions.
  Example: `LogicManagerTest` exercises parsing, command execution, model changes, and saving.
- **UI tests:** check JavaFX controls and their interaction with the application.
  Example: `MainWindowTest` checks command feedback, selection, and persisted changes.
- **Workflow acceptance tests:** check documented user outcomes across several commands.
  Example: `UserCsvWorkflowTest` checks CSV sharing, imports, and persistence against the User Guide.
- **Regression tests:** preserve previously verified behavior after a change. Any of the suites above
  can serve as regression coverage.

## Randomised CSV and recovery checks

### What each suite covers

- **`UserCsvWorkflowTest`:** uses commands, real CSV fixtures, and fresh storage sessions.
  Checks full-roster exports while filtering, duplicate imports, field boundaries, rejected imports,
  and save-failure recovery guidance.
- **`CommandSequenceOracleTest`:** compares the application with an independent reference roster.
  Runs 3,200 steps across eight fixed seeds, covering filters, sorting, selections, tags, CSV,
  deletion confirmation, reloads, and saved-file preservation after read-only commands.
  A separate scenario checks view rollback after a failed clear.
- **`CsvRoundTripRandomizedTest`:** checks that exported data can be imported without losing fields or order.
  Covers quoted and Unicode text, shuffled columns, empty and large rosters, malformed UTF-8,
  invalid final rows, and cleanup after a failed export.
- **`RosterRecoveryRandomizedTest`:** checks damaged-file preservation, backup restoration, and retrying saves.
  Covers truncated JSON, repeated corruption/recovery cycles, blocked backup replacement,
  invalid backups, and deeply nested JSON.

### Running selected suites

```shell
./gradlew test --tests '*UserCsvWorkflowTest' --tests '*CommandSequenceOracleTest'
./gradlew test --tests '*CsvRoundTripRandomizedTest' --tests '*RosterRecoveryRandomizedTest'
```

### Interpreting results

- Expected workflow outcomes come from the [User Guide](UserGuide.html#import-members-from-csv-import)
  and [user stories](DeveloperGuide.html#user-stories). Investigate a mismatch before changing the expectation.
- Generated tests use fixed seeds. Record the reported seed, step, or round to reproduce a failure.
- CSV and recovery fixtures use JUnit temporary directories and synthetic members.
- Large datasets are robustness inputs, not supported-capacity or performance guarantees.
- The command oracle calls filter-removal methods directly; it does not test native clicks or keyboard focus.
- Simulated write failures do not reproduce every full-disk or power-loss condition.
- These suites run in CI on Windows, macOS, and Linux. Passing them does not establish that every recovery
  failure is covered; known gaps remain in the issue tracker.

## Release verification

1. **Check and package:** run `./gradlew check shadowJar` with Java 25.
   This runs tests and style checks, then creates `build/libs/trackcall.jar`.
2. **Check the website:** from `docs/`, run `npm ci`, `npm test`, and `npm run build`.
   Inspect navigation, images, tables, and section links at desktop and phone widths.
3. **Test the actual JAR:** copy it to an empty writable folder and use synthetic members.
   Follow the [manual tests](DeveloperGuide.html#appendix-instructions-for-manual-testing) on Windows,
   macOS, and Linux. Check startup, commands, invalid input, CSV, saving, recovery, and restart.
4. **Record evidence:** save the source revision, Java version, OS, JAR checksum, and completed scenarios.
   State which platforms or scenarios were not tested.
5. **Recheck changes:** run relevant regression tests after each fix and repeat release checks after integration.

Automated test results and packaged-JAR acceptance results are separate evidence. Record both for the release.

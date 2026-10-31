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

- Configure IDEA to run tests using Gradle so the build's JVM options and platform-specific JavaFX
  classpath apply. This is needed for the project's Apple Silicon development configuration.
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
  Checks displayed-member exports and hidden counts while filtering, duplicate imports, field boundaries, rejected imports,
  and save-failure recovery guidance.
- **`CommandSequenceOracleTest`:** compares the application with an independent reference roster.
  Runs 8,000 steps across sixteen fixed seeds, covering filters, sorting, selections, tags, CSV,
  deletion confirmation, reloads, and saved-file preservation after read-only commands.
  A separate scenario checks condition and stored-order rollback after a failed clear.
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

The generated operations also reject malformed commands, out-of-range indices, duplicate additions,
imports with an invalid row after a valid row, and exports to a blocked destination. Exported CSV fields
and displayed row order are checked against the displayed reference records using a CSV parser independently of the
application's importer. Full list/export/clear/import/reload cycles check that a successful round trip restores
the complete roster. Twelve additional seeded workflows guarantee that filtering, sorting, show selections,
editing, tagging, deletion cancellation and confirmation, rejected operations, and export/restore are exercised
together with nonempty targets. These expectations describe this checkout's command contracts; adapt them
to another application's documented behavior before treating a difference as a bug.

To replay or expand the generated run in PowerShell:

```powershell
$env:ORACLE_FIRST_SEED = '0'
$env:ORACLE_SEEDS = '32'
$env:ORACLE_STEPS = '1000'
.\gradlew.bat test --tests '*CommandSequenceOracleTest' --rerun-tasks
Remove-Item Env:ORACLE_FIRST_SEED, Env:ORACLE_SEEDS, Env:ORACLE_STEPS
```

Generated-operation failures include the seed, step, recent commands, and a path to `failure-trace.txt` containing the full
command history. Failing sessions retain their JUnit temporary directory. The trace is for replay and
investigation; automatic reduction to a minimal command sequence is not implemented.

To measure whether the guaranteed workflows detect selected known faults, run:

```shell
python tools/calibrate_command_oracle.py --gradle /absolute/path/to/gradle
```

On Windows pass the path to `gradle.bat`; `--gradle-user-home` can select an existing dependency cache.
The calibration runs offline and therefore requires a populated Gradle cache. It copies tracked working-tree
files (including local edits) into an isolated `.integration/oracle-calibration-*` directory, verifies a passing
baseline, and separately introduces four faults: exporting hidden members, editing by the stored index,
not saving edits, and reversing the sort order. It retains per-run logs and `summary.json`, restores each
mutated source file, and does not modify production files in the original checkout. Compilation or setup
failures do not count as detections. This is a small calibration set, not a general bug-detection percentage
or evidence of performance on an unseen repository.

## Designing useful test cases

Use heuristics when they reveal a meaningful risk. A test does not need to use every heuristic.
Add a short comment when the partition, boundary, or interaction is not obvious from its name.

| Technique | TrackCall example | What to assert |
| --- | --- | --- |
| Equivalence partitioning | Valid input, rejected input, and valid input that makes no change | Correct result and unchanged state for rejected commands |
| Boundary values | Index 0, first/last displayed index, one past the end; CSV limit and limit + 1 | Correct acceptance or rejection at the boundary |
| State transitions | Failed save → repair → read-only command → explicit save retry | Unsaved changes remain in memory; only the explicit retry updates the roster file |
| Feature interactions | Filter + Show selection + delete/clear + save failure | Only the intended members change; hidden members and selections remain correct |
| Regression tests | A minimal reproduction of a confirmed issue | The original failure is detected, without depending on unrelated setup |

### Checking recovery across commands

`SaveFailureTransitionTest` complements the existing CSV and recovery suites:

- **Unsaved edit:** after storage is repaired, browsing, help, and export must not silently save the roster.
  Export must reflect the displayed in-memory data. An explicit unchanged edit retries persistence.
- **Failed confirmed deletion:** the deletion stays applied in memory, but its confirmation is consumed.
  A second `yes` must not replay it or remove another member. An explicit retry saves the surviving members.
- **Failed clear after an unsaved edit:** rollback must restore the pre-clear session, including the earlier
  unsaved edit and its filters. A later successful clear removes only the intended displayed member.

These tests use real temporary files, exact-byte backup checks, synthetic contacts, and a fresh storage reader.
A nonempty directory at the backup path injects a portable write failure; it does not simulate every disk failure.

```shell
./gradlew test --tests '*SaveFailureTransitionTest'
```

### Keeping assertions trustworthy

- Derive expected results from documented behavior. Write expected member identities, order, and CSV content
  independently of the production helper being tested.
- Check both visible results and side effects: hidden members, active conditions, saved bytes, backup bytes,
  and data loaded by a fresh storage session.
- Snapshot mutable state before the command. A reference that changes with the app is not an independent expectation.
- Give each test its own fixture. Do not rely on the execution order of test methods.
- Use fixed seeds and retain failure traces for generated tests. Reduce discovered failures to focused regressions.
- Check test sensitivity with isolated, deliberately introduced faults. Compilation errors do not count as detections.
- Treat coverage as a way to locate gaps, not proof of correct behavior. Native UI and packaged-release checks
  remain necessary even when automated checks pass.

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

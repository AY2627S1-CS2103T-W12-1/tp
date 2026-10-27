---
layout: default.md
title: DevOps guide
pageNav: 6
---

# DevOps guide

Use this guide to build TrackCall, understand CI checks, and prepare a verified release.

## Build automation

- Gradle manages Java dependencies, builds, tests, and packaging.
- Use **Java 25** and run commands from the project root.
- On Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.
- For task basics, see the [SE-EDU Gradle tutorial](https://se-education.org/guides/tutorials/gradle.html).

**Common commands**

- `./gradlew run`: build and launch the application.
- `./gradlew test`: run automated tests.
- `./gradlew check`: run tests and Checkstyle checks.
- `./gradlew checkstyleMain checkstyleTest`: check production and test code style.
- `./gradlew coverage`: run tests and generate JaCoCo HTML/XML coverage reports.
- `./gradlew shadowJar`: create the self-contained `build/libs/trackcall.jar`.
- `./gradlew runShadow`: build and launch the packaged application.
- `./gradlew clean`: remove previous build outputs. Combine with another task when a clean build is needed.

### Local runs and release verification

- Gradle selects ARM JavaFX libraries for development runs and tests on Apple Silicon.
- The portable JAR bundles both macOS architectures separately; `Main` selects the matching native libraries.
  Windows and Linux use their bundled JavaFX libraries.
- Verify the packaged JAR separately from development runs. Use an empty writable folder and synthetic members.
- Follow the [release checks](Testing.html#release-verification) and
  [manual scenarios](DeveloperGuide.html#appendix-instructions-for-manual-testing) on each target OS.

## Continuous integration (CI)

- **Java CI** (`.github/workflows/gradle.yml`) runs on pull requests, pushes to `master`, and tags.
- It checks the Gradle wrapper, then runs `check coverage` with Java 25 on Linux, macOS, and Windows.
- Linux runs the JavaFX tests through Xvfb and also runs repository-wide checks.
- Failed jobs upload test reports and available crash diagnostics. Inspect these before retrying a failure.
- **Documentation** (`.github/workflows/docs.yml`) checks documentation changes separately.
  See the [publishing guide](Documentation.html#publishing-the-site) for its deployment rules.

### Documentation dependencies

Run these commands from `docs/`:

```shell
npm ci
npm test
npm run build
```

- `npm ci` installs locked dependencies and applies the required patches.
- `npm test` checks dependency compatibility; `npm run build` verifies the generated site and local links.
- `npm audit` reports known vulnerabilities in the installed dependency tree.
- MarkBind 7.2.1 uses dependency overrides in `package.json` and API adaptations in `docs/patches`.
  Keep them together when upgrading dependencies.
- Keep install scripts enabled: `patch-package` applies the adaptations during installation.
  The unpatched APIs are incompatible with the selected dependency versions.

### Code coverage

- JaCoCo reports show which code ran during tests; they do not prove correctness.
- Run `./gradlew coverage`, then open `build/reports/jacoco/coverage/html/index.html`.
- Team-repository Linux CI uploads `build/reports/jacoco/coverage/coverage.xml` to Codecov using OIDC.
  Upload errors fail that CI job. Fork repositories skip this upload.
- Review uncovered behavior when adding or fixing features; avoid tests that only raise a percentage.
- See the [SE-EDU Codecov guide](https://se-education.org/guides/tutorials/codecov.html) for background setup.

### Repository-wide checks

- These scripts check tracked text files for final newlines, line endings, and trailing whitespace.
- They inspect the Git index. Stage intended changes before checking them.
- On macOS or Linux, run from the project root:

  ```shell
  (cd .github && ./run-checks.sh)
  ```

- Read warnings and errors in the console. CI runs the same scripts on Linux.
- To add a check, create an executable `.github/check-*` script.
  Print diagnostics as `SEVERITY:FILENAME:LINE: MESSAGE`, using `ERROR` or `WARN`.
  Exit with a nonzero status when errors occur.

## Making a release

1. **Select the revision:** integrate the intended changes and update the version in `MainApp.java`.
   Check that guides and release notes describe that revision.
2. **Build and verify:** run `./gradlew clean check coverage shadowJar` with Java 25.
   Complete the [release checks](Testing.html#release-verification), including the packaged JAR on each target OS.
3. **Record evidence:** capture the source commit, JAR SHA-256 checksum, test results, and any untested scenarios.
   Prepare release notes and matching documentation assets.
4. **Tag the verified commit:** create a lightweight version tag, such as `v1.5`, at that exact revision.
5. **Publish:** create the GitHub release for that tag and attach the verified `build/libs/trackcall.jar`,
   release notes, and supporting assets. Check the download against the recorded checksum.

Keep the source revision, executable, checksum, and validation record together so others can reproduce the release.

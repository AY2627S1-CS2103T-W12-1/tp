---
layout: default.md
title: Setting up and getting started
pageNav: 6
---

# Setting up and getting started

## Setting up the project on your computer

<box type="warning" header="Caution">

Follow the steps below precisely. The setup may fail if you skip or change a step.

</box>

First, **fork** this repo, and **clone** the fork into your computer.

If you plan to use IntelliJ IDEA (highly recommended):
1. **Configure the JDK**: Follow the guide [_[se-edu/guides] IDEA: Configuring the JDK_](https://se-education.org/guides/tutorials/intellijJdk.html) to use **JDK 25** for both the project SDK and Gradle JVM.
1. **Import the project as a Gradle project**: Follow the guide [_[se-edu/guides] IDEA: Importing a Gradle project_](https://se-education.org/guides/tutorials/intellijImportGradleProject.html) to import the project into IDEA.<br>
  **Note:** Importing a Gradle project is slightly different from importing a normal Java project.
1. **Verify the setup**:
   1. Run the Gradle `run` task from IDEA's Gradle tool window, or run `./gradlew run` from the project root.
      On Windows PowerShell, use `.\gradlew.bat run`. Try a few commands in the application.
   1. [Run the tests](Testing.html) through Gradle to check the setup.

Gradle applies the project's assertion settings and selects the matching JavaFX native libraries for
local runs and tests on Apple Silicon. A plain IDEA Application run of `seedu.address.Main` does not
automatically use the Gradle `run` task's adjusted classpath.

## Before writing code

1. **Configure the coding style**

   If using IDEA, follow the guide [_[se-edu/guides] IDEA: Configuring the code style_](https://se-education.org/guides/tutorials/intellijCodeStyle.html) to set up IDEA's coding style to match ours.

   <box type="tip" header="Tip">

   Optionally, follow the guide [_[se-edu/guides] Using Checkstyle_](https://se-education.org/guides/tutorials/checkstyle.html) to learn how to use Checkstyle in IDEA, for example to report problems _as_ you write code.

   </box>

1. **Set up CI**

   This project includes GitHub Actions configuration files in `.github/workflows`. GitHub runs Java CI for pull requests, pushes to `master`, and tags. Documentation changes also run the website build. No workflow setup is required.

   Java CI uses **plain Zulu JDK 25** on Linux, Windows and macOS, with JavaFX supplied by the dependencies
   in `build.gradle`. The course-compatible JDK+FX setup can load its bundled JavaFX instead. To reproduce
   a CI-only failure, point `JAVA_HOME` (terminal) or the **Gradle JVM** setting (IDEA) to a plain Zulu JDK 25
   installation, then run the same Gradle task as CI. This explains the CI environment; the course setup
   guidance for local development still applies.

1. **Learn the design**

   When you are ready to start coding, we recommend that you get some sense of the overall design by reading about [TrackCall’s architecture](DeveloperGuide.html#architecture).

1. **Do the tutorials**
   These tutorials will help you get acquainted with the codebase.

   * [Tracing code](https://se-education.org/guides/tutorials/ab3TracingCode.html)
   * [Adding a new command](https://se-education.org/guides/tutorials/ab3AddRemark.html)
   * [Removing fields](https://se-education.org/guides/tutorials/ab3RemovingFields.html)

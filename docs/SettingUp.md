---
layout: page
title: Setting up and getting started
---

Follow these steps in order to prepare a local development checkout.

* Table of Contents
{:toc}

## Setting up the project on your computer

### Get the source

Fork the [team repository](https://github.com/AY2627S1-CS2103T-W12-1/tp), then clone your fork.

### Configure IntelliJ IDEA

1. Configure **JDK 25** using the
   [IntelliJ JDK guide](https://se-education.org/guides/tutorials/intellijJdk.html).
2. Import the checkout as a **Gradle project** using the
   [Gradle import guide](https://se-education.org/guides/tutorials/intellijImportGradleProject.html).

Importing a Gradle project differs from importing a regular Java project.
Follow the linked steps to keep the dependencies configured correctly.

### Verify the setup

1. Run `./gradlew run`. On Windows, use `gradlew.bat run`.
   This selects the matching JavaFX libraries for local development, including Apple Silicon.
2. Try a few commands in the app.
3. [Run the tests](Testing.md) and confirm that they pass.

## Before writing code

### Configure the coding style

Follow the [IntelliJ code-style guide](https://se-education.org/guides/tutorials/intellijCodeStyle.html)
to match the project's coding style.
Optionally, configure [Checkstyle in IntelliJ](https://se-education.org/guides/tutorials/checkstyle.html)
to see style problems while editing.

### Check continuous integration

GitHub Actions runs CI for every push and pull request.
Workflow files are in `.github/workflows`. No additional setup is required.

### Learn the design

Read [TrackCall's architecture](DeveloperGuide.md#architecture) before changing the code.
These starter-code tutorials explain common development tasks:

* [Trace code](https://se-education.org/guides/tutorials/ab3TracingCode.html).
* [Add a command](https://se-education.org/guides/tutorials/ab3AddRemark.html).
* [Remove fields](https://se-education.org/guides/tutorials/ab3RemovingFields.html).

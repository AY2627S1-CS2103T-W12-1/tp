---
layout: page
title: Testing guide
---

Use Java **25** for test tasks. See [Setup](SettingUp.md) if the checkout is not ready.

* Table of Contents
{:toc}

## Running tests

### With IntelliJ IDEA

* **All tests:** right-click `src/test/java` and select **Run 'All Tests'**.
* **Selected tests:** right-click a package, class, or test and select its **Run** action.

### With Gradle

Run the command for your operating system from the repository root:

| Platform | Command |
| --- | --- |
| macOS or Linux | `./gradlew clean test` |
| Windows | `gradlew.bat clean test` |

See the [Gradle tutorial](https://se-education.org/guides/tutorials/gradle.html) for more tasks.
For interactive checks, use the [manual testing instructions](DeveloperGuide.md#appendix-instructions-for-manual-testing).

## Types of tests

| Type | Purpose | Example |
| --- | --- | --- |
| Unit | Check individual methods and classes. | `seedu.address.commons.util.StringUtilTest` |
| Integration | Check how multiple code units work together. | `seedu.address.storage.StorageManagerTest` |
| Hybrid | Check individual units and their interactions. | `seedu.address.logic.LogicManagerTest` |

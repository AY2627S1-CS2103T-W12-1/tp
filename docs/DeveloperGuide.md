---
layout: default.md
title: Developer Guide
pageNav: 6
---

<style>
#content-wrapper .dg-diagram {
  display: flex;
  justify-content: center;
  width: 100%;
  max-width: 900px;
  margin: 24px auto;
}
#content-wrapper .dg-diagram img {
  display: block;
  box-sizing: border-box;
  width: 100%;
  height: auto;
  object-fit: contain;
  max-width: 100%;
  max-height: 680px;
  padding: 12px;
  border: 1px solid #000;
  background: #fffdf8;
}
#content-wrapper .dg-diagram:focus-visible {
  outline: 2px solid #005fcc;
  outline-offset: 4px;
}
</style>

# Developer Guide

<span id="value-proposition"></span>

TrackCall is a local membership roster for club secretaries. Its main workflow is to find a group,
update its records or tags, and save valid changes without a separate save command.

This guide describes the **unreleased v1.5 candidate**. Use the [User Guide](UserGuide.html)
for command syntax and the sections below for implementation decisions and failure handling.

## Acknowledgements

* TrackCall builds on [AddressBook Level 3](https://se-education.org/addressbook-level3/) by the
  [SE-EDU initiative](https://se-education.org/). The starter supplies the architecture, source code, tests,
  and original diagrams.
* The application uses [JavaFX](https://openjfx.io/) for its interface and
  [Jackson](https://github.com/FasterXML/jackson) for JSON storage.
* [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) handles CSV import/export parsing and quoting.
  Its use was [approved by the teaching team](https://github.com/NUS-CS2103-AY2627-S1/forum/issues/627).
* The documentation website uses [MarkBind](https://markbind.org/) and adapts the
  [CS2103T starter layout](https://github.com/NUS-CS2103-AY2627-S1/tp/tree/switch-docs-to-markbind)
  with TrackCall's name, repository links, and guide sections.
* The feature grouping and release-note presentation were inspired by the
  [W12-3 team's User Guide](https://ay2627s1-cs2103t-w12-3.github.io/tp/UserGuide.html) and
  [CoordiMate releases](https://github.com/AY2627S1-CS2103T-W12-3/tp/releases).
  The adaptation concerns documentation layout and presentation; TrackCall retains its own feature descriptions,
  examples, screenshots, and application behaviour.
* The previous Jekyll website adapted the
  [W13-1 team's layout](https://github.com/AY2627S1-CS2103T-W13-1/tp/blob/master/docs/stylesheets/main.css),
  including its sidebar widths, reading width, and responsive breakpoints, under its
  [MIT licence](https://github.com/AY2627S1-CS2103T-W13-1/tp/blob/master/LICENSE).
* [JUnit](https://junit.org/junit5/), [Gradle](https://gradle.org/),
  [Checkstyle](https://checkstyle.org/), and [JaCoCo](https://www.jacoco.org/jacoco/) support testing and builds.


## Feature Map {id="feature-map"}

| Task | Commands | Implementation |
| --- | --- | --- |
| Restore or reset the roster | `list`, `clear` | [View reset and save rollback](#list-and-clear-commands) |
| Order the displayed members | `sort` | [Name sorting](#name-sorting) |
| Find members | `find`, `findemail` | [Model views](#model-component), [email search](#find-members-by-email) |
| Select and narrow a group | `show`, `filter` | [Selections](#selecting-members-and-sorting), [tag filters](#filter-members-by-tag) |
| Maintain records | `add`, `edit`, `delete` | [Parsing](#command-parsing), [deletion confirmation](#deletion-confirmation) |
| Manage tags | `tag`, `untag`, `tagall`, `untagall`, `renametag` | [Individual](#individual-tag-editing) and [bulk tag editing](#bulk-tag-editing) |
| Exchange records | `import`, `export` | [CSV import](#csv-import), [CSV export](#csv-export) |
| Get help or finish | `help`, `exit` | [Offline help](#offline-command-help), [UI](#ui-component) |
| Persist and protect data | Automatic | [Saving and recovery](#automatic-data-saving-and-load-recovery) |



## Setting Up {id="setting-up-getting-started"}

Refer to the guide [_Setting up and getting started_](SettingUp.html).


## Design

<box type="tip">

:bulb: **Tip:** Diagram sources are in `docs/diagrams`.
See the [PlantUML tutorial](https://se-education.org/guides/tutorials/plantUml.html) to create or edit a diagram.
</box>

### Architecture

<a class="dg-diagram" href="images/ArchitectureDiagram.svg" aria-label="Open diagram at full size: TrackCall architecture and component dependencies"><img src="images/ArchitectureDiagram.svg" alt="TrackCall architecture and component dependencies"></a>

The architecture diagram shows the main components and their dependencies.

#### Main components

`Main` manages startup and shutdown through two classes:
[`Main`](https://github.com/AY2627S1-CS2103T-W12-1/tp/tree/master/src/main/java/seedu/address/Main.java)
and [`MainApp`](https://github.com/AY2627S1-CS2103T-W12-1/tp/tree/master/src/main/java/seedu/address/MainApp.java).

* At startup, it acquires the application-instance lock, initialises preferences and components,
  then loads or recovers the roster before opening the main window.
* At shutdown, it stops the components and runs their cleanup methods.

| Component | Responsibility |
| --- | --- |
| [UI](#ui-component) | Display the interface and receive user input. |
| [Logic](#logic-component) | Parse and execute commands. |
| [Model](#model-component) | Hold the app's data in memory. |
| [Storage](#storage-component) | Read and write data on disk. |
| [Commons](#common-classes) | Provide classes shared by multiple components. |

#### Component interactions

The sequence diagram shows how the components handle `delete 1` and the `y` that confirms it.

<a class="dg-diagram" href="images/ArchitectureSequenceDiagram.svg" aria-label="Open diagram at full size: Component interactions when executing delete 1 and confirming it with y"><img src="images/ArchitectureSequenceDiagram.svg" alt="Component interactions when executing delete 1 and confirming it with y"></a>

Each of the four main components defines an API through an interface with the component's name.
A corresponding manager class implements that interface.
For example, `Logic.java` defines the API and `LogicManager.java` implements it.

Components communicate through these interfaces.
This reduces their dependence on each other's implementation details, as shown below.

<a class="dg-diagram" href="images/ComponentManagers.svg" aria-label="Open diagram at full size: Component interfaces and their manager implementations"><img src="images/ComponentManagers.svg" alt="Component interfaces and their manager implementations"></a>

### UI component

**API:** [`Ui.java`](https://github.com/AY2627S1-CS2103T-W12-1/tp/tree/master/src/main/java/seedu/address/ui/Ui.java)

<a class="dg-diagram" href="images/UiClassDiagram.svg" aria-label="Open diagram at full size: Structure of the UI component"><img src="images/UiClassDiagram.svg" alt="Structure of the UI component"></a>

**Structure**

- `MainWindow` contains the command box, result display, member list, details panel and status bar.
- These visible parts extend `UiPart`. JavaFX layouts are defined in `src/main/resources/view`.
- `HelpWindow` builds command cards from `CommandHelp` in Logic.
- `WindowPlacement` restores a usable window position and size.
- `MainApp` opens `RosterRecoveryDialog` before exposing an invalid roster for editing.

**Interactions**

- Call the `Logic` interface to execute commands.
- Observe model data so displayed records update when data changes.
- Use `Person` and other model values to render the selected member's details.

### Logic component

**API:** [`Logic.java`](https://github.com/AY2627S1-CS2103T-W12-1/tp/tree/master/src/main/java/seedu/address/logic/Logic.java)

<a class="dg-diagram" href="images/LogicClassDiagram.svg" aria-label="Open diagram at full size: Main classes in the Logic component"><img src="images/LogicClassDiagram.svg" alt="Main classes in the Logic component"></a>

The following sequence diagram illustrates parsing and executing `delete 1` to request confirmation.

<a class="dg-diagram" href="images/DeleteSequenceDiagram.svg" aria-label="Open diagram at full size: Parsing delete 1 and requesting confirmation"><img src="images/DeleteSequenceDiagram.svg" alt="Parsing delete 1 and requesting confirmation"></a>

The sequence shows a valid index. Temporary parser objects are left to Java garbage collection,
not explicitly destroyed. Result construction and other small internal operations are abbreviated.

#### Command execution

1. `AddressBookParser` selects the matching parser, such as `DeleteCommandParser`.
2. That parser produces a `Command` subclass, such as `DeleteCommand`.
3. `LogicManager` executes the command. The command interacts with `Model` as needed.
4. `Logic` returns a `CommandResult` containing the result.

The diagram groups model interactions into one step for clarity.
An implementation may need several calls to complete that step.

#### Deletion confirmation

<a class="dg-diagram" href="images/ConfirmDeleteSequenceDiagram.svg" aria-label="Open diagram at full size: Confirmed deletion and automatic saving"><img src="images/ConfirmDeleteSequenceDiagram.svg" alt="Confirmed deletion and automatic saving"></a>

**Flow**

1. **Request:** `DeleteCommand` validates the displayed index. It returns a prompt and a
   `ConfirmDeleteCommand` holding the selected `Person`. No record changes or save occur yet.
2. **Wait:** `LogicManager` retains the pending command. `MainWindow` highlights the command box
   and displays the confirmation hint.
3. **Answer:** the next input bypasses `AddressBookParser`.
   - Exactly `y`, after trimming surrounding whitespace, executes the pending deletion and saves.
   - Any other input cancels. That input is consumed, not executed as another command.
4. **Finish:** clear the pending command in either case. A later `y` cannot repeat the deletion.

**Safety and failure handling**

- Store the `Person`, not its index. Removing a filter while waiting cannot redirect the deletion.
- If a filter is removed, show the confirmation prompt again beneath the filter feedback.
- If saving fails, keep the deletion in memory and show retry guidance. See [save failures](#failed-saves).
- Without a pending deletion, `y` is an unknown command.

**Design decisions**

- A typed confirmation preserves the keyboard workflow; a modal pop-up would interrupt it.
- Consuming cancellation avoids treating replies such as `n` as new commands.

#### Parsing classes

<a class="dg-diagram" href="images/ParserClasses.svg" aria-label="Open diagram at full size: Command parsers and shared parsing helpers"><img src="images/ParserClasses.svg" alt="Command parsers and shared parsing helpers"></a>

- **Dispatch:** `AddressBookParser` selects a command-specific parser and returns its `Command`.
- **Parser contract:** command-specific parsers implement `Parser`. `XYZCommandParser` represents
  concrete classes such as `AddCommandParser`.
- **Tokenization:** `ArgumentTokenizer` splits prefixed fields into an `ArgumentMultimap`.
  `CliSyntax` defines the known `Prefix` values.
- **Value conversion:** `ParserUtil` validates and converts indexes and member fields.
- **Prefix checks:** add/edit parsers use `PrefixValidator` to reject unsupported field prefixes.
- **Tag checks:** tag, untag, bulk-tag and rename parsers share `BulkTagArgumentParser`.

### Model component

**API:** [`Model.java`](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/master/src/main/java/seedu/address/model/Model.java)

The model separates stored records from display state. The diagrams show one responsibility at a time.

**1. Stored records and preferences**

<a class="dg-diagram" href="images/ModelClassDiagram.svg" aria-label="Open diagram at full size: Model ownership and stored roster structure"><img src="images/ModelClassDiagram.svg" alt="Model ownership and stored roster structure"></a>

- `ModelManager` holds an `AddressBook` and `UserPrefs`.
- `AddressBook` owns a `UniquePersonList`. It exposes a live, unmodifiable list of members.
- `UserPrefs` holds window settings. Other components read it through `ReadOnlyUserPrefs`.
- Model has no dependency on UI, Logic or Storage.

**2. Member values**

<a class="dg-diagram" href="images/PersonValuesDiagram.svg" aria-label="Open diagram at full size: Person references immutable contact values and tags"><img src="images/PersonValuesDiagram.svg" alt="Person references immutable contact values and tags"></a>

- A `Person` references a name, phone, email, address and a set of tags.
- An absent address is an empty `Address` value, not a missing object.
- Contact values and tags are immutable and can be reused when replacing a record.
  Associations therefore fit these references better than exclusive composition.
- There is no separate shared tag registry.

**3. Display pipeline**

<a class="dg-diagram" href="images/ModelViewDiagram.svg" aria-label="Open diagram at full size: Stored records feed the filtered view and then the sorted display"><img src="images/ModelViewDiagram.svg" alt="Stored records feed the filtered view and then the sorted display"></a>

- `FilteredList` applies the current conditions to stored records.
- `SortedList` optionally orders the filtered results by name.
- `getFilteredPersonList()` returns the sorted view. Indexed commands use this displayed order.
- Changing filters or sorting does not reorder the stored roster.

**4. Filter state**

<a class="dg-diagram" href="images/FilterStateDiagram.svg" aria-label="Open diagram at full size: Active conditions and additional predicate relationships"><img src="images/FilterStateDiagram.svg" alt="Active conditions and additional predicate relationships"></a>

- `activeFilters` contains removable name, email and tag conditions in application order.
- `additionalFilters` contains `Predicate<Person>` objects, including `PersonSelectionPredicate`.
- `refreshFilteredPersons()` combines all conditions with AND.
- Record replacement updates selection predicates so selections follow the edited member.
- Removing **Show: active** clears selection predicates only. `list` clears all conditions and sorting.

Open any diagram to inspect it at full size.

### Storage component

**API:** [`Storage.java`](https://github.com/AY2627S1-CS2103T-W12-1/tp/blob/master/src/main/java/seedu/address/storage/Storage.java)

<a class="dg-diagram" href="images/StorageClassDiagram.svg" aria-label="Open diagram at full size: JSON and CSV storage classes"><img src="images/StorageClassDiagram.svg" alt="JSON and CSV storage classes"></a>

**Responsibilities**

- `StorageManager` delegates roster JSON to `JsonAddressBookStorage` and preferences to `JsonUserPrefsStorage`.
- `ImportCommand` and `ExportCommand` call the CSV helpers directly.
- `CsvAddressBookCsv` provides their shared header and format. Commons CSV handles quoting and escaping.
- CSV export is a user-requested copy. It does not replace automatic JSON persistence.

**Write safeguards**

- `LogicManager` gives export the active roster and preferences paths for destination checks.
- Protected destinations include those files, backups, recovery copies, the running JAR and session lock.
- `FileUtil#writeAtomically` writes a sibling temporary file, closes it, then atomically replaces the target.
- On failure, preserve the previous destination and attempt temporary-file cleanup.
- An export's parent directory must already exist.

See [saving and recovery](#automatic-data-saving-and-load-recovery) for JSON failures and
[CSV export](#csv-export) for its separate write workflow.

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.


## Implementation

Feature sections describe their **purpose**, **flow**, **rules** and **tests**.
Sequence diagrams omit parsing unless shown. A `ref` frame names a detail diagram in the same section.
Detail diagrams may introduce helper lifelines omitted from the overview. Open any diagram to view it at full size.

### 1. Offline command help {id="offline-command-help"}

**Purpose:** show command instructions without a browser or network connection.

<span id="catalogue-and-command-flow"></span>

**Flow**

1. `HelpCommandParser` accepts no topic or one known lowercase topic.
2. `HelpCommand` returns the topic in a `CommandResult`.
3. `MainWindow` calls `HelpWindow#show(topic)`.
4. `HelpWindow` reads the local `CommandHelp` entry and displays the card.

<a class="dg-diagram" href="images/HelpSequenceDiagram.svg" aria-label="Open diagram at full size: Offline help routing"><img src="images/HelpSequenceDiagram.svg" alt="Offline help routing"></a>

<span id="help-navigation"></span>

**Rules and design**

- Keep the catalogue in Logic and window handling in UI. Commands do not construct windows.
- Include all nineteen commands, each with syntax, examples, expected results and common errors.
- `help COMMAND` and overview buttons open the same topic. F1 and the Help menu open the overview.
- Use two card columns in wide windows and one in narrow windows.
- Arrow/Page keys scroll; Home/End move to the ends; Escape closes help.
- Help is read-only. Invalid or multiple topics do not open a new topic.

<span id="test-coverage"></span>

**Tests:** `CommandHelpTest` checks catalogue completeness and executable examples.
`HelpWindowTest` covers layout and keyboard navigation; `MainWindowTest` covers routing.

### 2. List and clear commands {id="list-and-clear-commands"}

**Purpose:** restore the complete view with `list`, or remove the complete roster with `clear`.

<span id="list-behaviour"></span>

**List flow**

1. Clear predicates with `updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS)`.
2. Disable sorting with `setNameSorting(false)`.
3. Report the full count. Do not change records or save the JSON file.

<span id="clear-and-save-recovery"></span>

**Clear flow**

1. `LogicManager` snapshots the current roster.
2. `ClearCommand` counts all records, including hidden ones, and installs an empty roster.
3. Retain predicates and sorting until the save outcome is known.
4. Save the empty roster. Return success only after saving completes.

<a class="dg-diagram" href="images/ClearSequenceDiagram.svg" aria-label="Open diagram at full size: Clear save success and rollback"><img src="images/ClearSequenceDiagram.svg" alt="Clear save success and rollback"></a>

`SHOW_ALL` in the diagram abbreviates `PREDICATE_SHOW_ALL_PERSONS`.

| Outcome | Records and view | Command input |
| --- | --- | --- |
| Save succeeds | Empty roster; conditions cleared; sorting setting retained. | Clear the input. |
| Save fails | Restore the snapshot under the original predicates and sorting. | Retain `clear` for retry. |

**Rules and design**

- Clear has no confirmation or undo. It does not change preferences.
- Even an empty clear attempts to save, allowing a retry after an unsaved deletion.
- Keep rollback in Logic because that layer knows the persistence outcome.
- Atomic roster replacement is separate from backup rotation; see [startup and validation](#startup-and-validation).

<span id="exact-feedback"></span>

**Feedback:** use the command constants for empty, singular and plural results.
Failed clear states that no members were removed. `SaveFailureException#isChangeApplied()` controls input retention.

<span id="test-coverage-1"></span>

**Tests:** `ListClearIntegrationTest`, command/parser tests and `FileUtilTest` cover hidden records,
no-op saves, rollback, retry, stored order and temporary-file cleanup.

### 3. Name sorting {id="name-sorting"}

**Purpose:** sort displayed members without rewriting stored roster order.

**Flow**

1. `SortCommand` enables the comparator on `ModelManager`'s `SortedList`.
2. For comparison, trim names and collapse repeated ASCII spaces.
3. Compare with `compareToIgnoreCase`; leave stored names unchanged.

| Operation | Sorting | Conditions |
| --- | --- | --- |
| `sort` | Enable A–Z. | Retain. |
| `list`, valid `import` | Disable. | Clear. |
| `findemail` | Retain. | Replace with email query. |
| `find`, `filter`, `show` | Retain. | Narrow the view. |
| Record or tag edit | Retain. | Re-evaluate against updated records. |

**Rules and design**

- Indexed commands read the sorted display, not the stored list.
- Export still uses stored order. No descending or multi-field sorting is provided.
- A view comparator avoids rewriting records for presentation changes.
- Equal-name order after record replacement is tracked in [#222](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/222).

**Tests:** `ModelManagerTest` covers ordering and resets; `AddressBookParserTest` checks command routing.
`LogicManagerTest` checks read-only execution.

### 4. Email search {id="find-members-by-email"}

**Purpose:** search the full roster using an address or email fragments.

**Flow**

1. Parse a nonblank query into `EmailContainsKeywordsPredicate`.
2. Replace prior name, email, tag and `show` conditions with one email `ActiveFilter`.
3. Re-evaluate the live view and return its count. Retain the current sort comparator.

<a class="dg-diagram" href="images/EmailSearchSequenceDiagram.svg" aria-label="Open diagram at full size: Email search and view replacement"><img src="images/EmailSearchSequenceDiagram.svg" alt="Email search and view replacement"></a>

**Rules and design**

- Lowercase keywords and email with `Locale.ROOT`; match literal substrings with OR.
- Do not search names or phone numbers. Regex syntax has no special meaning.
- Later `find` and `filter` commands narrow the results. Removing the email box retains later conditions.
- An email edit can remove a record from the view without deleting it.
- Blank input preserves the view. Searching never saves data.

**Tests:** `EmailContainsKeywordsPredicateTest`, `FindEmailCommandTest`,
`FindEmailCommandParserTest` and `LogicManagerTest` cover matching, resets, edits and persistence isolation.

### 5. Member details and feedback {id="readable-member-details-and-feedback"}

**Purpose:** keep the roster scannable while revealing contact details only when selected.

<span id="member-rows-and-results"></span>

**Display responsibilities**

- `PersonCard`: show index, name and sorted tags.
- `MemberDetailsPanel`: show and copy the selected member's phone, email and address.
- `Messages#format(Person)`: produce labelled contact lines and sorted tags for command results.
- `MainWindow`: hide contact lines from successful add/edit/delete feedback.
- `UntagCommand`: return name and remaining tags directly, without contact fields.
- `ResultDisplay`: wrap and scroll feedback. Split panes let users resize the working areas.

**Interaction rules**

- Clear selection before executing any command, including commands that fail.
- F2 focuses the roster. Escape clears selection and hides details.
- Report data-changing success only after saving. Explain unsaved changes separately.
- Use `LightTheme.css` for shared colours and `HelpWindow.css` for help cards.

<span id="window-placement"></span>

**Window placement**

1. `WindowPlacement` checks saved bounds against available screens.
2. Keep a usable position; otherwise choose the screen with greatest overlap, or the primary screen.
3. Clamp off-screen or oversized bounds to the chosen screen. Replace invalid dimensions.

**Tests:** `PersonCardTest`, `MainWindowTest` and `WindowPlacementTest` cover rendering, selection
privacy and restored bounds. Manual tests check long values and real display scaling.

### 6. Saving and recovery {id="automatic-data-saving-and-load-recovery"}

**Purpose:** persist valid record changes and recover safely when storage is unavailable or invalid.

<span id="save-policy"></span>

**Save flow**

1. Parse and execute the command.
2. Return immediately for read-only commands, deletion prompts or cancellations.
3. Save the complete roster for valid data-changing commands, including no-ops.
4. Return success only after saving. Parsing or execution errors do not save.

<a class="dg-diagram" href="images/AutomaticSavingData.svg" aria-label="Open diagram at full size: Automatic saving policy"><img src="images/AutomaticSavingData.svg" alt="Automatic saving policy"></a>

<span id="file-writes"></span>

**Atomic writes**

- `StorageManager` delegates roster writes to `JsonAddressBookStorage`.
- The default path is `data/addressbook.json`, relative to the JAR folder in packaged builds.
- `FileUtil#writeToFile` creates missing parents and writes UTF-8 to a sibling temporary file.
- Close the writer, then replace atomically. If atomic replacement is unavailable, report failure.
- Preserve the previous target on failure and attempt temporary-file cleanup.

<span id="failed-saves"></span>

**Save failures**

| Command | In-memory state | Retry behavior |
| --- | --- | --- |
| `clear` | Restore prior records and view. | Keep the input; fix storage and retry. |
| Other data-changing commands | Keep the applied change. | Clear the input to prevent accidental repetition. |

- `SaveFailureException` records whether the change remains applied.
- The previous roster file is unchanged. Exiting can lose unsaved in-memory changes.
- After restoring access, inspect the list and use `edit` with an existing value to retry saving.
- If no records remain, use `clear`. Exit never retries a roster save.
- Preferences are saved separately during startup and shutdown.

<span id="startup-and-validation"></span>

**Startup and recovery**

1. Acquire `AppInstanceLock` before reading preferences or the roster.
2. Load valid records in stored order. A missing file loads sample members.
3. For invalid data, preserve the exact bytes in a unique `.corrupt-....json` copy.
4. Offer a validated backup, an explicit empty roster, or closing without replacement.
5. Verify the original still matches its preserved copy before recovery replacement.

**Safeguards and limits**

- Hold the instance lock through shutdown. A second instance is rejected; do not delete the lock file.
- Reject null roots, null members, null tags and invalid or duplicate member values.
- Preservation failure prevents an editable roster from opening.
- Normal saves validate the existing roster and rotate `.bak` only when member data differs.
- An unchanged save still retries the write but retains the previous distinct backup.
- Backup rotation and roster replacement are separate operations. A failure after rotation can replace
  the older backup; this is tracked in [#242](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/242).
- Invalid preferences fall back to defaults. Logger initialization failure falls back to the console.

**Tests:** `JsonAddressBookStorageTest` and `LogicManagerTest` cover persistence and failure policies.
See [manual recovery tests](#persistence-and-invalid-files) for packaged acceptance.

### 7. Command parsing {id="command-parsing"}

**Purpose:** reject malformed input before changing data or view state.

1. `AddressBookParser` reads a case-sensitive keyword and selects its parser.
2. Command-specific parsers check argument structure, indexes and field values.
3. Add/edit use tokenized prefixes and `PrefixValidator`; tag commands share `BulkTagArgumentParser`.
4. A successful parse returns a `Command`; failures return usage or field guidance.

**Rules**

- Add/edit accept fields in any order and reject repeated non-tag prefixes.
- Reject unsupported standalone single-letter prefixes in add/edit, including wrong-case prefixes.
- Preserve the `c/o` address abbreviation, URLs, numeric slashes and slashes embedded within words.
- `list`, `sort`, `clear` and `exit` take no arguments. Each other command enforces its own grammar.

**Tests:** parser-specific suites and `AddressBookParserTest` cover valid input, boundaries and rejection.

### 8. CSV import {id="csv-import"}

**Purpose:** validate an entire file before appending unique members.

**Flow**

1. `ImportCommandParser` accepts one `p/FILE_PATH`; quote paths containing whitespace.
2. Resolve the path and validate the complete CSV before changing the model.
3. Skip duplicates against the stored roster and earlier accepted rows.
4. Append unique records in source order; clear conditions and sorting.
5. Save through `LogicManager`, even if the file is header-only or all rows are duplicates.

<span id="import-sequence"></span>

**Overview — `sd ImportRoster`**

<a class="dg-diagram" href="images/CsvImportSequenceDiagram.svg" aria-label="Open diagram at full size: Import overview with reference frames"><img src="images/CsvImportSequenceDiagram.svg" alt="Import overview with reference frames"></a>

**Detail — `sd ValidateAndAppendCsv`**

<a class="dg-diagram" href="images/CsvImportApplyDiagram.svg" aria-label="Open diagram at full size: Validate the entire file before adding unique records"><img src="images/CsvImportApplyDiagram.svg" alt="Validate the entire file before adding unique records"></a>

**Detail — `sd SaveImportedRoster`**

<a class="dg-diagram" href="images/CsvImportSaveDiagram.svg" aria-label="Open diagram at full size: Save imported records and report unsaved changes"><img src="images/CsvImportSaveDiagram.svg" alt="Save imported records and report unsaved changes"></a>

**Validation and failure rules**

- Require a case-insensitive `.csv` extension. Preserve path case.
- Read strict UTF-8, allowing an optional byte-order mark.
- Require the five headers in any order; validate CSV quoting and every member field.
- Trim surrounding field/tag whitespace. Reject embedded line breaks.
- Skip blank records but include them when reporting physical row numbers.
- Read or validation errors leave records, the view and saved data unchanged. Never modify the source CSV.
- Save failures keep the imported batch in memory and report retry guidance.

**Design:** whole-file validation prevents partial imports. `Person#isSamePerson` provides the same
identity rule as manual entry. Commons CSV handles quoting instead of manual string splitting.

**Tests:** importer/parser suites and `LogicManagerTest` cover malformed files, duplicates, view resets and save failures.

### 9. CSV export {id="csv-export"}

**Purpose:** write all stored members to a separate CSV without changing the displayed view.

**Flow**

1. `LogicManager` supplies protected roster and preferences paths to `ExportCommand`.
2. Resolve and validate the destination before writing.
3. Export the full roster, including hidden members, in stored order.
4. Return the exported count and path. Do not save the automatic JSON roster.

**Overview — `sd ExportRoster`**

<a class="dg-diagram" href="images/CsvExportSequenceDiagram.svg" aria-label="Open diagram at full size: Export overview with reference frames"><img src="images/CsvExportSequenceDiagram.svg" alt="Export overview with reference frames"></a>

**Detail — `sd CheckExportDestination`**

<a class="dg-diagram" href="images/CsvExportDestinationDiagram.svg" aria-label="Open diagram at full size: Resolve the path and reject protected application destinations"><img src="images/CsvExportDestinationDiagram.svg" alt="Resolve the path and reject protected application destinations"></a>

**Detail — `sd WriteCsvAtomically`**

<a class="dg-diagram" href="images/CsvExportWriteDiagram.svg" aria-label="Open diagram at full size: Write a complete CSV to a temporary file before replacement"><img src="images/CsvExportWriteDiagram.svg" alt="Write a complete CSV to a temporary file before replacement"></a>

**Rules and design**

- Write UTF-8 with a header row. Sort each member's tags and join them with semicolons.
- Use Commons CSV for quoting and escaping. An empty roster produces a header-only file.
- Require an existing parent folder; apply packaged file-location restrictions.
- Close the CSV writer before atomic replacement. Attempt temporary-file cleanup on failure.
- Preserve an existing export if writing fails. Keep roster data, filters and sorting unchanged.
- Reserved-path alias handling is tracked separately in [#219](https://github.com/AY2627S1-CS2103T-W12-1/tp/pull/219).

**Tests:** `ExportCommandTest`, `ExportCommandParserTest` and `LogicManagerTest` cover destinations,
protected files, full-roster export and read-only behavior.

### 10. Selecting displayed members {id="selecting-members-and-sorting"}

**Purpose:** narrow the current view using displayed indexes.

1. Validate every requested index before changing the view.
2. Resolve indexes against the sorted display and create a `PersonSelectionPredicate`.
3. Add the selection condition to the existing conditions.

**Rules**

- Repeated indexes collapse to one member. Argument order does not reorder the display.
- Each `show` can only narrow the view. Record replacement updates selected references.
- Removing **Show: active** clears all selection predicates but retains other conditions.
- `list`, successful `add`/`clear`, `findemail` and valid `import` clear selections.
- Show is read-only and does not save.

**Tests:** `ShowCommandParserTest`, `ShowCommandTest` and `LogicManagerTest` cover indexes,
selection narrowing, replacements and read-only execution.

### 11. Tag filtering {id="filter-members-by-tag"}

**Purpose:** narrow the view to members with one exact tag.

<span id="parsing"></span>

**Input:** `FilterCommandParser` accepts exactly one `t/TAG`, trims surrounding whitespace and
checks the shared 1–30 ASCII-alphanumeric constraint before constructing `Tag`.

<span id="execution"></span>

**Flow**

1. `FilterCommand` calls `ModelManager#filterFilteredPersonListByTag`.
2. Add a new tag condition unless the same tag is already active.
3. Combine conditions with AND, refresh the observable view and report the result count.

<span id="design-considerations"></span>

**Rules and design**

- Retain earlier name/email/tag conditions and sorting. A zero-match result is successful.
- Keep predicates, not frozen matches, so edits re-evaluate membership automatically.
- Show conditions in application order. Removing one retains the others.
- Renumber visible records; later indexed commands use that new view. Filtering never saves.

<span id="test-coverage-2"></span>

**Tests:** `FilterCommandParserTest`, `FilterCommandTest`, `LogicManagerTest` and `EditCommandTest`
cover validation, intersections, hidden records and live edits.

### 12. Individual tag editing {id="individual-tag-editing"}

**Purpose:** add or remove one tag without replacing the member's other tags.

1. Resolve the member from the displayed index.
2. Copy the tag set and add or remove the requested tag.
3. Construct a new `Person` with unchanged contact fields and call `Model#setPerson`.
4. Save the complete roster and report the result.

**Rules**

- Retain searches, filters, sorting and `show` selections. Removing a required tag can hide the record.
- An existing addition or absent removal is a valid no-op and still saves.
- Invalid indexes or arguments fail before saving. Applied-but-unsaved changes remain in memory.

<span id="individual-tag-test-coverage"></span>

**Tests:** tag/untag command and parser suites cover boundaries, no-ops and preserved fields.
`LogicManagerTest` covers persistence and active conditions.

### 13. Bulk tag editing {id="bulk-tag-editing"}

**Purpose:** update one tag for every displayed member.

<span id="parsing-1"></span>

**Input:** `TagAllCommandParser` and `UntagAllCommandParser` share `BulkTagArgumentParser`.
Require exactly one valid `t/TAG`; reject preambles, missing/empty values and repeated tags.

<span id="execution-1"></span>

**Flow**

1. Snapshot the displayed list. This prevents updates from changing the targets mid-loop.
2. Reject an empty target set before saving.
3. Replace each target that needs a change, preserving contact fields and unrelated tags.
4. Count changed and skipped members against the original snapshot, then save the full roster.

<span id="design-considerations-1"></span>

**Rules and design**

- Never visit hidden members. Removing an active filter tag can hide targets after their update.
- Retain conditions and sorting. Skip redundant assignments rather than rejecting the batch.
- A valid all-skipped batch still saves. A save failure leaves the complete batch in memory.

<span id="test-coverage-3"></span>

**Tests:** bulk command/parser suites and `LogicManagerTest` cover empty views, all-skipped batches,
hidden records and removal of the tag that defines the current view.

<span id="renaming-a-tag"></span>

**Renaming across the roster**

1. Parse exactly two valid tags; reject identical names.
2. Snapshot every stored member with the old tag, including hidden records.
3. Reject an unused old tag. Otherwise replace it with the new tag on every target.
4. Merge an already-present new tag into one assignment; report changed and merged counts.
5. Retain conditions/sorting and save the roster.

A roster-wide rename avoids splitting one group between old and new names.
`RenameTagCommandTest`, `RenameTagCommandParserTest` and `LogicManagerTest` cover this behavior.

### 14. Limitations {id="limitations"}

- JSON uses a `persons` array and member `tags` arrays. Unknown fields are ignored and repeated fields
  retain the last value. Numeric JSON values in text fields become text. Comments and trailing commas fail.
- Confirmed deletion and clear have no undo. Clear has no confirmation.
- The backup retains the previous distinct saved roster, not a full history. Later changed saves replace it.
- After an accidental clear, stop changing data and follow the [manual recovery steps](UserGuide.html#automatic-backup-and-recovery).

## Development workflow {id="documentation-logging-testing-dev-ops"}

* [Documentation guide](Documentation.html)
* [Testing guide](Testing.html)
* [Logging guide](Logging.html)
* [DevOps guide](DevOps.html)


## Appendix: Requirements {id="appendix-requirements"}

These requirements distinguish implemented features from [future user stories](#considered-stories-and-extensions-to-the-selected-mvp).
Command syntax for current features is in the [User Guide](UserGuide.html).
Acceptance targets are listed separately under [non-functional requirements](#non-functional-requirements).

### Product scope

#### Target user profile

- A membership secretary managing one club's roster on their own computer.
- Comfortable with typing commands and reviewing results before group updates.
- Maintains contact details and overlapping groups such as committees, cohorts and alumni.
- Typical workload: up to 500 members. This is a performance target, not a record limit.

#### Selected MVP

- **Records:** add, edit, delete, list, clear and select displayed members.
- **Search and order:** name search, email search, cumulative tag filters and name sorting.
- **Groups:** individual tags, bulk tags and roster-wide tag renaming.
- **Files:** CSV import/export, automatic JSON saving and startup recovery.
- **Help and session:** offline help, keyboard navigation and exit.

See the [Feature Map](#feature-map) for all nineteen command names.

#### Scope boundaries

- Groups are tags, not separate stored objects.
- Bulk tag commands target the displayed group. Tag renaming targets the entire roster.
- The app is local and single-user. It does not provide cloud sync, shared accounts or concurrent editing.
- No phone search, vCard exchange, archiving, undo, billing, messaging or event management is implemented.
- Manual JSON editing is supported only while the app is closed, with a backup retained.

### User stories

Priorities: `***` essential, `**` useful, `*` convenience. Implemented and future stories are listed separately.
Story IDs are retained for traceability with the use cases and earlier requirements.

#### Implemented user stories {id="stories-selected-for-the-mvp"}

| ID | Priority | As a... | I want to... | So that I can... |
| --- | --- | --- | --- | --- |
| US01 | `***` | new secretary | see an offline summary of all available commands and help for one command | check syntax without leaving the app |
| US02 | `***` | club secretary | add a member with name, phone, email, an optional address and optional tags | record a new member even before their address is known |
| US03 | `***` | club secretary | see every member in a readable list with their current displayed index | review the roster and select the right record |
| US04 | `***` | club secretary | edit a member's contact details | correct mistakes and keep the roster up to date |
| US05 | `***` | club secretary | find members using complete or partial words from their names | retrieve a member's contact details quickly |
| US06 | `***` | club secretary | delete one selected member record | remove a record that is no longer needed |
| US07 | `***` | club secretary | clear the whole roster | reset the directory when all existing records are no longer needed |
| US08 | `***` | club secretary | filter the displayed members by an exact tag | work with one group or an overlap of groups |
| US09 | `***` | club secretary | add one tag to all displayed members without removing other tags | assign a group without editing each member separately |
| US10 | `***` | club secretary | review a group and remove one tag from every displayed member | remove an obsolete group assignment while keeping member records and other tags |
| US11 | `***` | club secretary | have valid changes saved automatically | recover my saved roster when I next open the app |
| US12 | `***` | club secretary | receive clear error messages for invalid commands and storage failures | correct my input or the storage problem and know whether my changes were saved |
| US13 | `***` | club secretary | close the app with a command | finish my work using the keyboard |
| US14 | `***` | experienced secretary | edit a backed-up data file while the app is closed | make awkward data corrections outside the app when necessary |
| US15 | `**` | club secretary | export member records as a CSV file | share a copy for verification, newsletters, mail merge, or submissions |
| US16 | `**` | club secretary | hide private contact details until a member is selected | reduce accidental disclosure to people nearby |
| US17 | `*` | club secretary | sort the displayed members by name | browse a long roster more easily |
| US18 | `***` | prospective club secretary | try TrackCall with sample member data | understand the workflow without risking real records |
| US19 | `**` | first-time user | follow the User Guide's quick start | learn the basic workflow |
| US20 | `***` | first-time user | read valid command examples and their expected results in offline help | learn to use commands independently |
| US21 | `**` | club secretary | import member records from a CSV file | avoid entering every member manually |
| US22 | `**` | club secretary | check import counts and inspect the resulting member list | verify accepted records and skipped duplicates |
| US23 | `***` | club secretary | assign a valid group tag to a member | represent groups such as Committee or Batch2026 |
| US24 | `***` | club secretary | add a tag to one member while retaining their other tags | let that member belong to several groups |
| US25 | `***` | club secretary | view a selected member's details and copy their contact fields | check the correct record and reuse contact details without retyping |
| US26 | `**` | club secretary | search using part of a name word | find someone when I cannot remember the complete word |
| US28 | `***` | club secretary | remove one member's tag while retaining their other tags | update their group membership without losing unrelated information |
| US29 | `**` | club secretary | rename a group tag across its members | correct a group name without rebuilding its membership |
| US34 | `**` | club secretary | cancel a pending single-member deletion before confirming it | avoid losing a record after choosing the wrong member |
| US35 | `***` | club secretary | have exact duplicate records rejected during data entry | avoid recording the same contact details twice |
| US37 | `**` | club secretary | find members by a full email address or domain fragment | retrieve contacts or select an email-domain group |
| US38 | `***` | club secretary | choose a startup recovery action when the roster cannot be read | recover usable records while keeping a copy of the damaged file |
| US39 | `**` | club secretary | remove one active search or tag filter, or all show selections | broaden the displayed results while keeping other conditions |
| US40 | `**` | club secretary | show a subset using current displayed member numbers | focus on selected records before applying a group command |

`tag` and `untag` change one assignment while preserving other tags. `edit` replaces the supplied tag set.
Import review consists of result counts and inspecting the roster; there is no staged review screen.

#### Future user stories {id="considered-stories-and-extensions-to-the-selected-mvp"}

These requirements are **not implemented in the current version**. They are retained for future planning,
not promised releases, in line with the
[course's Developer Guide guidance](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-deliverables.html#deliverable-developer-guide-dg).

| ID | Priority | As a... | I want to... | So that I can... | Current limitation |
| --- | --- | --- | --- | --- | --- |
| US27 | `**` | club secretary | search by phone number | identify a member when I only have their number | Considered; the MVP searches names and emails only. |
| US30 | `**` | club secretary | update a non-tag field for a group in one operation | handle repeated renewal changes efficiently | Considered; bulk changes in the MVP affect tags only. |
| US31 | `**` | long-time secretary | archive an inactive member | reduce clutter while keeping their history | Considered; deletion in the MVP does not retain history. |
| US32 | `*` | long-time secretary | archive an unused group tag | keep old groups out of my active work without losing their history | Considered; no tag archive or separate tag catalogue in the MVP. |
| US33 | `*` | club secretary | sort members by class | review members in the order relevant to my task | Considered; no class field or class sorting is implemented. |

### Shared behaviour rules

1. **Input validation**
   - Keywords and prefixes are lowercase. Trim surrounding field whitespace.
   - Reject invalid input before changing records, view state or the saved file.
   - Add/edit share validation for non-empty fields. Only edit accepts bare `a/` to clear an address.
2. **Duplicate identity**
   - Compare trimmed name, phone, email and address exactly, including case and internal spaces.
   - Ignore tags. An edit excludes the record being edited.
   - Equal names are allowed when another contact field differs. This is not real-world identity verification.
3. **Tags**
   - Accept 1–30 ASCII letters or digits. Matching is case-sensitive; internal spaces are invalid.
   - Store each tag once per member. Display tags in ASCII order: digits, uppercase, then lowercase.
4. **Listing and search**
   - `list` restores all records in stored order and clears every condition.
   - `find` narrows the current view using case-insensitive partial name words.
   - `findemail` replaces conditions with a case-insensitive literal email search across the full roster.
   - Keywords within one search use OR. Successive conditions use AND. Searches retain sorting.
5. **Tag filtering**
   - Each `filter` adds one exact tag condition. Repeating a tag does not add another condition.
   - An empty result is valid. Use `list` first to filter the full roster.
6. **Displayed indexes**
   - Use ASCII integers from 1 to the current displayed count. Leading zeroes are accepted.
   - Reject signs, decimals, internal spaces and out-of-range values.
   - Indexes can change after any view update. Always check the displayed list.
7. **Individual updates**
   - `edit` changes supplied fields only; at least one field is required.
   - Bare `a/` clears the address. Supplied tags replace all tags; lone `t/` clears them.
   - `tag` and `untag` change only the requested tag. A valid no-op still saves.
   - Re-evaluate active conditions after replacement. A member can leave the view without being deleted.
8. **Bulk tag updates**
   - Snapshot the displayed targets before changing them. Hidden members and unrelated tags are unaffected.
   - Skip already-present additions or already-absent removals. Count against the original targets.
   - Reject an empty target set. `renametag` instead targets the whole roster and rejects an unused old tag.
9. **Save policy**
   - Valid record-changing commands save the entire roster, including no-ops.
   - View commands, help, export and exit do not save the JSON roster.
   - A deletion prompt or cancellation does not save. Confirmed deletion does.
10. **Save failures**
    - Report success only after saving. A failed write preserves the previous roster file.
    - Except for `clear`, applied changes remain in memory and can be lost on exit.
    - Failed clear restores records and the prior view. Fix storage access, then retry with a valid no-op.
    - Roster replacement and backup rotation are separate; see [recovery limitations](#startup-and-validation).
11. **Add, delete, clear and exit**
    - Add appends the record and clears conditions. Delete retains conditions and requires the next input `y`.
    - Any other reply cancels deletion and is consumed. Confirmed deletion has no undo.
    - Clear removes hidden records too, clears conditions after saving and has no confirmation or undo.
    - Exit performs no final roster save. List, sort, clear and exit reject extra arguments.
12. **Startup**
    - Read `data/addressbook.json` relative to the packaged JAR, or the working directory in development.
    - A missing file loads samples. A valid empty file stays empty.
    - An unreadable or invalid file requires preservation and recovery before editing is allowed.
    - Normal startup and read-only commands do not rewrite the roster.
13. **Feedback**
    - Input errors explain the problem and give usage or a route to help.
    - Duplicate errors reject the record; they never merge or overwrite it.
    - Storage errors are separate because changes may already exist in memory (rule 10).

#### Member field rules

- Add requires name, phone and email. Address and tags are optional.
- Edit requires at least one field. Omitted fields remain unchanged.
- Reject repeated `n/`, `p/`, `e/` and `a/` prefixes. Validate structure, fields, then duplicates.
- Unsupported single-letter prefixes are rejected by add/edit.

| Field | Accepted values |
| --- | --- |
| Name | ASCII letters, digits and spaces; at least one letter or digit. Preserve case and internal spaces. |
| Phone | At least three ASCII digits. No signs, punctuation or internal whitespace. |
| Email | One `@`; no whitespace. Follow the local-part and domain rules below. |
| Address | Optional, single-line text. Add rejects explicit blank `a/`; edit uses it to clear the value. |
| Tags | Rule 3 applies. Repeated identical tags collapse into one. Only edit accepts lone `t/` to clear all tags. |

**Email rules**

- Local part: non-empty alphanumeric runs, separated by single `+`, `_`, `.`, or `-` characters.
- Domain: dot-separated alphanumeric labels with optional single internal hyphens.
- Final domain label: at least two characters.
- Validation checks syntax, not mailbox existence. Phone validation does not verify reachability.

### Offline help and readable feedback {id="offline-help-and-readable-feedback"}

#### Offline help

- Bundle help for all nineteen executable commands. It must work without network access.
- Each topic gives purpose, syntax, rules, a valid example, expected result and relevant errors.
- Explain displayed indexes before examples that use them.
- Support overview/topic navigation, scrolling and returning to command entry using the keyboard.
- Opening help leaves member data and the displayed roster unchanged.

#### Member details and command feedback

- Roster cards show index, name and tags. Show contact details only for the selected member.
- Provide copy controls; F2 focuses the list and Escape hides details.
- Add/edit/delete success feedback shows the action, name and tags after saving. Hide contact fields there.
- Wrap or scroll long values and feedback. Input errors should explain how to correct the request.

### Use cases

**System:** TrackCall.

**Primary actor:** the membership secretary.
**MSS:** main success scenario.
 The app is open unless a use case states otherwise.
These workflows describe implemented features. See Implementation for internal method calls.

#### UC01: Register a member

**Related stories:** US02, US23, US35.

**Goal:** add a new member and retain the record for later sessions.

**MSS**

1. The secretary submits the member's required contact details and optional tags.
2. TrackCall validates the details and checks for an exact duplicate record.
3. TrackCall adds the member, saves the roster, and restores the complete member list.
4. TrackCall reports the added member's name and tags. The secretary can select the member to check
   contact details in the details panel. The use case ends.

**Extensions**

* **2a.** The command structure or a field is invalid, or the member duplicates an existing record.
  TrackCall explains the error and changes nothing. The secretary can correct the input at step 1.
* **3a.** Saving fails. TrackCall reports the storage error instead of success. The new member remains
  in memory, but the saved file is unchanged. The use case ends without confirmed persistence;
  recovery follows shared rule 10.

#### UC02: Find and update a member

**Related stories:** US03, US04, US05, US24, US25, US26, US28.

**Goal:** correct one member's record.

**MSS**

1. The secretary searches for complete words or parts of words from the member's name.
2. TrackCall lists matching records and their current indices.
3. The secretary checks the contact details to identify the intended member, then submits its
   displayed index and the fields to change, or uses `tag INDEX t/TAG` / `untag INDEX t/TAG` to
   adjust one tag while retaining the rest.
4. TrackCall validates the request, updates the record, saves the roster, and reapplies the search
   and filters.
5. TrackCall reports the update with the member's name and tags. The secretary can select the member
   to check contact details in the details panel. The use case ends.

**Extensions**

* **1a.** The search has no keyword. TrackCall reports the input error and preserves the view.
  The secretary can retry step 1.
* **2a.** No record matches. TrackCall shows an empty result. The secretary can search again at step 1
  or end the use case without changing data.
* **4a.** The index or fields are invalid, no field is supplied, or the edit duplicates another record.
  TrackCall reports the error without changing records or the view. The secretary can retry step 3.
* **4b.** Saving fails. The edit remains in memory, the saved file is unchanged, and TrackCall reports
  the storage error. The use case ends without confirmed persistence; recovery follows rule 10.

An edited member that no longer matches the search or filters disappears from the displayed list
but remains in the roster. The secretary can use `list` to see it again.

#### UC03: Update a group's tags {id="uc03-update-a-groups-tags"}

**Related stories:** US08, US09, US10.

**Goal:** change one group assignment without changing unrelated tags.

**MSS**

1. The secretary requests the complete roster.
2. TrackCall lists all members.
3. The secretary searches or filters to identify the intended group.
4. TrackCall displays the matching members.
5. The secretary checks the affected group and requests adding or removing one tag from it.
6. TrackCall validates the request, fixes the target set, updates its tags, and saves the roster.
7. TrackCall reapplies the search and filters and reports changed and skipped counts. The use case ends.

**Example:** enter `list`, `filter t/year1`, and `tagall t/committee` as separate commands.
This adds `committee` to the displayed year-one members and retains their other tags.

**Extensions**

* **3a.** A search or filter request is invalid. TrackCall reports the error and preserves the prior
  view. The secretary can correct the request at step 3.
* **4a.** No members match. The secretary can restart at step 1 or end the use case. A bulk update
  on an empty list is rejected without changing or saving data.
* **6a.** The tag is empty or invalid, or more than one tag or an unknown prefix is supplied.
  TrackCall reports the error and changes nothing. The secretary can retry step 5.
* **6b.** Some targets already have the added tag or lack the removed tag. TrackCall skips those
  assignments, saves the roster, and continues to step 7. A non-empty target with no changes is
  still a valid operation; it does not create duplicate tags.
* **6c.** Saving fails. The complete batch remains in memory, the saved file is unchanged, and
  TrackCall reports the storage error. The use case ends without confirmed persistence;
  recovery follows rule 10.

Removing a tag required by an active filter can hide the affected members after step 7. It does
not delete them. Counts still refer to the target set fixed at step 6.

#### UC04: Remove one member

**Related stories:** US05, US06, US25, US34.

**Goal:** remove one unwanted record while retaining other members.

**MSS**

1. The secretary lists, searches, or filters the roster.
2. TrackCall displays the resulting records and their current indices.
3. The secretary checks the intended member's details and requests deletion using its displayed index.
4. TrackCall validates the request and asks for confirmation, naming the member and their tags.
5. The secretary confirms the deletion.
6. TrackCall removes that record, saves the roster, and renumbers the remaining displayed records
   while retaining the active search and filters.
7. TrackCall reports the deleted record. The use case ends.

**Extensions**

* **1a.** The listing, search, or filter command is invalid. TrackCall reports the error and preserves
  the current view. The secretary can retry step 1.
* **2a.** The result is empty. No member can be selected. The use case ends without changes.
* **4a.** The deletion command or index is invalid. TrackCall reports the error and removes nothing.
  The secretary checks the current list and retries step 3.
* **5a.** The secretary enters anything other than `y`. TrackCall cancels the deletion, removes
  nothing, does not run that input, and does not save. The use case ends.
* **6a.** Saving fails. The record remains deleted in memory but may return after restart. TrackCall
  reports the storage error and leaves the saved file unchanged. The use case ends without
  confirmed persistence; recovery follows rule 10.

Deletion has no undo after confirmation. It removes the underlying record, not just its visible card.

#### UC05: Reset the whole roster

**Related story:** US07.

**Goal:** remove all member records, including records outside the current view.

**MSS**

1. The secretary decides to remove all records and requests clearing the roster.
2. TrackCall validates the request and removes every member.
3. TrackCall saves the empty roster, clears the search, tag, and selection filters, and reports the
   number removed. The use case ends.

**Extensions**

* **2a.** The command contains extra arguments. TrackCall rejects it without changing anything.
  The secretary can retry step 1.
* **2b.** The roster is already empty. TrackCall still attempts to save it and reports that no changes
  were needed only if saving succeeds. The use case ends. If saving fails, extension 3a applies.
* **3a.** Saving fails. TrackCall restores the previous records, displayed list, and filters and
  leaves the saved file unchanged. It reports that no members were removed. The secretary can
  resolve the storage problem and retry step 1.

There is no confirmation or undo. Recovery after a successful clear requires a usable backup, such as the automatic previous-version backup.

#### UC06: Look up a command

**Related stories:** US01, US20.

**Goal:** learn how to perform a task offline without leaving the app.

**MSS**

1. The secretary requests the command summary with `help`.
2. TrackCall displays a local summary of all commands available in the running version.
3. The secretary requests one command's details, for example `help tagall`.
4. TrackCall displays that command's purpose, syntax, examples with expected results, parameter
   rules, and errors. The use case ends.

**Extensions**

* **1a.** The request uses an unknown command keyword, such as `-help`, `-h`, or `HELP`.
  TrackCall reports `Unknown command. Type help for available commands.` The secretary can retry step 1.
* **3a.** The topic is unknown or uses the wrong case. TrackCall reports that there is no help entry
  for that topic and suggests `help` to list commands. The secretary can retry step 3.
* **3b.** More than one topic is supplied. TrackCall reports that only one command can be inspected
  at a time and shows `help [COMMAND]` as the usage. The secretary can retry step 3.

#### UC07: Load a manually edited data file

**Related stories:** US11, US12, US14.

**Goal:** load valid corrections made outside the app.

**Preconditions:** TrackCall is closed and the secretary has backed up the data file.

**MSS**

1. The secretary edits and saves the JSON data file while preserving its structure and valid member fields.
2. The secretary starts TrackCall.
3. TrackCall validates the complete file and loads its members in file order.
4. TrackCall displays the loaded roster. The secretary checks the corrections. The use case ends.

**Extensions**

* **3a.** The file is missing. TrackCall loads sample members and logs that the file was missing.
  It creates the file only after a valid data-changing command saves successfully. The use case ends.
* **3b.** The file is invalid or unreadable. TrackCall preserves its bytes in a uniquely named
  recovery copy and opens a dialog before the roster can be edited. The secretary can restore a
  valid backup, start with an empty roster, or close without replacing the file. No partial roster
  is loaded. The use case ends after the chosen recovery action.
* **3c.** TrackCall cannot preserve the damaged file. It reports the failure and closes without
  opening an editable roster. The secretary fixes access or restores a backup, then retries step 2.

#### UC08: Finish a TrackCall session

**Related stories:** US11, US12, US13.

**Goal:** close TrackCall after the secretary has finished
working without changing the persisted roster.

**MSS**

1. The secretary finishes reviewing or changing the roster.
2. The secretary confirms that the latest data-changing command reported a successful save, then
   submits `exit`.
3. TrackCall validates the parameterless command and closes the main window and application process.
4. The secretary can start TrackCall again later and retrieve the last successfully saved roster. The
   use case ends.

**Extensions**

* **2a.** The previous data-changing command reported a saving failure. The secretary may resolve the
  storage problem and retry a data-changing command before returning to step 2. If the secretary
  proceeds with `exit`, TrackCall still closes, but changes that exist only in memory are not
  guaranteed to be present after restart.
* **3a.** The command contains an argument or uses the wrong command spelling or case. TrackCall
  reports `Invalid command format.` followed by the `exit` usage for extra arguments, or the unknown-command error
  `Unknown command. Type help for available commands.` for a wrong-case command such as `EXIT`.
  TrackCall keeps the window open, and the secretary can retry step 2.

`exit` does not restore the full list, clear active searches or filters, display a success message, or
perform a separate final save. The operating-system close button has the same termination behaviour.

#### UC09: Recover from an invalid command

**Related stories:** US01, US12, US20, US35.

**Goal:** understand an input error and complete
the intended task without unintended data changes.

**MSS**

1. The secretary submits a command with an incorrect keyword or arguments.
2. TrackCall rejects it, explains the input error, and preserves the roster, current view,
   and saved file.
3. The secretary requests the command summary or help for the intended command.
4. TrackCall provides its syntax, rules, and an example with the expected result, even offline.
5. The secretary corrects the input and resubmits the command.
6. TrackCall validates and performs the requested operation, saving first when required, then
   reports its result. The use case ends.

**Extensions**

* **5a.** The secretary decides not to continue. The use case ends without changes.
* **6a.** The corrected input is still invalid. TrackCall explains the remaining problem and
  changes nothing. The secretary can return to step 3 or 5.
* **6b.** An add or edit would create a duplicate under shared rule 2. TrackCall rejects the
  duplicate without merging or overwriting records. The secretary checks the existing roster
  before correcting the input at step 5 or ending the use case.
* **6c.** A data-changing command cannot save. TrackCall reports the storage failure instead
  of success. The use case ends without confirmed persistence; shared rule 10 explains
  which changes remain in memory and how to retry.

### Non-functional requirements

These are acceptance targets, not a record of passed tests. They include the
[course constraints](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-constraints.html).

1. **NFR01 — Platforms:** run on Windows, macOS and Linux using Java 25. Do not require another Java version.
2. **NFR02 — Distribution:** deliver one JAR of at most 100 MB. Require no installer or separate application dependencies.
3. **NFR03 — Offline use:** core commands and help work with networking disabled. No team-operated server is required.
4. **NFR04 — Keyboard access:** complete every in-app operation using commands and keyboard navigation.
   Manual file editing uses an external editor.
5. **NFR05 — Display:** keep controls and text readable, without overlap, at:
   - 1920 × 1080 or higher with 100% and 125% scaling.
   - 1280 × 720 or higher with 150% scaling.
   - Long lists and help text remain reachable through scrolling.
6. **NFR06 — Privacy:** core operations do not transmit contacts to external services.
   Local files rely on OS access controls; the app provides no login or encryption.
7. **NFR07 — Storage:** use local, human-editable UTF-8 JSON without a DBMS.
   Support corrections in a text editor while the app is closed.
8. **NFR08 — Response time:** complete each valid command within two seconds under this test workload:
   - 500 members; writable data file up to 1 MB; command up to 256 characters.
   - 2 GHz dual-core CPU, 4 GB RAM and local SSD, without competing heavy work.
   - Measure submission to visible result, including saving. Exclude startup and shutdown.
   - Record OS, Java version, hardware, dataset and timings. These are test conditions, not input limits.
9. **NFR09 — Reliability:** retain successfully saved data after restart.
   - A write failure preserves the previous roster and never reports success.
   - Apply the in-memory failure policy in shared rule 10, including clear rollback.
   - Exit closes without a separate roster save.

Validation, duplicates and filters are functional rules. Development-process rules are not product NFRs.

### Glossary

| Term | Meaning |
| --- | --- |
| Membership secretary | The single operator responsible for keeping a club or organisation's member roster up to date. |
| Roster / address book | The complete collection of member records, including records hidden from the displayed list. The file and starter code use the term address book. |
| Member record | One member's name, phone number, email address, address, and tag assignments. |
| Group | A set of members sharing a tag, such as a committee or cohort. Groups are not separate stored objects in the MVP. |
| Tag | A case-sensitive label containing 1 to 30 ASCII letters or digits, assigned at most once to each member. |
| Displayed list | The records currently visible after the active search, tag filters, and index selections. It is the target set for a bulk tag command. |
| Active search and filters | The name-search, email-search, tag, and index-selection conditions used to determine which records are displayed. A new `find` adds a condition; `findemail` replaces them, and `list` clears them all. |
| Index | A record's current displayed number, starting at 1. It is not a permanent member ID and can change after another command. |
| Partial-word matching | Matching any contiguous part of a name word, ignoring case. For example, `John`, `Jo`, and `oh` all match `John Tan`. |
| Bulk tag update | Adding or removing one tag for the fixed set of members displayed when the command is submitted. |
| Duplicate record | A record whose trimmed name, phone, email, and address all exactly match another record, including case and internal spacing. Tags are ignored. |
| No-op | A valid operation that leaves record values unchanged. A valid data-changing no-op can still retry saving; a bulk command with no targets is an error. |
| In-memory data | The working roster held by the running app. After a save failure it can differ from the file and may be lost on exit. |
| Persisted data | The roster successfully written to the local data file and available for a later session. |
| Session | One period of use beginning when TrackCall starts and ending when the application closes. |
| Exit command | The parameterless `exit` command that closes TrackCall without changing records or performing a final save. |
| Data-changing command | `add`, `edit`, `tag`, `untag`, confirmed `delete`, `clear`, `import`, `tagall`, `untagall`, or `renametag`. A valid invocation attempts to save even if its values do not change. |
| Read-only command | A command that does not change member records or save the data file, such as `help`, `list`, `find`, or `filter`. It may change the displayed list. |
| JSON | The structured text format of the local data file. It uses a `persons` array and each member's `tags` array. |
| CSV | Comma-separated values, a tabular text format for exchanging member data with spreadsheets and other tools. `export [FILEPATH]` writes CSV; `import p/FILE_PATH` validates and imports it. |
| MVP | Minimum viable product: the core feature set delivered by TrackCall. |


## Appendix: Manual Testing {id="appendix-instructions-for-manual-testing"}

Use a disposable folder and synthetic contacts. These are test instructions, not a record of passed tests.
Record the commit or release tag, JAR checksum, Java version, OS, steps, and actual result for each run.

### Test setup

1. Select Java 25. Build with `./gradlew shadowJar` (`gradlew.bat shadowJar` on Windows).
2. Copy `build/libs/trackcall.jar` into the disposable folder. Run `java -jar trackcall.jar` there.
   See [DevOps](DevOps.html#build-automation) for local ARM builds and release testing.
3. Before each independent scenario below, recreate this fixture by entering each command separately:

```text
clear
add n/Zara Lim p/90000001 e/zara@example.com a/Test Road t/friends
add n/Bob Lee p/90000002 e/bob@school.edu t/committee
add n/Alice Tan p/90000003 e/alice@example.com a/Main Street t/committee
```

**Expected baseline:** three members in stored order: Zara, Bob, Alice. Bob has no address.
Use `list` and check the displayed indices before any indexed command. Back up test files before fault injection.

### Launch and shutdown

1. Start in a new folder without a roster file. **Expect:** sample members and a usable command box.
2. Run `list extra`, `clear extra`, and `exit extra`. **Expect:** input errors, unchanged records, and an open app.
3. Run `exit`, then restart from the same JAR. **Expect:** the last successfully saved roster.
4. Launch the same JAR twice. **Expect:** the second process reports that the app is already running.
   Close the first process and relaunch. **Expect:** startup succeeds without deleting `.trackcall.lock`.
5. Move and resize the window, close, and restart. **Expect:** valid saved bounds are restored.
   Repeat after disconnecting its former display. **Expect:** the window remains reachable on an available display.

### Offline help and readable feedback {id="offline-help-and-readable-feedback-1"}

**Setup:** restore the fixture and disable networking.

1. Open `help`, F1, and the Help menu. **Expect:** the same offline overview of all 19 commands.
2. Open `help edit` and the overview's edit button. **Expect:** matching syntax, examples, rules, and errors.
   Use Tab, arrow keys, Page Up/Down, Home/End, and Escape. **Expect:** reachable content and visible focus.
3. Try `help ADD`, `help unknown`, and `help add edit`. **Expect:** useful errors without record or view changes.
4. Press F2 and select a member with arrow keys. **Expect:** that member's complete contact details.
   Copy a field, then press Escape. **Expect:** hidden details and focus in the command box.
5. Edit a selected member. **Expect:** the details panel clears; feedback contains the name and sorted tags,
   but no phone, email, or address. Repeat for add, untag, and confirmed delete.
6. Add long synthetic values and several 30-character tags. Resize the main and help windows.
   **Expect:** reachable text through wrapping or scrolling, readable feedback, and no overlapping controls.
7. Apply many name filters and navigate their removal controls using the keyboard.
   **Expect:** each control is reachable and the roster remains usable. Remove a filter and check the remaining conditions.
8. Inspect new routine logs after these commands. **Expect:** no synthetic contact values in command/result logging.

### Commands and displayed indices

Run each scenario from the baseline fixture.

#### Add, edit and validation

1. Add a fourth member without `a/`. **Expect:** an absent address that survives restart.
2. Run `edit 1 a/`. **Expect:** Zara's address is cleared and stays absent after restart.
3. Run `edit 1 t/committee t/year1`, then `edit 1 t/`. **Expect:** replaced tags, then no tags.
4. Try a missing required field, repeated `n/`, invalid phone/email, empty `add ... a/`, and `edit 1`.
   **Expect:** rejection without changes. Test each invalid input separately.
5. Add Alice's exact name, phone, email, and address with different tags. **Expect:** duplicate rejection.
   Change the phone and retry. **Expect:** a separate record; names alone do not identify duplicates.
6. Use tab separators before valid prefixes. **Expect:** the same parsing as spaces.
   Append `x/extra` or `T/committee`. **Expect:** rejection without record or view changes.

#### Search, filter and selection

1. Run `find ali`. **Expect:** Alice. Run `list`. **Expect:** Zara, Bob, Alice in stored order.
2. Run `find Zara Bob`. **Expect:** Zara and Bob. Run `find Nobody`. **Expect:** no matches.
3. Restore with `list`; run `filter t/committee`, then `find Alice`. **Expect:** Alice only.
   Remove the name-filter box. **Expect:** Bob and Alice; the committee filter remains.
4. Run `list`, `show 1 3`, then `show 2`. **Expect:** Zara and Alice, then Alice only.
5. Try `find` without a keyword, an empty tag filter, and an out-of-range selection.
   **Expect:** errors preserving the preceding view. Read-only commands must not change roster-file bytes or timestamps.

#### Individual and bulk tags

1. Run `tag 1 t/year1` twice. **Expect:** Zara gains one tag; repetition reports no change.
2. Run `list`, `filter t/committee`, then `tagall t/year1` twice.
   **Expect:** Bob and Alice gain the tag; the second command skips both.
3. Run `untagall t/committee`. **Expect:** both members disappear from the filtered view.
   Run `list`. **Expect:** all three remain stored with their other tags intact.
4. Search for one member, then run `renametag t/year1 t/alumni`.
   **Expect:** all matching tags are renamed, including those on hidden members.
5. Restore the fixture. Run `filter t/committee`, then `untag 1 t/committee`.
   **Expect:** Bob disappears from the view but remains in the roster.
6. Try invalid indices, empty tags, two tags for `tagall`, and identical old/new tags for `renametag`.
   **Expect:** errors without changes. A bulk update on an empty view must also fail.
7. Restart after successful updates. **Expect:** all changes persist.

#### Delete and clear

1. Run `find Alice`, then `delete 1`. **Expect:** a confirmation naming Alice; no deletion yet.
2. Enter `list` at the prompt. **Expect:** deletion is cancelled; `list` is consumed as the answer and does not run.
3. Run `delete 1`, then `y`. **Expect:** Alice is deleted. `list` still shows Zara and Bob.
4. Try `delete 0`, `delete -1`, a very large integer, and an index beyond the displayed list.
   **Expect:** errors without deletion.
5. Restore the fixture, run `find Alice`, then `clear`. **Expect:** all three records are removed, including hidden members.
6. Run `clear` again. **Expect:** the already-empty message after saving. Check that `.bak` still holds the previous
   distinct roster. Clear has no confirmation or undo.

### Email search

**Setup:** restore the fixture before each independent check.

1. Run `sort`, `find Nobody`, then `findemail @EXAMPLE.COM`.
   **Expect:** Alice and Zara in name order; the previous search is replaced.
2. Run `filter t/committee`, then `findemail @school.edu`.
   **Expect:** Bob; the new email search replaces the tag filter too.
3. Run `findemail alice@example.com @school.edu`. **Expect:** Alice and Bob (OR matching).
4. Search for a nonmatching literal such as `.*`. **Expect:** zero matches, not regular-expression matching.
   Run `findemail` without arguments. **Expect:** invalid input and the preceding view retained.
5. Run `findemail @example.com`, then edit Alice's email to `alice@other.net` using her displayed index.
   **Expect:** Alice leaves the view but remains stored; `list` shows all three members.
6. Record roster-file bytes and modification time before valid email searches.
   **Expect:** neither changes after the searches.

### Sorting and CSV workflows

**Setup:** restore the fixture. Use only files inside the disposable application folder.

1. Run `sort` twice. **Expect:** Alice, Bob, Zara both times. Run `list`. **Expect:** Zara, Bob, Alice.
   Try `sort date`. **Expect:** an error preserving the view. Sorting must not save the roster.
2. Run `filter t/committee`, `sort`, then `export members.csv`.
   **Expect:** all three records exported in stored order, including hidden Zara. The view stays unchanged.
3. Inspect the CSV. **Expect:** the documented UTF-8 header, correctly quoted fields, and sorted semicolon-separated tags.
4. Run `import p/members.csv`. **Expect:** zero imports and three skipped duplicates; filters and sorting reset.
   Run `clear`, then import again. **Expect:** three imports, zero skipped duplicates, and persistence after restart.
5. Copy the CSV to `member list.csv`; run `import p/"member list.csv"`.
   **Expect:** the path with spaces works. A missing source must fail without changes.
6. Create a CSV containing a valid row followed by an invalid-phone row. Import it.
   **Expect:** a row-specific error with no partial import, save, or view change.
   Repeat separately for a bad header, broken quoting, and a line break inside a quoted field.
7. Import a header-only CSV and export an empty roster. **Expect:** valid zero-record operations.
8. Export to a missing directory, a protected application file, and a path outside the application folder.
   **Expect:** handled errors without replacing protected data. Repeat with symbolic-link paths.

### Persistence and invalid files

Use a fresh fixture and backup for each failure scenario. Restore access before exiting the app.

1. Save a change, restart the packaged JAR from a different working directory, and check the roster.
   **Expect:** the same application-folder data and complete fields/tags.
2. With the app closed, replace the test roster with malformed JSON, `null`, a null member, or a null tag.
   Test each separately. **Expect:** a recovery dialog and an exact-byte, uniquely named recovery copy.
3. Exercise each recovery choice: Close, Restore backup, and Start empty.
   **Expect:** Close preserves the invalid file; recovery preserves the damaged copy and existing backup.
   If no recovery copy can be written, startup must stop before exposing an editable roster.
4. Test null preferences and null `guiSettings`. **Expect:** usable defaults at startup.
5. While the app is open, block writing to its test data file. Test each data-changing command independently:
   `add`, `edit`, `tag`, `untag`, confirmed `delete`, `tagall`, `untagall`, `renametag`, and `import`.
   - **Expect:** a save error with retry guidance; the change remains in memory.
   - Previously saved bytes remain unchanged. Pressing Enter again must not repeat the applied command.
   - Restore write access. Reapply an existing field with `edit`, then restart. **Expect:** the current roster is saved.
6. Repeat the write failure with `clear` while a filter hides members.
   - **Expect:** no members removed, restoration of the full roster and previous filtered view, and unchanged saved bytes.
   - Restore access and retry. **Expect:** successful removal of all members. Repeat with an already-empty roster.

### Release acceptance checks still required

Test the actual release JAR, not only a development checkout. Record pass/fail and evidence for:

- Windows, macOS and Linux with the required Java 25 distribution.
- Startup from a folder containing spaces and operation with networking disabled.
- Keyboard access, long values, and every resolution/scaling combination in NFR05.
- Every User Guide example, comparing the actual output with the documented outcome.
- The NFR08 response-time workload, including machine details and measured timings.
- Save failures, recovery, empty inputs, overlapping filters, hidden members, and restart persistence.

Passing automated tests alone does not establish release readiness. Record unresolved failures against the tested commit.

# TrackCall v1.4 alpha

**Prepared locally; not yet published.** This alpha keeps TrackCall's fourteen-command membership
workflow and improves input validation. Final release coordination is tracked in
[#143](https://github.com/AY2627S1-CS2103T-W12-1/tp/issues/143).

## Changes since v1.3

- `edit` now rejects unknown or uppercase single-letter field prefixes before changing a member.
  For example, `edit 1 a/Main Road T/committee` reports invalid input instead of storing the mistyped
  tag prefix in the address. Use lowercase `t/` to assign tags.
- `add` and `edit` share the same validation. Standalone `c/o` in any letter case, URLs, numeric
  slashes such as `12/3`, and slashes embedded in words remain valid address text.
- The Developer Guide's Model UML now shows the actual JavaFX filtering and sorting views and
  explains how `list` restores all members in stored order.
- Gradle application runs enable Java assertions. The application reports version `v1.4`.

## Run the alpha

Use Java 25 and place `trackcall.jar` in a writable folder. Run:

```text
java -ea -jar trackcall.jar
```

Try `help`, `list`, and `sort`. Changes are saved automatically beside the JAR in
`data/addressbook.json`. Packaged CSV paths must remain inside the JAR folder or its subfolders.

## Validation

The local preparation passed `./gradlew clean check coverage shadowJar` on Java 25:
**510 tests passed**, with zero failures, errors, or skipped tests. Checkstyle passed.
New regressions cover the edit-prefix error, ordinary slash-containing addresses, and preservation
of the roster, displayed view, saved contents, and modification time after rejection.

A separate smoke check loaded production classes from this JAR and exercised all fourteen command
types, CSV round trips, protected paths, and backup recovery. It confirmed the edit-prefix fix,
version `v1.4`, a JAR folder containing spaces, and launch from a different working directory.
This checks the packaged logic; a fresh manual UI check remains pending.

The existing Model `.puml` and regenerated PNG were checked with PlantUML 1.2026.8 and visually reviewed.
The final published artifact must be rebuilt and verified from the merged release commit.

## Remaining limitations

- Deletion and clearing remain immediate, with no confirmation or undo.
- Tags are case-sensitive; names accept ASCII letters, digits, and spaces.
- JSON uses `persons` and per-member `tags`; the proposed `tagged` schema is not implemented.
  Unknown and repeated JSON fields are not yet rejected strictly.
- Save failures after most data-changing commands can leave changes in memory; follow the User Guide's
  recovery instructions before exiting. A failed `clear` restores the previous roster and view.
- Separate manual execution of this alpha JAR on Windows and Linux remains pending.

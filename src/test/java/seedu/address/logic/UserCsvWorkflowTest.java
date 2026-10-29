package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Acceptance scenarios derived from User Guide: CSV import/export and correcting save errors (US11, US15, US21). */
public class UserCsvWorkflowTest {
    private static final String HEADER = "name,phone,email,address,tags\n";

    @TempDir
    public Path directory;

    private Path rosterFile;
    private ModelManager model;
    private LogicManager logic;
    private JsonAddressBookStorage rosterStorage;

    @BeforeEach
    public void setUp() throws Exception {
        rosterFile = directory.resolve("roster.json");
        rosterStorage = new JsonAddressBookStorage(rosterFile);
        model = new ModelManager();
        logic = logicFor(model, rosterFile);
        logic.execute("add n/Zulu p/00123 e/zulu@example.com t/old");
        logic.execute("add n/Alice p/456 e/alice@example.com a/Block 1, \"Main\" Street t/z t/A");
    }

    @Test
    public void import_corruptOrDisguisedFiles_preservesRosterViewAndSavedFiles() throws Exception {
        logic.execute("filter t/old");
        logic.execute("sort");
        AddressBook before = new AddressBook(model.getAddressBook());
        var displayed = List.copyOf(model.getFilteredPersonList());
        var filters = List.copyOf(model.getActiveFilters());
        byte[] saved = Files.readAllBytes(rosterFile);
        Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
        byte[] savedBackup = Files.readAllBytes(backup);
        List<byte[]> corruptFiles = List.of(
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10},
                "PK\u0003\u0004not a spreadsheet CSV".getBytes(StandardCharsets.UTF_8),
                "<html><body>not CSV</body></html>".getBytes(StandardCharsets.UTF_8),
                utf8(HEADER + "Bob,789,bob@example.com,Home,\n\"unterminated"),
                utf8(HEADER + "Bob,789,bob@example.com,Home,\nBad,12,bad@example.com,Home,\n"));
        for (int index = 0; index < corruptFiles.size(); index++) {
            Path source = directory.resolve("disguised-" + index + ".csv");
            byte[] input = corruptFiles.get(index);
            Files.write(source, input);
            CommandException error = assertThrows(CommandException.class, () ->
                    logic.execute("import p/\"" + source + "\""));
            assertTrue(error.getMessage().contains("No members were imported."));
            assertEquals(before, model.getAddressBook());
            assertEquals(displayed, model.getFilteredPersonList());
            assertEquals(filters, model.getActiveFilters());
            assertArrayEquals(saved, Files.readAllBytes(rosterFile));
            assertArrayEquals(savedBackup, Files.readAllBytes(backup));
            assertArrayEquals(input, Files.readAllBytes(source));
        }
        Path typo = directory.resolve("members.cvs");
        Files.writeString(typo, HEADER);
        assertThrows(ParseException.class, () -> logic.execute("import p/\"" + typo + "\""));
        assertFalse(model.getFilteredPersonList().isEmpty());
        assertArrayEquals(saved, Files.readAllBytes(rosterFile));
    }

    @Test
    public void import_overLimit_preservesRosterFiltersAndSavedFiles() throws Exception {
        logic.execute("filter t/old");
        logic.execute("sort");
        AddressBook before = new AddressBook(model.getAddressBook());
        var displayed = List.copyOf(model.getFilteredPersonList());
        var filters = List.copyOf(model.getActiveFilters());
        byte[] saved = Files.readAllBytes(rosterFile);
        Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
        byte[] savedBackup = Files.readAllBytes(backup);
        Path oversized = directory.resolve("oversized.csv");
        try (java.io.RandomAccessFile output = new java.io.RandomAccessFile(oversized.toFile(), "rw")) {
            output.setLength(CsvAddressBookImporter.MAX_FILE_BYTES + 1L);
        }
        Path tooManyRows = directory.resolve("many-rows.csv");
        Files.writeString(tooManyRows, HEADER + "Bob,789,bob@example.com,Home,\n"
                .repeat(CsvAddressBookImporter.MAX_MEMBER_ROWS + 1));
        for (Path source : List.of(oversized, tooManyRows)) {
            CommandException error = assertThrows(CommandException.class, () ->
                    logic.execute("import p/\"" + source + "\""));
            String reason = source.equals(oversized) ? CsvAddressBookImporter.MESSAGE_FILE_TOO_LARGE
                    : CsvAddressBookImporter.MESSAGE_TOO_MANY_ROWS;
            assertEquals("Could not import CSV: " + reason + " No members were imported.", error.getMessage());
            assertEquals(before, model.getAddressBook());
            assertEquals(displayed, model.getFilteredPersonList());
            assertEquals(filters, model.getActiveFilters());
            assertArrayEquals(saved, Files.readAllBytes(rosterFile));
            assertArrayEquals(savedBackup, Files.readAllBytes(backup));
        }
    }

    @Test
    public void edit_emptyAddress_removesUnknownAddressAndPersistsIt() throws Exception {
        // Updated UG: edit INDEX a/ clears an address that is no longer known.
        logic.execute("edit 2 a/");

        assertEquals("", model.getAddressBook().getPersonList().get(1).getAddress().value);
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(rosterFile).readAddressBook().orElseThrow());
    }

    @Test
    public void edit_unchangedContact_preservesPreviousDistinctBackup() throws Exception {
        // Updated UG: retrying persistence without changing member data must retain the previous distinct version.
        Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
        byte[] previousDistinct = Files.readAllBytes(backup);
        AddressBook current = new AddressBook(model.getAddressBook());

        logic.execute("edit 1 p/00123");

        assertEquals(current, rosterStorage.readAddressBook().orElseThrow());
        assertArrayEquals(previousDistinct, Files.readAllBytes(backup),
                "An unchanged save must not replace the user's previous distinct roster backup");
    }

    @Test
    public void clear_alreadyEmpty_preservesRecoverableMembers() throws Exception {
        // Updated UG explicitly includes clear on an already-empty roster in backup preservation.
        AddressBook original = new AddressBook(model.getAddressBook());
        logic.execute("clear");
        logic.execute("yes");
        assertEquals(original, rosterStorage.readBackup().orElseThrow());
        byte[] previousDistinct = Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(rosterFile));

        logic.execute("clear");

        assertTrue(rosterStorage.readAddressBook().orElseThrow().getPersonList().isEmpty());
        assertArrayEquals(previousDistinct, Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(rosterFile)),
                "Clearing an empty roster must not destroy the backup containing the removed members");
    }

    @Test
    public void import_saveFails_explainsUnsavedStateAndHowToRetry() throws Exception {
        // Updated UG promises the import-save error is followed by an unsaved-change warning and retry steps.
        Path csv = directory.resolve("unsaved.csv");
        Files.writeString(csv, HEADER + "Carol,999,carol@example.com,,new\n");
        Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
        Files.delete(backup);
        Files.createDirectory(backup);
        Files.writeString(backup.resolve("blocked.txt"), "Blocks backup replacement");

        CommandException error = assertThrows(CommandException.class, () -> logic.execute("import p/\"" + csv + "\""));

        String feedback = error.getMessage().toLowerCase(Locale.ROOT);
        assertTrue(feedback.startsWith("could not save data to file:"));
        assertTrue(feedback.contains("not saved"), "The user must be told their imported members are not saved");
        assertTrue(feedback.contains("edit"), "The user must be told how to retry saving");
        assertTrue(feedback.contains("exit"), "The user must be warned about losing unsaved members on exit");
    }

    @Test
    public void export_filteredSortedRoster_sharesAllMembersAndCanRestoreThemInStoredOrder() throws Exception {
        // UG: export includes hidden members, preserves the current view, and sorts tags in the CSV.
        logic.execute("find Alice");
        logic.execute("sort");
        List<Person> visible = List.copyOf(model.getFilteredPersonList());
        var filters = model.getActiveFilters();
        byte[] saved = Files.readAllBytes(rosterFile);
        Path csv = directory.resolve("member list.csv");

        logic.execute("export " + csv);

        assertEquals("name,phone,email,address,tags\r\nZulu,00123,zulu@example.com,,old\r\n"
                + "Alice,456,alice@example.com,\"Block 1, \"\"Main\"\" Street\",A;z\r\n", Files.readString(csv));
        assertEquals(visible, model.getFilteredPersonList());
        assertEquals(filters, model.getActiveFilters());
        assertArrayEquals(saved, Files.readAllBytes(rosterFile));
        ModelManager restored = new ModelManager();
        Path restoredFile = directory.resolve("restored.json");
        logicFor(restored, restoredFile).execute("import p/\"" + csv + "\"");
        assertEquals(model.getAddressBook(), restored.getAddressBook());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(restoredFile).readAddressBook().orElseThrow());
    }

    @Test
    public void import_hiddenDuplicatesAndRepeatedRows_skipsDuplicatesWithoutMergingTags() throws Exception {
        // UG: duplicate checks include hidden members; trim contact fields and retain the first record's tags.
        logic.execute("find Nobody");
        logic.execute("sort");
        Path csv = directory.resolve("duplicates.csv");
        Files.writeString(csv, HEADER + " Zulu , 00123 , zulu@example.com , , replacement \n"
                + "Bob,789,bob@example.com,,first\nBob,789,bob@example.com,,second\n"
                + "Zulu,00123,zulu@example.com,,another\n");

        CommandResult result = logic.execute("import p/\"" + csv + "\"");

        assertTrue(result.getFeedbackToUser().startsWith("Imported 1 member(s)"));
        assertTrue(result.getFeedbackToUser().endsWith("Skipped 3 duplicate record(s)."));
        List<Person> people = model.getFilteredPersonList();
        assertEquals(List.of("Zulu", "Alice", "Bob"), people.stream().map(p -> p.getName().fullName).toList());
        assertEquals(Set.of("old"), tags(people.get(0)));
        assertEquals(Set.of("first"), tags(people.get(2)));
        assertTrue(model.getActiveFilters().isEmpty());
        assertEquals(model.getAddressBook(), rosterStorage.readAddressBook().orElseThrow());
    }

    @Test
    public void import_documentedBoundaryValues_acceptsMinimumsAndPreservesMeaningfulWhitespace() throws Exception {
        // UG: three digits, optional address, 30-character case-insensitive tags, and trimmed CSV values.
        Path csv = directory.resolve("boundaries.csv");
        Files.writeString(csv, "\uFEFFtags,address,email,phone,name\r\n"
                + "\" a ; A ; a ; " + "Z".repeat(30) + " \",\" \tHome\t  Block 1 \t\","
                + " a+b@example.co , 000 , Alice  Tan \r\n"
                + ",,b@example.co,123,B\r\n");

        logic.execute("import p/\"" + csv + "\"");

        List<Person> people = model.getAddressBook().getPersonList();
        Person alice = people.get(2);
        assertEquals("Alice  Tan", alice.getName().fullName);
        assertEquals("000", alice.getPhone().value);
        assertEquals("a+b@example.co", alice.getEmail().value);
        assertEquals("Home\t  Block 1", alice.getAddress().value);
        assertEquals(Set.of("a", "Z".repeat(30)), tags(alice));
        assertEquals("", people.get(3).getAddress().value);
        assertTrue(people.get(3).getTags().isEmpty());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(rosterFile).readAddressBook().orElseThrow());
    }

    @Test
    public void import_invalidUserFields_preservesRosterViewAndSavedData() throws Exception {
        // UG field rules and whole-file validation: no partial import and no view changes on rejection.
        List<String> invalidRows = List.of(
                ",123,a@example.com,Home,", "Al!ce,123,a@example.com,Home,",
                "\u0301,123,a@example.com,Home,", "Alice,12,a@example.com,Home,",
                "Alice,\u0661\u0662\u0663,a@example.com,Home,", "Alice,123-456,a@example.com,Home,",
                "Alice,123,a..b@example.com,Home,", "Alice,123,a@-example.com,Home,",
                "Alice,123,a@example.c,Home,", "Alice,123,a@example.com,Home," + "x".repeat(31),
                "Alice,123,a@example.com,Home,two words", "Alice,123,a@example.com,Home,tag;",
                "Alice,123,a@example.com,Home\" Street,", "Alice,123,a@example.com,\"Two\nlines\",");
        logic.execute("find Alice");
        logic.execute("sort");
        AddressBook before = new AddressBook(model.getAddressBook());
        List<Person> visible = List.copyOf(model.getFilteredPersonList());
        var filters = model.getActiveFilters();
        byte[] saved = Files.readAllBytes(rosterFile);
        Path csv = directory.resolve("invalid.csv");
        for (String invalid : invalidRows) {
            Files.writeString(csv, HEADER + "Valid,999,valid@example.com,,\n" + invalid);
            byte[] source = Files.readAllBytes(csv);

            CommandException error = assertThrows(
                    CommandException.class, () -> logic.execute("import p/\"" + csv + "\""),
                    invalid);

            assertTrue(error.getMessage().contains("row 3:"), error.getMessage());
            assertEquals(before, model.getAddressBook(), invalid);
            assertEquals(visible, model.getFilteredPersonList(), invalid);
            assertEquals(filters, model.getActiveFilters(), invalid);
            assertArrayEquals(saved, Files.readAllBytes(rosterFile), invalid);
            assertArrayEquals(source, Files.readAllBytes(csv), invalid);
        }
    }

    @Test
    public void import_saveFails_retriesUnsavedBatchUsingHeaderOnlyOrDuplicateOnlyFile() throws Exception {
        // UG: retain an unsaved batch in memory; even zero-import files retry saving after repair.
        for (boolean isDuplicateOnly : List.of(false, true)) {
            Path csv = directory.resolve("retry.csv");
            String row = "Carol,999,carol@example.com,,new\n";
            String name = isDuplicateOnly ? "Dora" : "Carol";
            row = row.replace("Carol", name).replace("carol", name.toLowerCase(Locale.ROOT));
            Files.writeString(csv, HEADER + row);
            Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
            byte[] saved = Files.readAllBytes(rosterFile);
            Files.delete(backup);
            Files.createDirectory(backup);
            Path blocker = backup.resolve("blocked.txt");
            Files.writeString(blocker, "Folder prevents automatic backup");

            CommandException error = assertThrows(
                    CommandException.class, () -> logic.execute("import p/\"" + csv + "\""));

            assertTrue(error.getMessage().startsWith("Could not save data to file:"));
            assertArrayEquals(saved, Files.readAllBytes(rosterFile));
            assertTrue(model.getAddressBook().getPersonList().stream()
                    .anyMatch(p -> p.getName().fullName.equals(name)));
            Files.delete(blocker);
            Files.delete(backup);
            Files.writeString(csv, HEADER + (isDuplicateOnly ? row : ""));

            CommandResult retried = logic.execute("import p/\"" + csv + "\"");

            assertTrue(retried.getFeedbackToUser().startsWith("Imported 0 member(s)"));
            assertEquals(model.getAddressBook(),
                    new JsonAddressBookStorage(rosterFile).readAddressBook().orElseThrow());
        }
    }

    private LogicManager logicFor(ModelManager sessionModel, Path file) throws Exception {
        JsonUserPrefsStorage prefs = new JsonUserPrefsStorage(directory.resolve("preferences.json"));
        prefs.saveUserPrefs(new UserPrefs());
        return new LogicManager(sessionModel, new StorageManager(new JsonAddressBookStorage(file), prefs));
    }

    private static Set<String> tags(Person person) {
        return person.getTags().stream().map(tag -> tag.tagName).collect(Collectors.toSet());
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

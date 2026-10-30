package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks ordinary name punctuation consistently across commands, CSV and persisted member records. */
public class CommonNamePunctuationWorkflowTest {
    private static final List<String> COMMON_NAMES = List.of("Ravi s/o Kumar", "Ravi S/O Kumar", "Ravi S/o Kumar",
            "Priya d/o Raman", "Priya D/O Raman", "Priya d/O Raman", "J. Tan", "J.Tan");

    @TempDir
    public Path temporaryFolder;

    @Test
    public void add_commonPunctuation_preservesExactNameAfterReload() {
        assertAll(COMMON_NAMES.stream().map(name -> () -> {
            Session session = new Session(temporaryFolder.resolve("add" + COMMON_NAMES.indexOf(name)));
            session.logic.execute(add(name));
            assertEquals(name, session.model.getAddressBook().getPersonList().getFirst().getName().fullName);
            assertEquals(name, session.saved().getFirst().getName().fullName);
        }));
    }

    @Test
    public void edit_commonPunctuation_preservesExactNameAndOtherFieldsAfterReload() {
        assertAll(COMMON_NAMES.stream().map(name -> () -> {
            Session session = new Session(temporaryFolder.resolve("edit" + COMMON_NAMES.indexOf(name)));
            session.logic.execute(add("Existing Member"));
            Person original = session.saved().getFirst();
            session.logic.execute("edit 1 n/" + name);
            Person edited = session.saved().getFirst();
            assertEquals(name, edited.getName().fullName);
            assertEquals(original.getPhone(), edited.getPhone());
            assertEquals(original.getEmail(), edited.getEmail());
            assertEquals(original.getAddress(), edited.getAddress());
            assertEquals(original.getTags(), edited.getTags());
        }));
    }

    @Test
    public void importExport_commonPunctuation_preservesBatchAndExactSpellingAcrossReloads() {
        assertAll(COMMON_NAMES.stream().map(name -> () -> {
            Path folder = temporaryFolder.resolve("csv" + COMMON_NAMES.indexOf(name));
            Session imported = new Session(folder);
            Path source = folder.resolve("members.csv");
            writeCsv(source, List.of(row("Valid New Member", "valid@example.com"), row(name, "member@example.com")));
            imported.logic.execute("import p/" + source);
            assertEquals(List.of("Valid New Member", name), names(imported.saved()));
            Path exported = folder.resolve("exported.csv");
            imported.logic.execute("export " + exported);
            assertEquals(List.of("Valid New Member", name), names(CsvAddressBookImporter.read(exported)));
            Session restored = new Session(folder.resolve("restored"));
            restored.logic.execute("import p/" + exported);
            assertEquals(imported.saved(), restored.saved());
            assertEquals(List.of("Valid New Member", name), names(restored.saved()));
        }));
    }

    @Test
    public void add_existingNameFormatsAndLiteralAddressSlashes_remainAccepted() throws Exception {
        Session session = new Session(temporaryFolder.resolve("controls"));
        List<String> controls = List.of("Anne-Marie", "O'Connor", "D’Arcy", "José Garcia", "Jose\u0301 Garcia", "李明");
        for (String name : controls) {
            session.logic.execute(add(name));
        }
        session.logic.execute("edit 1 a/Block A/12 c/o Reception");
        assertEquals(controls, names(session.saved()));
        assertEquals("Block A/12 c/o Reception", session.saved().getFirst().getAddress().value);
    }

    @Test
    public void addEdit_malformedPrefixes_remainRejectedWithoutChangingRecords() throws Exception {
        Session session = new Session(temporaryFolder.resolve("prefixes"));
        session.logic.execute(add("Existing Member"));
        byte[] saved = Files.readAllBytes(session.file);
        List<Person> original = session.saved();
        List<String> malformed = List.of("x/extra", "T/committee", "N/Other", "s/other", "d/order", "S/Other",
                "D/Other", "c/other", "s/oops", "d/other");
        for (String prefix : malformed) {
            assertThrows(ParseException.class, () -> session.logic.execute(
                    "add n/Alice " + prefix + " p/123 e/alice@example.com"), prefix);
            assertThrows(ParseException.class, () -> session.logic.execute("edit 1 n/Alice " + prefix), prefix);
            assertEquals(original, session.model.getAddressBook().getPersonList());
            assertArrayEquals(saved, Files.readAllBytes(session.file));
        }
        assertThrows(ParseException.class, () -> session.logic.execute("add N/Alice p/123 e/alice@example.com"));
        assertThrows(ParseException.class, () -> session.logic.execute("edit 1 N/Alice"));
    }

    @Test
    public void addEdit_invalidNames_remainRejectedWithoutChangingRecords() throws Exception {
        Session session = new Session(temporaryFolder.resolve("invalid"));
        session.logic.execute(add("Existing Member"));
        byte[] saved = Files.readAllBytes(session.file);
        List<Person> original = session.saved();
        for (String name : List.of("", " ", ".Tan", "/Ravi", "Ravi@Kumar", "Alice\tBob", "😀")) {
            assertThrows(ParseException.class, () -> session.logic.execute(add(name)), name);
            assertThrows(ParseException.class, () -> session.logic.execute("edit 1 n/" + name), name);
            assertEquals(original, session.model.getAddressBook().getPersonList());
            assertArrayEquals(saved, Files.readAllBytes(session.file));
        }
    }

    @Test
    public void duplicateNames_punctuationAndCaseVariants_keepExistingDuplicateRules() {
        List<String> names = List.of("Ravi s/o Kumar", "Priya d/o Raman", "J. Tan", "J.Tan");
        assertAll(IntStream.range(0, names.size()).mapToObj(index -> () -> {
            String name = names.get(index);
            String variant = name.toUpperCase(Locale.ROOT).replace(" ", "  ");
            Path folder = temporaryFolder.resolve("duplicates" + index);
            Session session = new Session(folder);
            session.logic.execute(add(name));
            session.logic.execute(add("Other Member"));
            byte[] saved = Files.readAllBytes(session.file);
            CommandException addFailure = assertThrows(CommandException.class, () ->
                    session.logic.execute(add(variant)));
            assertEquals(AddCommand.MESSAGE_DUPLICATE_PERSON, addFailure.getMessage());
            assertThrows(CommandException.class, () -> session.logic.execute("edit 2 n/" + variant));
            assertEquals(List.of(name, "Other Member"), names(session.model.getAddressBook().getPersonList()));
            assertArrayEquals(saved, Files.readAllBytes(session.file));
            Path csv = folder.resolve("duplicates.csv");
            writeCsv(csv, List.of(row(variant, "member@example.com"), row("Fresh Member", "fresh@example.com")));
            String feedback = session.logic.execute("import p/" + csv).getFeedbackToUser();
            assertTrue(feedback.contains("Skipped 1 duplicate record(s)."), feedback);
            assertEquals(List.of(name, "Other Member", "Fresh Member"), names(session.saved()));
        }));
    }

    @Test
    public void import_invalidRowAfterPunctuatedName_keepsWholeBatchAtomic() {
        assertAll(List.of("Ravi s/o Kumar", "Priya d/o Raman", "J.Tan").stream().map(name -> () -> {
            Path folder = temporaryFolder.resolve("atomic" + name.charAt(0));
            Session session = new Session(folder);
            session.logic.execute(add("Existing Member"));
            session.logic.execute("find Existing");
            byte[] saved = Files.readAllBytes(session.file);
            List<Person> original = session.saved();
            Path csv = folder.resolve("invalid.csv");
            writeCsv(csv, List.of(row(name, "member@example.com"), row("Invalid@Name", "invalid@example.com")));
            byte[] source = Files.readAllBytes(csv);
            CommandException failure = assertThrows(CommandException.class, () ->
                    session.logic.execute("import p/" + csv));
            assertTrue(failure.getMessage().contains("row 3"), failure.getMessage());
            assertTrue(failure.getMessage().contains("No members were imported"), failure.getMessage());
            assertEquals(original, session.model.getAddressBook().getPersonList());
            assertEquals(original, session.model.getFilteredPersonList());
            assertEquals(1, session.model.getActiveFilters().size());
            assertArrayEquals(saved, Files.readAllBytes(session.file));
            assertArrayEquals(source, Files.readAllBytes(csv));
        }));
    }

    private static String add(String name) {
        return "add n/" + name + " p/123 e/member@example.com a/Block 1 t/committee";
    }

    private static List<String> row(String name, String email) {
        return List.of(name, "123", email, "Block 1", "committee");
    }

    private static List<String> names(List<Person> people) {
        return people.stream().map(person -> person.getName().fullName).toList();
    }

    private static void writeCsv(Path file, List<List<String>> rows) throws IOException {
        StringWriter contents = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(contents, CSVFormat.RFC4180)) {
            printer.printRecord("name", "phone", "email", "address", "tags");
            printer.printRecords(rows);
        }
        Files.writeString(file, contents.toString());
    }

    /** A fresh application-logic instance using only disposable JSON and CSV files. */
    private static class Session {
        private final Path file;
        private final ModelManager model = new ModelManager();
        private final LogicManager logic;

        private Session(Path folder) throws IOException {
            Files.createDirectories(folder);
            file = folder.resolve("members.json");
            logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                    new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
        }

        private List<Person> saved() throws Exception {
            return new JsonAddressBookStorage(file).readAddressBook().orElseThrow().getPersonList();
        }
    }
}

package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks common contact formats through commands, persistence and CSV round trips. */
public class CommonContactFormatsWorkflowTest {
    @TempDir
    public Path directory;

    private Path roster;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() {
        roster = directory.resolve("roster.json");
        model = new ModelManager();
        logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(roster),
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));
    }

    @Test
    public void names_unicodeHyphensAndApostrophes_preservedThroughAddEditAndRoundTrip() throws Exception {
        List<String> names = List.of("Anne-Marie", "Jos\u00e9 Garcia", "Jose\u0301 Garcia", "O'Connor",
                "D\u2019Arcy", "\u674e\u660e");
        for (String name : names) {
            logic.execute("add n/" + name + " p/123 e/alice@example.com");
        }
        assertEquals(names, model.getAddressBook().getPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        logic.execute("edit 1 n/Ana-Mar\u00eda");
        assertEquals("Ana-Mar\u00eda", model.getAddressBook().getPersonList().getFirst().getName().fullName);
        assertRoundTrip();
    }

    @Test
    public void phone_countryCodeAndSpaces_preservedThroughAddEditAndRoundTrip() throws Exception {
        List<String> phones = List.of("+6591234567", "9123 4567", "+65 9123 4567");
        for (String phone : phones) {
            logic.execute("add n/Alice p/" + phone + " e/alice@example.com");
        }
        assertEquals(phones, model.getAddressBook().getPersonList().stream()
                .map(person -> person.getPhone().value).toList());
        logic.execute("edit 1 p/+44 20 1234 5678");
        assertEquals("+44 20 1234 5678", model.getAddressBook().getPersonList().getFirst().getPhone().value);
        assertRoundTrip();
    }

    @Test
    public void email_apostrophes_preservedThroughAddEditAndRoundTrip() throws Exception {
        logic.execute("add n/O Connor p/123 e/o'connor@example.com");
        assertEquals("o'connor@example.com", model.getAddressBook().getPersonList().getFirst().getEmail().value);
        logic.execute("edit 1 e/d'arcy@example.com");
        assertEquals("d'arcy@example.com", model.getAddressBook().getPersonList().getFirst().getEmail().value);
        assertRoundTrip();
    }

    @Test
    public void address_uppercaseUnitLabel_preservedWhileUnknownPrefixesAreRejected() throws Exception {
        logic.execute("add n/Alice p/123 e/alice@example.com a/Block A/12");
        logic.execute("add a/Block A/12 n/Bob p/456 e/bob@example.com");
        assertEquals("Block A/12", model.getAddressBook().getPersonList().getFirst().getAddress().value);
        logic.execute("edit 1 a/Block A/24 t/committee");
        assertEquals("Block A/24", model.getAddressBook().getPersonList().getFirst().getAddress().value);
        byte[] saved = Files.readAllBytes(roster);
        for (String prefix : List.of("x/extra", "T/committee", "P/12345678", "A/Block")) {
            assertThrows(ParseException.class, () -> logic.execute("edit 1 a/Block A/12 " + prefix));
            assertThrows(ParseException.class, () ->
                    logic.execute("add n/Other p/789 e/other@example.com a/Block A/12 " + prefix));
            assertArrayEquals(saved, Files.readAllBytes(roster));
        }
        assertThrows(ParseException.class, () -> logic.execute("add A/12 n/Other p/789 e/other@example.com"));
        assertRoundTrip();
    }

    /** Verifies JSON reload, CSV export, and CSV import preserve all contact values. */
    private void assertRoundTrip() throws Exception {
        var expected = List.copyOf(model.getAddressBook().getPersonList());
        assertEquals(expected, new JsonAddressBookStorage(roster).readAddressBook().orElseThrow().getPersonList());
        Path csv = directory.resolve("contacts.csv");
        logic.execute("export " + csv);
        assertEquals(expected, CsvAddressBookImporter.read(csv));
        ModelManager importedModel = new ModelManager();
        LogicManager importedLogic = new LogicManager(importedModel,
                new StorageManager(new JsonAddressBookStorage(directory.resolve("imported.json")),
                        new JsonUserPrefsStorage(directory.resolve("imported-prefs.json"))));
        importedLogic.execute("import p/\"" + csv + "\"");
        assertEquals(expected, importedModel.getAddressBook().getPersonList());
    }
}

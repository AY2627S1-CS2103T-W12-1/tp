package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks case-insensitive Unicode duplicate detection through commands and saved data. */
public class UnicodeContactWorkflowTest {
    @TempDir
    public Path folder;

    private ModelManager model;
    private Logic logic;
    private JsonAddressBookStorage storage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        storage = new JsonAddressBookStorage(folder.resolve("roster.json"));
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
    }

    @Test
    public void execute_addGreekCaseVariant_rejectsDuplicateWithoutSaving() throws Exception {
        logic.execute("add n/ΟΣ p/123 e/member@example.com");
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("add n/οσ p/123 e/member@example.com"));
        assertEquals(AddCommand.MESSAGE_DUPLICATE_PERSON, error.getMessage());
        assertEquals(1, model.getAddressBook().getPersonList().size());
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_editGreekCaseVariant_rejectsDuplicateWithoutChangingView() throws Exception {
        logic.execute("add n/ΟΣ p/123 e/member@example.com");
        logic.execute("add n/Other p/123 e/member@example.com");
        logic.execute("show 2");
        assertThrows(CommandException.class, () -> logic.execute("edit 1 n/οσ"));
        assertEquals("Other", model.getFilteredPersonList().getFirst().getName().fullName);
        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_importGreekCaseVariants_skipsDuplicatesAndKeepsFirstSpelling() throws Exception {
        Path csv = folder.resolve("variants.csv");
        Files.writeString(csv, "name,phone,email,address,tags\n"
                + "ΟΣ,123,member@example.com,,\nοσ,123,member@example.com,,\n"
                + "ος,123,member@example.com,,\n");
        assertTrue(logic.execute("import p/" + csv).getFeedbackToUser().contains("Skipped 2 duplicate record(s)."));
        assertEquals(1, storage.readAddressBook().orElseThrow().getPersonList().size());
        assertEquals("ΟΣ", model.getAddressBook().getPersonList().getFirst().getName().fullName);
    }
    @Test
    public void execute_addCanonicalVariants_rejectsDuplicateAndPreservesSpelling() throws Exception {
        logic.execute("add n/Zoe\u0308 p/123 e/member@example.com");
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        assertThrows(CommandException.class, () -> logic.execute("add n/Zoë p/123 e/member@example.com"));
        assertEquals("Zoe\u0308", storage.readAddressBook().orElseThrow()
                .getPersonList().getFirst().getName().fullName);
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_editCanonicalVariant_checksHiddenMembersAndAllowsSelfEdit() throws Exception {
        logic.execute("add n/Zoe\u0308 p/123 e/member@example.com");
        logic.execute("add n/Other p/123 e/member@example.com");
        logic.execute("show 2");
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        assertThrows(CommandException.class, () -> logic.execute("edit 1 n/Zoë"));
        assertEquals("Other", model.getFilteredPersonList().getFirst().getName().fullName);
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        logic.execute("list");
        logic.execute("edit 1 n/Zoë");
        assertEquals("Zoë", storage.readAddressBook().orElseThrow().getPersonList().getFirst().getName().fullName);
    }

    @Test
    public void execute_importCanonicalVariants_skipsExistingAndEarlierRows() throws Exception {
        logic.execute("add n/Zoe\u0308 p/123 e/member@example.com");
        Path csv = folder.resolve("canonical.csv");
        Files.writeString(csv, "name,phone,email,address,tags\n"
                + "Zoë,123,member@example.com,,\n"
                + "Rene\u0301,456,rene@example.com,,\nRené,456,rene@example.com,,\n");
        byte[] source = Files.readAllBytes(csv);
        String feedback = logic.execute("import p/" + csv).getFeedbackToUser();
        assertTrue(feedback.contains("Imported 1 member(s)"), feedback);
        assertTrue(feedback.contains("Skipped 2 duplicate record(s)."), feedback);
        assertEquals(List.of("Zoe\u0308", "Rene\u0301"), storage.readAddressBook().orElseThrow()
                .getPersonList().stream().map(person -> person.getName().fullName).toList());
        assertArrayEquals(source, Files.readAllBytes(csv));
    }

    @Test
    public void execute_findLegacyVariants_matchesBothWithoutRewritingSavedNames() throws Exception {
        // Legacy JSON identity remains literal, so loading cannot silently drop either pre-existing record.
        model.addPerson(new PersonBuilder().withName("Zoe\u0308").build());
        model.addPerson(new PersonBuilder().withName("Zoë").build());
        storage.saveAddressBook(model.getAddressBook());
        model = new ModelManager(storage.readAddressBook().orElseThrow(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        logic.execute("find Zoë");
        assertEquals(2, model.getFilteredPersonList().size());
        String label = model.getActiveFilters().getFirst().getLabel();
        logic.execute("find Zoe\u0308");
        assertEquals(2, model.getFilteredPersonList().size());
        assertEquals(1, model.getActiveFilters().size());
        assertEquals(label, model.getActiveFilters().getFirst().getLabel());
        logic.execute("list");
        logic.execute("find oe\u0308");
        assertEquals(2, model.getFilteredPersonList().size());
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        logic.execute("list");
        logic.execute("sort");
        assertEquals(List.of("Zoe\u0308", "Zoë"), storage.readAddressBook().orElseThrow()
                .getPersonList().stream().map(person -> person.getName().fullName).toList());
    }

}

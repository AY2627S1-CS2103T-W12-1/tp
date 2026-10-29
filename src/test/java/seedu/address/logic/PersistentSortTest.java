package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.SortCommand;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Verifies the saved ordering contract through actual commands, JSON storage and CSV export. */
public class PersistentSortTest {
    @TempDir
    public Path directory;

    private Path rosterFile;
    private JsonAddressBookStorage storage;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws Exception {
        rosterFile = directory.resolve("members.json");
        AddressBook roster = new AddressBook();
        roster.setPersons(List.of(member("Zara", "zara", "committee"), member("bob", "bob"),
                member("Alice", "alice", "committee")));
        storage = new JsonAddressBookStorage(rosterFile);
        storage.saveAddressBook(roster);
        restart(storage);
    }

    @Test
    public void sort_listRestartAndExport_preserveSortedRosterOrder() throws Exception {
        assertEquals("Sorted all 3 members by name (A to Z).", logic.execute("sort").getFeedbackToUser());
        logic.execute("list");
        assertNames(model.getFilteredPersonList(), "Alice", "bob", "Zara");
        assertNames(storage.readAddressBook(rosterFile).orElseThrow().getPersonList(), "Alice", "bob", "Zara");

        restart(new JsonAddressBookStorage(rosterFile));
        assertNames(model.getFilteredPersonList(), "Alice", "bob", "Zara");
        Path csv = directory.resolve("sorted.csv");
        logic.execute("export " + csv);
        assertNames(CsvAddressBookImporter.read(csv), "Alice", "bob", "Zara");
    }

    @Test
    public void sort_composedView_sortsHiddenMembersAndRetainsConditions() throws Exception {
        logic.execute("findemail example");
        logic.execute("find Zara Alice");
        logic.execute("filter t/committee");
        logic.execute("show 1 2");
        var conditions = model.getActiveFilters();

        logic.execute("sort");

        assertEquals(conditions, model.getActiveFilters());
        assertNames(model.getFilteredPersonList(), "Alice", "Zara");
        assertNames(model.getAddressBook().getPersonList(), "Alice", "bob", "Zara");
        logic.execute("list");
        assertNames(model.getFilteredPersonList(), "Alice", "bob", "Zara");
    }

    @Test
    public void sort_addImportAndRename_keepPositionsUntilNextSort() throws Exception {
        logic.execute("sort");
        logic.execute("add n/Aaron p/123 e/aaron@example.com");
        Path csv = directory.resolve("new.csv");
        Files.writeString(csv, "name,phone,email,address,tags\nAbel,456,abel@example.com,,\n");
        logic.execute("import p/" + csv);
        logic.execute("edit 3 n/Abe");

        assertNames(model.getFilteredPersonList(), "Alice", "bob", "Abe", "Aaron", "Abel");
        restart(new JsonAddressBookStorage(rosterFile));
        assertNames(model.getFilteredPersonList(), "Alice", "bob", "Abe", "Aaron", "Abel");
        logic.execute("sort");
        assertNames(model.getFilteredPersonList(), "Aaron", "Abe", "Abel", "Alice", "bob");
    }

    @Test
    public void sort_activeWriteFails_preservesDiskAndKeepsSortedSessionForRetry() throws Exception {
        Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
        Files.writeString(backup, "previous backup bytes");
        byte[] before = Files.readAllBytes(rosterFile);
        byte[] backupBefore = Files.readAllBytes(backup);
        JsonAddressBookStorage failing = new JsonAddressBookStorage(rosterFile) {
            private boolean failNextWrite = true;

            @Override
            protected void writeActiveRoster(ReadOnlyAddressBook roster, Path target) throws IOException {
                if (failNextWrite) {
                    failNextWrite = false;
                    throw new IOException("Injected sort save failure");
                }
                super.writeActiveRoster(roster, target);
            }
        };
        restart(failing);
        logic.execute("filter t/committee");
        var conditions = model.getActiveFilters();

        SaveFailureException failure = assertThrows(SaveFailureException.class, () -> logic.execute("sort"));

        assertTrue(failure.isChangeApplied());
        assertTrue(failure.getMessage().contains("Injected sort save failure"));
        assertNames(model.getAddressBook().getPersonList(), "Alice", "bob", "Zara");
        assertNames(model.getFilteredPersonList(), "Alice", "Zara");
        assertEquals(conditions, model.getActiveFilters());
        assertArrayEquals(before, Files.readAllBytes(rosterFile));
        assertArrayEquals(backupBefore, Files.readAllBytes(backup));
        logic.execute("sort");
        assertNames(failing.readAddressBook(rosterFile).orElseThrow().getPersonList(), "Alice", "bob", "Zara");
        assertArrayEquals(before, Files.readAllBytes(backup));
    }

    @Test
    public void sort_emptyOrAlreadySorted_stillAttemptsToSave() throws Exception {
        assertFalse(new SortCommand().isReadOnly());
        for (List<Person> members : List.of(List.<Person>of(), List.of(member("Alice", "single")))) {
            AddressBook roster = new AddressBook();
            roster.setPersons(members);
            storage.saveAddressBook(roster, rosterFile);
            JsonAddressBookStorage failing = new JsonAddressBookStorage(rosterFile) {
                @Override
                protected void writeActiveRoster(ReadOnlyAddressBook data, Path target) throws IOException {
                    throw new IOException("No-op sort still saves");
                }
            };
            restart(failing);

            SaveFailureException failure = assertThrows(SaveFailureException.class, () -> logic.execute("sort"));

            assertTrue(failure.getMessage().contains("No-op sort still saves"));
            assertEquals(members, model.getAddressBook().getPersonList());
        }
    }

    @Test
    public void sort_changedOutside_refusesReplacementAndAllowsExplicitRetry() throws Exception {
        AddressBook outside = new AddressBook();
        outside.addPerson(member("Outside", "outside"));
        new JsonAddressBookStorage(rosterFile).saveAddressBook(outside);
        byte[] outsideBytes = Files.readAllBytes(rosterFile);
        Path backup = JsonAddressBookStorage.getBackupFilePath(rosterFile);
        byte[] backupBefore = Files.readAllBytes(backup);

        SaveFailureException failure = assertThrows(SaveFailureException.class, () -> logic.execute("sort"));

        assertTrue(failure.isChangeApplied());
        assertTrue(failure.getMessage().contains("changed outside TrackCall"));
        assertArrayEquals(outsideBytes, Files.readAllBytes(rosterFile));
        assertArrayEquals(backupBefore, Files.readAllBytes(backup));
        logic.execute("sort");
        assertArrayEquals(outsideBytes, Files.readAllBytes(backup));
        assertNames(storage.readAddressBook(rosterFile).orElseThrow().getPersonList(), "Alice", "bob", "Zara");
    }

    /** Recreates the session from the actual saved file, as startup does. */
    private void restart(JsonAddressBookStorage currentStorage) throws Exception {
        model = new ModelManager(currentStorage.readAddressBook().orElseThrow(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(currentStorage,
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));
    }

    private Person member(String name, String email, String... tags) {
        return new PersonBuilder().withName(name).withEmail(email + "@example.com").withTags(tags).build();
    }

    private void assertNames(List<Person> people, String... expected) {
        assertEquals(List.of(expected), people.stream().map(person -> person.getName().fullName).toList());
    }
}

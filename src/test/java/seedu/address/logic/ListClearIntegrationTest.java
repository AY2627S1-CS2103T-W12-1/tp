package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Verifies Ian's list/clear commands across parsing, model state, and real JSON persistence.
 */
public class ListClearIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private RecordingStorage storage;
    private Logic logic;
    private Path dataFile;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        dataFile = temporaryFolder.resolve("members.json");
        storage = new RecordingStorage(dataFile);
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
    }

    @Test
    public void list_filteredRoster_restoresOrderAndCountWithoutSaving() throws Exception {
        List<Person> all = List.copyOf(model.getAddressBook().getPersonList());
        model.updateFilteredPersonList(all.getLast()::equals);
        storage.failure = new AccessDeniedException(dataFile.toString());

        String expected = String.format(ListCommand.MESSAGE_SUCCESS, all.size());
        assertEquals(expected, logic.execute(" \tlist \t").getFeedbackToUser());
        assertEquals(all, model.getFilteredPersonList());
        assertEquals(expected, logic.execute("list").getFeedbackToUser());
        assertEquals(0, storage.saveAttempts);
        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void list_noSearchMatches_restoresAllMembers() throws Exception {
        model.updateFilteredPersonList(person -> false);
        logic.execute("list");
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
        assertEquals(0, storage.saveAttempts);
    }

    @Test
    public void list_emptyRoster_reportsEmptyWithoutCreatingFile() throws Exception {
        model.setAddressBook(new AddressBook());
        assertEquals(ListCommand.MESSAGE_EMPTY, logic.execute("list").getFeedbackToUser());
        assertEquals(0, storage.saveAttempts);
        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void list_singleMember_reportsSingularAndPreservesFile() throws Exception {
        AddressBook single = singleMemberRoster();
        model.setAddressBook(single);
        Files.writeString(dataFile, "Existing file must not be read or rewritten by list.");
        String original = Files.readString(dataFile);

        assertEquals(ListCommand.MESSAGE_SINGLE, logic.execute("list").getFeedbackToUser());
        assertEquals(single.getPersonList(), model.getFilteredPersonList());
        assertEquals(original, Files.readString(dataFile));
        assertEquals(0, storage.saveAttempts);
    }

    @Test
    public void invalidListOrClear_preservesRosterViewAndFile() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        Person shown = before.getPersonList().getLast();
        model.updateFilteredPersonList(shown::equals);
        storage.saveAddressBook(before);
        storage.saveAttempts = 0;
        String saved = Files.readString(dataFile);

        for (String input : List.of("list 1", "list John", "list t/committee", "clear 1", "clear John",
                "clear t/committee", "clear stop", "LIST", "CLEAR", "li st", "cl ear")) {
            assertThrows(ParseException.class, () -> logic.execute(input));
            assertEquals(before, model.getAddressBook());
            assertEquals(List.of(shown), model.getFilteredPersonList());
            assertEquals(saved, Files.readString(dataFile));
        }
        assertEquals(0, storage.saveAttempts);
    }

    @Test
    public void clear_filteredRoster_savesEmptyAndResetsViewWithoutChangingSettings() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        model.updateFilteredPersonList(before.getPersonList().getLast()::equals);
        GuiSettings settings = new GuiSettings(800, 650, 100, 100);
        model.setGuiSettings(settings);

        String expected = String.format(ClearCommand.MESSAGE_SUCCESS, before.getPersonList().size());
        assertEquals(expected, logic.execute(" \tclear \t").getFeedbackToUser());
        assertEquals(new AddressBook(), model.getAddressBook());
        assertEquals(List.of(), model.getFilteredPersonList());
        assertEquals(settings, model.getGuiSettings());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
        assertEquals(1, storage.saveAttempts);

        // Reloading records directly exposes whether clear removed the old predicate.
        model.setAddressBook(before);
        assertEquals(before.getPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void clear_noSearchMatches_removesHiddenMembers() throws Exception {
        int total = model.getAddressBook().getPersonList().size();
        model.updateFilteredPersonList(person -> false);
        assertEquals(String.format(ClearCommand.MESSAGE_SUCCESS, total), logic.execute("clear").getFeedbackToUser());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void clear_singleMember_reportsSingularThenEmptyAndSavesBothTimes() throws Exception {
        model.setAddressBook(singleMemberRoster());
        assertEquals(ClearCommand.MESSAGE_SINGLE, logic.execute("clear").getFeedbackToUser());
        assertEquals(ClearCommand.MESSAGE_EMPTY, logic.execute("clear").getFeedbackToUser());
        assertEquals(2, storage.saveAttempts);
        assertEquals(ListCommand.MESSAGE_EMPTY, logic.execute("list").getFeedbackToUser());
        assertEquals(2, storage.saveAttempts);
    }

    @Test
    public void clear_emptyRoster_createsValidEmptyFile() throws Exception {
        model.setAddressBook(new AddressBook());
        assertEquals(ClearCommand.MESSAGE_EMPTY, logic.execute("clear").getFeedbackToUser());
        assertEquals(1, storage.saveAttempts);
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void clear_saveFailure_restoresRosterPredicateSettingsAndFile() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        Person shown = before.getPersonList().getLast();
        model.updateFilteredPersonList(shown::equals);
        GuiSettings settings = model.getGuiSettings();
        storage.saveAddressBook(before);
        String saved = Files.readString(dataFile);
        storage.saveAttempts = 0;

        for (IOException failure : List.of(new IOException("Disk full"),
                new AccessDeniedException(dataFile.toString()))) {
            storage.failure = failure;
            CommandException exception = assertThrows(CommandException.class, () -> logic.execute("clear"));
            assertEquals(ClearCommand.MESSAGE_SAVE_FAILURE, exception.getMessage());
            assertEquals(failure, exception.getCause());
            assertEquals(before, model.getAddressBook());
            assertEquals(List.of(shown), model.getFilteredPersonList());
            assertEquals(settings, model.getGuiSettings());
            assertEquals(saved, Files.readString(dataFile));
        }
        assertEquals(2, storage.saveAttempts);
        storage.failure = null;
        assertEquals(String.format(ClearCommand.MESSAGE_SUCCESS, before.getPersonList().size()),
                logic.execute("clear").getFeedbackToUser());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void clear_failedEmptySave_reportsFailureAndRetainsEmptyFilter() {
        model.setAddressBook(new AddressBook());
        model.updateFilteredPersonList(person -> false);
        storage.failure = new IOException("Disk full");
        CommandException exception = assertThrows(CommandException.class, () -> logic.execute("clear"));
        assertEquals(ClearCommand.MESSAGE_SAVE_FAILURE, exception.getMessage());
        assertEquals(1, storage.saveAttempts);
        model.setAddressBook(getTypicalAddressBook());
        assertEquals(List.of(), model.getFilteredPersonList());
        assertFalse(Files.exists(dataFile));
    }

    private AddressBook singleMemberRoster() {
        AddressBook single = new AddressBook();
        single.addPerson(getTypicalAddressBook().getPersonList().getFirst());
        return single;
    }

    /**
     * Counts persistence attempts and injects failures while retaining real JSON reads and successful writes.
     */
    private static class RecordingStorage extends JsonAddressBookStorage {
        private int saveAttempts;
        private IOException failure;

        RecordingStorage(Path file) {
            super(file);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            saveAttempts++;
            if (failure != null) {
                throw failure;
            }
            super.saveAddressBook(addressBook);
        }
    }
}

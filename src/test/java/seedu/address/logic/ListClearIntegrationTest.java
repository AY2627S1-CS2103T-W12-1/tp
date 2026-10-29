package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    public void clear_promptAndCancellation_preserveHiddenMembersViewAndSavedBytes() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        Person shown = before.getPersonList().getLast();
        model.updateFilteredPersonList(shown::equals);
        storage.saveAddressBook(before);
        String saved = Files.readString(dataFile);
        storage.saveAttempts = 0;

        for (String cancellation : List.of("", "n", "no", "exit", "clear", "delete 1")) {
            String prompt = logic.execute("clear").getFeedbackToUser();
            assertEquals(String.format(ClearCommand.MESSAGE_CONFIRMATION, before.getPersonList().size()), prompt);
            assertTrue(logic.isAwaitingConfirmation());
            assertEquals(before, model.getAddressBook());
            assertEquals(List.of(shown), model.getFilteredPersonList());
            assertEquals(saved, Files.readString(dataFile));
            assertEquals(0, storage.saveAttempts);

            String expectedCancellation = ClearCommand.MESSAGE_CANCELLED;
            if (List.of("exit", "clear", "delete 1").contains(cancellation)) {
                expectedCancellation += "\n\"" + cancellation + "\" was not run. Enter it again to run it.";
            }
            assertEquals(expectedCancellation, logic.execute(cancellation).getFeedbackToUser());
            assertFalse(logic.isAwaitingConfirmation());
            assertEquals(before, model.getAddressBook());
            assertEquals(List.of(shown), model.getFilteredPersonList());
            assertEquals(saved, Files.readString(dataFile));
            assertEquals(0, storage.saveAttempts);
            assertThrows(ParseException.class, () -> logic.execute("yes"));
        }
    }

    @Test
    public void clear_inexactConfirmation_remindsAndKeepsPromptPending() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        logic.execute("clear");

        assertEquals("To confirm, type yes exactly, in lowercase, and press Enter. To cancel, type no.",
                logic.execute(" y ").getFeedbackToUser());
        assertTrue(logic.isAwaitingConfirmation());
        assertEquals(before, model.getAddressBook());
        assertEquals(0, storage.saveAttempts);

        logic.execute("yes");
        assertFalse(logic.isAwaitingConfirmation());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
    }

    @Test
    public void clear_confirmationAfterFilterChange_removesEntireRosterOnlyOnce() throws Exception {
        int count = model.getAddressBook().getPersonList().size();
        logic.execute("clear");
        model.updateFilteredPersonList(person -> false);
        assertEquals(String.format(ClearCommand.MESSAGE_SUCCESS, count),
                logic.execute(" \tyes \t").getFeedbackToUser());
        assertFalse(logic.isAwaitingConfirmation());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
        assertThrows(ParseException.class, () -> logic.execute("yes"));
        assertEquals(1, storage.saveAttempts);
    }

    @Test
    public void clear_filteredRoster_savesEmptyAndResetsViewWithoutChangingSettings() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        model.updateFilteredPersonList(before.getPersonList().getLast()::equals);
        GuiSettings settings = new GuiSettings(800, 650, 100, 100);
        model.setGuiSettings(settings);

        String expected = String.format(ClearCommand.MESSAGE_SUCCESS, before.getPersonList().size());
        logic.execute(" \tclear \t");
        assertEquals(expected, logic.execute("yes").getFeedbackToUser());
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
        logic.execute("clear");
        assertEquals(String.format(ClearCommand.MESSAGE_SUCCESS, total), logic.execute("yes").getFeedbackToUser());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void clear_singleMember_reportsSingularThenEmptyAndSavesBothTimes() throws Exception {
        model.setAddressBook(singleMemberRoster());
        logic.execute("clear");
        assertEquals(ClearCommand.MESSAGE_SINGLE, logic.execute("yes").getFeedbackToUser());
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
            logic.execute("clear");
            CommandException exception = assertThrows(CommandException.class, () -> logic.execute("yes"));
            assertFalse(logic.isAwaitingConfirmation());
            assertThrows(ParseException.class, () -> logic.execute("yes"));
            assertEquals(ClearCommand.MESSAGE_SAVE_FAILURE, exception.getMessage());
            assertEquals(failure, exception.getCause());
            assertEquals(before, model.getAddressBook());
            assertEquals(List.of(shown), model.getFilteredPersonList());
            assertEquals(settings, model.getGuiSettings());
            assertEquals(saved, Files.readString(dataFile));
        }
        assertEquals(2, storage.saveAttempts);
        storage.failure = null;
        logic.execute("clear");
        assertEquals(String.format(ClearCommand.MESSAGE_SUCCESS, before.getPersonList().size()),
                logic.execute("yes").getFeedbackToUser());
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

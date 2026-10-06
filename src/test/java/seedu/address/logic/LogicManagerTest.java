package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.AMY;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.SortCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Tests command execution, persistence failures, and preservation of model state. */
public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_EMPTY, model);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage())
                + LogicManager.UNSAVED_CHANGE_GUIDANCE);
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage())
                + LogicManager.UNSAVED_CHANGE_GUIDANCE);
    }

    @Test
    public void execute_readOnlyCommands_doesNotCreateDataFile() throws Exception {
        for (String command : new String[] {"list", "sort", "find Amy", "help", "help add", "exit"}) {
            logic.execute(command);
            assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")), command);
        }
    }

    @Test
    public void execute_readOnlyAndInvalidCommands_preservesExistingFile() throws Exception {
        Path dataFile = temporaryFolder.resolve("addressBook.json");
        String original = "{ malformed data awaiting repair";
        Files.writeString(dataFile, original);
        model.addPerson(AMY);
        for (String command : new String[] {"list", "sort", "find Amy", "help", "help add", "exit"}) {
            logic.execute(command);
            assertEquals(original, Files.readString(dataFile), command);
        }
        assertThrows(ParseException.class, () -> logic.execute("add invalid"));
        assertThrows(ParseException.class, () -> logic.execute("unknown"));
        assertEquals(original, Files.readString(dataFile));
        assertEquals(1, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_readOnlyCommandsWithUnwritablePath_stillSucceeds() throws Exception {
        Files.createDirectory(temporaryFolder.resolve("addressBook.json"));
        for (String command : new String[] {"list", "sort", "find Amy", "help", "help add", "exit"}) {
            logic.execute(command);
        }
    }

    @Test
    public void execute_export_preservesExistingRosterFileContentsAndModificationTime() throws Exception {
        Path rosterFile = temporaryFolder.resolve("addressBook.json");
        String original = "{ malformed roster awaiting repair";
        Files.writeString(rosterFile, original);
        Files.setLastModifiedTime(rosterFile, FileTime.fromMillis(946684800000L));
        FileTime originalTime = Files.getLastModifiedTime(rosterFile);
        model.addPerson(AMY);
        Path csvFile = temporaryFolder.resolve("members.csv");

        logic.execute("export " + csvFile);

        assertTrue(Files.isRegularFile(csvFile));
        assertEquals(original, Files.readString(rosterFile));
        assertEquals(originalTime, Files.getLastModifiedTime(rosterFile));
        assertEquals(List.of(AMY), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_exportWithUnwritableRosterFile_preservesRosterAndFilter() throws Exception {
        Path rosterFile = temporaryFolder.resolve("addressBook.json");
        Files.createDirectory(rosterFile);
        model.addPerson(AMY);
        model.updateFilteredPersonList(unused -> false);
        Path csvFile = temporaryFolder.resolve("members.csv");

        CommandResult result = logic.execute("export " + csvFile);

        assertTrue(Files.isRegularFile(csvFile));
        assertTrue(Files.isDirectory(rosterFile));
        assertTrue(result.getFeedbackToUser().startsWith("Exported 1 people to "));
        assertEquals(List.of(AMY), model.getAddressBook().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_import_savesUpdatedRoster() throws Exception {
        Path csvFile = temporaryFolder.resolve("members.csv");
        Files.writeString(csvFile, "name,phone,email,address,tags\n"
                + "New Member,123,new@example.com,New address,committee\n");

        CommandResult result = logic.execute("import p/" + csvFile);

        Person imported = new PersonBuilder().withName("New Member").withPhone("123")
                .withEmail("new@example.com").withAddress("New address").withTags("committee").build();
        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, csvFile.toAbsolutePath().normalize(), 0),
                result.getFeedbackToUser());
        assertEquals(List.of(imported), model.getAddressBook().getPersonList());
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(
                temporaryFolder.resolve("addressBook.json")).readAddressBook().orElseThrow();
        assertEquals(List.of(imported), savedAddressBook.getPersonList());
    }

    @Test
    public void execute_importSaveFailure_keepsBatchInMemoryAndReportsImportMessage() throws Exception {
        Path csvFile = temporaryFolder.resolve("members.csv");
        String csvContents = "name,phone,email,address,tags\n"
                + "New Member,123,new@example.com,New address,\n";
        Files.writeString(csvFile, csvContents);
        useFailingStorage(DUMMY_IO_EXCEPTION);

        SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                SaveFailureException.class, () -> logic.execute("import p/" + csvFile));

        assertEquals(String.format(ImportCommand.MESSAGE_SAVE_FAILURE, DUMMY_IO_EXCEPTION.getMessage()),
                failure.getMessage());
        assertTrue(failure.isChangeApplied());
        assertEquals(1, model.getAddressBook().getPersonList().size());
        assertEquals(csvContents, Files.readString(csvFile));
    }

    @Test
    public void execute_importSaveFailureWithoutMessage_usesExceptionDescription() throws Exception {
        Path csvFile = temporaryFolder.resolve("members.csv");
        Files.writeString(csvFile, "name,phone,email,address,tags\n"
                + "New Member,123,new@example.com,New address,\n");
        IOException failureWithoutMessage = new IOException();
        useFailingStorage(failureWithoutMessage);

        SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                SaveFailureException.class, () -> logic.execute("import p/" + csvFile));

        assertEquals(String.format(ImportCommand.MESSAGE_SAVE_FAILURE, failureWithoutMessage), failure.getMessage());
        assertTrue(failure.isChangeApplied());
        assertEquals(1, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_sort_ordersFilteredViewIgnoringCaseAndListRestoresStoredOrder() throws Exception {
        Person amy = new PersonBuilder(AMY).withName("amy Bee").build();
        Person hidden = new PersonBuilder(AMY).withName("Hidden Member").build();
        model.addPerson(BOB);
        model.addPerson(amy);
        model.addPerson(hidden);
        model.updateFilteredPersonList(person -> !person.equals(hidden));

        assertEquals(SortCommand.MESSAGE_SUCCESS, logic.execute("sort").getFeedbackToUser());
        assertEquals(List.of(amy, BOB), model.getFilteredPersonList());
        assertEquals(List.of(BOB, amy, hidden), model.getAddressBook().getPersonList());
        logic.execute("sort");
        assertEquals(List.of(amy, BOB), model.getFilteredPersonList());
        logic.execute("list");
        assertEquals(List.of(BOB, amy, hidden), model.getFilteredPersonList());
    }

    @Test
    public void execute_sort_keepsLiveOrderingAndUsesDisplayedIndexes() throws Exception {
        model.addPerson(BOB);
        model.addPerson(AMY);
        logic.execute("sort");
        logic.execute("add n/Aaron p/91234567 e/aaron@example.com a/Orchard Road");
        Person aaron = model.getFilteredPersonList().get(0);
        assertEquals("Aaron", aaron.getName().fullName);
        assertEquals(List.of(aaron, AMY, BOB), model.getFilteredPersonList());

        logic.execute("edit 3 n/Abel");
        Person abel = new PersonBuilder(BOB).withName("Abel").build();
        assertEquals(List.of(aaron, abel, AMY), model.getFilteredPersonList());
        logic.execute("delete 2");
        assertEquals(List.of(aaron, AMY), model.getFilteredPersonList());
        assertEquals(List.of(AMY, aaron), model.getAddressBook().getPersonList());
        logic.execute("find Amy");
        assertEquals(List.of(AMY), model.getFilteredPersonList());
    }

    @Test
    public void execute_failedFilteredDelete_marksCommandAppliedAndRetainsOtherMembers() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        Person hidden = new PersonBuilder(AMY).withName("Hidden Member").build();
        model.addPerson(hidden);
        useFailingStorage(DUMMY_IO_EXCEPTION);
        logic.execute("find Amy Bob");

        SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                SaveFailureException.class, () -> logic.execute("delete 1"));

        assertTrue(failure.isChangeApplied());
        assertEquals(List.of(BOB, hidden), model.getAddressBook().getPersonList());
        assertEquals(List.of(BOB), model.getFilteredPersonList());
        assertTrue(failure.getMessage().contains("previous saved roster is unchanged"));
        // The command box clears applied commands. Submitting blank input cannot delete the next member.
        assertThrows(ParseException.class, () -> logic.execute(""));
        assertEquals(List.of(BOB, hidden), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_failedFilteredEdit_marksCommandAppliedAfterListChanges() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        useFailingStorage(DUMMY_IO_EXCEPTION);
        logic.execute("find Bob");

        SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                SaveFailureException.class, () -> logic.execute("edit 1 p/99988877"));

        assertTrue(failure.isChangeApplied());
        Person editedBob = new PersonBuilder(BOB).withPhone("99988877").build();
        assertEquals(List.of(AMY, editedBob), model.getFilteredPersonList());
    }

    @Test
    public void execute_failedClear_restoresRosterAndExistingFilter() throws Exception {
        for (IOException error : List.of(DUMMY_IO_EXCEPTION, DUMMY_AD_EXCEPTION)) {
            model = new ModelManager();
            model.addPerson(AMY);
            model.addPerson(BOB);
            AddressBook original = new AddressBook(model.getAddressBook());
            useFailingStorage(error);
            logic.execute("find Amy");

            SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                    SaveFailureException.class, () -> logic.execute("clear"));

            assertFalse(failure.isChangeApplied());
            assertEquals(original, model.getAddressBook());
            assertEquals(List.of(AMY), model.getFilteredPersonList());
            assertTrue(failure.getMessage().contains("No members were removed."));
        }
    }

    /** Replaces persistence with a deterministic failure after a command has changed the model. */
    private void useFailingStorage(IOException failure) {
        JsonAddressBookStorage addressStorage = new JsonAddressBookStorage(temporaryFolder.resolve("roster.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw failure;
            }
        };
        JsonUserPrefsStorage preferences = new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"));
        logic = new LogicManager(model, new StorageManager(addressStorage, preferences));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPersonList().remove(0));
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonAddressBookStorage that throws the IOException e when saving
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(prefPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveAddressBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        Person expectedPerson = new PersonBuilder(AMY).withTags().build();
        ModelManager expectedModel = new ModelManager();
        expectedModel.addPerson(expectedPerson);
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
}

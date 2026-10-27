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
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.AMY;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.BOB;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.ExportCommand;
import seedu.address.logic.commands.FindEmailCommand;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.RenameTagCommand;
import seedu.address.logic.commands.SortCommand;
import seedu.address.logic.commands.TagAllCommand;
import seedu.address.logic.commands.UntagAllCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.storage.CsvAddressBookExporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

/** Tests command execution, persistence failures, and preservation of model state. */
public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");
    private static final String[] READ_ONLY_COMMANDS = {
        "list", "sort", "find Amy", "find m", "findemail @example.com", "findemail absent@", "filter t/friend",
        "help", "help add", "help filter", "help findemail", "exit"
    };

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
    public void execute_saveFailure_logsOperationAndTypeWithoutPrivateExceptionContents() {
        useFailingStorage(new IOException("private.member@example.com", new IOException("Private Road")));
        StringBuilder records = new StringBuilder();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.append(new SimpleFormatter().format(record));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger logger = Logger.getLogger("ab3");
        logger.addHandler(capture);
        try {
            assertThrows(SaveFailureException.class, () -> logic.execute(PersonUtil.getAddCommand(AMY)));
            assertTrue(records.toString().contains("Could not save roster after AddCommand (IOException)"));
            assertFalse(records.toString().contains("private.member@example.com"));
            assertFalse(records.toString().contains("Private Road"));
        } finally {
            logger.removeHandler(capture);
        }
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_deleteThenConfirm_deletesNamedMemberAndSaves() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);

        CommandResult prompt = logic.execute("delete 2");
        assertEquals(String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE, BOB.getName(), Messages.formatTags(BOB)),
                prompt.getFeedbackToUser());
        assertTrue(logic.isAwaitingConfirmation());
        // Asking for confirmation neither removes the member nor saves the roster.
        assertEquals(List.of(AMY, BOB), model.getAddressBook().getPersonList());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));

        CommandResult result = logic.execute(" y ");
        assertEquals(String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS, Messages.format(BOB)),
                result.getFeedbackToUser());
        assertFalse(logic.isAwaitingConfirmation());
        assertEquals(List.of(AMY), model.getAddressBook().getPersonList());
        ReadOnlyAddressBook saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(List.of(AMY), saved.getPersonList());
    }

    @Test
    public void execute_deleteThenOtherInput_cancelsWithoutRunningInput() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        String[] cancellingInputs = {"n", "Y", "yes", "", "clear", "delete 1"};

        for (String input : cancellingInputs) {
            logic.execute("delete 1");
            CommandResult result = logic.execute(input);

            assertEquals(ConfirmDeleteCommand.MESSAGE_CANCELLED, result.getFeedbackToUser(), input);
            assertFalse(logic.isAwaitingConfirmation(), input);
            assertEquals(List.of(AMY, BOB), model.getAddressBook().getPersonList(), input);
        }
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_confirmationWordWithoutPendingDeletion_isUnknownCommand() {
        model.addPerson(AMY);

        assertParseException("y", MESSAGE_UNKNOWN_COMMAND);
        assertEquals(List.of(AMY), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_invalidDeleteIndex_doesNotAwaitConfirmation() {
        model.addPerson(AMY);

        assertCommandException("delete 2", MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertFalse(logic.isAwaitingConfirmation());
    }

    @Test
    public void execute_addAfterLoadingPaddedContact_rejectsDuplicateWithoutSaving() throws Exception {
        Person padded = new PersonBuilder(BOB).withName(BOB.getName().fullName + " ")
                .withAddress(BOB.getAddress().value + " ").build();
        model.addPerson(padded);
        assertThrows(CommandException.class, () -> logic.execute(PersonUtil.getAddCommand(BOB)));
        assertEquals(List.of(padded), model.getAddressBook().getPersonList());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_importAfterLoadingPaddedContact_skipsDuplicate() throws Exception {
        Person padded = new PersonBuilder(BOB).withName(BOB.getName().fullName + " ").build();
        model.addPerson(padded);
        Path source = temporaryFolder.resolve("member.csv");
        CsvAddressBookExporter.export(List.of(BOB), source);
        CommandResult result = logic.execute("import p/\"" + source + "\"");
        assertTrue(result.getFeedbackToUser().startsWith("Imported 0 member(s)"));
        assertTrue(result.getFeedbackToUser().endsWith("Skipped 1 duplicate record(s)."));
        assertEquals(List.of(padded), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_nonAsciiIndices_rejectsWithoutChangingRosterOrView() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        model.updateFilteredPersonList(AMY::equals);
        for (String command : List.of("delete \u0661", "delete \uFF11", "edit \u0661 n/Changed")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(List.of(AMY, BOB), model.getAddressBook().getPersonList());
            assertEquals(List.of(AMY), model.getFilteredPersonList());
            assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
        }
    }

    @Test
    public void execute_editWithUnsupportedPrefix_preservesRosterViewAndSavedFile() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        Path dataFile = temporaryFolder.resolve("addressBook.json");
        new JsonAddressBookStorage(dataFile).saveAddressBook(model.getAddressBook());
        String original = Files.readString(dataFile);
        FileTime originalModifiedTime = Files.getLastModifiedTime(dataFile);
        logic.execute("find Amy");

        String expectedMessage = String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE);
        for (String arguments : List.of("a/Main Road T/committee", "a/Main Road\tx/extra", "n/Changed c/office")) {
            assertThrows(ParseException.class, expectedMessage, () -> logic.execute("edit 1 " + arguments));
            assertEquals(List.of(AMY, BOB), model.getAddressBook().getPersonList());
            assertEquals(List.of(AMY), model.getFilteredPersonList());
            assertEquals(original, Files.readString(dataFile));
            assertEquals(originalModifiedTime, Files.getLastModifiedTime(dataFile));
        }
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
        for (String command : READ_ONLY_COMMANDS) {
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
        for (String command : READ_ONLY_COMMANDS) {
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
        for (String command : READ_ONLY_COMMANDS) {
            logic.execute(command);
        }
    }

    @Test
    public void execute_findEmail_resetsSearchAndTagsWithoutChangingSavedFiles() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        Path dataFile = temporaryFolder.resolve("addressBook.json");
        Path backupFile = temporaryFolder.resolve("addressBook.json.bak");
        new JsonAddressBookStorage(dataFile).saveAddressBook(model.getAddressBook());
        Files.writeString(backupFile, "Preserve the previous backup");
        FileTime originalTime = FileTime.fromMillis(946684800000L);
        Files.setLastModifiedTime(dataFile, originalTime);
        Files.setLastModifiedTime(backupFile, originalTime);
        String originalData = Files.readString(dataFile);
        logic.execute("find Bob");
        logic.execute("filter t/husband");
        assertEquals(List.of(BOB), model.getFilteredPersonList());

        assertEquals("1 member listed.", logic.execute("findemail AMY@").getFeedbackToUser());
        assertEquals(List.of(AMY), model.getFilteredPersonList());
        assertTrue(model.getActiveTagFilters().isEmpty());
        assertEquals("0 members listed.", logic.execute("findemail missing@").getFeedbackToUser());
        assertEquals("2 members listed.", logic.execute("findemail @example.com").getFeedbackToUser());

        assertEquals(List.of(AMY, BOB), model.getAddressBook().getPersonList());
        assertEquals(originalData, Files.readString(dataFile));
        assertEquals("Preserve the previous backup", Files.readString(backupFile));
        assertEquals(originalTime, Files.getLastModifiedTime(dataFile));
        assertEquals(originalTime, Files.getLastModifiedTime(backupFile));
    }

    @Test
    public void execute_findEmail_retainsSortingAndUpdatesLiveResultsAfterEdits() throws Exception {
        model.addPerson(BOB);
        model.addPerson(AMY);
        logic.execute("sort");
        logic.execute("find Bob");
        logic.execute("filter t/husband");

        logic.execute("findemail @example.com");
        assertEquals(List.of(AMY, BOB), model.getFilteredPersonList());
        assertEquals(List.of(BOB, AMY), model.getAddressBook().getPersonList());
        logic.execute("filter t/husband");
        assertEquals(List.of(BOB), model.getFilteredPersonList());
        logic.execute("edit 1 p/9876543");
        Person updatedBob = new PersonBuilder(BOB).withPhone("9876543").build();
        assertEquals(List.of(updatedBob), model.getFilteredPersonList());
        logic.execute("edit 1 e/bob@another.org");
        Person changedEmail = new PersonBuilder(updatedBob).withEmail("bob@another.org").build();
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(changedEmail, AMY), model.getAddressBook().getPersonList());

        logic.execute("list");
        assertEquals(List.of(changedEmail, AMY), model.getFilteredPersonList());
    }

    @Test
    public void execute_blankEmailSearch_preservesCurrentViewAndStorage() throws Exception {
        model.addPerson(BOB);
        model.addPerson(AMY);
        logic.execute("sort");
        logic.execute("find Bob");
        logic.execute("filter t/husband");
        String expected = String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, FindEmailCommand.MESSAGE_USAGE);

        for (String command : List.of("findemail", "findemail \t  ")) {
            assertThrows(ParseException.class, expected, () -> logic.execute(command));
            assertEquals(List.of(BOB), model.getFilteredPersonList());
            assertEquals(List.of(new Tag("husband")), model.getActiveTagFilters());
            assertEquals(List.of(BOB, AMY), model.getAddressBook().getPersonList());
            assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
            assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json.bak")));
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
        assertTrue(result.getFeedbackToUser().startsWith("Exported 1 member to "));
        assertEquals(List.of(AMY), model.getAddressBook().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_exportToApplicationStorage_rejectsWithoutChangingFilesOrView() throws Exception {
        model.addPerson(AMY);
        model.updateFilteredPersonList(unused -> false);
        for (String filename : List.of("addressBook.json", "userPrefs.json", "addressBook.json.bak",
                "addressBook.json.corrupt-123.json", "ADDRESSBOOK.JSON.CORRUPT-456.JSON")) {
            Path target = temporaryFolder.resolve(filename);
            Files.writeString(target, "Keep the existing application file");
            assertThrows(ParseException.class, () -> logic.execute("export " + target));
            assertThrows(CommandException.class, () -> new ExportCommand(target).execute(model,
                    temporaryFolder.resolve("addressBook.json"), temporaryFolder.resolve("userPrefs.json")));
            assertEquals("Keep the existing application file", Files.readString(target));
            assertEquals(List.of(AMY), model.getAddressBook().getPersonList());
            assertTrue(model.getFilteredPersonList().isEmpty());
        }
    }

    @Test
    public void execute_exportToNormalisedStoragePath_rejectsBeforeCreatingFile() throws Exception {
        Path target = temporaryFolder.resolve("nested/../addressBook.json");
        assertThrows(ParseException.class, () -> logic.execute("export " + target));
        assertThrows(CommandException.class, () -> new ExportCommand(target).execute(model,
                temporaryFolder.resolve("addressBook.json"), temporaryFolder.resolve("userPrefs.json")));
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
        assertFalse(Files.exists(temporaryFolder.resolve("nested")));
    }

    @Test
    public void execute_exportToStorageHardLink_rejectsWithoutChangingOriginal() throws Exception {
        Path dataFile = temporaryFolder.resolve("addressBook.json");
        Files.writeString(dataFile, "Keep the original roster");
        Path alias = temporaryFolder.resolve("alias.csv");
        try {
            Files.createLink(alias, dataFile);
        } catch (IOException | UnsupportedOperationException error) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Hard links are unavailable: " + error.getMessage());
        }

        assertThrows(CommandException.class, () -> logic.execute("export " + alias));

        assertEquals("Keep the original roster", Files.readString(dataFile));
        assertEquals("Keep the original roster", Files.readString(alias));
    }

    @Test
    public void execute_exportToExistingCsvBeforeFirstSave_preservesMissingStorageFiles() throws Exception {
        Path destination = temporaryFolder.resolve("members.csv");
        Files.writeString(destination, "Old CSV contents");

        logic.execute("export " + destination);

        assertEquals("name,phone,email,address,tags\r\n", Files.readString(destination));
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
        assertFalse(Files.exists(temporaryFolder.resolve("userPrefs.json")));
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

        assertEquals(String.format(ImportCommand.MESSAGE_SAVE_FAILURE, DUMMY_IO_EXCEPTION.getMessage())
                + LogicManager.UNSAVED_CHANGE_GUIDANCE,
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

        assertEquals(String.format(ImportCommand.MESSAGE_SAVE_FAILURE, failureWithoutMessage)
                + LogicManager.UNSAVED_CHANGE_GUIDANCE, failure.getMessage());
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
        logic.execute("y");
        assertEquals(List.of(aaron, AMY), model.getFilteredPersonList());
        assertEquals(List.of(AMY, aaron), model.getAddressBook().getPersonList());
        logic.execute("find Amy");
        assertEquals(List.of(AMY), model.getFilteredPersonList());
    }

    @Test
    public void execute_filterByYearAndCommittee_narrowsAndResetsView() throws Exception {
        AddressBook clubRoster = getTypicalAddressBook();
        Person alice = new PersonBuilder(ALICE).withTags("year1").build();
        Person committeeMember = new PersonBuilder(BENSON).withTags("year1", "committee").build();
        Person daniel = new PersonBuilder(DANIEL).withTags("year1").build();
        clubRoster.setPerson(ALICE, alice);
        clubRoster.setPerson(BENSON, committeeMember);
        clubRoster.setPerson(DANIEL, daniel);
        model.setAddressBook(clubRoster);

        assertEquals("3 member(s) listed with tag \"year1\".",
                logic.execute("filter t/year1").getFeedbackToUser());
        assertEquals(List.of(alice, committeeMember, daniel), logic.getFilteredPersonList());
        assertEquals("1 member(s) listed with tags \"year1\", \"committee\".",
                logic.execute("filter t/committee").getFeedbackToUser());
        assertEquals(List.of(committeeMember), logic.getFilteredPersonList());
        assertEquals("0 member(s) listed with tags \"year1\", \"committee\", \"missing\".",
                logic.execute("filter t/missing").getFeedbackToUser());
        assertEquals(List.of(), logic.getFilteredPersonList());

        logic.execute("find Alice");
        assertEquals(List.of(), logic.getFilteredPersonList());
        logic.removeActiveFilter(logic.getActiveFilters().get(2));
        assertEquals(List.of(), logic.getFilteredPersonList());
        logic.removeActiveFilter(logic.getActiveFilters().get(1));
        assertEquals(List.of(alice), logic.getFilteredPersonList());
        assertEquals("1 member(s) listed with tag \"year1\".",
                logic.execute("filter t/year1").getFeedbackToUser());
        assertEquals(List.of(alice), logic.getFilteredPersonList());
        assertEquals("1 member(s) listed with tag \"year1\".",
                logic.execute("filter t/year1").getFeedbackToUser());
        logic.execute("list");
        assertEquals(clubRoster.getPersonList(), logic.getFilteredPersonList());
        assertEquals("3 member(s) listed with tag \"Year1\".",
                logic.execute("filter t/Year1").getFeedbackToUser());
        assertEquals(List.of(alice, committeeMember, daniel), logic.getFilteredPersonList());
    }

    @Test
    public void execute_filterAfterSort_retainsNameOrderWithoutChangingStoredOrder() throws Exception {
        Person amy = new PersonBuilder(AMY).withTags("committee").build();
        Person bob = new PersonBuilder(BOB).withTags("committee").build();
        model.addPerson(bob);
        model.addPerson(ALICE);
        model.addPerson(amy);

        logic.execute("sort");
        assertEquals("2 member(s) listed with tag \"committee\".",
                logic.execute("filter t/committee").getFeedbackToUser());
        assertEquals(List.of(amy, bob), logic.getFilteredPersonList());
        assertEquals(List.of(bob, ALICE, amy), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_caseVariantTags_matchAcrossCommandsAndSave() throws Exception {
        logic.execute("add n/Alice p/123 e/alice@example.com t/Student t/student");
        logic.execute("add n/Bob p/456 e/bob@example.com t/student");
        assertEquals(1, model.getAddressBook().getPersonList().getFirst().getTags().size());

        logic.execute("edit 1 t/Student t/STUDENT");
        assertEquals(1, model.getAddressBook().getPersonList().getFirst().getTags().size());

        assertEquals("2 member(s) listed with tag \"STUDENT\".",
                logic.execute("filter t/STUDENT").getFeedbackToUser());
        assertEquals(2, logic.getFilteredPersonList().size());
        assertEquals(1, model.getActiveTagFilters().size());
        logic.execute("filter t/student");
        assertEquals(1, model.getActiveTagFilters().size());

        logic.execute("tag 1 t/student");
        assertEquals(1, model.getFilteredPersonList().getFirst().getTags().size());
        assertEquals("Added tag \"StUdEnT\" to 0 member(s). 2 already had this tag.",
                logic.execute("tagall t/StUdEnT").getFeedbackToUser());

        logic.execute("untag 1 t/sTuDeNt");
        assertEquals(1, logic.getFilteredPersonList().size());
        logic.execute("list");
        assertEquals("Renamed tag \"STUDENT\" to \"Alumni\" for 1 member(s).",
                logic.execute("renametag t/STUDENT t/Alumni").getFeedbackToUser());
        assertEquals("Removed tag \"ALUMNI\" from 1 member(s). 1 did not have this tag.",
                logic.execute("untagall t/ALUMNI").getFeedbackToUser());
        assertTrue(model.getAddressBook().getPersonList().stream().allMatch(person -> person.getTags().isEmpty()));

        ReadOnlyAddressBook saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook().getPersonList(), saved.getPersonList());
    }

    @Test
    public void execute_invalidFilter_preservesDisplayedList() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/friends");
        assertThrows(ParseException.class, () -> logic.execute("filter t/friends t/committee"));
        assertEquals(List.of(ALICE, BENSON, DANIEL), logic.getFilteredPersonList());
    }

    @Test
    public void execute_tagAllOnFilteredList_tagsDisplayedMembersAndSaves() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/friends");

        assertEquals("Added tag \"committee\" to 3 member(s).",
                logic.execute("tagall t/committee").getFeedbackToUser());

        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(
                temporaryFolder.resolve("addressBook.json")).readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook().getPersonList(), savedAddressBook.getPersonList());
        Tag committee = new Tag("committee");
        for (Person person : model.getAddressBook().getPersonList()) {
            boolean wasDisplayed = person.isSamePerson(ALICE) || person.isSamePerson(BENSON)
                    || person.isSamePerson(DANIEL);
            assertEquals(wasDisplayed, person.getTags().contains(committee), person.getName().fullName);
        }
    }

    @Test
    public void execute_tagOnFilteredList_updatesOneMemberAndSaves() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/friends");

        logic.execute("tag 2 t/committee");

        assertTrue(model.getFilteredPersonList().get(1).getTags().contains(new Tag("committee")));
        assertFalse(model.getFilteredPersonList().get(0).getTags().contains(new Tag("committee")));
        ReadOnlyAddressBook saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook().getPersonList(), saved.getPersonList());
    }

    @Test
    public void execute_tagWithExistingTag_stillSaves() throws Exception {
        model.addPerson(ALICE);

        logic.execute("tag 1 t/friends");

        assertTrue(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_tagAllWithNoChanges_stillSaves() throws Exception {
        model.addPerson(BOB);
        Path dataFile = temporaryFolder.resolve("addressBook.json");

        assertEquals("Added tag \"friend\" to 0 member(s). 1 already had this tag.",
                logic.execute("tagall t/friend").getFeedbackToUser());
        assertTrue(Files.exists(dataFile));
    }

    @Test
    public void execute_tagAllOnEmptyList_doesNotSave() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/missing");

        assertThrows(CommandException.class, TagAllCommand.MESSAGE_EMPTY_LIST, () ->
                logic.execute("tagall t/committee"));
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_untagAllFilteredTag_hidesUpdatedMembersAndSaves() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/friends");

        assertEquals("Removed tag \"friends\" from 3 member(s).",
                logic.execute("untagall t/friends").getFeedbackToUser());

        // The filter stays applied, so the updated members are hidden but still stored.
        assertEquals(List.of(), logic.getFilteredPersonList());
        Tag friends = new Tag("friends");
        assertEquals(getTypicalAddressBook().getPersonList().size(), model.getAddressBook().getPersonList().size());
        assertTrue(model.getAddressBook().getPersonList().stream().noneMatch(person ->
                person.getTags().contains(friends)));
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(
                temporaryFolder.resolve("addressBook.json")).readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook().getPersonList(), savedAddressBook.getPersonList());
    }

    @Test
    public void execute_untagOnFilteredList_updatesOneMemberAndSaves() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/friends");

        logic.execute("untag 2 t/friends");

        assertEquals(List.of(ALICE, DANIEL), logic.getFilteredPersonList());
        assertFalse(model.getAddressBook().getPersonList().get(1).getTags().contains(new Tag("friends")));
        ReadOnlyAddressBook saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook().getPersonList(), saved.getPersonList());
    }

    @Test
    public void execute_untagWithMissingTag_stillSaves() throws Exception {
        model.addPerson(ALICE);

        logic.execute("untag 1 t/committee");

        assertTrue(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_untagAllWithNoChanges_stillSaves() throws Exception {
        model.addPerson(BOB);
        Path dataFile = temporaryFolder.resolve("addressBook.json");

        assertEquals("Removed tag \"committee\" from 0 member(s). 1 did not have this tag.",
                logic.execute("untagall t/committee").getFeedbackToUser());
        assertTrue(Files.exists(dataFile));
    }

    @Test
    public void execute_untagAllOnEmptyList_doesNotSave() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("filter t/missing");

        assertThrows(CommandException.class, UntagAllCommand.MESSAGE_EMPTY_LIST, () ->
                logic.execute("untagall t/friends"));
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_renameTagWithActiveSearch_renamesWholeRosterAndSaves() throws Exception {
        model.setAddressBook(getTypicalAddressBook());
        logic.execute("find Alice");

        assertEquals("Renamed tag \"friends\" to \"buddies\" for 3 member(s).",
                logic.execute("renametag t/friends t/buddies").getFeedbackToUser());

        // The search stays applied, while hidden members are renamed too.
        assertEquals(1, logic.getFilteredPersonList().size());
        Tag friends = new Tag("friends");
        assertTrue(model.getAddressBook().getPersonList().stream().noneMatch(person ->
                person.getTags().contains(friends)));
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(
                temporaryFolder.resolve("addressBook.json")).readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook().getPersonList(), savedAddressBook.getPersonList());
    }

    @Test
    public void execute_caseOnlyRename_savesRequestedSpelling() throws Exception {
        model.addPerson(new PersonBuilder().withName("Alice").withTags("student").build());
        model.addPerson(new PersonBuilder().withName("Bob").withTags("Student").build());

        assertEquals("Renamed tag \"STUDENT\" to \"Student\" for 1 member(s).",
                logic.execute("renametag t/STUDENT t/Student").getFeedbackToUser());

        ReadOnlyAddressBook saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertTrue(saved.getPersonList().stream().allMatch(person ->
                person.getTags().stream().anyMatch(tag -> tag.tagName.equals("Student"))));
    }

    @Test
    public void execute_renameUnusedTag_doesNotSave() throws Exception {
        model.setAddressBook(getTypicalAddressBook());

        assertThrows(CommandException.class, String.format(RenameTagCommand.MESSAGE_TAG_NOT_FOUND, "committee"), () ->
                logic.execute("renametag t/committee t/buddies"));
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_failedFilteredDelete_marksCommandAppliedAndRetainsOtherMembers() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        Person hidden = new PersonBuilder(AMY).withName("Hidden Member").build();
        model.addPerson(hidden);
        useFailingStorage(DUMMY_IO_EXCEPTION);
        logic.execute("find Amy Bob");

        logic.execute("delete 1");
        SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                SaveFailureException.class, () -> logic.execute("y"));

        assertTrue(failure.isChangeApplied());
        assertFalse(logic.isAwaitingConfirmation());
        assertEquals(List.of(BOB, hidden), model.getAddressBook().getPersonList());
        assertEquals(List.of(BOB), model.getFilteredPersonList());
        assertTrue(failure.getMessage().contains("previous saved roster is unchanged"));
        // The command box clears applied commands. Submitting blank input cannot delete the next member.
        assertThrows(ParseException.class, () -> logic.execute(""));
        assertEquals(List.of(BOB, hidden), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_failedFilteredEdit_marksCommandAppliedAndRetainsFilter() throws Exception {
        model.addPerson(AMY);
        model.addPerson(BOB);
        useFailingStorage(DUMMY_IO_EXCEPTION);
        logic.execute("find Bob");

        SaveFailureException failure = org.junit.jupiter.api.Assertions.assertThrows(
                SaveFailureException.class, () -> logic.execute("edit 1 p/99988877"));

        assertTrue(failure.isChangeApplied());
        Person editedBob = new PersonBuilder(BOB).withPhone("99988877").build();
        assertEquals(List.of(editedBob), model.getFilteredPersonList());
        assertEquals(List.of(AMY, editedBob), model.getAddressBook().getPersonList());
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

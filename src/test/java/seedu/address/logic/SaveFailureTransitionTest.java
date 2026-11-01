package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Checks User Guide save-error recovery across commands, real files, and fresh storage sessions.
 * Faults use a nonempty backup directory instead of OS-dependent permission changes.
 */
public class SaveFailureTransitionTest {
    @TempDir
    public Path directory;

    private ModelManager model;
    private LogicManager logic;
    private Path roster;
    private Path backup;
    private byte[] previousBackup;

    @BeforeEach
    public void setUp() throws Exception {
        roster = directory.resolve("roster.json");
        backup = JsonAddressBookStorage.getBackupFilePath(roster);
        model = new ModelManager(new AddressBook(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(roster),
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));
        logic.execute("add n/Alice p/111 e/alice@example.com t/team");
        logic.execute("add n/Bob p/222 e/bob@example.com t/team");
        logic.execute("add n/Cara p/333 e/cara@example.com t/other");
    }

    @Test
    public void edit_saveFailsThenStorageRepaired_readOnlyCommandsDoNotFlushUnsavedChanges() throws Exception {
        // EP: repair the filesystem before read-only commands, so an accidental save could succeed.
        byte[] saved = Files.readAllBytes(roster);
        blockBackup();
        SaveFailureException failure = assertThrows(
                SaveFailureException.class, () -> logic.execute("edit 1 p/999"));
        assertTrue(failure.isChangeApplied());
        assertArrayEquals(saved, Files.readAllBytes(roster));
        repairBackup();

        Path export = directory.resolve("unsaved.csv");
        for (String command : List.of("list", "find Alice", "show 1", "help edit", "export " + export)) {
            logic.execute(command);
            assertArrayEquals(saved, Files.readAllBytes(roster), command);
            assertArrayEquals(previousBackup, Files.readAllBytes(backup), command);
            assertEquals("999", model.getAddressBook().getPersonList().getFirst().getPhone().value);
        }
        // Independent CSV expectation: exporting may share the visible unsaved edit, but must not save the roster.
        assertEquals("name,phone,email,address,tags\r\nAlice,999,alice@example.com,,team\r\n",
                Files.readString(export));

        logic.execute("edit 1 p/999");
        assertArrayEquals(saved, Files.readAllBytes(backup));
        assertSavedSessionMatchesMemory();
        assertEquals("999", new JsonAddressBookStorage(roster).readAddressBook().orElseThrow()
                .getPersonList().getFirst().getPhone().value);
    }

    @Test
    public void delete_confirmedSaveFails_confirmationCannotBeReplayedAfterRepair() throws Exception {
        // Boundary/interaction: deleting the last displayed index must not expose the hidden member to a replay.
        logic.execute("filter t/team");
        byte[] saved = Files.readAllBytes(roster);
        blockBackup();
        logic.execute("delete 2");
        assertTrue(logic.isAwaitingConfirmation());
        SaveFailureException failure = assertThrows(SaveFailureException.class, () -> logic.execute("yes"));
        assertTrue(failure.isChangeApplied());
        assertFalse(logic.isAwaitingConfirmation());
        assertEquals(List.of("Alice", "Cara"), storedNames());
        assertEquals(List.of("Alice"), displayedNames());
        assertArrayEquals(saved, Files.readAllBytes(roster));
        repairBackup();

        assertThrows(ParseException.class, () -> logic.execute("yes"));
        assertEquals(List.of("Alice", "Cara"), storedNames());
        assertArrayEquals(saved, Files.readAllBytes(roster));
        assertArrayEquals(previousBackup, Files.readAllBytes(backup));
        logic.execute("edit 1 p/111");
        assertArrayEquals(saved, Files.readAllBytes(backup));
        assertSavedSessionMatchesMemory();
        assertEquals(List.of("Alice", "Cara"), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow()
                .getPersonList().stream().map(person -> person.getName().fullName).toList());
    }

    @Test
    public void clear_saveFailsWithEarlierUnsavedEdit_restoresSessionBeforeRetryingCorrectScope() throws Exception {
        // State-transition pair: failed edit -> failed clear. Rollback must retain the earlier unsaved edit.
        logic.execute("filter t/team");
        logic.execute("find Bob");
        logic.execute("show 1");
        byte[] saved = Files.readAllBytes(roster);
        blockBackup();
        SaveFailureException editFailure = assertThrows(
                SaveFailureException.class, () -> logic.execute("edit 1 p/999"));
        assertTrue(editFailure.isChangeApplied());
        assertEquals("999", model.getFilteredPersonList().getFirst().getPhone().value);
        AddressBook unsaved = new AddressBook(model.getAddressBook());
        List<String> labels = model.getActiveFilters().stream().map(filter -> filter.getLabel()).toList();

        logic.execute("clear");
        SaveFailureException clearFailure = assertThrows(SaveFailureException.class, () -> logic.execute("yes"));
        assertFalse(clearFailure.isChangeApplied());
        assertFalse(logic.isAwaitingConfirmation());
        assertEquals(unsaved, model.getAddressBook());
        assertEquals(List.of("Bob"), displayedNames());
        assertEquals(labels, model.getActiveFilters().stream().map(filter -> filter.getLabel()).toList());
        assertArrayEquals(saved, Files.readAllBytes(roster));
        repairBackup();

        logic.execute("edit 1 p/999");
        assertSavedSessionMatchesMemory();
        assertArrayEquals(saved, Files.readAllBytes(backup));
        byte[] repaired = Files.readAllBytes(roster);
        logic.execute("clear");
        logic.execute("yes");
        assertEquals(List.of("Alice", "Cara"), storedNames());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertArrayEquals(repaired, Files.readAllBytes(backup));
        assertSavedSessionMatchesMemory();
    }

    /** Blocks atomic backup replacement while preserving the previous backup for repair. */
    private void blockBackup() throws Exception {
        previousBackup = Files.readAllBytes(backup);
        Files.delete(backup);
        Files.createDirectory(backup);
        Files.writeString(backup.resolve("blocker.txt"), "Synthetic storage failure");
    }

    /** Removes the injected fault and restores the exact pre-failure backup bytes. */
    private void repairBackup() throws Exception {
        Files.delete(backup.resolve("blocker.txt"));
        Files.delete(backup);
        Files.write(backup, previousBackup);
    }

    private List<String> storedNames() {
        return model.getAddressBook().getPersonList().stream().map(person -> person.getName().fullName).toList();
    }

    private List<String> displayedNames() {
        return model.getFilteredPersonList().stream().map(person -> person.getName().fullName).toList();
    }

    /** Uses a fresh reader so cached session state cannot stand in for successfully persisted data. */
    private void assertSavedSessionMatchesMemory() throws Exception {
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow());
    }
}

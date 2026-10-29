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

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Exercises typed name searches through real parsing, live filters, and temporary storage. */
public class PartialNameSearchWorkflowTest {
    @TempDir
    public Path directory;

    private ModelManager model;
    private LogicManager logic;
    private Path roster;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        roster = directory.resolve("roster.json");
        logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(roster),
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        logic.execute("add n/Bob Tan p/123 e/bob@example.com t/team");
        logic.execute("add n/Alice Tan p/456 e/alice@example.com t/team");
        logic.execute("add n/Alicia Lee p/789 e/alicia@example.com t/guest");
    }

    @Test
    public void search_keywordSeparatorsAndInvalidQueries_preserveSavedRoster() throws Exception {
        byte[] saved = Files.readAllBytes(roster);
        Path backup = JsonAddressBookStorage.getBackupFilePath(roster);
        byte[] savedBackup = Files.readAllBytes(backup);
        var modified = Files.getLastModifiedTime(roster);
        var backupModified = Files.getLastModifiedTime(backup);
        for (String separator : List.of(" ", "\t", "\u00a0", "\u2007", "\u202f", "\u2003")) {
            logic.execute("list");
            logic.execute("find " + separator + "LIC" + separator + "bob" + separator + "lic");
            assertNames("Bob Tan", "Alice Tan", "Alicia Lee");
            var filters = List.copyOf(model.getActiveFilters());
            assertThrows(ParseException.class, () -> logic.execute("find " + separator));
            assertEquals(filters, model.getActiveFilters());
            assertNames("Bob Tan", "Alice Tan", "Alicia Lee");
        }
        for (String keyword : List.of(".*", "[", "alce", "ceTa", "z".repeat(1000), "example.com", "123")) {
            logic.execute("list");
            logic.execute("find " + keyword);
            assertNames();
        }
        assertArrayEquals(saved, Files.readAllBytes(roster));
        assertArrayEquals(savedBackup, Files.readAllBytes(backup));
        assertEquals(modified, Files.getLastModifiedTime(roster));
        assertEquals(backupModified, Files.getLastModifiedTime(backup));
    }

    @Test
    public void search_orWithinQueryAndAcrossQueries_removingOneConditionRestoresMatches() throws Exception {
        logic.execute("find lic bob");
        assertNames("Bob Tan", "Alice Tan", "Alicia Lee");
        assertEquals(1, model.getActiveFilters().size());
        assertTrue(logic.execute("find BOB LIC lic").getFeedbackToUser().contains("already active"));
        assertEquals(1, model.getActiveFilters().size());
        logic.execute("find tan");
        assertNames("Bob Tan", "Alice Tan");
        logic.execute("find ice");
        assertNames("Alice Tan");
        logic.removeActiveFilter(model.getActiveFilters().getLast());
        assertNames("Bob Tan", "Alice Tan");
        logic.execute("find nobody");
        assertNames();
        assertTrue(logic.execute("find NOBODY nobody").getFeedbackToUser().contains("already active"));
        assertEquals(3, model.getActiveFilters().size());
        logic.removeActiveFilter(model.getActiveFilters().getLast());
        assertNames("Bob Tan", "Alice Tan");
    }

    @Test
    public void search_emailTagShowAndSort_conditionsRemainIndependent() throws Exception {
        logic.execute("sort");
        logic.execute("findemail @example.com");
        logic.execute("filter t/team");
        logic.execute("show 1 2");
        logic.execute("find lic");
        assertNames("Alice Tan");
        assertEquals(4, model.getActiveFilters().size());
        logic.removeActiveFilter(model.getActiveFilters().getLast());
        assertNames("Alice Tan", "Bob Tan");
        logic.execute("list");
        assertNames("Alice Tan", "Alicia Lee", "Bob Tan");
        assertTrue(model.getActiveFilters().isEmpty());
    }

    @Test
    public void search_editAndConfirmedDelete_updateMatchesWithoutLosingHiddenMembers() throws Exception {
        logic.execute("find lic");
        assertNames("Alice Tan", "Alicia Lee");
        logic.execute("edit 1 n/Carol Tan");
        assertNames("Alicia Lee");
        logic.execute("delete 1");
        assertNames("Alicia Lee");
        logic.execute("yes");
        assertNames();
        logic.execute("list");
        assertNames("Bob Tan", "Carol Tan");
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow());
    }

    /** Checks names and order without using the production matching algorithm as the oracle. */
    private void assertNames(String... expected) {
        assertEquals(List.of(expected), model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
    }
}

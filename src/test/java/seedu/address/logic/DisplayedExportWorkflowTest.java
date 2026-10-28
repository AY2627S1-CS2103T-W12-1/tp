package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.ModelManager;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks issue #299 through the real command, model and storage workflow. */
public class DisplayedExportWorkflowTest {
    @TempDir
    public Path directory;

    private Path roster;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws Exception {
        roster = directory.resolve("roster.json");
        model = new ModelManager();
        logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(roster),
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        logic.execute("add n/Bob p/123 e/bob@example.com t/team");
        logic.execute("add n/Carol p/456 e/carol@example.com");
        logic.execute("add n/Alice p/789 e/alice@example.com t/team");
    }

    @Test
    public void export_nameEmailTagAndShowConditions_preservesDisplayedOrderAndSessionState() throws Exception {
        for (List<String> commands : List.of(List.of("filter t/team", "sort"), List.of("find Alice"),
                List.of("findemail bob@"), List.of("show 1 3", "sort"),
                List.of("filter t/team", "find Alice", "show 1"), List.of("find Nobody"))) {
            logic.execute("list");
            for (String command : commands) {
                logic.execute(command);
            }
            var stored = List.copyOf(model.getAddressBook().getPersonList());
            var displayed = List.copyOf(model.getFilteredPersonList());
            var filters = List.copyOf(model.getActiveFilters());
            Path backup = JsonAddressBookStorage.getBackupFilePath(roster);
            byte[] saved = Files.readAllBytes(roster);
            byte[] savedBackup = Files.readAllBytes(backup);
            FileTime rosterTime = Files.getLastModifiedTime(roster);
            FileTime backupTime = Files.getLastModifiedTime(backup);
            Path csv = directory.resolve("displayed.csv");

            String feedback = logic.execute("export " + csv).getFeedbackToUser();

            assertEquals(displayed, CsvAddressBookImporter.read(csv), commands.toString());
            int count = displayed.size();
            int hidden = stored.size() - count;
            assertEquals("Exported " + count + " displayed " + (count == 1 ? "member" : "members")
                    + " to " + csv + " (" + hidden + " hidden " + (hidden == 1 ? "member" : "members")
                    + " not exported).", feedback);
            assertEquals(stored, model.getAddressBook().getPersonList());
            assertEquals(displayed, model.getFilteredPersonList());
            assertEquals(filters, model.getActiveFilters());
            assertArrayEquals(saved, Files.readAllBytes(roster));
            assertArrayEquals(savedBackup, Files.readAllBytes(backup));
            assertEquals(rosterTime, Files.getLastModifiedTime(roster));
            assertEquals(backupTime, Files.getLastModifiedTime(backup));
            if (displayed.isEmpty()) {
                assertEquals("name,phone,email,address,tags\r\n", Files.readString(csv));
            }
        }
    }

    @Test
    public void export_allVisibleSorted_keepsExistingFeedbackAndUsesDisplayOrder() throws Exception {
        logic.execute("sort");
        Path csv = directory.resolve("all.csv");
        assertEquals("Exported 3 members to " + csv + ".", logic.execute("export " + csv).getFeedbackToUser());
        assertEquals(List.of("Alice", "Bob", "Carol"), CsvAddressBookImporter.read(csv).stream()
                .map(person -> person.getName().fullName).toList());
    }
}

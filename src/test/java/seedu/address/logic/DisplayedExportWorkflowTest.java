package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.ExportCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

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
    @Test
    public void export_zeroOneAndTwoMemberBoundaries_useExactCountsAndGrammar() throws Exception {
        String[][] messages = {
            {"Exported 0 members to %s."},
            {"Exported 0 displayed members to %s (1 hidden member not exported).", "Exported 1 member to %s."},
            {"Exported 0 displayed members to %s (2 hidden members not exported).",
                "Exported 1 displayed member to %s (1 hidden member not exported).", "Exported 2 members to %s."}
        };
        for (int total = 0; total <= 2; total++) {
            for (int visible = 0; visible <= total; visible++) {
                ModelManager boundaryModel = new ModelManager();
                for (int index = 0; index < total; index++) {
                    boundaryModel.addPerson(new PersonBuilder().withName("Member " + index)
                            .withPhone("10" + index).build());
                }
                int firstHiddenPhone = 100 + visible;
                boundaryModel.updateFilteredPersonList(person ->
                        Integer.parseInt(person.getPhone().value) < firstHiddenPhone);
                Path csv = directory.resolve("counts-" + total + "-" + visible + ".csv");
                assertEquals(String.format(messages[total][visible], csv),
                        new ExportCommand(csv).execute(boundaryModel).getFeedbackToUser());
                assertEquals(boundaryModel.getFilteredPersonList(), CsvAddressBookImporter.read(csv));
                assertEquals(total, boundaryModel.getAddressBook().getPersonList().size());
            }
        }
    }

    @Test
    public void export_overwritePopulatedWithEmptyThenSingle_removesStaleRows() throws Exception {
        Path csv = directory.resolve("reused.csv");
        logic.execute("export " + csv);
        assertEquals(4, Files.readAllLines(csv).size());

        logic.execute("find Nobody");
        assertEquals("Exported 0 displayed members to " + csv + " (3 hidden members not exported).",
                logic.execute("export " + csv).getFeedbackToUser());
        assertEquals("name,phone,email,address,tags\r\n", Files.readString(csv));

        logic.execute("list");
        logic.execute("show 3");
        assertEquals("Exported 1 displayed member to " + csv + " (2 hidden members not exported).",
                logic.execute("export " + csv).getFeedbackToUser());
        assertEquals(List.of("Alice"), CsvAddressBookImporter.read(csv).stream()
                .map(person -> person.getName().fullName).toList());
        assertEquals(2, Files.readAllLines(csv).size());
    }

    @Test
    public void export_firstLastRepeatedAndInvalidShowIndexes_neverLeaksHiddenMembers() throws Exception {
        logic.execute("sort");
        logic.execute("show 3 1 1");
        assertEquals(List.of("Alice", "Carol"), model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        var filters = List.copyOf(model.getActiveFilters());
        assertThrows(ParseException.class, () -> logic.execute("show 0"));
        assertThrows(CommandException.class, () -> logic.execute("show 3"));
        assertEquals(filters, model.getActiveFilters());
        Path csv = directory.resolve("edges.csv");
        logic.execute("export " + csv);
        assertEquals(List.of("Alice", "Carol"), CsvAddressBookImporter.read(csv).stream()
                .map(person -> person.getName().fullName).toList());

        logic.execute("show 1");
        logic.execute("export " + csv);
        assertEquals(List.of("Alice"), CsvAddressBookImporter.read(csv).stream()
                .map(person -> person.getName().fullName).toList());
        logic.removeActiveFilter(model.getActiveFilters().getLast());
        logic.execute("export " + csv);
        assertEquals(List.of("Alice", "Carol"), CsvAddressBookImporter.read(csv).stream()
                .map(person -> person.getName().fullName).toList());
    }

    @Test
    public void export_equalNameSortAfterEdit_keepsStableOrderAndUpdatedFields() throws Exception {
        logic.execute("add n/ALICE p/999 e/another@example.com");
        logic.execute("sort");
        logic.execute("find Alice");
        logic.execute("show 2 1");
        logic.execute("edit 1 p/777 a/Block 1, Cafe");
        Path csv = directory.resolve("ties.csv");
        assertEquals("Exported 2 displayed members to " + csv + " (2 hidden members not exported).",
                logic.execute("export " + csv).getFeedbackToUser());
        var imported = CsvAddressBookImporter.read(csv);
        assertEquals(List.of("777", "999"), imported.stream().map(person -> person.getPhone().value).toList());
        assertEquals(List.of("Alice", "ALICE"), imported.stream().map(person -> person.getName().fullName).toList());
        assertEquals("Block 1, Cafe", imported.getFirst().getAddress().value);
        assertEquals(model.getFilteredPersonList(), imported);
    }
}

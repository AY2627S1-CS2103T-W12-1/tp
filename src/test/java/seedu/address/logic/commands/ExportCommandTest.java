package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class ExportCommandTest {

    @TempDir
    public Path tempDir;

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void execute_filteredList_exportsAllPeopleAndPreservesModel() throws Exception {
        Model model = new ModelManager();
        Person person = new PersonBuilder().withName("Alice").withAddress("Block 1").build();
        model.addPerson(person);
        model.updateFilteredPersonList(unused -> false);
        Path destination = tempDir.resolve("member contacts.csv");

        CommandResult result = parser.parseCommand("export   " + destination + "   ").execute(model);

        assertEquals("name,phone,email,address,tags\r\n"
                + "Alice,85355255,amy@gmail.com,Block 1,\r\n", Files.readString(destination));
        assertEquals(String.format(ExportCommand.MESSAGE_SUCCESS, 1, destination.toAbsolutePath().normalize()),
                result.getFeedbackToUser());
        assertEquals(List.of(person), model.getAddressBook().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_directoryDestination_reportsFailureAndPreservesModel() {
        Model model = new ModelManager();
        Person person = new PersonBuilder().build();
        model.addPerson(person);

        CommandException error = assertThrows(CommandException.class, () ->
                new ExportCommand(tempDir).execute(model));

        assertInstanceOf(IOException.class, error.getCause());
        assertEquals(String.format(ExportCommand.MESSAGE_FAILURE, tempDir, error.getCause().getMessage()),
                error.getMessage());
        assertEquals(List.of(person), model.getAddressBook().getPersonList());
        assertEquals(List.of(person), model.getFilteredPersonList());
    }

    @Test
    public void execute_relativePath_writesInCurrentDirectoryAndReportsAbsolutePath() throws Exception {
        Path directory = Files.createTempDirectory(Path.of("."), "csv-export-test-");
        Path destination = directory.resolve("member contacts \u00e9.csv");
        try {
            CommandResult result = parser.parseCommand("export " + destination).execute(new ModelManager());

            assertEquals("name,phone,email,address,tags\r\n", Files.readString(destination));
            assertEquals(String.format(ExportCommand.MESSAGE_SUCCESS, 0, destination.toAbsolutePath().normalize()),
                    result.getFeedbackToUser());
        } finally {
            Files.deleteIfExists(destination);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void execute_emptyAddressBook_writesHeaderAndReportsZero() throws Exception {
        Model model = new ModelManager();
        Path destination = tempDir.resolve("empty.csv");

        CommandResult result = new ExportCommand(destination).execute(model);

        assertEquals("name,phone,email,address,tags\r\n", Files.readString(destination));
        assertEquals(String.format(ExportCommand.MESSAGE_SUCCESS, 0, destination.toAbsolutePath().normalize()),
                result.getFeedbackToUser());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
    }

    @Test
    public void execute_partialFilter_exportsVisibleAndHiddenPeopleInStoredOrder() throws Exception {
        Model model = new ModelManager();
        Person alice = new PersonBuilder().withName("Alice").withAddress("Block 1").build();
        Person bob = new PersonBuilder().withName("Bob").withAddress("Block 2").build();
        model.addPerson(bob);
        model.addPerson(alice);
        model.updateFilteredPersonList(person -> person.equals(alice));
        Path destination = tempDir.resolve("members.csv");

        CommandResult result = new ExportCommand(destination).execute(model);

        assertEquals(List.of("name,phone,email,address,tags", "Bob,85355255,amy@gmail.com,Block 2,",
                "Alice,85355255,amy@gmail.com,Block 1,"), Files.readAllLines(destination));
        assertEquals(String.format(ExportCommand.MESSAGE_SUCCESS, 2, destination.toAbsolutePath().normalize()),
                result.getFeedbackToUser());
        assertEquals(List.of(bob, alice), model.getAddressBook().getPersonList());
        assertEquals(List.of(alice), model.getFilteredPersonList());
    }

    @Test
    public void execute_missingParent_reportsFailureAndPreservesFilter() {
        Model model = new ModelManager();
        Person person = new PersonBuilder().build();
        model.addPerson(person);
        model.updateFilteredPersonList(unused -> false);
        Path destination = tempDir.resolve("missing").resolve("members.csv");

        CommandException error = assertThrows(CommandException.class, () ->
                new ExportCommand(destination).execute(model));

        assertInstanceOf(IOException.class, error.getCause());
        assertEquals(String.format(ExportCommand.MESSAGE_FAILURE, destination, error.getCause().getMessage()),
                error.getMessage());
        assertEquals(List.of(person), model.getAddressBook().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void equalsAndHashCode_destinationPaths_defineCommandIdentity() {
        ExportCommand first = new ExportCommand(tempDir.resolve("members.csv"));
        ExportCommand sameDestination = new ExportCommand(tempDir.resolve("members.csv"));
        ExportCommand differentDestination = new ExportCommand(tempDir.resolve("other.csv"));

        assertTrue(first.equals(first));
        assertEquals(first, sameDestination);
        assertEquals(sameDestination, first);
        assertEquals(first.hashCode(), sameDestination.hashCode());
        assertNotEquals(first, differentDestination);
        assertFalse(first.equals(null));
        assertFalse(first.equals("members.csv"));
        Set<ExportCommand> commands = new HashSet<>(List.of(first, sameDestination, differentDestination));
        assertEquals(2, commands.size());
    }

    @Test
    public void parseCommand_invalidPath_throwsParseException() {
        assertThrows(ParseException.class, () -> parser.parseCommand("export invalid\u0000path.csv"));
    }
}

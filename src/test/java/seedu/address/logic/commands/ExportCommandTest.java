package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        assertEquals("Name,Phone,Email,Address,Tags\r\n"
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
    public void parseCommand_invalidPath_throwsParseException() {
        assertThrows(ParseException.class, () -> parser.parseCommand("export invalid\u0000path.csv"));
    }
}

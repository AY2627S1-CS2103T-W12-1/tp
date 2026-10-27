package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class ImportCommandTest {

    @TempDir
    public Path tempDir;

    @Test
    public void execute_validFile_appendsUniqueMembersInOrder() throws Exception {
        Person existing = new PersonBuilder().withName("Existing Member").withPhone("111")
                .withEmail("existing@example.com").withAddress("Existing address").withTags("old").build();
        Person sameNameNewContact = new PersonBuilder(existing).withPhone("222")
                .withEmail("new@example.com").withAddress("New address").withTags("first").build();
        Person another = new PersonBuilder().withName("Another Member").withPhone("333")
                .withEmail("another@example.com").withAddress("Another address").withTags().build();
        Model model = new ModelManager();
        model.addPerson(existing);
        Path file = write("name,phone,email,address,tags\n"
                + "Existing Member,111,existing@example.com,Existing address,replacement\n"
                + "Existing Member,222,new@example.com,New address,first\n"
                + "Existing Member,222,new@example.com,New address,second\n"
                + "Another Member,333,another@example.com,Another address,\n");

        CommandResult result = new ImportCommand(file).execute(model);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 2, file.toAbsolutePath().normalize(), 2),
                result.getFeedbackToUser());
        assertEquals(List.of(existing, sameNameNewContact, another), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_paddedDuplicates_skipsExistingAndEarlierImportedMembers() throws Exception {
        Person existing = new PersonBuilder().withName("Alice").withPhone("111")
                .withEmail("alice@example.com").withAddress("Home").withTags("old").build();
        Person bob = new PersonBuilder().withName("Bob").withPhone("222")
                .withEmail("bob@example.com").withAddress("New address").withTags("first").build();
        Model model = new ModelManager();
        model.addPerson(existing);
        model.updateFilteredPersonList(unused -> false);
        Path file = write("name,phone,email,address,tags\n"
                + "Alice ,111,alice@example.com,Home ,replacement\n"
                + " Bob , 222 , bob@example.com , New address , first \n"
                + "Bob ,222,bob@example.com,New address ,second\n");

        CommandResult result = new ImportCommand(file).execute(model);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, file.toAbsolutePath().normalize(), 2),
                result.getFeedbackToUser());
        assertEquals(List.of(existing, bob), model.getAddressBook().getPersonList());
        assertEquals(List.of(existing, bob), model.getFilteredPersonList());
    }

    @Test
    public void execute_laterSyntaxError_preservesRosterAndView() throws Exception {
        Person existing = new PersonBuilder().withName("Zulu").build();
        Person another = new PersonBuilder().withName("Alpha").build();
        Model model = new ModelManager();
        model.addPerson(existing);
        model.addPerson(another);
        model.setNameSorting(true);
        model.updateFilteredPersonList(unused -> false);
        for (String invalidRow : List.of("Invalid,123,invalid@example.com,\"Unclosed address\n",
                "Invalid,123,invalid@example.com,Unescaped\" address,\n")) {
            model.updateFilteredPersonList(unused -> false);
            String contents = "name,phone,email,address,tags\nValid,123,valid@example.com,Home,\n" + invalidRow;
            Path file = write(contents);

            CommandException error = assertThrows(CommandException.class, () -> new ImportCommand(file).execute(model));

            assertEquals("Could not import CSV: row 3: invalid CSV syntax. No members were imported.",
                    error.getMessage());
            assertEquals(List.of(existing, another), model.getAddressBook().getPersonList());
            assertEquals(List.of(), model.getFilteredPersonList());
            assertEquals(contents, Files.readString(file));
            model.updateFilteredPersonList(unused -> true);
            assertEquals(List.of(another, existing), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_headerOnly_resetsFilteredAndSortedView() throws Exception {
        Person first = new PersonBuilder().withName("Zulu Member").withPhone("111")
                .withEmail("zulu@example.com").withAddress("First address").build();
        Person second = new PersonBuilder().withName("Alpha Member").withPhone("222")
                .withEmail("alpha@example.com").withAddress("Second address").build();
        Model model = new ModelManager();
        model.addPerson(first);
        model.addPerson(second);
        model.setNameSorting(true);
        model.updateFilteredPersonList(unused -> false);
        Path file = write("name,phone,email,address,tags\n");

        CommandResult result = new ImportCommand(file).execute(model);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 0, file.toAbsolutePath().normalize(), 0),
                result.getFeedbackToUser());
        assertEquals(List.of(first, second), model.getFilteredPersonList());
    }

    @Test
    public void execute_duplicateOnly_reportsSkippedAndResetsView() throws Exception {
        Person existing = new PersonBuilder().withName("Existing Member").withPhone("111")
                .withEmail("existing@example.com").withAddress("Existing address").build();
        Model model = new ModelManager();
        model.addPerson(existing);
        model.updateFilteredPersonList(unused -> false);
        Path file = write("name,phone,email,address,tags\n"
                + "Existing Member,111,existing@example.com,Existing address,replacement\n");

        CommandResult result = new ImportCommand(file).execute(model);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 0, file.toAbsolutePath().normalize(), 1),
                result.getFeedbackToUser());
        assertEquals(List.of(existing), model.getFilteredPersonList());
        assertEquals(existing.getTags(), model.getFilteredPersonList().getFirst().getTags());
    }

    @Test
    public void execute_laterInvalidRow_preservesRosterAndView() throws Exception {
        Person existing = new PersonBuilder().build();
        Model model = new ModelManager();
        model.addPerson(existing);
        model.updateFilteredPersonList(unused -> false);
        Path file = write("name,phone,email,address,tags\nValid,123,valid@example.com,Home,\n"
                + "Invalid,12,invalid@example.com,Home,\n");

        CommandException error = assertThrows(CommandException.class, () -> new ImportCommand(file).execute(model));

        assertEquals("Could not import CSV: row 3: " + seedu.address.model.person.Phone.MESSAGE_CONSTRAINTS
                + ". No members were imported.", error.getMessage());
        assertEquals(List.of(existing), model.getAddressBook().getPersonList());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_invalidHeader_preservesRosterAndUsesCompleteSentence() throws Exception {
        Model model = new ModelManager();
        Path file = write("Name,phone,email,address,tags\n");

        CommandException error = assertThrows(CommandException.class, () -> new ImportCommand(file).execute(model));

        assertEquals("Could not import CSV: invalid header. Expected the columns name, phone, email, address, and tags "
                + "exactly once. "
                + "No members were imported.", error.getMessage());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
    }

    @Test
    public void execute_missingFile_reportsReadFailure() {
        Path missingFile = tempDir.resolve("missing.csv");

        CommandException error = assertThrows(CommandException.class, () ->
                new ImportCommand(missingFile).execute(new ModelManager()));

        assertInstanceOf(IOException.class, error.getCause());
        assertTrue(error.getMessage().startsWith("Could not import CSV: "));
    }

    @Test
    public void equalsAndHashCode_filePaths_defineCommandIdentity() {
        ImportCommand first = new ImportCommand(tempDir.resolve("members.csv"));
        ImportCommand samePath = new ImportCommand(tempDir.resolve("members.csv"));
        ImportCommand differentPath = new ImportCommand(tempDir.resolve("other.csv"));

        assertEquals(first, first);
        assertEquals(first, samePath);
        assertEquals(first.hashCode(), samePath.hashCode());
        assertNotEquals(first, differentPath);
        assertFalse(first.equals(null));
        assertFalse(first.equals("members.csv"));
        Set<ImportCommand> commands = new HashSet<>(List.of(first, samePath, differentPath));
        assertEquals(2, commands.size());
    }

    private Path write(String contents) throws Exception {
        Path file = tempDir.resolve("members.csv");
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}

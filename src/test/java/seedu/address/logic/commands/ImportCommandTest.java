package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private Path write(String contents) throws Exception {
        Path file = tempDir.resolve("members.csv");
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}

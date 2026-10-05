package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class CsvAddressBookImporterTest {

    @TempDir
    public Path tempDir;

    @Test
    public void read_headerOnly_returnsEmptyList() throws Exception {
        Path file = write("name,phone,email,address,tags\n");

        assertEquals(List.of(), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_validSimpleRecords_returnsMembersInFileOrder() throws Exception {
        Path file = write("name,phone,email,address,tags\n"
                + " John Doe , 98765432 , john@example.com , Main Street , year1;committee;year1 \n"
                + "Jane Doe,123,jane@example.com,Second Street,\n");
        Person john = new PersonBuilder().withName("John Doe").withPhone("98765432")
                .withEmail("john@example.com").withAddress("Main Street")
                .withTags("year1", "committee").build();
        Person jane = new PersonBuilder().withName("Jane Doe").withPhone("123")
                .withEmail("jane@example.com").withAddress("Second Street").withTags().build();

        assertEquals(List.of(john, jane), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_invalidHeader_rejected() throws Exception {
        Path file = write("Name,phone,email,address,tags\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals(CsvAddressBookImporter.MESSAGE_INVALID_HEADER, error.getMessage());
    }

    @Test
    public void read_wrongFieldCount_reportsRow() throws Exception {
        Path file = write("name,phone,email,address,tags\nAlice,123,a@example.com,Home\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 2: expected 5 fields but found 4.", error.getMessage());
    }

    @Test
    public void read_invalidMemberField_reportsRowAndValidationMessage() throws Exception {
        Path file = write("name,phone,email,address,tags\nAlice,12,a@example.com,Home,\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 2: Phone numbers should only contain digits, and should be at least 3 digits long",
                error.getMessage());
    }

    @Test
    public void read_missingFile_throwsIoException() {
        assertThrows(IOException.class, () -> CsvAddressBookImporter.read(tempDir.resolve("missing.csv")));
    }

    private Path write(String contents) throws IOException {
        Path file = tempDir.resolve("members.csv");
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}

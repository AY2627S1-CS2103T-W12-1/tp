package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class CsvAddressBookExporterTest {

    @TempDir
    public Path tempDir;

    @Test
    public void export_specialCharacters_writesEscapedUtf8Csv() throws IOException {
        Path destination = tempDir.resolve("contacts.csv");
        Person person = new PersonBuilder().withName("Alice").withPhone("001234")
                .withEmail("alice@example.com").withAddress("Block 1, \"Caf\u00e9\"")
                .withTags("friend", "colleague").build();

        CsvAddressBookExporter.export(List.of(person), destination);

        assertEquals("Name,Phone,Email,Address,Tags\r\n"
                + "Alice,001234,alice@example.com,\"Block 1, \"\"Caf\u00e9\"\"\",colleague;friend\r\n",
                Files.readString(destination));
    }

    @Test
    public void export_emptyList_replacesExistingFileWithHeader() throws IOException {
        Path destination = tempDir.resolve("contacts.csv");
        Files.writeString(destination, "Previous contents that must not remain after exporting an empty address book.");

        CsvAddressBookExporter.export(List.of(), destination);

        assertEquals("Name,Phone,Email,Address,Tags\r\n", Files.readString(destination));
    }

    @Test
    public void export_missingParent_throwsIoException() {
        Path destination = tempDir.resolve("missing").resolve("contacts.csv");

        assertThrows(IOException.class, () -> CsvAddressBookExporter.export(List.of(), destination));
        assertFalse(Files.exists(destination.getParent()));
    }
}

package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_CSV;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_HEADER;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_RECORD_ENDING;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_UTF8;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
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
    public void read_validRecords_decodesBomQuotesBlankRowsAndTags() throws Exception {
        Path file = write("\uFEFFname,phone,email,address,tags\r\n"
                + " John Doe , 98765432 , john@example.com ,\"John \"\"Main\"\" Street, Block 1\","
                + " year1;committee;year1 \r\n\r\n"
                + "Jane Doe,123,jane@example.com,Second Street,\"year2\"\r\n");
        Person john = new PersonBuilder().withName("John Doe").withPhone("98765432")
                .withEmail("john@example.com").withAddress("John \"Main\" Street, Block 1")
                .withTags("year1", "committee").build();
        Person jane = new PersonBuilder().withName("Jane Doe").withPhone("123")
                .withEmail("jane@example.com").withAddress("Second Street").withTags("year2").build();

        assertEquals(List.of(john, jane), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_invalidHeader_rejectsEmptyIncorrectAndExtraColumns() throws Exception {
        for (String contents : List.of("", "Name,phone,email,address,tags\n",
                "name,phone,email,address,tags,extra\n", "\"name\",phone,email,address,tags\n",
                "\"name,phone,email,address,tags\n")) {
            Path file = write(contents);
            CsvImportException error = assertThrows(CsvImportException.class, () ->
                    CsvAddressBookImporter.read(file));
            assertEquals(MESSAGE_INVALID_HEADER, error.getMessage());
        }
    }

    @Test
    public void read_blankRecordsStillCountTowardsRowNumber() throws Exception {
        Path file = write("name,phone,email,address,tags\n\nAlice,12,a@example.com,Home,\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 3: Phone numbers should only contain digits, and should be at least 3 digits long",
                error.getMessage());
    }

    @Test
    public void read_invalidRows_reportsFirstError() throws Exception {
        List<String> invalidRecords = List.of(
                "Alice,123,a@example.com,Home",
                "Alice,123,a@example.com,\"Home,committee",
                "\"Alice\"x,123,a@example.com,Home,",
                "Al\"ice,123,a@example.com,Home,",
                "Alice,123,a@example.com,Home,year1;;committee",
                "Alice,123,a@example.com,Home," + "a".repeat(31));
        List<String> expectedReasons = List.of(
                "expected 5 fields but found 4.",
                MESSAGE_INVALID_CSV,
                MESSAGE_INVALID_CSV,
                MESSAGE_INVALID_CSV,
                Tag.MESSAGE_CONSTRAINTS,
                Tag.MESSAGE_CONSTRAINTS);

        for (int index = 0; index < invalidRecords.size(); index++) {
            Path file = write("name,phone,email,address,tags\n" + invalidRecords.get(index) + "\n");
            CsvImportException error = assertThrows(CsvImportException.class, () ->
                    CsvAddressBookImporter.read(file));
            assertEquals("row 2: " + expectedReasons.get(index), error.getMessage());
        }
    }

    @Test
    public void read_embeddedLineBreak_rejectsAtOpeningRecord() throws Exception {
        Path file = write("name,phone,email,address,tags\nAlice,123,a@example.com,\"First\nSecond\",\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 2: " + MESSAGE_INVALID_CSV, error.getMessage());
    }

    @Test
    public void read_invalidUtf8_rejectsFile() throws Exception {
        Path file = tempDir.resolve("members.csv");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals(MESSAGE_INVALID_UTF8, error.getMessage());
    }

    @Test
    public void read_loneCarriageReturn_rejectsRecordEnding() throws Exception {
        for (String contents : List.of("name,phone,email,address,tags\r",
                "name,phone,email,address,tags\rAlice")) {
            Path file = write(contents);
            CsvImportException error = assertThrows(CsvImportException.class, () ->
                    CsvAddressBookImporter.read(file));
            assertEquals(MESSAGE_INVALID_RECORD_ENDING, error.getMessage());
        }
    }

    @Test
    public void read_invalidNameEmailAndAddress_reportsMatchingValidationMessage() throws Exception {
        List<String> records = List.of(
                ",123,a@example.com,Home,",
                "Alice,123,invalid-email,Home,",
                "Alice,123,a@example.com,   ,");
        List<String> reasons = List.of(
                Name.MESSAGE_CONSTRAINTS,
                Email.MESSAGE_CONSTRAINTS,
                Address.MESSAGE_CONSTRAINTS);

        for (int index = 0; index < records.size(); index++) {
            Path file = write("name,phone,email,address,tags\n" + records.get(index) + "\n");
            CsvImportException error = assertThrows(CsvImportException.class, () ->
                    CsvAddressBookImporter.read(file));
            assertEquals("row 2: " + reasons.get(index), error.getMessage());
        }
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

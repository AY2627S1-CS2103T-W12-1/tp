package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_CSV;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_HEADER;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_RECORD_ENDING;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_INVALID_UTF8;
import static seedu.address.storage.CsvAddressBookImporter.MESSAGE_MULTILINE_FIELD;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class CsvAddressBookImporterTest {

    @TempDir
    public Path tempDir;

    @Test
    public void read_unescapedQuoteInUnquotedField_rejectsCsvSyntaxAtTheCorrectRow() throws Exception {
        Path file = write("name,phone,email,address,tags\n"
                + "Alice,123,a@example.com,Main\" Street,\n");
        CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));
        assertEquals("row 2: " + MESSAGE_INVALID_CSV, error.getMessage());
    }

    @Test
    public void read_invalidFieldBeforeLaterUnescapedQuote_reportsEarlierRow() throws Exception {
        Path file = write("name,phone,email,address,tags\n"
                + "Alice,12,a@example.com,Main Street,\n"
                + "Bob,123,b@example.com,Other\" Street,\n");
        CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));
        assertEquals("row 2: " + Phone.MESSAGE_CONSTRAINTS, error.getMessage());
    }

    @Test
    public void read_headerOnly_returnsEmptyList() throws Exception {
        Path file = write("name,phone,email,address,tags\n");

        assertEquals(List.of(), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_validRecords_decodesBomQuotesBlankRowsAndTags() throws Exception {
        Path file = write("\uFEFFname,phone,email,address,tags\r\n"
                + "John Doe,98765432,john@example.com,\"John \"\"Main\"\" Street, Block 1\","
                + "year1;committee;year1\r\n\r\n"
                + "Jane Doe,123,jane@example.com,Second Street,\"year2\"\r\n");
        Person john = new PersonBuilder().withName("John Doe").withPhone("98765432")
                .withEmail("john@example.com").withAddress("John \"Main\" Street, Block 1")
                .withTags("year1", "committee").build();
        Person jane = new PersonBuilder().withName("Jane Doe").withPhone("123")
                .withEmail("jane@example.com").withAddress("Second Street").withTags("year2").build();

        assertEquals(List.of(john, jane), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_reorderedColumnsAndPaddedValues_trimsSurroundingWhitespace() throws Exception {
        Person person = new PersonBuilder().withName("Alice").withPhone("123")
                .withEmail("alice@example.com").withAddress("Main Street, Suite 2")
                .withTags("club").build();
        Path file = write("address,tags,email,name,phone\r\n"
                + "\" \tMain Street, Suite 2 \t\", club ; club , alice@example.com ,Alice ,\t123 \r\n");

        assertEquals(List.of(person), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_exportedRecord_normalisesSurroundingWhitespace() throws Exception {
        Person person = new PersonBuilder().withName("Alice ").withAddress("Main Street ").build();
        Person expected = new PersonBuilder(person).withName("Alice").withAddress("Main Street").build();
        Path file = tempDir.resolve("exported.csv");

        CsvAddressBookExporter.export(List.of(person), file);

        assertEquals(List.of(expected), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_exportedRecordWithoutAddress_restoresMember() throws Exception {
        Person person = new PersonBuilder().withName("Alice").withAddress("").build();
        Path file = tempDir.resolve("exported.csv");

        CsvAddressBookExporter.export(List.of(person), file);

        assertEquals(List.of(person), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_invalidHeader_rejectsEmptyIncorrectAndExtraColumns() throws Exception {
        for (String contents : List.of("", "Name,phone,email,address,tags\n", "name,phone,email,address\n",
                "name,phone,email,address,tags,extra\n", "name,name,email,address,tags\n")) {
            Path file = write(contents);
            CsvImportException error = assertThrows(CsvImportException.class, () ->
                    CsvAddressBookImporter.read(file));
            assertEquals(MESSAGE_INVALID_HEADER, error.getMessage());
        }
    }

    @Test
    public void read_malformedHeader_rejectsCsvSyntax() throws Exception {
        Path file = write("\"name,phone,email,address,tags\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 1: " + MESSAGE_INVALID_CSV, error.getMessage());
    }

    @Test
    public void read_blankRecordsStillCountTowardsRowNumber() throws Exception {
        Path file = write("name,phone,email,address,tags\n\nAlice,12,a@example.com,Home,\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 3: " + Phone.MESSAGE_CONSTRAINTS,
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
    public void read_paddedValues_preservesInternalWhitespaceAndCase() throws Exception {
        Person person = new PersonBuilder().withName("Alice  Smith").withPhone("00123")
                .withEmail("Alice@example.com").withAddress("Main\tStreet  Block 1")
                .withTags("Club", "club").build();
        Path file = write("name,phone,email,address,tags\n"
                + " Alice  Smith , 00123 , Alice@example.com , Main\tStreet  Block 1 , Club ; club \n");

        assertEquals(List.of(person), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_spacesBeforeQuotedFields_decodesQuotesAndCommas() throws Exception {
        Person person = new PersonBuilder().withName("Alice").withPhone("00123")
                .withEmail("alice@example.com").withAddress("Main \"Street\", Block 1")
                .withTags("club").build();
        Path file = write("name,phone,email,address,tags\n"
                + " \" Alice \" , \" 00123 \" , alice@example.com ,"
                + " \t\" Main \"\"Street\"\", Block 1 \" , \" club \" \n");

        assertEquals(List.of(person), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_blankWhitespaceRows_countsTowardsSyntaxErrorRow() throws Exception {
        Path file = write("name,phone,email,address,tags\r\n \t \r\n"
                + "Alice,123,a@example.com,\"Unclosed address\r\n");

        CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));

        assertEquals("row 3: " + MESSAGE_INVALID_CSV, error.getMessage());
    }

    @Test
    public void read_earlierInvalidFieldBeforeLaterSyntaxError_reportsFirstRow() throws Exception {
        Path file = write("name,phone,email,address,tags\nAlice,12,a@example.com,Home,\n"
                + "Bob,123,b@example.com,\"Unclosed address\n");

        CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));

        assertEquals("row 2: " + Phone.MESSAGE_CONSTRAINTS,
                error.getMessage());
    }

    @Test
    public void read_multilineFields_reportsClearErrorBeforeTrimming() throws Exception {
        for (String record : List.of("Alice,123,a@example.com,\"Main\nStreet\",",
                "Alice,123,a@example.com,\"Home\r\n\",", "Alice,123,a@example.com,Home,\"\n\"")) {
            Path file = write("name,phone,email,address,tags\n\n" + record + "\n");

            CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));

            assertEquals("row 3: " + MESSAGE_MULTILINE_FIELD, error.getMessage());
        }
    }

    @Test
    public void read_singleColumnRow_reportsFieldCount() throws Exception {
        Path file = write("name,phone,email,address,tags\nnot a record\n");

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.read(file));

        assertEquals("row 2: expected 5 fields but found 1.", error.getMessage());
    }

    @Test
    public void parseRecords_readerCloseFailure_reportsInvalidCsv() {
        Reader reader = new Reader() {
            private final StringReader source = new StringReader("name,phone,email,address,tags\n");

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                return source.read(buffer, offset, length);
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        CsvImportException error = assertThrows(CsvImportException.class, () ->
                CsvAddressBookImporter.parseRecords(reader));

        assertEquals(MESSAGE_INVALID_CSV, error.getMessage());
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
    public void read_invalidNameAndEmail_reportsMatchingValidationMessage() throws Exception {
        List<String> records = List.of(
                ",123,a@example.com,Home,",
                "Alice,123,invalid-email,Home,");
        List<String> reasons = List.of(
                Name.MESSAGE_CONSTRAINTS,
                Email.MESSAGE_CONSTRAINTS);

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

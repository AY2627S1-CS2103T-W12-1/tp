package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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

        assertEquals("name,phone,email,address,tags,trackcall_encoding\r\n"
                + "Alice,001234,alice@example.com,\"Block 1, \"\"Caf\u00e9\"\"\",colleague;friend,apostrophe-v1\r\n",
                Files.readString(destination));
    }

    @Test
    public void export_emptyList_replacesExistingFileWithHeader() throws IOException {
        Path destination = tempDir.resolve("contacts.csv");
        Files.writeString(destination, "Previous contents that must not remain after exporting an empty address book.");

        CsvAddressBookExporter.export(List.of(), destination);

        assertEquals("name,phone,email,address,tags,trackcall_encoding\r\n", Files.readString(destination));
    }

    @Test
    public void export_fiveHundredPeople_preservesEveryFieldAndRosterOrder() throws IOException {
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            people.add(new PersonBuilder().withName("Member " + (500 - i)).withPhone("000" + i)
                    .withEmail("member" + i + "@example.com").withAddress("Block " + i + ", Caf\u00e9")
                    .withTags(i % 2 == 0 ? new String[] {"year1", "committee"} : new String[0]).build());
        }
        Path destination = tempDir.resolve("contacts.csv");

        CsvAddressBookExporter.export(people, destination);

        try (Reader reader = Files.newBufferedReader(destination);
                CSVParser csvParser = CSVFormat.RFC4180.parse(reader)) {
            List<CSVRecord> records = csvParser.getRecords();
            assertEquals(501, records.size());
            assertEquals(CsvAddressBookCsv.ENCODED_HEADERS, records.getFirst().toList());
            for (int i = 0; i < people.size(); i++) {
                Person person = people.get(i);
                assertEquals(List.of(person.getName().fullName, person.getPhone().value, person.getEmail().value,
                        person.getAddress().value, i % 2 == 0 ? "committee;year1" : "",
                        CsvAddressBookCsv.SPREADSHEET_ENCODING), records.get(i + 1).toList());
            }
        }
    }

    @Test
    public void export_populatedList_replacesOldRecords() throws IOException {
        Path destination = tempDir.resolve("contacts.csv");
        Person alice = new PersonBuilder().withName("Alice").withAddress("Block 1").build();
        Person bob = new PersonBuilder().withName("Bob").withAddress("Block 2").build();
        CsvAddressBookExporter.export(List.of(alice, bob), destination);

        CsvAddressBookExporter.export(List.of(bob), destination);

        assertEquals("name,phone,email,address,tags,trackcall_encoding\r\n"
                + "Bob,85355255,amy@gmail.com,Block 2,,apostrophe-v1\r\n", Files.readString(destination));
    }

    @Test
    public void export_validPeople_canBeReadByImporter() throws Exception {
        Path destination = tempDir.resolve("contacts.csv");
        Person alice = new PersonBuilder().withName("Alice").withAddress("Block 1, Main Street")
                .withTags("friend", "year-1").build();

        CsvAddressBookExporter.export(List.of(alice), destination);

        assertEquals(List.of(alice), CsvAddressBookImporter.read(destination));
    }

    @Test
    public void export_formulaLikeFields_arePrefixedAndRoundTrip() throws Exception {
        Path destination = tempDir.resolve("contacts.csv");
        List<Person> people = List.of(
                new PersonBuilder().withName("李明").withPhone("+6591111131")
                        .withAddress("=SUM(A1)").build(),
                new PersonBuilder().withName("Alice").withAddress("-5 Road").build(),
                new PersonBuilder().withName("Bob").withEmail("'+bob@example.com")
                        .withAddress("'@home").build(),
                new PersonBuilder().withName("Carol").withEmail("''+carol@example.com")
                        .withAddress("+5 Road").build(),
                new PersonBuilder().withName("Dan").withAddress("@home").build(),
                new PersonBuilder().withName("Eve").withAddress("'").build());

        CsvAddressBookExporter.export(people, destination);

        String csv = Files.readString(destination);
        assertTrue(csv.contains("李明,'+6591111131,amy@gmail.com,'=SUM(A1),,apostrophe-v1"));
        assertTrue(csv.contains("Alice,85355255,amy@gmail.com,'-5 Road,,apostrophe-v1"));
        assertTrue(csv.contains("Bob,85355255,''+bob@example.com,''@home,,apostrophe-v1"));
        assertTrue(csv.contains("Carol,85355255,'''+carol@example.com,'+5 Road,,apostrophe-v1"));
        assertTrue(csv.contains("Dan,85355255,amy@gmail.com,'@home,,apostrophe-v1"));
        assertTrue(csv.contains("Eve,85355255,amy@gmail.com,',,apostrophe-v1"));
        assertEquals('n', csv.charAt(0)); // UTF-8 export deliberately has no BOM.
        assertEquals(people, CsvAddressBookImporter.read(destination));
    }

    @Test
    public void export_missingParent_throwsIoException() {
        Path destination = tempDir.resolve("missing").resolve("contacts.csv");

        assertThrows(IOException.class, () -> CsvAddressBookExporter.export(List.of(), destination));
        assertFalse(Files.exists(destination.getParent()));
    }

    @Test
    public void export_encodingFailure_preservesPreviousFileAndCleansTemporaryFiles() throws IOException {
        Path destination = tempDir.resolve("contacts.csv");
        String previous = "Previous complete export";
        Files.writeString(destination, previous);
        Person invalidUnicode = new PersonBuilder().withAddress("Road \uD800").build();

        assertThrows(IOException.class, () -> CsvAddressBookExporter.export(List.of(invalidUnicode), destination));

        assertEquals(previous, Files.readString(destination));
        try (var entries = Files.list(tempDir)) {
            assertEquals(List.of(destination), entries.toList());
        }
    }
}

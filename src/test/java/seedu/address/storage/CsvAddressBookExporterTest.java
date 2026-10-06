package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertEquals("name,phone,email,address,tags\r\n"
                + "Alice,001234,alice@example.com,\"Block 1, \"\"Caf\u00e9\"\"\",colleague;friend\r\n",
                Files.readString(destination));
    }

    @Test
    public void export_emptyList_replacesExistingFileWithHeader() throws IOException {
        Path destination = tempDir.resolve("contacts.csv");
        Files.writeString(destination, "Previous contents that must not remain after exporting an empty address book.");

        CsvAddressBookExporter.export(List.of(), destination);

        assertEquals("name,phone,email,address,tags\r\n", Files.readString(destination));
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
            assertEquals(List.of("name", "phone", "email", "address", "tags"), records.getFirst().toList());
            for (int i = 0; i < people.size(); i++) {
                Person person = people.get(i);
                assertEquals(List.of(person.getName().fullName, person.getPhone().value, person.getEmail().value,
                        person.getAddress().value, i % 2 == 0 ? "committee;year1" : ""), records.get(i + 1).toList());
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

        assertEquals("name,phone,email,address,tags\r\n"
                + "Bob,85355255,amy@gmail.com,Block 2,\r\n", Files.readString(destination));
    }

    @Test
    public void export_validPeople_canBeReadByImporter() throws Exception {
        Path destination = tempDir.resolve("contacts.csv");
        Person alice = new PersonBuilder().withName("Alice").withAddress("Block 1, Main Street")
                .withTags("friend", "committee").build();

        CsvAddressBookExporter.export(List.of(alice), destination);

        assertEquals(List.of(alice), CsvAddressBookImporter.read(destination));
    }

    @Test
    public void export_missingParent_throwsIoException() {
        Path destination = tempDir.resolve("missing").resolve("contacts.csv");

        assertThrows(IOException.class, () -> CsvAddressBookExporter.export(List.of(), destination));
        assertFalse(Files.exists(destination.getParent()));
    }
}

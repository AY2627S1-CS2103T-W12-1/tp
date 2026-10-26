package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.RandomPersons;

/** Checks CSV invariants against generated data, with fixed seeds to reproduce failures. */
public class CsvRoundTripRandomizedTest {
    @TempDir
    public Path directory;

    @Test
    public void exportImport_fiveThousandMembers_preservesEntireRoster() throws Exception {
        List<Person> expected = RandomPersons.generate(new Random(5000), 5000);
        Path file = directory.resolve("large roster.csv");

        CsvAddressBookExporter.export(expected, file);

        assertEquals(expected, CsvAddressBookImporter.read(file));
    }

    @Test
    public void exportImport_veryLongFieldsAndManyTags_preservesEveryCharacter() throws Exception {
        String[] tags = IntStream.range(0, 2000).mapToObj(index -> "Tag" + index).toArray(String[]::new);
        Person extreme = new PersonBuilder().withName("Member" + "Ab9 ".repeat(4000) + "End")
                .withPhone("0".repeat(16000) + "123")
                .withEmail("a-b".repeat(4000) + "@example.com")
                .withAddress("Start" + ",\"\t;\\\u4e2d\uD83C\uDFE0".repeat(16000) + "End")
                .withTags(tags).build();
        Path file = directory.resolve("long fields.csv");

        CsvAddressBookExporter.export(List.of(extreme), file);

        assertEquals(List.of(extreme), CsvAddressBookImporter.read(file));
    }

    @Test
    public void read_shuffledQuotedColumnsAndLineEndings_preservesMemberAndSourceBytes() throws Exception {
        for (int seed = 0; seed < 60; seed++) {
            Random random = new Random(seed);
            Person expected = RandomPersons.generate(random, 1).getFirst();
            List<String> headers = List.of("name", "phone", "email", "address", "tags");
            List<String> fields = List.of(expected.getName().fullName, expected.getPhone().value,
                    expected.getEmail().value, expected.getAddress().value,
                    String.join(";", expected.getTags().stream().map(tag -> tag.tagName).toList()));
            List<Integer> columns = new ArrayList<>(List.of(0, 1, 2, 3, 4));
            Collections.shuffle(columns, random);
            String ending = seed % 2 == 0 ? "\n" : "\r\n";
            String header = String.join(",", columns.stream().map(index -> "\"" + headers.get(index) + "\"").toList());
            String record = String.join(",", columns.stream().map(index -> quote(fields.get(index))).toList());
            String contents = (seed % 3 == 0 ? "\uFEFF" : "") + header + ending
                    + ending.repeat(seed) + record + (seed % 4 == 0 ? "" : ending);
            Path file = directory.resolve("shuffled.csv");
            Files.writeString(file, contents);
            byte[] before = Files.readAllBytes(file);

            assertEquals(List.of(expected), CsvAddressBookImporter.read(file), "seed=" + seed);
            assertArrayEquals(before, Files.readAllBytes(file), "seed=" + seed);
        }
    }

    @Test
    public void import_invalidFinalRecordAfterThousandValidRows_leavesModelAndSourceUnchanged() throws Exception {
        Path file = directory.resolve("late error.csv");
        CsvAddressBookExporter.export(RandomPersons.generate(new Random(1000), 1000), file);
        String validCsv = Files.readString(file);
        List<String> invalidRecords = List.of("Alice,12,a@example.com,Home,",
                "Alice,123,a@example.com,Home,tag;", "Alice,123,a@example.com,\"Unclosed",
                "Alice,123,a@example.com,\"Multiple\nlines\",", "Alice,123,a@example.com,Home,,");
        ModelManager model = new ModelManager();
        RandomPersons.generate(new Random(77), 5).forEach(model::addPerson);
        AddressBook before = new AddressBook(model.getAddressBook());
        for (String invalidRecord : invalidRecords) {
            Files.writeString(file, validCsv + invalidRecord);
            byte[] source = Files.readAllBytes(file);

            assertThrows(CommandException.class, () -> new ImportCommand(file).execute(model), invalidRecord);

            assertEquals(before, model.getAddressBook(), invalidRecord);
            assertArrayEquals(source, Files.readAllBytes(file), invalidRecord);
        }
    }

    @Test
    public void read_invalidUtf8Sequences_rejectsWithoutChangingSource() throws Exception {
        List<byte[]> invalidSequences = List.of(new byte[] {(byte) 0x80}, new byte[] {(byte) 0xC0, (byte) 0xAF},
                new byte[] {(byte) 0xED, (byte) 0xA0, (byte) 0x80},
                new byte[] {(byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80},
                new byte[] {(byte) 0xE2, (byte) 0x82}, new byte[] {(byte) 0xFF});
        Path file = directory.resolve("invalid encoding.csv");
        for (byte[] invalid : invalidSequences) {
            CsvAddressBookExporter.export(RandomPersons.generate(new Random(8), 2), file);
            Files.write(file, invalid, StandardOpenOption.APPEND);
            byte[] source = Files.readAllBytes(file);

            CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));

            assertEquals(CsvAddressBookImporter.MESSAGE_INVALID_UTF8, error.getMessage());
            assertArrayEquals(source, Files.readAllBytes(file));
        }
    }

    @Test
    public void read_hugeUnclosedQuotedField_reportsCheckedSyntaxError() throws Exception {
        Path file = directory.resolve("unclosed.csv");
        Files.writeString(file, "name,phone,email,address,tags\nAlice,123,a@example.com,\""
                + "\"\"".repeat(50000) + "end");
        byte[] source = Files.readAllBytes(file);

        CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));

        assertEquals("row 2: " + CsvAddressBookImporter.MESSAGE_INVALID_CSV, error.getMessage());
        assertArrayEquals(source, Files.readAllBytes(file));
    }

    @Test
    public void read_tenThousandBlankRows_reportsPhysicalErrorRow() throws Exception {
        Path file = directory.resolve("blank rows.csv");
        Files.writeString(file, "name,phone,email,address,tags\r\n" + " \t \r\n".repeat(10000)
                + "Alice,123,a@example.com,Home,tag;");

        CsvImportException error = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(file));

        assertEquals("row 10002: " + Tag.MESSAGE_CONSTRAINTS, error.getMessage());
    }

    @Test
    public void exportImport_generatedRosters_preservesFieldsOrderAndRepeatedRoundTrips() throws Exception {
        for (int seed = 0; seed < 40; seed++) {
            Random random = new Random(seed);
            int size = seed == 0 ? 0 : seed == 1 ? 500 : 1 + random.nextInt(100);
            List<Person> expected = RandomPersons.generate(random, size);
            Path file = directory.resolve("member list.csv");
            List<Person> actual = expected;
            for (int round = 0; round < 3; round++) {
                // Re-export decoded values to detect escaping drift across repeated round trips.
                CsvAddressBookExporter.export(actual, file);
                actual = CsvAddressBookImporter.read(file);
                assertEquals(expected, actual, "seed=" + seed + ", round=" + round);
            }
        }
    }

    @Test
    public void export_encodingFailureAfterValidRows_preservesPreviousExportAndRemovesTemporaryFile() throws Exception {
        for (int seed = 0; seed < 20; seed++) {
            Random random = new Random(seed);
            List<Person> original = RandomPersons.generate(random, 10);
            Path file = directory.resolve("members.csv");
            CsvAddressBookExporter.export(original, file);
            byte[] previous = Files.readAllBytes(file);
            List<Person> replacement = new ArrayList<>(RandomPersons.generate(random, 50));
            int brokenIndex = 1 + random.nextInt(replacement.size() - 1);
            replacement.set(brokenIndex, new PersonBuilder(replacement.get(brokenIndex))
                    .withAddress("Broken \uD800 unicode").build());

            assertThrows(IOException.class, () -> CsvAddressBookExporter.export(replacement, file), "seed=" + seed);

            assertArrayEquals(previous, Files.readAllBytes(file), "seed=" + seed);
            assertEquals(original, CsvAddressBookImporter.read(file), "seed=" + seed);
            try (var entries = Files.list(directory)) {
                assertEquals(List.of(file), entries.toList(), "seed=" + seed);
            }
        }
    }

    /** Encodes independent CSV fixtures with whitespace outside and inside each quoted field. */
    private static String quote(String field) {
        return " \t\" \t" + field.replace("\"", "\"\"") + " \t\" \t";
    }
}

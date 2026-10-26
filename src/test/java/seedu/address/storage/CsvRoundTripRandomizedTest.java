package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.RandomPersons;

/** Checks CSV invariants against generated data, with fixed seeds to reproduce failures. */
public class CsvRoundTripRandomizedTest {
    @TempDir
    public Path directory;

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
}

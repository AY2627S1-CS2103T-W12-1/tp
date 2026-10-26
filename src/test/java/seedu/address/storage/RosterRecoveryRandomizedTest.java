package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.RandomPersons;

/** Exercises failed saves and recovery across fresh storage instances against disposable files. */
public class RosterRecoveryRandomizedTest {
    @TempDir
    public Path directory;

    @Test
    public void read_deeplyNestedJson_reportsLoadingErrorAndProtectsBackup() throws Exception {
        Path file = directory.resolve("nested.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(roster(new Random(41)));
        storage.saveAddressBook(new AddressBook());
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        byte[] backupBytes = Files.readAllBytes(backup);
        // UG requires persons to contain member objects, never nested arrays of numbers.
        Files.writeString(file, "{\"persons\":" + "[".repeat(2000) + "0" + "]".repeat(2000) + "}");
        byte[] damaged = Files.readAllBytes(file);

        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));

        assertArrayEquals(damaged, Files.readAllBytes(file));
        assertArrayEquals(backupBytes, Files.readAllBytes(backup));
    }

    @Test
    public void read_everyTruncatedByteOffset_rejectsWithoutTouchingBackup() throws Exception {
        Path file = directory.resolve("truncated.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook expected = new AddressBook();
        expected.addPerson(new PersonBuilder().withAddress("Caf\u00e9 \u4e2d\u6587 \uD83C\uDFE0").build());
        storage.saveAddressBook(new AddressBook());
        storage.saveAddressBook(expected);
        byte[] complete = Files.readAllBytes(file);
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        byte[] backupBytes = Files.readAllBytes(backup);
        for (int offset = 0; offset < complete.length; offset++) {
            byte[] truncated = Arrays.copyOf(complete, offset);
            Files.write(file, truncated);

            assertThrows(DataLoadingException.class, storage::readAddressBook, "offset=" + offset);
            assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()), "offset=" + offset);

            assertArrayEquals(truncated, Files.readAllBytes(file), "offset=" + offset);
            assertArrayEquals(backupBytes, Files.readAllBytes(backup), "offset=" + offset);
        }
    }

    @Test
    public void recovery_repeatedCorruptionCycles_keepsAllPreservedCopiesAndRotatesBackupCorrectly() throws Exception {
        Random random = new Random(2026);
        Path file = directory.resolve("cycles.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook expected = roster(random);
        storage.saveAddressBook(expected);
        storage.saveAddressBook(roster(random));
        Map<Path, byte[]> preservedCopies = new LinkedHashMap<>();
        for (int cycle = 0; cycle < 20; cycle++) {
            byte[] damaged = {(byte) 0xFF, '{', (byte) cycle, (byte) random.nextInt(256)};
            Files.write(file, damaged);
            storage = new JsonAddressBookStorage(file);
            Path preserved = storage.preserveDamagedFile();
            preservedCopies.put(preserved, damaged);
            storage.recoverAddressBook(storage.readBackup().orElseThrow(), preserved);
            assertEquals(expected, storage.readAddressBook().orElseThrow(), "cycle=" + cycle);
            AddressBook next = cycle % 4 == 0 ? new AddressBook() : roster(random);
            // Updated UG: unchanged saves retain the previous distinct backup.
            storage.saveAddressBook(next);
            assertEquals(expected, storage.readBackup().orElseThrow(), "cycle=" + cycle);
            storage.saveAddressBook(next);
            assertEquals(expected, storage.readBackup().orElseThrow(), "unchanged save, cycle=" + cycle);
            storage.saveAddressBook(roster(random));
            expected = next;
            assertEquals(expected, new JsonAddressBookStorage(file).readBackup().orElseThrow());
            for (Map.Entry<Path, byte[]> entry : preservedCopies.entrySet()) {
                assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), "cycle=" + cycle);
            }
        }
        assertEquals(20, preservedCopies.size());
        try (var entries = Files.list(directory)) {
            assertEquals(22, entries.count());
        }
    }

    @Test
    public void recovery_sameLengthOriginalChanges_refusesReplacementUntilNewCopyIsPreserved() throws Exception {
        Path file = directory.resolve("changed.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(roster(new Random(19)));
        storage.saveAddressBook(new AddressBook());
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        byte[] backupBytes = Files.readAllBytes(backup);
        Files.writeString(file, "Broken roster A");
        Path oldCopy = storage.preserveDamagedFile();
        Files.writeString(file, "Broken roster B");

        assertThrows(IOException.class, () -> storage.recoverAddressBook(new AddressBook(), oldCopy));

        assertEquals("Broken roster B", Files.readString(file));
        assertEquals("Broken roster A", Files.readString(oldCopy));
        assertArrayEquals(backupBytes, Files.readAllBytes(backup));
        Path newCopy = storage.preserveDamagedFile();
        assertNotEquals(oldCopy, newCopy);
        storage.recoverAddressBook(storage.readBackup().orElseThrow(), newCopy);
        assertEquals("Broken roster B", Files.readString(newCopy));
        assertArrayEquals(backupBytes, Files.readAllBytes(backup));
    }

    @Test
    public void recovery_invalidOriginalAndBackup_preservesBothWhenStartingEmpty() throws Exception {
        List<String> corruptions = List.of("null", "[]", "{\"persons\":[null]}", "{\"persons\":[]}{\"persons\":[]}",
                "{\"persons\":[{\"name\":\"Alice\",\"phone\":\"12\",\"email\":\"a@example.com\","
                        + "\"address\":\"Home\",\"tags\":[]}]}");
        for (int index = 0; index < corruptions.size(); index++) {
            Path file = directory.resolve("invalid" + index + ".json");
            Path backup = JsonAddressBookStorage.getBackupFilePath(file);
            Files.writeString(file, corruptions.get(index));
            Files.writeString(backup, corruptions.get((index + 1) % corruptions.size()));
            byte[] originalBytes = Files.readAllBytes(file);
            byte[] backupBytes = Files.readAllBytes(backup);
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

            assertThrows(DataLoadingException.class, storage::readAddressBook, "case=" + index);
            assertThrows(DataLoadingException.class, storage::readBackup, "case=" + index);
            Path preserved = storage.preserveDamagedFile();
            storage.recoverAddressBook(new AddressBook(), preserved);

            assertEquals(new AddressBook(), new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
            assertArrayEquals(originalBytes, Files.readAllBytes(preserved));
            assertArrayEquals(backupBytes, Files.readAllBytes(backup));
        }
    }

    @Test
    public void save_truncatedRosters_refusesOverwriteAndRecoversPreviousVersionAcrossRestart() throws Exception {
        for (int seed = 0; seed < 30; seed++) {
            Random random = new Random(seed);
            Path file = directory.resolve("members" + seed + ".json");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
            AddressBook previous = roster(random);
            storage.saveAddressBook(previous);
            storage.saveAddressBook(roster(random));
            Path backup = JsonAddressBookStorage.getBackupFilePath(file);
            byte[] backupBytes = Files.readAllBytes(backup);
            byte[] saved = Files.readAllBytes(file);
            byte[] damaged = Arrays.copyOf(saved, random.nextInt(saved.length - 1));
            Files.write(file, damaged);

            JsonAddressBookStorage restarted = new JsonAddressBookStorage(file);
            assertThrows(DataLoadingException.class, restarted::readAddressBook, "seed=" + seed);
            assertThrows(IOException.class, () -> restarted.saveAddressBook(new AddressBook()), "seed=" + seed);
            assertArrayEquals(damaged, Files.readAllBytes(file), "seed=" + seed);
            assertArrayEquals(backupBytes, Files.readAllBytes(backup), "seed=" + seed);

            Path preserved = restarted.preserveDamagedFile();
            restarted.recoverAddressBook(restarted.readBackup().orElseThrow(), preserved);

            assertEquals(previous, new JsonAddressBookStorage(file).readAddressBook().orElseThrow(), "seed=" + seed);
            assertArrayEquals(damaged, Files.readAllBytes(preserved), "seed=" + seed);
            assertArrayEquals(backupBytes, Files.readAllBytes(backup), "seed=" + seed);
            AddressBook next = roster(random);
            restarted.saveAddressBook(next);
            assertEquals(previous, restarted.readBackup().orElseThrow(), "seed=" + seed);
            assertEquals(next, new JsonAddressBookStorage(file).readAddressBook().orElseThrow(), "seed=" + seed);
        }
    }

    @Test
    public void save_backupBlockedThenRepaired_retriesWithoutLosingRoster() throws Exception {
        Random random = new Random(2103);
        Path file = directory.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook previous = roster(random);
        storage.saveAddressBook(previous);
        byte[] previousBytes = Files.readAllBytes(file);
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        Files.createDirectory(backup);
        Path blocker = backup.resolve("keep.txt");
        Files.writeString(blocker, "Blocks backup replacement on every supported OS");
        AddressBook replacement = roster(random);

        assertThrows(IOException.class, () -> storage.saveAddressBook(replacement));
        assertArrayEquals(previousBytes, Files.readAllBytes(file));
        assertEquals(previous, new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        Files.delete(blocker);
        Files.delete(backup);
        storage.saveAddressBook(replacement);

        assertArrayEquals(previousBytes, Files.readAllBytes(backup));
        assertEquals(replacement, new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        try (var entries = Files.list(directory)) {
            assertEquals(2, entries.count());
        }
    }

    private static AddressBook roster(Random random) {
        AddressBook result = new AddressBook();
        RandomPersons.generate(random, 1 + random.nextInt(25)).forEach(result::addPerson);
        return result;
    }
}

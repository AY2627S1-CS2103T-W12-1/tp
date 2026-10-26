package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.testutil.RandomPersons;

/** Exercises failed saves and recovery across fresh storage instances against disposable files. */
public class RosterRecoveryRandomizedTest {
    @TempDir
    public Path directory;

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

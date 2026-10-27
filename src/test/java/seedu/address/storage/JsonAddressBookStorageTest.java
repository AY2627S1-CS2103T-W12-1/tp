package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.HOON;
import static seedu.address.testutil.TypicalPersons.IDA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;

/** Tests complete roster persistence and rejection of invalid data files. */
public class JsonAddressBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonAddressBookStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readAddressBook(null));
    }

    private java.util.Optional<ReadOnlyAddressBook> readAddressBook(String filePath) throws Exception {
        return new JsonAddressBookStorage(Paths.get(filePath)).readAddressBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readAddressBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void readAddressBook_nullRoot_throwsDataLoadingException() throws IOException {
        assertInvalidDataRejected("null");
    }

    @Test
    public void readAddressBook_nullPerson_throwsDataLoadingException() throws IOException {
        assertInvalidDataRejected("{\"persons\":[null]}");
    }

    @Test
    public void readAddressBook_nullTag_throwsDataLoadingException() throws IOException {
        assertInvalidDataRejected("{\"persons\":[{\"name\":\"Alice\",\"phone\":\"123\","
                + "\"email\":\"alice@example.com\",\"address\":\"Main Street\",\"tags\":[null]}]}");
    }

    @Test
    public void readAddressBook_contactsDifferingOnlyInPadding_rejectsDuplicateRecords() throws IOException {
        assertInvalidDataRejected("{\"persons\":[{\"name\":\"Alice\",\"phone\":\"123\","
                + "\"email\":\"alice@example.com\",\"address\":\"Main Street\",\"tags\":[]},"
                + "{\"name\":\"Alice \",\"phone\":\"123\",\"email\":\"alice@example.com\","
                + "\"address\":\"Main Street \",\"tags\":[\"committee\"]}]}");
    }

    @Test
    public void readAddressBook_trailingContent_throwsDataLoadingException() throws IOException {
        for (String suffix : new String[] {"{}", "null", "trailing text", "[]", "42"}) {
            assertInvalidDataRejected("{\"persons\":[]} " + suffix);
        }
    }

    @Test
    public void readAddressBook_invalidUtf8_throwsDataLoadingExceptionWithoutChangingBytes() throws IOException {
        byte[] contents = ("{\"persons\":[{\"name\":\"Alice\",\"phone\":\"123\","
                + "\"email\":\"alice@example.com\",\"address\":\"Main ? Street\",\"tags\":[]}]}")
                .getBytes(StandardCharsets.UTF_8);
        for (int index = 0; index < contents.length; index++) {
            if (contents[index] == '?') {
                contents[index] = (byte) 0x80;
            }
        }
        Path file = testFolder.resolve("invalid-utf8.json");
        Files.write(file, contents);

        assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(file).readAddressBook());
        assertArrayEquals(contents, Files.readAllBytes(file));
    }

    @Test
    public void readAddressBook_trailingWhitespace_loadsEmptyRoster() throws Exception {
        Path file = testFolder.resolve("empty.json");
        Files.writeString(file, "{\"persons\":[]} \n\t\r\n");
        assertEquals(new AddressBook(), new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
    }

    /** Verifies corrupt data is rejected without changing the file or escaping as an unchecked exception. */
    private void assertInvalidDataRejected(String contents) throws IOException {
        Path file = testFolder.resolve("invalid.json");
        Files.writeString(file, contents);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertEquals(contents, Files.readString(file));
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("notJsonFormatAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidAndValidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidAndValidPersonAddressBook.json"));
    }

    @Test
    public void readAndSaveAddressBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempAddressBook.json");
        AddressBook original = getTypicalAddressBook();
        JsonAddressBookStorage jsonAddressBookStorage = new JsonAddressBookStorage(filePath);

        // Save in new file and read back
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        ReadOnlyAddressBook readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonAddressBookStorage.saveAddressBook(original); // file path not specified
        readBack = jsonAddressBookStorage.readAddressBook().get(); // file path not specified
        assertEquals(original, new AddressBook(readBack));

    }

    @Test
    public void saveAddressBook_nullAddressBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code addressBook} at the specified {@code filePath}.
     */
    private void saveAddressBook(ReadOnlyAddressBook addressBook, String filePath) {
        try {
            new JsonAddressBookStorage(Paths.get(filePath))
                    .saveAddressBook(addressBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(new AddressBook(), null));
    }

    @Test
    public void save_previousValidRoster_rotatesBackup() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook original = getTypicalAddressBook();
        storage.saveAddressBook(original);
        assertFalse(Files.exists(JsonAddressBookStorage.getBackupFilePath(file)));
        byte[] originalBytes = Files.readAllBytes(file);

        storage.saveAddressBook(new AddressBook());

        assertArrayEquals(originalBytes, Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file)));
        assertEquals(original, storage.readBackup().orElseThrow());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
        storage.saveAddressBook(original);
        assertEquals(new AddressBook(), storage.readBackup().orElseThrow());
    }

    @Test
    public void save_unchangedEmptyRoster_preservesLastDistinctBackup() throws Exception {
        Path file = testFolder.resolve("unchanged.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook original = getTypicalAddressBook();
        storage.saveAddressBook(original);
        storage.saveAddressBook(new AddressBook());
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        byte[] recoveryBytes = Files.readAllBytes(backup);

        storage.saveAddressBook(new AddressBook());

        assertArrayEquals(recoveryBytes, Files.readAllBytes(backup));
        assertEquals(original, storage.readBackup().orElseThrow());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void save_corruptRoster_blocksReplacementAndPreservesGoodBackup() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(getTypicalAddressBook());
        storage.saveAddressBook(new AddressBook());
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        byte[] backupBytes = Files.readAllBytes(backup);
        Files.write(file, new byte[] {(byte) 0x80, '{'});

        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));

        assertArrayEquals(new byte[] {(byte) 0x80, '{'}, Files.readAllBytes(file));
        assertArrayEquals(backupBytes, Files.readAllBytes(backup));
    }

    @Test
    public void save_backupCannotBeWritten_leavesOriginalUntouched() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(getTypicalAddressBook());
        byte[] original = Files.readAllBytes(file);
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        Files.createDirectory(backup);
        Files.writeString(backup.resolve("keep.txt"), "Do not replace this directory");

        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));

        assertArrayEquals(original, Files.readAllBytes(file));
        assertTrue(Files.exists(backup.resolve("keep.txt")));
    }

    @Test
    public void recovery_repeatedRestarts_preservesExactBytesInDistinctFiles() throws Exception {
        Path file = testFolder.resolve("members.json");
        byte[] damaged = {(byte) 0x80, '{', '}'};
        Files.write(file, damaged);
        Path firstCopy = new JsonAddressBookStorage(file).preserveDamagedFile();
        Path secondCopy = new JsonAddressBookStorage(file).preserveDamagedFile();

        assertNotEquals(firstCopy, secondCopy);
        assertArrayEquals(damaged, Files.readAllBytes(firstCopy));
        assertArrayEquals(damaged, Files.readAllBytes(secondCopy));
        assertArrayEquals(damaged, Files.readAllBytes(file));
    }

    @Test
    public void recovery_restoreBackup_preservesBackupAndDamagedOriginal() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook original = getTypicalAddressBook();
        storage.saveAddressBook(original);
        storage.saveAddressBook(new AddressBook());
        Files.writeString(file, "damaged roster");
        Path preserved = storage.preserveDamagedFile();
        byte[] backup = Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file));

        storage.recoverAddressBook(storage.readBackup().orElseThrow(), preserved);

        assertEquals(original, storage.readAddressBook().orElseThrow());
        assertArrayEquals(backup, Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file)));
        assertEquals("damaged roster", Files.readString(preserved));
        assertEquals(original, new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
    }

    @Test
    public void recovery_invalidBackup_canExplicitlyStartEmptyAfterPreservingOriginal() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        Files.writeString(file, "damaged roster");
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        Files.writeString(backup, "damaged backup");
        assertThrows(DataLoadingException.class, storage::readBackup);
        Path preserved = storage.preserveDamagedFile();

        storage.recoverAddressBook(new AddressBook(), preserved);

        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
        assertEquals("damaged roster", Files.readString(preserved));
        assertEquals("damaged backup", Files.readString(backup));
    }

    @Test
    public void recovery_originalChangedOrNotPreserved_refusesReplacement() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        Files.writeString(file, "first damaged version");
        Path preserved = storage.preserveDamagedFile();
        Files.writeString(file, "changed damaged version");

        assertThrows(IOException.class, () -> storage.recoverAddressBook(new AddressBook(), preserved));
        assertThrows(IOException.class, () -> storage.recoverAddressBook(new AddressBook(), file));
        assertThrows(IOException.class, () -> storage.recoverAddressBook(new AddressBook(),
                testFolder.resolve("missing.json")));
        assertEquals("changed damaged version", Files.readString(file));
        assertEquals("first damaged version", Files.readString(preserved));
    }

    @Test
    public void recovery_nonFileOriginal_cannotBePreservedOrOverwritten() throws Exception {
        Path file = testFolder.resolve("members.json");
        Files.createDirectory(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        assertThrows(IOException.class, storage::preserveDamagedFile);
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
        assertTrue(Files.isDirectory(file));
    }
}

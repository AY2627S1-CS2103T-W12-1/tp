package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

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
    public void readAddressBook_inaccessibleParent_throwsWithoutChangingRoster() throws Exception {
        assumeTrue(Files.getFileAttributeView(testFolder, PosixFileAttributeView.class) != null);
        Path dataDirectory = Files.createDirectory(testFolder.resolve("data"));
        Path file = dataDirectory.resolve("addressbook.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(getTypicalAddressBook());
        byte[] original = Files.readAllBytes(file);
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(dataDirectory);

        try {
            Files.setPosixFilePermissions(dataDirectory, PosixFilePermissions.fromString("r--------"));
            assumeFalse(Files.isReadable(file), "Requires enforced directory access restrictions");
            assertThrows(DataLoadingException.class, storage::readAddressBook);
        } finally {
            Files.setPosixFilePermissions(dataDirectory, originalPermissions);
        }

        assertArrayEquals(original, Files.readAllBytes(file));
        assertEquals(getTypicalAddressBook(), storage.readAddressBook().orElseThrow());
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

    @Test
    public void readAddressBook_byteOrderMark_loadsRosterWithoutChangingFile() throws Exception {
        Path file = testFolder.resolve("bom.json");
        String contents = "\uFEFF{\"persons\":[{\"name\":\"Alice\",\"phone\":\"123\","
                + "\"email\":\"alice@example.com\",\"address\":\"Main Street\",\"tags\":[\"friends\"]}]}";
        Files.writeString(file, contents);
        Person alice = new PersonBuilder().withName("Alice").withPhone("123").withEmail("alice@example.com")
                .withAddress("Main Street").withTags("friends").build();

        ReadOnlyAddressBook loaded = new JsonAddressBookStorage(file).readAddressBook().orElseThrow();

        assertEquals(List.of(alice), loaded.getPersonList());
        assertEquals(contents, Files.readString(file));
    }

    @Test
    public void readAddressBook_byteOrderMarkInsideRoster_throwsDataLoadingException() throws IOException {
        assertInvalidDataRejected("\uFEFF\uFEFF{\"persons\":[]}");
    }

    @Test
    public void saveAddressBook_fileCreatedAfterMissingRead_refusesThenBacksUpOutsideVersion() throws Exception {
        for (AddressBook outside : List.of(new AddressBook(), getTypicalAddressBook())) {
            Path file = testFolder.resolve("appeared-" + outside.getPersonList().size() + ".json");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
            assertTrue(storage.readAddressBook().isEmpty());
            new JsonAddressBookStorage(file).saveAddressBook(outside);
            Path backup = JsonAddressBookStorage.getBackupFilePath(file);
            Files.writeString(backup, "Retain the previous backup");
            byte[] outsideBytes = Files.readAllBytes(file);
            byte[] backupBytes = Files.readAllBytes(backup);
            AddressBook session = new AddressBook();
            session.addPerson(IDA);

            assertThrows(IOException.class, JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, () ->
                    storage.saveAddressBook(session));
            assertArrayEquals(outsideBytes, Files.readAllBytes(file));
            assertArrayEquals(backupBytes, Files.readAllBytes(backup));

            storage.saveAddressBook(session);
            assertEquals(session, storage.readAddressBook(file).orElseThrow());
            assertEquals(outside, storage.readBackup().orElseThrow());
        }
    }

    @Test
    public void saveAddressBook_fileStillMissingAfterRead_allowsInitialSave() throws Exception {
        Path file = testFolder.resolve("first-save.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        assertTrue(storage.readAddressBook().isEmpty());

        storage.saveAddressBook(getTypicalAddressBook());

        assertEquals(getTypicalAddressBook(), storage.readAddressBook(file).orElseThrow());
        assertFalse(Files.exists(JsonAddressBookStorage.getBackupFilePath(file)));
    }

    @Test
    public void saveAddressBook_appearedFileMatchesSession_retryBacksUpExactOutsideBytes() throws Exception {
        for (AddressBook outside : List.of(new AddressBook(), getTypicalAddressBook())) {
            for (boolean hasBackup : new boolean[] {false, true}) {
                Path file = testFolder.resolve("matching-" + outside.getPersonList().size() + hasBackup + ".json");
                JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
                assertTrue(storage.readAddressBook().isEmpty());
                JsonUtil.saveJsonFile(new JsonSerializableAddressBook(outside), file);
                Files.writeString(file, "\uFEFF" + Files.readString(file) + "\n\t");
                Path backup = JsonAddressBookStorage.getBackupFilePath(file);
                if (hasBackup) {
                    Files.writeString(backup, "Retain the previous backup");
                }
                byte[] outsideBytes = Files.readAllBytes(file);
                AddressBook session = new AddressBook(outside);

                assertThrows(IOException.class, JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, () ->
                        storage.saveAddressBook(session));
                assertArrayEquals(outsideBytes, Files.readAllBytes(file));
                if (hasBackup) {
                    assertEquals("Retain the previous backup", Files.readString(backup));
                } else {
                    assertFalse(Files.exists(backup));
                }

                storage.saveAddressBook(session);

                assertArrayEquals(outsideBytes, Files.readAllBytes(backup));
                assertEquals(session, storage.readAddressBook(file).orElseThrow());
                storage.saveAddressBook(session);
                assertArrayEquals(outsideBytes, Files.readAllBytes(backup));
            }
        }
    }

    @Test
    public void saveAddressBook_changedFileMatchesSession_retryBacksUpOutsideVersion() throws Exception {
        Path file = testFolder.resolve("changed-matching.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(getTypicalAddressBook());
        AddressBook outside = new AddressBook();
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(outside), file);
        Files.writeString(file, Files.readString(file) + "\n\t");
        byte[] outsideBytes = Files.readAllBytes(file);

        assertThrows(IOException.class, JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, () ->
                storage.saveAddressBook(outside));
        storage.saveAddressBook(outside);

        assertArrayEquals(outsideBytes, Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file)));
    }

    @Test
    public void saveAddressBook_matchingRetryWriteFails_restoresBackupAndPreservesNextRetry() throws Exception {
        for (boolean hasBackup : new boolean[] {false, true}) {
            Path file = testFolder.resolve("failed-matching-" + hasBackup + ".json");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file) {
                private boolean shouldFailWrite = true;

                @Override
                protected void writeActiveRoster(ReadOnlyAddressBook addressBook, Path target) throws IOException {
                    if (shouldFailWrite) {
                        shouldFailWrite = false;
                        throw new IOException("Injected active roster write failure");
                    }
                    super.writeActiveRoster(addressBook, target);
                }
            };
            assertTrue(storage.readAddressBook().isEmpty());
            AddressBook outside = getTypicalAddressBook();
            JsonUtil.saveJsonFile(new JsonSerializableAddressBook(outside), file);
            Files.writeString(file, Files.readString(file) + "\n\t");
            byte[] outsideBytes = Files.readAllBytes(file);
            Path backup = JsonAddressBookStorage.getBackupFilePath(file);
            if (hasBackup) {
                Files.writeString(backup, "Retain the previous backup");
            }

            assertThrows(IOException.class, JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, () ->
                    storage.saveAddressBook(outside));
            assertThrows(IOException.class, "Injected active roster write failure", () ->
                    storage.saveAddressBook(outside));
            assertArrayEquals(outsideBytes, Files.readAllBytes(file));
            if (hasBackup) {
                assertEquals("Retain the previous backup", Files.readString(backup));
            } else {
                assertFalse(Files.exists(backup));
            }

            storage.saveAddressBook(outside);

            assertArrayEquals(outsideBytes, Files.readAllBytes(backup));
            assertEquals(outside, storage.readAddressBook(file).orElseThrow());
        }
    }

    @Test
    public void saveAddressBook_fileChangedOutsideAfterLoad_refusesOnceThenKeepsOutsideVersionInBackup()
            throws Exception {
        Path file = testFolder.resolve("members.json");
        new JsonAddressBookStorage(file).saveAddressBook(getTypicalAddressBook());
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.readAddressBook();
        AddressBook outside = getTypicalAddressBook();
        outside.addPerson(HOON);
        new JsonAddressBookStorage(file).saveAddressBook(outside);
        AddressBook session = getTypicalAddressBook();
        session.addPerson(IDA);

        assertThrows(IOException.class, JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, () ->
                storage.saveAddressBook(session));
        assertEquals(outside, storage.readAddressBook(file).orElseThrow());

        storage.saveAddressBook(session);
        assertEquals(session, storage.readAddressBook(file).orElseThrow());
        assertEquals(outside, storage.readBackup().orElseThrow());
    }

    @Test
    public void saveAddressBook_fileChangedOutsideAfterSave_refusesSave() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(getTypicalAddressBook());
        new JsonAddressBookStorage(file).saveAddressBook(new AddressBook());

        assertThrows(IOException.class, JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, () ->
                storage.saveAddressBook(getTypicalAddressBook()));
        assertEquals(new AddressBook(), storage.readAddressBook(file).orElseThrow());
    }

    @Test
    public void saveAddressBook_fileReformattedOutsideWithSameData_saves() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(getTypicalAddressBook());
        Files.writeString(file, Files.readString(file).replace("\n", "\n\n"));
        AddressBook session = getTypicalAddressBook();
        session.addPerson(HOON);

        storage.saveAddressBook(session);
        assertEquals(session, storage.readAddressBook(file).orElseThrow());
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
    public void save_caseOnlyTagChange_createsBackupWithPreviousSpelling() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(rosterWithTag("Committee"));
        byte[] previousRoster = Files.readAllBytes(file);

        storage.saveAddressBook(rosterWithTag("committee"));

        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        assertArrayEquals(previousRoster, Files.readAllBytes(backup));
        assertTrue(Files.readString(file).contains("committee"));
    }

    @Test
    public void save_caseOnlyTagChange_replacesExistingBackup() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(rosterWithTag("Earlier"));
        storage.saveAddressBook(rosterWithTag("Committee"));
        byte[] previousRoster = Files.readAllBytes(file);

        storage.saveAddressBook(rosterWithTag("committee"));

        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        assertArrayEquals(previousRoster, Files.readAllBytes(backup));
        byte[] backupAfterRename = Files.readAllBytes(backup);
        storage.saveAddressBook(rosterWithTag("committee"));
        assertArrayEquals(backupAfterRename, Files.readAllBytes(backup));
    }

    @Test
    public void save_caseOnlyTagChangeWithFailedWrite_preservesExistingBackup() throws Exception {
        Path file = testFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(rosterWithTag("Earlier"));
        storage.saveAddressBook(rosterWithTag("Committee"));
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        byte[] activeBefore = Files.readAllBytes(file);
        byte[] backupBefore = Files.readAllBytes(backup);

        JsonAddressBookStorage failingStorage = storageWithFailingActiveWrite(file);
        assertThrows(IOException.class, () -> failingStorage.saveAddressBook(rosterWithTag("committee")));

        assertArrayEquals(activeBefore, Files.readAllBytes(file));
        assertArrayEquals(backupBefore, Files.readAllBytes(backup));
    }

    private AddressBook rosterWithTag(String tagName) {
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(new PersonBuilder().withTags(tagName).build());
        return addressBook;
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
    public void save_activeWriteFails_preservesPreviousDistinctBackup() throws Exception {
        Path file = testFolder.resolve("members.json");
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        AddressBook earlier = getTypicalAddressBook();
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(new AddressBook()), file);
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(earlier), backup);
        byte[] activeBefore = Files.readAllBytes(file);
        byte[] backupBefore = Files.readAllBytes(backup);

        JsonAddressBookStorage storage = storageWithFailingActiveWrite(file);
        assertThrows(IOException.class, () -> storage.saveAddressBook(earlier));

        assertArrayEquals(activeBefore, Files.readAllBytes(file));
        assertArrayEquals(backupBefore, Files.readAllBytes(backup));
    }

    @Test
    public void save_activeWriteFails_removesNewBackup() throws Exception {
        Path file = testFolder.resolve("members.json");
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(new AddressBook()), file);
        byte[] activeBefore = Files.readAllBytes(file);

        JsonAddressBookStorage storage = storageWithFailingActiveWrite(file);
        assertThrows(IOException.class, () -> storage.saveAddressBook(getTypicalAddressBook()));

        assertArrayEquals(activeBefore, Files.readAllBytes(file));
        assertFalse(Files.exists(backup));
    }

    /** Injects a failure at the active-file write, after backup rotation has completed. */
    private JsonAddressBookStorage storageWithFailingActiveWrite(Path file) {
        return new JsonAddressBookStorage(file) {
            @Override
            protected void writeActiveRoster(ReadOnlyAddressBook addressBook, Path target) throws IOException {
                throw new IOException("Injected active roster write failure");
            }
        };
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
    @Test
    public void saveAddressBook_rollbackFails_retainsBackupCopyAndFailureReasons() throws Exception {
        Path file = testFolder.resolve("double-failure.json");
        Path backup = JsonAddressBookStorage.getBackupFilePath(file);
        JsonAddressBookStorage initial = new JsonAddressBookStorage(file);
        initial.saveAddressBook(getTypicalAddressBook());
        byte[] activeBefore = Files.readAllBytes(file);
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(new AddressBook()), backup);
        byte[] previousBackup = Files.readAllBytes(backup);
        JsonAddressBookStorage failing = new JsonAddressBookStorage(file) {
            @Override
            protected void writeActiveRoster(ReadOnlyAddressBook roster, Path target) throws IOException {
                // Simulate storage changing again between backup rotation and rollback.
                Files.delete(backup);
                Files.createDirectory(backup);
                Files.writeString(backup.resolve("blocked"), "Prevent replacing the backup directory");
                throw new IOException("Active write failed");
            }
        };

        IOException error = org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () ->
                failing.saveAddressBook(new AddressBook()));

        assertEquals("Active write failed", error.getMessage());
        assertEquals(1, error.getSuppressed().length);
        assertTrue(error.getSuppressed()[0] instanceof IOException);
        assertArrayEquals(activeBefore, Files.readAllBytes(file));
        try (var siblings = Files.list(testFolder)) {
            List<Path> preserved = siblings.filter(path -> path.getFileName().toString()
                    .startsWith(".trackcall-backup-")).toList();
            assertEquals(1, preserved.size());
            assertArrayEquals(previousBackup, Files.readAllBytes(preserved.getFirst()));
        }
    }

}

package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.FileUtil;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;

/**
 * A class to access AddressBook data stored as a JSON file on the hard disk.
 */
public class JsonAddressBookStorage {

    public static final String MESSAGE_CHANGED_OUTSIDE = "The data file was changed outside TrackCall after it "
            + "was last loaded or saved, so it was not replaced. To keep those outside changes, exit and restart "
            + "TrackCall. To replace them with this session's roster, retry the save; the outside version will "
            + "be kept in the backup file.";

    private static final Logger logger = LogsCenter.getLogger(JsonAddressBookStorage.class);

    private Path filePath;
    // The roster this storage last read from or wrote to filePath, or null if it has not done so yet.
    private ReadOnlyAddressBook lastKnownRoster;

    public JsonAddressBookStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getAddressBookFilePath() {
        return filePath;
    }

    /** Returns the sibling file containing the previous valid saved roster. */
    public static Path getBackupFilePath(Path rosterPath) {
        return rosterPath.resolveSibling(rosterPath.getFileName() + ".bak");
    }

    /** Reads the backup using the same validation as the active roster. */
    public Optional<ReadOnlyAddressBook> readBackup() throws DataLoadingException {
        return readAddressBook(getBackupFilePath(filePath));
    }

    /**
     * Preserves the exact bytes of an unreadable roster in a unique sibling file.
     * Neither the active roster nor the last valid backup is changed.
     *
     * @throws IOException If the original cannot be read or its recovery copy cannot be written.
     */
    public Path preserveDamagedFile() throws IOException {
        Path source = FileUtil.resolvePath(filePath).toAbsolutePath();
        if (!Files.isRegularFile(source)) {
            throw new IOException("The roster is not a readable regular file: " + source);
        }
        Path preserved = source.resolveSibling(source.getFileName() + ".corrupt-" + UUID.randomUUID() + ".json");
        FileUtil.writeAtomically(preserved,
                temporary -> Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING));
        return preserved;
    }

    /**
     * Saves the user's chosen recovery roster without rotating the last valid backup.
     * Refuses replacement if the original changed after its recovery copy was made.
     *
     * @throws IOException If preservation cannot be verified or the replacement cannot be saved.
     */
    public void recoverAddressBook(ReadOnlyAddressBook addressBook, Path preservedFile) throws IOException {
        requireNonNull(addressBook);
        requireNonNull(preservedFile);
        Path source = FileUtil.resolvePath(filePath);
        Path preserved = FileUtil.resolvePath(preservedFile);
        if (Files.isSameFile(source, preserved) || Files.mismatch(source, preserved) != -1) {
            throw new IOException("The roster changed during recovery. Restart TrackCall to try again.");
        }
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(addressBook), source);
    }

    /**
     * Returns AddressBook data as a {@link ReadOnlyAddressBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        Optional<ReadOnlyAddressBook> loaded = readAddressBook(filePath);
        lastKnownRoster = loaded.map(AddressBook::new).orElse(null);
        return loaded;
    }

    /**
     * Similar to {@link #readAddressBook()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<JsonSerializableAddressBook> jsonAddressBook = JsonUtil.readJsonFile(
                filePath, JsonSerializableAddressBook.class);
        if (!jsonAddressBook.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonAddressBook.get().toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyAddressBook} to the storage.
     * The first save after the file was changed outside TrackCall is refused, so those changes are not
     * replaced without warning. Retrying the save replaces them and keeps them in the backup file.
     *
     * @param addressBook cannot be null.
     * @throws IOException if there was any problem writing to the file, or it was changed outside TrackCall.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        requireNonNull(addressBook);
        Optional<ReadOnlyAddressBook> changedOutside = findChangeOutside();
        if (changedOutside.isPresent()) {
            logger.warning("Refused to replace " + filePath + " because it was changed outside TrackCall");
            lastKnownRoster = changedOutside.get();
            throw new IOException(MESSAGE_CHANGED_OUTSIDE);
        }
        saveAddressBook(addressBook, filePath);
        lastKnownRoster = new AddressBook(addressBook);
    }

    /**
     * Similar to {@link #saveAddressBook(ReadOnlyAddressBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook, Path filePath) throws IOException {
        requireNonNull(addressBook);
        requireNonNull(filePath);

        Path target = FileUtil.resolvePath(filePath);
        // Validate before rotating the backup: a damaged roster must never replace the last good copy.
        try {
            Optional<ReadOnlyAddressBook> saved = readAddressBook(target);
            if (saved.isPresent() && !hasSameSavedData(saved.get(), addressBook)) {
                Path backup = FileUtil.resolvePath(getBackupFilePath(target));
                saveWithBackup(addressBook, target, backup);
                return;
            }
        } catch (DataLoadingException e) {
            throw new IOException("The existing roster could not be loaded. Restart TrackCall to recover it. "
                    + "The original file and backup have been left unchanged.", e);
        }
        writeActiveRoster(addressBook, target);
    }

    /**
     * Returns the roster now in the data file if it is valid and differs from the roster this storage last
     * read or wrote. An unreadable or invalid file is left for {@link #saveAddressBook(ReadOnlyAddressBook, Path)}
     * to report.
     */
    private Optional<ReadOnlyAddressBook> findChangeOutside() {
        if (lastKnownRoster == null) {
            return Optional.empty();
        }
        try {
            return readAddressBook(filePath)
                    .filter(current -> !hasSameSavedData(current, lastKnownRoster))
                    .map(AddressBook::new);
        } catch (DataLoadingException e) {
            return Optional.empty();
        }
    }

    /** Compares stored member values, including tag spelling that {@code Tag.equals} ignores. */
    private boolean hasSameSavedData(ReadOnlyAddressBook saved, ReadOnlyAddressBook updated) {
        List<Person> savedPersons = saved.getPersonList();
        List<Person> updatedPersons = updated.getPersonList();
        if (!savedPersons.equals(updatedPersons)) {
            return false;
        }
        for (int index = 0; index < savedPersons.size(); index++) {
            Set<String> savedTagNames = savedPersons.get(index).getTags().stream()
                    .map(tag -> tag.tagName).collect(Collectors.toSet());
            Set<String> updatedTagNames = updatedPersons.get(index).getTags().stream()
                    .map(tag -> tag.tagName).collect(Collectors.toSet());
            if (!savedTagNames.equals(updatedTagNames)) {
                return false;
            }
        }
        return true;
    }

    /** Keeps the previous backup available until the active roster has been saved successfully. */
    private void saveWithBackup(ReadOnlyAddressBook addressBook, Path target, Path backup) throws IOException {
        Path previousBackup = null;
        boolean backupRotated = false;
        boolean retainPreviousBackup = false;
        try {
            if (Files.exists(backup)) {
                previousBackup = Files.createTempFile(backup.toAbsolutePath().getParent(),
                        ".trackcall-backup-", ".tmp");
                Files.copy(backup, previousBackup, StandardCopyOption.REPLACE_EXISTING);
            }
            FileUtil.writeAtomically(backup,
                    temporary -> Files.copy(target, temporary, StandardCopyOption.REPLACE_EXISTING));
            backupRotated = true;
            writeActiveRoster(addressBook, target);
        } catch (IOException | RuntimeException failure) {
            if (backupRotated) {
                try {
                    if (previousBackup == null) {
                        Files.deleteIfExists(backup);
                    } else {
                        Files.move(previousBackup, backup, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                    retainPreviousBackup = true;
                }
            }
            throw failure;
        } finally {
            if (previousBackup != null && !retainPreviousBackup) {
                try {
                    Files.deleteIfExists(previousBackup);
                } catch (IOException cleanupFailure) {
                    logger.warning("Could not remove temporary backup copy: " + previousBackup);
                }
            }
        }
    }

    /** Writes the active roster after any required backup rotation. */
    protected void writeActiveRoster(ReadOnlyAddressBook addressBook, Path target) throws IOException {
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(addressBook), target);
    }

}

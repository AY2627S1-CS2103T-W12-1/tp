package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.FileUtil;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.AppInstanceLock;
import seedu.address.storage.CsvAddressBookExporter;
import seedu.address.storage.JsonAddressBookStorage;

/**
 * Exports the address book to a CSV file.
 */
public class ExportCommand extends Command {

    public static final String COMMAND_WORD = "export";
    public static final String DEFAULT_FILE_NAME = "addressbook.csv";
    public static final String MESSAGE_USAGE = COMMAND_WORD + " [FILEPATH]\n"
            + "Exports all members to a CSV file. If FILEPATH is omitted, writes " + DEFAULT_FILE_NAME
            + " beside the JAR, or in the working directory during development. The path must end in .csv.";
    public static final String MESSAGE_SUCCESS = "Exported %1$d members to %2$s.";
    public static final String MESSAGE_SINGLE = "Exported 1 member to %1$s.";
    public static final String MESSAGE_FAILURE = "Could not export members to %1$s: %2$s";
    public static final String MESSAGE_PROTECTED_DESTINATION =
            "The export destination is used by TrackCall. Choose a different file.";

    public static final String MESSAGE_DIRECTORY_DESTINATION =
            "The export destination is a folder. Choose a CSV file instead.";
    public static final String MESSAGE_MISSING_PARENT =
            "The parent folder does not exist. Create it first, or export to an existing folder.";
    public static final String MESSAGE_INVALID_PARENT =
            "The parent path is not a folder. Export to an existing folder.";
    public static final String MESSAGE_WRITE_DENIED =
            "Permission denied. Check that the destination file and folder are writable.";
    public static final String MESSAGE_WRITE_FAILURE =
            "Could not write the CSV file. Check the destination, folder permissions and available disk space.";

    private static final Logger logger = LogsCenter.getLogger(ExportCommand.class);

    private final Path filePath;

    /**
     * Creates an export command for the specified output path.
     */
    public ExportCommand(Path filePath) {
        this.filePath = requireNonNull(filePath);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        return execute(model, new Path[0]);
    }

    /**
     * Exports without replacing the running application or files used by automatic storage.
     *
     * @param model Model whose complete roster should be exported.
     * @param protectedFiles Active roster and preferences paths supplied by the logic layer.
     * @return Export feedback after the complete CSV has been saved.
     * @throws CommandException If the destination is protected or exporting fails.
     */
    public CommandResult execute(Model model, Path... protectedFiles) throws CommandException {
        requireNonNull(model);
        List<Person> people = model.getAddressBook().getPersonList();
        Path targetPath = filePath;
        try {
            targetPath = FileUtil.resolvePath(filePath).toAbsolutePath().normalize();
            checkDestination(targetPath, FileUtil.getApplicationLocation());
            checkDestination(targetPath, FileUtil.getHomeFolder().resolve(AppInstanceLock.FILE_NAME));
            for (Path protectedFile : protectedFiles) {
                checkDestination(targetPath, protectedFile);
                checkDestination(targetPath, JsonAddressBookStorage.getBackupFilePath(protectedFile));
                Path protectedPath = FileUtil.resolvePath(protectedFile).toAbsolutePath().normalize();
                if (hasSamePathIgnoringCase(targetPath.getParent(), protectedPath.getParent())
                        && targetPath.getFileName().toString().toLowerCase(Locale.ROOT)
                                .startsWith((protectedPath.getFileName() + ".corrupt-").toLowerCase(Locale.ROOT))) {
                    throw new IOException(MESSAGE_PROTECTED_DESTINATION);
                }
            }
            if (Files.isDirectory(targetPath)) {
                throw new IOException(MESSAGE_DIRECTORY_DESTINATION);
            }
            Path parent = targetPath.getParent();
            if (!Files.exists(parent)) {
                throw new IOException(MESSAGE_MISSING_PARENT);
            }
            if (!Files.isDirectory(parent)) {
                throw new IOException(MESSAGE_INVALID_PARENT);
            }
            try {
                CsvAddressBookExporter.export(people, targetPath);
            } catch (IOException e) {
                logger.warning("CSV export failed (" + e.getClass().getSimpleName() + ")");
                String details = e instanceof AccessDeniedException ? MESSAGE_WRITE_DENIED : MESSAGE_WRITE_FAILURE;
                throw new CommandException(String.format(MESSAGE_FAILURE, targetPath, details), e);
            }
        } catch (IOException e) {
            logger.warning("CSV export failed (" + e.getClass().getSimpleName() + ")");
            throw new CommandException(String.format(MESSAGE_FAILURE, targetPath, e.getMessage()), e);
        }
        logger.info(String.format("CSV export completed: %d member(s) written", people.size()));
        return new CommandResult(people.size() == 1 ? String.format(MESSAGE_SINGLE, targetPath)
                : String.format(MESSAGE_SUCCESS, people.size(), targetPath));
    }

    /**
     * Reserves application paths without regard to case, including before the files exist.
     * The same reservation on every platform prevents exports that become unsafe on a case-insensitive file system.
     * Existing aliases such as hard links are still rejected independently of their names.
     */
    private void checkDestination(Path target, Path protectedFile) throws IOException {
        Path protectedPath = FileUtil.resolvePath(protectedFile).toAbsolutePath().normalize();
        if (hasSamePathIgnoringCase(target, protectedPath)
                || (Files.exists(target) && Files.exists(protectedPath)
                && Files.isSameFile(target, protectedPath))) {
            throw new IOException(MESSAGE_PROTECTED_DESTINATION);
        }
    }

    /** Compares reserved path spellings, allowing root paths to have no parent. */
    private boolean hasSamePathIgnoringCase(Path first, Path second) {
        return first != null && second != null && first.toString().equalsIgnoreCase(second.toString());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ExportCommand otherExport && filePath.equals(otherExport.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
}

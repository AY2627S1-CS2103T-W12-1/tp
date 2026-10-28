package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.FileUtil;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.CsvImportException;

/**
 * Imports member records from a local CSV file.
 */
public class ImportCommand extends Command {

    public static final String COMMAND_WORD = "import";
    public static final String MESSAGE_USAGE = "import p/FILE_PATH";
    public static final String MESSAGE_SUCCESS =
            "Imported %1$d member(s) from \"%2$s\". Skipped %3$d duplicate record(s).";
    public static final String MESSAGE_FAILURE = "Could not import CSV: %s";
    public static final String MESSAGE_FILE_NOT_FOUND =
            "Could not import CSV: File not found: %s. Check the file name and location.";
    public static final String MESSAGE_INVALID_FILE = "Could not import CSV: %s No members were imported.";
    public static final String MESSAGE_SAVE_FAILURE = "Could not save data to file: %s";

    private static final Logger logger = LogsCenter.getLogger(ImportCommand.class);

    private final Path filePath;

    public ImportCommand(Path filePath) {
        this.filePath = requireNonNull(filePath);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        List<Person> candidates;
        Path resolvedPath = filePath;
        try {
            resolvedPath = FileUtil.resolvePath(filePath).toAbsolutePath().normalize();
            candidates = CsvAddressBookImporter.read(resolvedPath);
        } catch (CsvImportException e) {
            logger.warning("CSV import rejected: invalid file");
            throw new CommandException(String.format(MESSAGE_INVALID_FILE, ensureSentence(e.getMessage())), e);
        } catch (NoSuchFileException e) {
            logger.warning("CSV import failed because the file was not found");
            throw new CommandException(String.format(MESSAGE_FILE_NOT_FOUND,
                    resolvedPath.toAbsolutePath().normalize()), e);
        } catch (IOException e) {
            logger.warning("CSV import failed due to an I/O error");
            throw new CommandException(String.format(MESSAGE_FAILURE, getErrorDetails(e)), e);
        }

        List<Person> knownPeople = new ArrayList<>(model.getAddressBook().getPersonList());
        List<Person> peopleToAdd = new ArrayList<>();
        int duplicateCount = 0;
        for (Person candidate : candidates) {
            if (knownPeople.stream().anyMatch(candidate::isDuplicateContactOf)) {
                duplicateCount++;
            } else {
                knownPeople.add(candidate);
                peopleToAdd.add(candidate);
            }
        }

        peopleToAdd.forEach(model::addPerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        model.setNameSorting(false);
        logger.info(String.format("CSV import processed: %d member(s) added, %d duplicate(s) skipped",
                peopleToAdd.size(), duplicateCount));
        return new CommandResult(String.format(MESSAGE_SUCCESS, peopleToAdd.size(),
                resolvedPath, duplicateCount));
    }

    private static String ensureSentence(String message) {
        return message.endsWith(".") ? message : message + ".";
    }

    private static String getErrorDetails(IOException exception) {
        return Objects.toString(exception.getMessage(), exception.toString());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ImportCommand otherImport && filePath.equals(otherImport.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }
}

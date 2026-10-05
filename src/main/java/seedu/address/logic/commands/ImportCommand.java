package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public static final String MESSAGE_INVALID_FILE = "Could not import CSV: %s No members were imported.";
    public static final String MESSAGE_SAVE_FAILURE = "Could not save data to file: %s";

    private final Path filePath;

    public ImportCommand(Path filePath) {
        this.filePath = requireNonNull(filePath);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        List<Person> candidates;
        try {
            candidates = CsvAddressBookImporter.read(filePath);
        } catch (CsvImportException e) {
            throw new CommandException(String.format(MESSAGE_INVALID_FILE, ensureSentence(e.getMessage())), e);
        } catch (IOException e) {
            throw new CommandException(String.format(MESSAGE_FAILURE, getErrorDetails(e)), e);
        }

        List<Person> knownPeople = new ArrayList<>(model.getAddressBook().getPersonList());
        List<Person> peopleToAdd = new ArrayList<>();
        int duplicateCount = 0;
        for (Person candidate : candidates) {
            if (knownPeople.stream().anyMatch(candidate::isSamePerson)) {
                duplicateCount++;
            } else {
                knownPeople.add(candidate);
                peopleToAdd.add(candidate);
            }
        }

        peopleToAdd.forEach(model::addPerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        model.setNameSorting(false);
        return new CommandResult(String.format(MESSAGE_SUCCESS, peopleToAdd.size(),
                filePath.toAbsolutePath().normalize(), duplicateCount));
    }

    private static String ensureSentence(String message) {
        return message.endsWith(".") ? message : message + ".";
    }

    private static String getErrorDetails(IOException exception) {
        return exception.getMessage() == null ? exception.toString() : exception.getMessage();
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

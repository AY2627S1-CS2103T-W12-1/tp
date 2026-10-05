package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvAddressBookExporter;

/**
 * Exports the address book to a CSV file.
 */
public class ExportCommand extends Command {

    public static final String COMMAND_WORD = "export";
    public static final String DEFAULT_FILE_NAME = "addressbook.csv";
    public static final String MESSAGE_USAGE = COMMAND_WORD + " [FILEPATH]\n"
            + "Exports all people to a CSV file. If FILEPATH is omitted, writes " + DEFAULT_FILE_NAME
            + " in the current directory.";
    public static final String MESSAGE_SUCCESS = "Exported %1$d people to %2$s.";
    public static final String MESSAGE_FAILURE = "Could not export people to %1$s: %2$s";

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
        requireNonNull(model);
        List<Person> people = model.getAddressBook().getPersonList();
        try {
            CsvAddressBookExporter.export(people, filePath);
        } catch (IOException e) {
            throw new CommandException(String.format(MESSAGE_FAILURE, filePath, e.getMessage()), e);
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS,
                people.size(), filePath.toAbsolutePath().normalize()));
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

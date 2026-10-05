package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses the path for the basic {@code import} command.
 */
public class ImportCommandParser implements Parser<ImportCommand> {

    public static final String MESSAGE_EMPTY_PATH = "File path cannot be empty.";
    public static final String MESSAGE_NON_CSV_FILE = "File must have a .csv extension.";

    @Override
    public ImportCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        if (!trimmedArgs.startsWith("p/")) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE));
        }

        String pathValue = trimmedArgs.substring(2).trim();
        if (pathValue.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_PATH);
        }
        if (pathValue.chars().anyMatch(Character::isWhitespace)) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE));
        }
        if (!pathValue.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ParseException(MESSAGE_NON_CSV_FILE);
        }

        try {
            return new ImportCommand(Path.of(pathValue));
        } catch (InvalidPathException e) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE), e);
        }
    }
}

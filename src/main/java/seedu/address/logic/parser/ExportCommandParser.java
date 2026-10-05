package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import seedu.address.logic.commands.ExportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses arguments for the {@code export} command.
 */
public class ExportCommandParser implements Parser<ExportCommand> {

    @Override
    public ExportCommand parse(String args) throws ParseException {
        String filePath = args.trim();
        if (filePath.isEmpty()) {
            filePath = ExportCommand.DEFAULT_FILE_NAME;
        }

        try {
            return new ExportCommand(Path.of(filePath));
        } catch (InvalidPathException e) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ExportCommand.MESSAGE_USAGE), e);
        }
    }
}

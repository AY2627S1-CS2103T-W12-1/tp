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

    /**
     * Parses an optional destination path, preserving spaces within the path.
     * Blank arguments select the default filename in the application's current directory.
     *
     * @param args the arguments following the export command word.
     * @return an export command for the supplied path or the default filename.
     * @throws ParseException if the path is invalid for the current filesystem.
     */
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

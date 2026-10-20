package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.ArrayList;
import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ShowCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses the space-separated displayed indexes accepted by {@code show}.
 */
public class ShowCommandParser implements Parser<ShowCommand> {
    @Override
    public ShowCommand parse(String args) throws ParseException {
        List<Index> indexes = new ArrayList<>();
        for (String part : args.trim().split("\\s+")) {
            try {
                indexes.add(ParserUtil.parseIndex(part));
            } catch (ParseException exception) {
                throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ShowCommand.MESSAGE_USAGE),
                        exception);
            }
        }
        return new ShowCommand(indexes);
    }
}

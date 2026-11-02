package seedu.address.logic.parser;

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
            indexes.add(ParserUtil.parseCommandIndex(part, ShowCommand.MESSAGE_USAGE));
        }
        return new ShowCommand(indexes);
    }
}

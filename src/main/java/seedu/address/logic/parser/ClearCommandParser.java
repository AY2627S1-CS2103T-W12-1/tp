package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses displayed clearing and the explicit whole-roster override. */
public class ClearCommandParser implements Parser<ClearCommand> {
    /** Accepts no argument or exactly one lowercase {@code -all} flag. */
    @Override
    public ClearCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String option = args.strip();
        if (!option.isEmpty() && !option.equals("-all")) {
            throw new ParseException(ClearCommand.MESSAGE_USAGE);
        }
        return new ClearCommand(option.equals("-all"));
    }
}

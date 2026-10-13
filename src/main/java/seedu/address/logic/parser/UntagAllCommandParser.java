package seedu.address.logic.parser;

import seedu.address.logic.commands.UntagAllCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses the single tag accepted by {@code untagall}.
 */
public class UntagAllCommandParser implements Parser<UntagAllCommand> {

    /**
     * Parses {@code args} into an untagall command with exactly one valid tag.
     *
     * @throws ParseException If {@code args} does not contain exactly one valid {@code t/TAG}.
     */
    @Override
    public UntagAllCommand parse(String args) throws ParseException {
        return new UntagAllCommand(BulkTagArgumentParser.parse(args, UntagAllCommand.MESSAGE_USAGE));
    }
}

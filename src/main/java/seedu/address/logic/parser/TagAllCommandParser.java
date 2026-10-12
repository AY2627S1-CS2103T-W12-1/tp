package seedu.address.logic.parser;

import seedu.address.logic.commands.TagAllCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses the single tag accepted by {@code tagall}.
 */
public class TagAllCommandParser implements Parser<TagAllCommand> {

    /**
     * Parses {@code args} into a tagall command with exactly one valid tag.
     *
     * @throws ParseException If {@code args} does not contain exactly one valid {@code t/TAG}.
     */
    @Override
    public TagAllCommand parse(String args) throws ParseException {
        return new TagAllCommand(BulkTagArgumentParser.parse(args, TagAllCommand.MESSAGE_USAGE));
    }
}

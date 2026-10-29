package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;

import seedu.address.logic.commands.RenameTagCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses the current and new tags accepted by {@code renametag}.
 */
public class RenameTagCommandParser implements Parser<RenameTagCommand> {

    /** The current tag and the new tag. */
    private static final int REQUIRED_TAG_COUNT = 2;

    /**
     * Parses {@code args} into a renametag command with exactly two valid tags.
     *
     * @throws ParseException If {@code args} does not contain exactly two valid {@code t/TAG} values,
     *         contains another member field prefix, or both tags are the same.
     */
    @Override
    public RenameTagCommand parse(String args) throws ParseException {
        List<String> tagValues = BulkTagArgumentParser.parseTagValues(args, RenameTagCommand.MESSAGE_USAGE);

        if (tagValues.size() != REQUIRED_TAG_COUNT) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, RenameTagCommand.MESSAGE_USAGE));
        }

        Tag oldTag = BulkTagArgumentParser.parseTagName(tagValues.get(0));
        Tag newTag = BulkTagArgumentParser.parseTagName(tagValues.get(1));
        if (oldTag.tagName.equals(newTag.tagName)) {
            throw new ParseException(RenameTagCommand.MESSAGE_SAME_TAG);
        }
        return new RenameTagCommand(oldTag, newTag);
    }
}

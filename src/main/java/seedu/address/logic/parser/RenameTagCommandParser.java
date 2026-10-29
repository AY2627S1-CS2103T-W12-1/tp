package seedu.address.logic.parser;

import java.util.List;

import seedu.address.logic.commands.RenameTagCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses the current and new tags accepted by {@code renametag}.
 */
public class RenameTagCommandParser implements Parser<RenameTagCommand> {

    public static final String MESSAGE_CURRENT_TAG_PREFIX = "Current tag: ";
    public static final String MESSAGE_NEW_TAG_PREFIX = "New tag: ";

    public static final String MESSAGE_TAG_COUNT =
            "Expected two t/TAG parameters (current tag, then new tag), but received %d.";

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
            throw new ParseException(String.format(MESSAGE_TAG_COUNT, tagValues.size())
                    + "\n" + RenameTagCommand.MESSAGE_USAGE);
        }

        Tag oldTag = parseTag(tagValues.get(0), MESSAGE_CURRENT_TAG_PREFIX);
        Tag newTag = parseTag(tagValues.get(1), MESSAGE_NEW_TAG_PREFIX);
        if (oldTag.tagName.equals(newTag.tagName)) {
            throw new ParseException(RenameTagCommand.MESSAGE_SAME_TAG);
        }
        return new RenameTagCommand(oldTag, newTag);
    }

    /**
     * Parses {@code tagName} into a tag. If it is invalid, the error starts with {@code prefix}, which says
     * whether the current tag or the new tag is the invalid one.
     */
    private static Tag parseTag(String tagName, String prefix) throws ParseException {
        try {
            return BulkTagArgumentParser.parseTagName(tagName);
        } catch (ParseException e) {
            throw new ParseException(prefix + e.getMessage());
        }
    }
}

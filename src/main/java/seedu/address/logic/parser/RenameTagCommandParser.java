package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;

import seedu.address.logic.commands.RenameTagCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses the current and new tags accepted by {@code renametag}.
 */
public class RenameTagCommandParser implements Parser<RenameTagCommand> {
    public static final String MESSAGE_SAME_TAG = "The new tag must be different from the current tag.";

    /**
     * Parses {@code args} into a renametag command with exactly two valid tags.
     *
     * @throws ParseException If {@code args} does not contain exactly two valid {@code t/TAG} values,
     *         or both tags are the same.
     */
    @Override
    public RenameTagCommand parse(String args) throws ParseException {
        // A leading space lets the tokenizer recognize a prefix at the very start of the arguments.
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(" " + args, PREFIX_TAG);
        List<String> tagValues = argMultimap.getAllValues(PREFIX_TAG);

        if (!argMultimap.getPreamble().isEmpty() || tagValues.size() != 2) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, RenameTagCommand.MESSAGE_USAGE));
        }

        Tag oldTag = BulkTagArgumentParser.parseTagName(tagValues.get(0));
        Tag newTag = BulkTagArgumentParser.parseTagName(tagValues.get(1));
        if (oldTag.equals(newTag)) {
            throw new ParseException(MESSAGE_SAME_TAG);
        }
        return new RenameTagCommand(oldTag, newTag);
    }
}

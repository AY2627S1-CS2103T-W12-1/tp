package seedu.address.logic.parser;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses the single {@code t/TAG} argument shared by the bulk tag commands.
 */
public class BulkTagArgumentParser {
    public static final String MESSAGE_EMPTY_TAG = "Tag cannot be empty.";
    public static final String MESSAGE_INVALID_TAG = Tag.MESSAGE_CONSTRAINTS + ".";
    public static final String MESSAGE_MULTIPLE_TAGS = "Only one tag can be applied per command.";

    private BulkTagArgumentParser() {
        // Prevents instantiation of this utility class.
    }

    /**
     * Parses {@code args} into exactly one tag of 1 to 30 ASCII letters or digits, with optional internal hyphens.
     * Surrounding whitespace around the tag is ignored.
     *
     * @param args Arguments following the command word.
     * @param usage Usage message of the calling command, shown when the {@code t/} prefix is missing
     *         or other text appears before it.
     * @throws ParseException If the prefix is missing, other text appears before it,
     *         more than one tag is given, or the tag is empty or invalid.
     */
    public static Tag parse(String args, String usage) throws ParseException {
        requireAllNonNull(args, usage);
        // A leading space lets the tokenizer recognize a prefix at the very start of the arguments.
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(" " + args, PREFIX_TAG);
        List<String> tagValues = argMultimap.getAllValues(PREFIX_TAG);

        if (!argMultimap.getPreamble().isEmpty() || tagValues.isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, usage));
        }
        if (tagValues.size() > 1) {
            throw new ParseException(MESSAGE_MULTIPLE_TAGS);
        }

        return parseTagName(tagValues.get(0));
    }

    /**
     * Parses one trimmed {@code t/} value into a tag of 1 to 30 ASCII letters or digits,
     * with optional internal hyphens.
     *
     * @throws ParseException If {@code tagName} is empty or invalid.
     */
    public static Tag parseTagName(String tagName) throws ParseException {
        requireAllNonNull(tagName);
        if (tagName.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_TAG);
        }
        if (!Tag.isValidTagName(tagName)) {
            throw new ParseException(MESSAGE_INVALID_TAG);
        }
        return new Tag(tagName);
    }
}

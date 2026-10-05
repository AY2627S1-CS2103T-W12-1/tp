package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.FilterCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses the single tag accepted by {@code filter}.
 */
public class FilterCommandParser implements Parser<FilterCommand> {
    private static final Pattern TAG_ARGUMENT = Pattern.compile("^\\s*t/(.*?)\\s*$", Pattern.DOTALL);
    private static final Pattern VALID_TAG = Pattern.compile("[A-Za-z0-9]{1,30}");
    private static final String MESSAGE_INVALID_TAG = "A filter tag must contain 1 to 30 letters or digits.";

    /**
     * Parses {@code args} into a filter command with exactly one valid tag.
     */
    @Override
    public FilterCommand parse(String args) throws ParseException {
        Matcher matcher = TAG_ARGUMENT.matcher(args);
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }

        String tagName = matcher.group(1).trim();
        if (!VALID_TAG.matcher(tagName).matches()) {
            throw new ParseException(MESSAGE_INVALID_TAG);
        }
        return new FilterCommand(new Tag(tagName));
    }
}

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
    private static final Pattern EXTRA_TAG_ARGUMENT = Pattern.compile("\\s+t/");
    private static final Pattern EXTRA_PARAMETER = Pattern.compile("\\s+[A-Za-z]+/");
    private static final String MESSAGE_EXTRA_TAG = "Only one t/TAG parameter is allowed per filter command. "
            + "Apply another tag in a separate command.";

    /**
     * Parses {@code args} into a filter command with exactly one valid tag.
     */
    @Override
    public FilterCommand parse(String args) throws ParseException {
        Matcher matcher = TAG_ARGUMENT.matcher(args);
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }

        String tagArgument = matcher.group(1);
        if (EXTRA_TAG_ARGUMENT.matcher(tagArgument).find()) {
            throw new ParseException(MESSAGE_EXTRA_TAG);
        }
        if (EXTRA_PARAMETER.matcher(tagArgument).find()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }

        String tagName = tagArgument.trim();
        if (!Tag.isValidTagName(tagName)) {
            throw new ParseException(Tag.MESSAGE_CONSTRAINTS);
        }
        return new FilterCommand(new Tag(tagName));
    }
}

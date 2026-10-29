package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.FilterCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses the one or more tags accepted by {@code filter}. Several tags in one command are alternatives.
 */
public class FilterCommandParser implements Parser<FilterCommand> {
    private static final Pattern TAG_ARGUMENT = Pattern.compile("^\\s*t/(.*?)\\s*$", Pattern.DOTALL);
    /** Whitespace followed by {@code t/}, which separates the tags of one command. */
    private static final Pattern TAG_SEPARATOR = Pattern.compile("\\s+t/");
    /** Whitespace followed by a prefix other than {@code t/}, which {@code filter} does not accept. */
    private static final Pattern EXTRA_PARAMETER = Pattern.compile("\\s+(?!t/)[A-Za-z]+/");

    /**
     * Parses {@code args} into a filter command with at least one valid tag.
     */
    @Override
    public FilterCommand parse(String args) throws ParseException {
        Matcher matcher = TAG_ARGUMENT.matcher(args);
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }

        String tagArgument = matcher.group(1);
        if (EXTRA_PARAMETER.matcher(tagArgument).find()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }

        List<Tag> tags = new ArrayList<>();
        // A negative limit keeps empty trailing values, so "t/a t/" is rejected instead of ignored.
        for (String untrimmedName : TAG_SEPARATOR.split(tagArgument, -1)) {
            // Shares the tag commands' diagnostics, and rejects the whole command if any tag is invalid.
            tags.add(BulkTagArgumentParser.parseTagName(untrimmedName.trim()));
        }
        return new FilterCommand(tags);
    }
}

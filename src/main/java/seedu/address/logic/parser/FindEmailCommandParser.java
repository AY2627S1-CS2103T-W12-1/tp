package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;

import seedu.address.logic.commands.FindEmailCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.EmailContainsKeywordsPredicate;

/**
 * Parses whitespace-separated keywords for an email search.
 */
public class FindEmailCommandParser implements Parser<FindEmailCommand> {
    /**
     * Parses at least one literal keyword into an email-search command.
     *
     * @param args Search keywords separated by whitespace.
     * @throws ParseException If no keyword is supplied.
     * @throws NullPointerException If {@code args} is null.
     */
    @Override
    public FindEmailCommand parse(String args) throws ParseException {
        String trimmedArgs = requireNonNull(args).strip();
        if (trimmedArgs.isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindEmailCommand.MESSAGE_USAGE));
        }

        String[] keywords = trimmedArgs.split("\\p{javaWhitespace}+");
        return new FindEmailCommand(new EmailContainsKeywordsPredicate(List.of(keywords)));
    }
}

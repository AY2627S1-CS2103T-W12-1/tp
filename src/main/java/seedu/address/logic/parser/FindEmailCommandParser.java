package seedu.address.logic.parser;

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
        return new FindEmailCommand(new EmailContainsKeywordsPredicate(
                ParserUtil.parseKeywords(args, FindEmailCommand.MESSAGE_USAGE)));
    }
}

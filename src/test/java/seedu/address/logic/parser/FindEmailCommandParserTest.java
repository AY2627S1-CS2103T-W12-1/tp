package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindEmailCommand;
import seedu.address.model.person.EmailContainsKeywordsPredicate;

/**
 * Tests required email keywords, whitespace separation, and preservation of literal search terms.
 */
public class FindEmailCommandParserTest {
    private final FindEmailCommandParser parser = new FindEmailCommandParser();

    @Test
    public void parse_nullArgs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_emptyOrWhitespaceOnlyArgs_showsUsage() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindEmailCommand.MESSAGE_USAGE);
        for (String input : List.of("", "   ", "\t\n\r", "\u2003")) {
            assertParseFailure(parser, input, expectedMessage);
        }
    }

    @Test
    public void parse_oneKeyword_returnsFindEmailCommand() {
        FindEmailCommand expected = new FindEmailCommand(new EmailContainsKeywordsPredicate(List.of("alice@")));
        assertParseSuccess(parser, "alice@", expected);
    }

    @Test
    public void parse_multipleWhitespaceSeparatedKeywords_returnsOrSearch() {
        FindEmailCommand expected =
                new FindEmailCommand(new EmailContainsKeywordsPredicate(List.of("alice", "@example.com")));
        assertParseSuccess(parser, "alice @example.com", expected);
        assertParseSuccess(parser, " \n alice \t @example.com \r ", expected);
        assertParseSuccess(parser, "\u2003alice\u2003@example.com\u2003", expected);
    }

    @Test
    public void parse_punctuationAndRegexCharacters_keepsLiteralKeywords() {
        FindEmailCommand expected =
                new FindEmailCommand(new EmailContainsKeywordsPredicate(List.of("+club@", ".*", "a|b")));
        assertParseSuccess(parser, "+club@ .* a|b", expected);
    }
}

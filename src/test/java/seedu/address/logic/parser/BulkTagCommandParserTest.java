package seedu.address.logic.parser;

import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_TAG_TOO_LONG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_UNSUPPORTED_CHARACTER;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_WHITESPACE;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.Command;
import seedu.address.model.tag.Tag;

/**
 * Tests shared by the parsers of the bulk tag commands, which each take exactly one {@code t/TAG}.
 *
 * @param <T> The command produced by the parser under test.
 */
public abstract class BulkTagCommandParserTest<T extends Command> {

    /** Returns the parser under test. */
    protected abstract Parser<T> getParser();

    /** Returns the command that the parser should produce for {@code tag}. */
    protected abstract T createCommand(Tag tag);

    /** Returns the usage message of the command, shown for a malformed command. */
    protected abstract String getUsage();

    @Test
    public void parse_validTag_returnsCommand() {
        assertParseSuccess(getParser(), " t/committee", createCommand(new Tag("committee")));
        assertParseSuccess(getParser(), "\t t/  Batch2026 \t", createCommand(new Tag("Batch2026")));
        assertParseSuccess(getParser(), " t/" + "a".repeat(30), createCommand(new Tag("a".repeat(30))));
    }

    @Test
    public void parse_missingPrefix_identifiesMissingTagPrefix() {
        String expected = BulkTagArgumentParser.MESSAGE_MISSING_PREFIX + "\n" + getUsage();
        for (String input : new String[] {"", "   ", " committee", " x/committee", " T/committee"}) {
            assertParseFailure(getParser(), input, expected);
        }
    }

    @Test
    public void parse_textBeforeTag_identifiesUnexpectedText() {
        assertParseFailure(getParser(), " 1 t/committee",
                "Unexpected text before t/: \"1\".\n" + getUsage());
    }

    @Test
    public void parse_emailField_identifiesUnsupportedField() {
        assertParseFailure(getParser(), " t/year1 e/year1@gmail.com",
                "Field e/ is not supported. This command accepts only t/TAG parameters.\n" + getUsage());
    }

    @Test
    public void parse_invalidHyphenPlacement_reportsSpecificReason() {
        assertParseFailure(getParser(), " t/-year1", BulkTagArgumentParser.MESSAGE_LEADING_HYPHEN);
        assertParseFailure(getParser(), " t/year1-", BulkTagArgumentParser.MESSAGE_TRAILING_HYPHEN);
        assertParseFailure(getParser(), " t/year--1", BulkTagArgumentParser.MESSAGE_REPEATED_HYPHENS);
        assertParseSuccess(getParser(), " t/year-1", createCommand(new Tag("year-1")));
    }

    @Test
    public void parse_invalidTagValue_rejectsCommand() {
        assertParseFailure(getParser(), " t/", MESSAGE_EMPTY_TAG);
        assertParseFailure(getParser(), " t/   ", MESSAGE_EMPTY_TAG);
        assertParseFailure(getParser(), " t/two words", MESSAGE_WHITESPACE);
        assertParseFailure(getParser(), " t/hello!", String.format(MESSAGE_UNSUPPORTED_CHARACTER, "!"));
        assertParseFailure(getParser(), " t/year_1", String.format(MESSAGE_UNSUPPORTED_CHARACTER, "_"));
        assertParseFailure(getParser(), " t/" + "a".repeat(31), String.format(MESSAGE_TAG_TOO_LONG, 31));
        assertParseFailure(getParser(), " t/one t/two", MESSAGE_MULTIPLE_TAGS);
    }
}

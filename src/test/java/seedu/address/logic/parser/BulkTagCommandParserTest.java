package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_INVALID_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
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
    public void parse_missingPrefixOrPreamble_showsUsage() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, getUsage());
        for (String input : new String[] {"", "   ", " committee", " x/committee", " T/committee", " 1 t/committee"}) {
            assertParseFailure(getParser(), input, expected);
        }
    }

    @Test
    public void parse_invalidTagValue_rejectsCommand() {
        assertParseFailure(getParser(), " t/", MESSAGE_EMPTY_TAG);
        assertParseFailure(getParser(), " t/   ", MESSAGE_EMPTY_TAG);
        assertParseFailure(getParser(), " t/two words", MESSAGE_INVALID_TAG);
        assertParseFailure(getParser(), " t/hello!", MESSAGE_INVALID_TAG);
        assertParseFailure(getParser(), " t/" + "a".repeat(31), MESSAGE_INVALID_TAG);
        assertParseFailure(getParser(), " t/one t/two", MESSAGE_MULTIPLE_TAGS);
    }
}

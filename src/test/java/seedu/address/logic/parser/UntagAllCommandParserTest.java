package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_INVALID_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.UntagAllCommand;
import seedu.address.model.tag.Tag;

public class UntagAllCommandParserTest {
    private final UntagAllCommandParser parser = new UntagAllCommandParser();

    @Test
    public void parse_validTag_returnsUntagAllCommand() {
        assertParseSuccess(parser, " t/committee", new UntagAllCommand(new Tag("committee")));
        assertParseSuccess(parser, "\t t/  Batch2026 \t", new UntagAllCommand(new Tag("Batch2026")));
        assertParseSuccess(parser, " t/" + "a".repeat(30), new UntagAllCommand(new Tag("a".repeat(30))));
    }

    @Test
    public void parse_missingPrefixOrPreamble_showsUntagAllUsage() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, UntagAllCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", "   ", " committee", " x/committee", " T/committee", " 1 t/committee"}) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void parse_invalidTagValue_rejectsCommand() {
        assertParseFailure(parser, " t/", MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, " t/   ", MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, " t/two words", MESSAGE_INVALID_TAG);
        assertParseFailure(parser, " t/hello!", MESSAGE_INVALID_TAG);
        assertParseFailure(parser, " t/" + "a".repeat(31), MESSAGE_INVALID_TAG);
        assertParseFailure(parser, " t/one t/two", MESSAGE_MULTIPLE_TAGS);
    }
}

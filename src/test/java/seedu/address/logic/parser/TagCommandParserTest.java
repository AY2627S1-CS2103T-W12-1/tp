package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_INVALID_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.TagCommand;
import seedu.address.model.tag.Tag;

public class TagCommandParserTest {
    private final TagCommandParser parser = new TagCommandParser();

    @Test
    public void parse_validIndexAndTag_returnsCommand() {
        assertParseSuccess(parser, " 1 t/committee", new TagCommand(Index.fromOneBased(1), new Tag("committee")));
        assertParseSuccess(parser, "\t002\tt/ Batch2026 ", new TagCommand(Index.fromOneBased(2),
                new Tag("Batch2026")));
    }

    @Test
    public void parse_invalidIndexOrMissingTag_showsUsage() {
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, TagCommand.MESSAGE_USAGE);
        String[] invalid = {"", "1", "0 t/a", "-1 t/a", "1.5 t/a", "2147483648 t/a",
            "1 2 t/a", "1 a", "t/a", "1 t/a p/extra"};
        for (String args : invalid) {
            assertParseFailure(parser, args, usage);
        }
    }

    @Test
    public void parse_invalidTag_reportsTagError() {
        assertParseFailure(parser, "1 t/", MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, "1 t/two words", MESSAGE_INVALID_TAG);
        assertParseFailure(parser, "1 t/" + "a".repeat(31), MESSAGE_INVALID_TAG);
        assertParseFailure(parser, "1 t/a t/b", MESSAGE_MULTIPLE_TAGS);
    }
}

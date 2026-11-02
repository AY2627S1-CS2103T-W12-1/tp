package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_TAG_TOO_LONG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_WHITESPACE;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.UntagCommand;
import seedu.address.model.tag.Tag;

public class UntagCommandParserTest {
    private final UntagCommandParser parser = new UntagCommandParser();

    @Test
    public void parse_validIndexAndTag_returnsCommand() {
        assertParseSuccess(parser, " 1 t/committee", new UntagCommand(Index.fromOneBased(1), new Tag("committee")));
        assertParseSuccess(parser, "\t002\tt/ Batch2026 ", new UntagCommand(Index.fromOneBased(2),
                new Tag("Batch2026")));
    }

    @Test
    public void parse_invalidIndexOrMissingTag_showsUsage() {
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, UntagCommand.MESSAGE_USAGE);
        String[] invalid = {"", "1", "1.5 t/a", "t/a"};
        for (String args : invalid) {
            assertParseFailure(parser, args, usage);
        }
    }

    @Test
    public void parse_invalidNumericIndex_reportsValueError() {
        for (String index : new String[] {"0", "-5", "99999999999999999999"}) {
            assertParseFailure(parser, index + " t/a", MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_missingTagPrefixOrExtraField_reportsReason() {
        assertParseFailure(parser, "1 a", BulkTagArgumentParser.MESSAGE_MISSING_PREFIX
                + "\n" + UntagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "1 2 t/a", "Unexpected text before t/: \"2\".\n" + UntagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "1 t/a p/extra",
                "Field p/ is not supported. This command accepts only t/TAG parameters.\n"
                        + UntagCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_invalidTag_reportsTagError() {
        assertParseFailure(parser, "1 t/", MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, "1 t/two words", MESSAGE_WHITESPACE);
        assertParseFailure(parser, "1 t/" + "a".repeat(31), String.format(MESSAGE_TAG_TOO_LONG, 31));
        assertParseFailure(parser, "1 t/a t/b", MESSAGE_MULTIPLE_TAGS);
    }
}

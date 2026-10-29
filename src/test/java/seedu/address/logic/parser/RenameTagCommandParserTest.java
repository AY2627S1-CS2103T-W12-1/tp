package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_INVALID_TAG;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.RenameTagCommand;
import seedu.address.model.tag.Tag;

public class RenameTagCommandParserTest {
    private final RenameTagCommandParser parser = new RenameTagCommandParser();

    @Test
    public void parse_twoValidTags_returnsRenameTagCommand() {
        RenameTagCommand expected = new RenameTagCommand(new Tag("batch2026"), new Tag("alumni"));
        assertParseSuccess(parser, " t/batch2026 t/alumni", expected);
        assertParseSuccess(parser, "\t t/  batch2026  t/alumni \t", expected);
        assertParseSuccess(parser, " t/committee t/Committee",
                new RenameTagCommand(new Tag("committee"), new Tag("Committee")));
    }

    @Test
    public void parse_wrongTagCountOrPreamble_showsRenameTagUsage() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RenameTagCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", "   ", " t/batch2026", " t/a t/b t/c", " batch2026 alumni",
            " x/batch2026 t/alumni", " 1 t/batch2026 t/alumni", " t/batch2026 n/Bob t/alumni",
            " t/batch2026 t/alumni a/Clementi", " p/91234567 t/batch2026 t/alumni"}) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void parse_invalidTagValue_rejectsCommand() {
        String current = RenameTagCommandParser.MESSAGE_CURRENT_TAG_PREFIX;
        String added = RenameTagCommandParser.MESSAGE_NEW_TAG_PREFIX;
        assertParseFailure(parser, " t/ t/alumni", current + MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, " t/batch2026 t/", added + MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, " t/two words t/alumni", current + MESSAGE_INVALID_TAG);
        assertParseFailure(parser, " t/batch2026 t/hello!", added + MESSAGE_INVALID_TAG);
        assertParseFailure(parser, " t/batch2026 t/" + "a".repeat(31), added + MESSAGE_INVALID_TAG);
        assertParseFailure(parser, " t/batch_2026 t/alumni", current + MESSAGE_INVALID_TAG);
    }

    @Test
    public void parse_sameTagTwice_rejectsCommand() {
        assertParseFailure(parser, " t/alumni t/alumni", RenameTagCommand.MESSAGE_SAME_TAG);
    }
}

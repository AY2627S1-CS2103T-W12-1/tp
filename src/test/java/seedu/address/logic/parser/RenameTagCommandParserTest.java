package seedu.address.logic.parser;

import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_TAG_TOO_LONG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_UNSUPPORTED_CHARACTER;
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
    public void parse_wrongTagCount_reportsReceivedCount() {
        for (String input : new String[] {"", "   "}) {
            assertParseFailure(parser, input, String.format(RenameTagCommandParser.MESSAGE_TAG_COUNT, 0)
                    + "\n" + RenameTagCommand.MESSAGE_USAGE);
        }
        assertParseFailure(parser, " t/batch2026", String.format(RenameTagCommandParser.MESSAGE_TAG_COUNT, 1)
                + "\n" + RenameTagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " t/a t/b t/c", String.format(RenameTagCommandParser.MESSAGE_TAG_COUNT, 3)
                + "\n" + RenameTagCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_missingPrefixOrPreamble_identifiesReason() {
        assertParseFailure(parser, " batch2026 alumni", BulkTagArgumentParser.MESSAGE_MISSING_PREFIX
                + "\n" + RenameTagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " x/batch2026 t/alumni",
                "Unexpected text before t/: \"x/batch2026\".\n" + RenameTagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " 1 t/batch2026 t/alumni",
                "Unexpected text before t/: \"1\".\n" + RenameTagCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_otherField_identifiesUnsupportedField() {
        assertParseFailure(parser, " t/batch2026 n/Bob t/alumni",
                "Field n/ is not supported. This command accepts only t/TAG parameters.\n"
                        + RenameTagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " t/batch2026 t/alumni a/Clementi",
                "Field a/ is not supported. This command accepts only t/TAG parameters.\n"
                        + RenameTagCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " p/91234567 t/batch2026 t/alumni",
                "Field p/ is not supported. This command accepts only t/TAG parameters.\n"
                        + RenameTagCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_invalidTagValue_rejectsCommand() {
        String current = RenameTagCommandParser.MESSAGE_CURRENT_TAG_PREFIX;
        String added = RenameTagCommandParser.MESSAGE_NEW_TAG_PREFIX;
        assertParseFailure(parser, " t/ t/alumni", current + MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, " t/batch2026 t/", added + MESSAGE_EMPTY_TAG);
        assertParseFailure(parser, " t/two words t/alumni",
                current + String.format(MESSAGE_UNSUPPORTED_CHARACTER, "whitespace"));
        assertParseFailure(parser, " t/batch2026 t/hello!", added + String.format(MESSAGE_UNSUPPORTED_CHARACTER, "!"));
        assertParseFailure(parser, " t/batch2026 t/" + "a".repeat(31), added + String.format(MESSAGE_TAG_TOO_LONG, 31));
        assertParseFailure(parser, " t/batch_2026 t/alumni",
                current + String.format(MESSAGE_UNSUPPORTED_CHARACTER, "_"));
    }

    @Test
    public void parse_sameTagTwice_rejectsCommand() {
        assertParseFailure(parser, " t/alumni t/alumni", RenameTagCommand.MESSAGE_SAME_TAG);
    }
}

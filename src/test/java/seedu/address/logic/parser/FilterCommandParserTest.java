package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FilterCommand;
import seedu.address.model.tag.Tag;

public class FilterCommandParserTest {
    private final FilterCommandParser parser = new FilterCommandParser();

    @Test
    public void parse_validTag_returnsFilterCommand() {
        assertParseSuccess(parser, " t/committee ", new FilterCommand(new Tag("committee")));
        assertParseSuccess(parser, "\t t/  Batch2026 \t", new FilterCommand(new Tag("Batch2026")));
        assertParseSuccess(parser, "t/" + "a".repeat(30), new FilterCommand(new Tag("a".repeat(30))));
    }

    @Test
    public void parse_missingPrefix_rejectsCommand() {
        String expected = String.format(seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                FilterCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", "committee", "x/committee", "T/committee"}) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void parse_invalidTag_rejectsCommand() {
        String expected = "A filter tag must contain 1 to 30 letters or digits.";
        String[] invalidInputs = {"t/", "t/   ", "t/two words", "t/hello!", "t/one t/two",
                "t/one x/two", "t/" + "a".repeat(31)};
        for (String input : invalidInputs) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void equals_sameTag_matches() {
        assertEquals(new FilterCommand(new Tag("committee")), new FilterCommand(new Tag("committee")));
    }
}

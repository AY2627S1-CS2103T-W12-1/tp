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
        assertParseSuccess(parser, "t/year-1", new FilterCommand(new Tag("year-1")));
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
        String expected = Tag.MESSAGE_CONSTRAINTS + ".";
        String overlongTag = "t/" + "a".repeat(31);
        String[] invalidInputs = {"t/", "t/   ", "t/two words", "t/hello!", "t/-year1", "t/year1-",
            "t/year--1", overlongTag};
        for (String input : invalidInputs) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void parse_extraTag_reportsOneTagPerCommand() {
        String expected = "Only one t/TAG parameter is allowed per filter command. "
                + "Apply another tag in a separate command.";
        String[] inputs = {"t/committee t/year1", "t/committee\tt/year1", "t/ t/year1",
            "t/invalid! t/year1", "t/year-1 t/year-2", "t/one t/two"};
        for (String input : inputs) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void parse_otherParameter_reportsCommandFormat() {
        String expected = String.format(seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                FilterCommand.MESSAGE_USAGE);
        for (String input : new String[] {"t/committee n/Bob", "t/one x/two"}) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void equals_sameTag_matches() {
        assertEquals(new FilterCommand(new Tag("committee")), new FilterCommand(new Tag("committee")));
    }
}

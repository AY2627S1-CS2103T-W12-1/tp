package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

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
    public void parse_multipleTags_returnsAlternativesInOneCommand() {
        Tag committee = new Tag("committee");
        Tag alumni = new Tag("alumni");
        assertParseSuccess(parser, "t/committee t/alumni", new FilterCommand(List.of(committee, alumni)));
        assertParseSuccess(parser, " t/committee\tt/alumni ", new FilterCommand(List.of(committee, alumni)));
        assertParseSuccess(parser, "t/ committee   t/  alumni",
                new FilterCommand(List.of(committee, alumni)));
        assertParseSuccess(parser, "t/year-1 t/year-2 t/year-3",
                new FilterCommand(List.of(new Tag("year-1"), new Tag("year-2"), new Tag("year-3"))));
    }

    @Test
    public void parse_repeatedTag_countsOnce() {
        assertParseSuccess(parser, "t/committee t/Committee", new FilterCommand(new Tag("committee")));
    }

    @Test
    public void parse_anyTagInvalid_rejectsWholeCommand() {
        String expected = Tag.MESSAGE_CONSTRAINTS + ".";
        String[] inputs = {"t/committee t/", "t/ t/year1", "t/invalid! t/year1", "t/committee t/bad!",
            "t/year-1 t/-year2", "t/one t/" + "a".repeat(31)};
        for (String input : inputs) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void parse_otherParameter_reportsCommandFormat() {
        String expected = String.format(seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                FilterCommand.MESSAGE_USAGE);
        for (String input : new String[] {"t/committee n/Bob", "t/one x/two", "t/one t/two n/Bob"}) {
            assertParseFailure(parser, input, expected);
        }
    }

    @Test
    public void equals_sameTag_matches() {
        assertEquals(new FilterCommand(new Tag("committee")), new FilterCommand(new Tag("committee")));
    }

    @Test
    public void equals_sameTagsInAnyOrder_matches() {
        assertEquals(new FilterCommand(List.of(new Tag("committee"), new Tag("alumni"))),
                new FilterCommand(List.of(new Tag("alumni"), new Tag("committee"))));
    }
}

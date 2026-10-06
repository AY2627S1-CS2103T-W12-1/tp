package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_INVALID_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

public class BulkTagArgumentParserTest {
    private static final String USAGE = "tagall: Adds one tag to every displayed member.";

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BulkTagArgumentParser.parse(null, USAGE));
        assertThrows(NullPointerException.class, () -> BulkTagArgumentParser.parse(" t/committee", null));
    }

    @Test
    public void parse_validTag_returnsTrimmedTag() throws Exception {
        assertEquals(new Tag("committee"), BulkTagArgumentParser.parse(" t/committee", USAGE));
        assertEquals(new Tag("committee"), BulkTagArgumentParser.parse("t/committee", USAGE));
        assertEquals(new Tag("year1"), BulkTagArgumentParser.parse(" t/ year1 ", USAGE));
        assertEquals(new Tag("Batch2026"), BulkTagArgumentParser.parse("\t t/\tBatch2026 \t", USAGE));
    }

    @Test
    public void parse_tagLengthBoundaries_acceptsOneToThirtyCharacters() throws Exception {
        assertEquals(new Tag("a"), BulkTagArgumentParser.parse(" t/a", USAGE));
        String thirtyCharacters = "abcdefghijklmnopqrstuvwxyz1234";
        assertEquals(new Tag(thirtyCharacters), BulkTagArgumentParser.parse(" t/" + thirtyCharacters, USAGE));
        assertThrows(ParseException.class, MESSAGE_INVALID_TAG, () ->
                BulkTagArgumentParser.parse(" t/" + thirtyCharacters + "5", USAGE));
    }

    @Test
    public void parse_tagsDifferingOnlyInCase_remainDistinct() throws Exception {
        Tag upper = BulkTagArgumentParser.parse(" t/Year1", USAGE);
        Tag lower = BulkTagArgumentParser.parse(" t/year1", USAGE);
        assertEquals(new Tag("Year1"), upper);
        assertEquals(new Tag("year1"), lower);
        assertNotEquals(upper, lower);
    }

    @Test
    public void parse_missingPrefix_throwsUsageMessage() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE);
        String[] inputs = {"", "   ", "committee", " x/committee", " T/committee", " committee t/year1"};
        for (String input : inputs) {
            assertThrows(ParseException.class, expected, () -> BulkTagArgumentParser.parse(input, USAGE));
        }
    }

    @Test
    public void parse_multipleTags_throwsMultipleTagsMessage() {
        for (String input : new String[] {" t/a t/b", " t/a t/", " t/ t/b", " t/a\tt/b"}) {
            assertThrows(ParseException.class, MESSAGE_MULTIPLE_TAGS, () ->
                    BulkTagArgumentParser.parse(input, USAGE));
        }
    }

    @Test
    public void parse_emptyTag_throwsEmptyTagMessage() {
        for (String input : new String[] {" t/", " t/   ", " t/\t"}) {
            assertThrows(ParseException.class, MESSAGE_EMPTY_TAG, () ->
                    BulkTagArgumentParser.parse(input, USAGE));
        }
    }

    @Test
    public void parse_invalidTag_throwsInvalidTagMessage() {
        String[] inputs = {" t/year-1", " t/year1 year2", " t/hello!", " t/one x/two", " t/caf\u00e9"};
        for (String input : inputs) {
            assertThrows(ParseException.class, MESSAGE_INVALID_TAG, () ->
                    BulkTagArgumentParser.parse(input, USAGE));
        }
    }
}

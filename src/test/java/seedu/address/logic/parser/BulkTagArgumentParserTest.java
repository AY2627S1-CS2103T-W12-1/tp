package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_INVALID_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

public class BulkTagArgumentParserTest {
    private static final String USAGE = "tagall: Adds one tag to every displayed member.";

    /**
     * Asserts that parsing each of {@code inputs} throws a {@code ParseException} with {@code expectedMessage}.
     */
    private static void assertParseRejected(String expectedMessage, String... inputs) {
        for (String input : inputs) {
            assertThrows(ParseException.class, expectedMessage, () -> BulkTagArgumentParser.parse(input, USAGE));
        }
    }

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
        assertEquals(new Tag("year-1"), BulkTagArgumentParser.parse(" t/year-1", USAGE));
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
    public void parse_tagsDifferingOnlyInCase_match() throws Exception {
        Tag upper = BulkTagArgumentParser.parse(" t/Year1", USAGE);
        Tag lower = BulkTagArgumentParser.parse(" t/year1", USAGE);
        assertEquals(new Tag("Year1"), upper);
        assertEquals(new Tag("year1"), lower);
        assertEquals(upper, lower);
        assertEquals(upper.hashCode(), lower.hashCode());
    }

    @Test
    public void parse_missingPrefix_throwsUsageMessage() {
        assertParseRejected(String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE),
                "", "   ", "committee", " x/committee", " T/committee");
    }

    @Test
    public void parse_textBeforePrefix_throwsUsageMessage() {
        assertParseRejected(String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE),
                " committee t/year1", " 1 t/year1", " x/a t/year1");
    }

    @Test
    public void parse_otherFieldPrefix_throwsUsageMessage() {
        assertParseRejected(String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE),
                " t/friend n/Bob", " t/friend p/91234567", " t/friend e/bob@example.com", " t/friend a/Clementi",
                " t/friend\tn/Bob", " t/friend n/", " n/Bob t/friend", " t/a t/b n/Bob");
    }

    @Test
    public void parseTagValues_tagPrefixesOnly_returnsValuesInOrder() throws Exception {
        assertEquals(List.of("a", "b"), BulkTagArgumentParser.parseTagValues(" t/a t/ b ", USAGE));
        assertEquals(List.of(), BulkTagArgumentParser.parseTagValues("", USAGE));
    }

    @Test
    public void parse_multipleTags_throwsMultipleTagsMessage() {
        assertParseRejected(MESSAGE_MULTIPLE_TAGS, " t/a t/b", " t/a t/", " t/ t/b", " t/a\tt/b");
    }

    @Test
    public void parse_emptyTag_throwsEmptyTagMessage() {
        assertParseRejected(MESSAGE_EMPTY_TAG, " t/", " t/   ", " t/\t");
    }

    @Test
    public void parse_invalidTag_throwsInvalidTagMessage() {
        String[] inputs = {" t/-year1", " t/year1-", " t/year--1", " t/year1 year2",
            " t/hello!", " t/one x/two", " t/caf\u00e9"};
        for (String input : inputs) {
            assertThrows(ParseException.class, MESSAGE_INVALID_TAG, () ->
                    BulkTagArgumentParser.parse(input, USAGE));
        }
    }
}

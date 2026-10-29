package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_EMPTY_TAG;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_MULTIPLE_TAGS;
import static seedu.address.logic.parser.BulkTagArgumentParser.MESSAGE_TAG_TOO_LONG;
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
        assertThrows(ParseException.class, String.format(MESSAGE_TAG_TOO_LONG, 31), () ->
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
    public void parse_missingPrefix_identifiesMissingTagPrefix() {
        assertParseRejected("Missing t/ prefix. Supply tags using t/TAG.\n" + USAGE,
                "", "   ", "committee", " x/committee", " T/committee");
    }

    @Test
    public void parse_textBeforePrefix_identifiesUnexpectedText() {
        assertParseRejected("Unexpected text before t/: \"committee\".\n" + USAGE, " committee t/year1");
        assertParseRejected("Unexpected text before t/: \"1\".\n" + USAGE, " 1 t/year1");
        assertParseRejected("Unexpected text before t/: \"x/a\".\n" + USAGE, " x/a t/year1");
    }

    @Test
    public void parse_otherFieldPrefix_identifiesUnsupportedField() {
        assertParseRejected("Field n/ is not supported. This command accepts only t/TAG parameters.\n" + USAGE,
                " t/friend n/Bob", " t/friend\tn/Bob", " t/friend n/", " n/Bob t/friend", " t/a t/b n/Bob");
        assertParseRejected("Field p/ is not supported. This command accepts only t/TAG parameters.\n" + USAGE,
                " t/friend p/91234567");
        assertParseRejected("Field e/ is not supported. This command accepts only t/TAG parameters.\n" + USAGE,
                " t/year1 e/year1@gmail.com");
        assertParseRejected("Field a/ is not supported. This command accepts only t/TAG parameters.\n" + USAGE,
                " t/friend a/Clementi");
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
    public void parse_leadingHyphen_explainsStartRestriction() {
        assertParseRejected("Tag cannot start with a hyphen (-). Start with a letter or digit, e.g. year-1.",
                " t/-year1", " t/-", " t/--year1", " t/-year1-");
    }

    @Test
    public void parse_trailingHyphen_explainsEndRestriction() {
        assertParseRejected("Tag cannot end with a hyphen (-). End with a letter or digit, e.g. year-1.",
                " t/year1-", " t/year1--");
    }

    @Test
    public void parse_repeatedHyphens_explainsSingleHyphenRestriction() {
        assertParseRejected("Tag cannot contain consecutive hyphens (--). Use a single hyphen, e.g. year-1.",
                " t/year--1", " t/year---1");
    }

    @Test
    public void parse_unsupportedCharacter_identifiesCharacterAndSupportedAlternatives() {
        String guidance = ". Use letters (A-Z, a-z), digits (0-9), or internal hyphens (-), e.g. year-1.";
        assertParseRejected("Tag contains unsupported character \"_\"" + guidance, " t/year_1");
        assertParseRejected("Tag contains unsupported character \"!\"" + guidance, " t/hello!");
        assertParseRejected("Tag contains unsupported character \"whitespace\"" + guidance, " t/year1 year2");
        assertParseRejected("Tag contains unsupported character \"/\"" + guidance, " t/onex/two");
        assertParseRejected("Tag contains unsupported character \"é\"" + guidance, " t/café");
    }
}

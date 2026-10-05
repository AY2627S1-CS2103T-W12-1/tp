package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class ImportCommandParserTest {

    private final ImportCommandParser parser = new ImportCommandParser();

    @Test
    public void parse_validRelativeAndQuotedPaths_success() throws Exception {
        assertEquals(new ImportCommand(Path.of("data/members.csv")), parser.parse(" p/data/members.csv "));
        assertEquals(new ImportCommand(Path.of("Club Files/MEMBERS.CSV")),
                parser.parse(" p/\"Club Files/MEMBERS.CSV\" "));
    }

    @Test
    public void parse_missingPrefixOrPath_reportsSpecificError() {
        assertParseFailure("", ImportCommandParser.MESSAGE_INVALID_FORMAT);
        assertParseFailure("members.csv", ImportCommandParser.MESSAGE_INVALID_FORMAT);
        assertParseFailure("p/", ImportCommandParser.MESSAGE_EMPTY_PATH);
        assertParseFailure("p/\"\"", ImportCommandParser.MESSAGE_EMPTY_PATH);
    }

    @Test
    public void parse_nonCsvExtension_rejectedCaseInsensitively() {
        assertParseFailure("p/members.txt", ImportCommandParser.MESSAGE_NON_CSV_FILE);
        assertParseFailure("p/members.csv.txt", ImportCommandParser.MESSAGE_NON_CSV_FILE);
    }

    @Test
    public void parse_unmatchedQuotes_rejected() {
        assertParseFailure("p/\"data/member list.csv", ImportCommandParser.MESSAGE_UNMATCHED_QUOTES);
        assertParseFailure("p/data\".csv", ImportCommandParser.MESSAGE_UNMATCHED_QUOTES);
    }

    @Test
    public void parse_repeatedAndUnknownPrefixes_reportsSpecificError() {
        assertParseFailure("p/a.csv p/b.csv", ImportCommandParser.MESSAGE_REPEATED_PATH);
        assertParseFailure("x/a.csv", String.format(ImportCommandParser.MESSAGE_UNKNOWN_PARAMETER, "x/"));
        assertParseFailure("p/a.csv x/value", String.format(ImportCommandParser.MESSAGE_UNKNOWN_PARAMETER, "x/"));
    }

    @Test
    public void parse_unquotedWhitespaceAndExtraArguments_rejected() {
        assertParseFailure("p/member list.csv", ImportCommandParser.MESSAGE_INVALID_FORMAT);
        assertParseFailure("p/a.csv extra", ImportCommandParser.MESSAGE_INVALID_FORMAT);
        assertParseFailure("p/\"a.csv\" extra", ImportCommandParser.MESSAGE_INVALID_FORMAT);
    }

    private void assertParseFailure(String input, String expectedMessage) {
        ParseException error = assertThrows(ParseException.class, () -> parser.parse(input));
        assertEquals(expectedMessage, error.getMessage());
    }
}

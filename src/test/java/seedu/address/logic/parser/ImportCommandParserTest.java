package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class ImportCommandParserTest {

    private final ImportCommandParser parser = new ImportCommandParser();

    @Test
    public void parse_validCsvPath_success() throws Exception {
        assertEquals(new ImportCommand(Path.of("data/members.csv")), parser.parse(" p/data/members.csv "));
        assertEquals(new ImportCommand(Path.of("MEMBERS.CSV")), parser.parse(" p/MEMBERS.CSV "));
    }

    @Test
    public void parse_missingPrefix_rejected() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE);
        assertParseFailure("", expected);
        assertParseFailure("members.csv", expected);
    }

    @Test
    public void parse_emptyPath_rejected() {
        assertParseFailure("p/", ImportCommandParser.MESSAGE_EMPTY_PATH);
    }

    @Test
    public void parse_pathWithWhitespace_rejectedByBasicParser() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE);
        assertParseFailure("p/member list.csv", expected);
    }

    @Test
    public void parse_nonCsvExtension_rejected() {
        assertParseFailure("p/members.txt", ImportCommandParser.MESSAGE_NON_CSV_FILE);
    }

    private void assertParseFailure(String input, String expectedMessage) {
        ParseException error = assertThrows(ParseException.class, () -> parser.parse(input));
        assertEquals(expectedMessage, error.getMessage());
    }
}

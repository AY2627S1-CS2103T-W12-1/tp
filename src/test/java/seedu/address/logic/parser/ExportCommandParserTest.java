package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.ExportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class ExportCommandParserTest {

    @TempDir
    public Path tempDir;

    private final ExportCommandParser parser = new ExportCommandParser();

    @Test
    public void parse_noPath_usesDefaultFilename() {
        for (String args : new String[] {"", " ", "\t  "}) {
            assertParseSuccess(parser, args, new ExportCommand(Path.of("addressbook.csv")));
        }
    }

    @Test
    public void parse_relativePath_preservesDirectoriesSpacesAndUnicode() {
        String path = "backups/member contacts \u00e9.csv";
        assertParseSuccess(parser, "  " + path + "  ", new ExportCommand(Path.of(path)));
    }

    @Test
    public void parse_absolutePath_preservesDestination() {
        Path path = tempDir.resolve("member contacts.csv").toAbsolutePath();
        assertParseSuccess(parser, "\t " + path + " \t", new ExportCommand(path));
    }

    @Test
    public void parse_invalidPath_reportsUsageAndCause() {
        ParseException error = assertThrows(ParseException.class, () -> parser.parse("invalid\u0000path.csv"));
        assertEquals(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ExportCommand.MESSAGE_USAGE), error.getMessage());
        assertInstanceOf(InvalidPathException.class, error.getCause());
    }
}

package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ShowCommand;

public class ShowCommandParserTest {
    private final ShowCommandParser parser = new ShowCommandParser();

    @Test
    public void parse_validIndexes_returnsShowCommand() {
        ShowCommand expected = new ShowCommand(List.of(Index.fromOneBased(1), Index.fromOneBased(3),
                Index.fromOneBased(4), Index.fromOneBased(5)));
        assertParseSuccess(parser, " 1  3\t4 5 ", expected);
    }

    @Test
    public void parse_invalidIndexes_rejectsWholeCommand() {
        String error = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ShowCommand.MESSAGE_USAGE);
        String[] invalidArgs = {"", " ", "one", "1, 3", "1,", ",1", "1,,2"};
        for (String args : invalidArgs) {
            assertParseFailure(parser, args, error);
        }
    }

    @Test
    public void parse_invalidNumericIndex_reportsValueError() {
        for (String args : new String[] {"0", "-1", "1 99999999999999999999"}) {
            assertParseFailure(parser, args, MESSAGE_INVALID_INDEX);
        }
    }
}

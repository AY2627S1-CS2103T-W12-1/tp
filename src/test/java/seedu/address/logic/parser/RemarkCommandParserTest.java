package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_REMARK;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {

    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_indexSpecified_success() {
        String nonEmptyRemark = "Likes baseball";
        String userInput = INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_REMARK + nonEmptyRemark;
        assertParseSuccess(parser, userInput, new RemarkCommand(INDEX_FIRST_PERSON, new Remark(nonEmptyRemark)));

        userInput = INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_REMARK;
        assertParseSuccess(parser, userInput, new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")));
    }

    @Test
    public void parse_invalidIndex_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "notAnIndex " + PREFIX_REMARK + "remark", expectedMessage);
    }
}

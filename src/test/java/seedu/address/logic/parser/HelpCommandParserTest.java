package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.CommandHelp;
import seedu.address.logic.commands.HelpCommand;

/**
 * Verifies that help arguments select real commands and reject silently ignored input.
 */
public class HelpCommandParserTest {

    private final HelpCommandParser parser = new HelpCommandParser();

    @Test
    public void parse_noTopic_opensCompleteGuide() {
        assertParseSuccess(parser, "", new HelpCommand());
        assertParseSuccess(parser, " \t ", new HelpCommand());
    }

    @Test
    public void parse_supportedTopic_opensSelectedCommand() {
        for (CommandHelp entry : CommandHelp.values()) {
            assertParseSuccess(parser, " \t" + entry.getKeyword() + "\t ", new HelpCommand(entry.getKeyword()));
        }
    }

    @Test
    public void parse_unknownOrWrongCaseTopic_reportsRecovery() {
        for (String topic : new String[] {"tagall", "untagall", "unknown", "ADD"}) {
            assertParseFailure(parser, topic, String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, topic));
        }
        // A valid command must still parse after an error.
        assertParseSuccess(parser, "edit", new HelpCommand("edit"));
    }

    @Test
    public void parse_extraTopics_reportsUsage() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "add edit", expected);
        assertParseFailure(parser, "add\tedit", expected);
        assertParseFailure(parser, "edit 1", expected);
    }
}

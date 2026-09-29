package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.CommandHelp;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses an optional, lowercase command topic for offline help.
 */
public class HelpCommandParser implements Parser<HelpCommand> {

    @Override
    public HelpCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String topic = args.trim();
        if (topic.isEmpty()) {
            return new HelpCommand();
        }
        if (topic.split("\\s+").length != 1) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }
        if (CommandHelp.find(topic).isEmpty()) {
            throw new ParseException(String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, topic));
        }
        return new HelpCommand(topic);
    }
}

package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import seedu.address.model.Model;

/**
 * Opens offline instructions for every supported command or one selected command.
 */
public class HelpCommand extends Command {

    public static final String COMMAND_WORD = "help";
    public static final String MESSAGE_USAGE = "help [COMMAND]: Opens the offline command guide.\n"
            + "Use help for all commands, or help edit for one command.";
    public static final String SHOWING_HELP_MESSAGE = "Opened the offline command guide.";
    public static final String MESSAGE_UNKNOWN_TOPIC = "Unknown help topic: %s. "
            + "Type help to see available commands.";

    /** An empty topic means the complete command guide. */
    private final String topic;

    /**
     * Creates a command that opens the complete guide.
     */
    public HelpCommand() {
        this("");
    }

    /**
     * Creates a command that opens help for a supported keyword, or the complete guide for an empty topic.
     */
    public HelpCommand(String topic) {
        requireNonNull(topic);
        checkArgument(topic.isEmpty() || CommandHelp.find(topic).isPresent());
        this.topic = topic;
    }

    @Override
    public CommandResult execute(Model model) {
        String feedback = topic.isEmpty() ? SHOWING_HELP_MESSAGE : "Opened help for " + topic + ".";
        return new CommandResult(feedback, true, false, topic);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof HelpCommand otherHelp && topic.equals(otherHelp.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }
}

package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean showHelp;

    /** Empty for the complete help guide; otherwise the command whose help should be shown. */
    private final String helpTopic;

    /** The application should exit. */
    private final boolean exit;

    /** The deletion that runs only if the user confirms it next, or null if nothing awaits confirmation. */
    private final ConfirmDeleteCommand pendingDeletion;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, "");
    }

    /**
     * Constructs a result with an optional command topic for the offline help window.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit, String helpTopic) {
        this(feedbackToUser, showHelp, exit, helpTopic, null);
    }

    /**
     * Constructs a result that asks the user to confirm {@code pendingDeletion} with their next input.
     */
    public CommandResult(String feedbackToUser, ConfirmDeleteCommand pendingDeletion) {
        this(feedbackToUser, false, false, "", requireNonNull(pendingDeletion));
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit, String helpTopic,
            ConfirmDeleteCommand pendingDeletion) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.showHelp = showHelp;
        this.exit = exit;
        this.helpTopic = requireNonNull(helpTopic);
        this.pendingDeletion = pendingDeletion;
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    /**
     * Returns the selected help keyword, or an empty string for the complete guide.
     */
    public String getHelpTopic() {
        return helpTopic;
    }

    public boolean isExit() {
        return exit;
    }

    /**
     * Returns the deletion awaiting the user's confirmation, if this result asks for one.
     */
    public Optional<ConfirmDeleteCommand> getPendingDeletion() {
        return Optional.ofNullable(pendingDeletion);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && showHelp == otherCommandResult.showHelp
                && exit == otherCommandResult.exit
                && helpTopic.equals(otherCommandResult.helpTopic)
                && Objects.equals(pendingDeletion, otherCommandResult.pendingDeletion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, helpTopic, pendingDeletion);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("helpTopic", helpTopic)
                .add("pendingDeletion", pendingDeletion)
                .toString();
    }

}

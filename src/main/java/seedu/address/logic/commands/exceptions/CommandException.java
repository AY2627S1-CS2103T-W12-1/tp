package seedu.address.logic.commands.exceptions;

import seedu.address.logic.commands.Command;

/**
 * Represents an error which occurs during execution of a {@link Command}.
 */
public class CommandException extends Exception {
    /**
     * Constructs a command failure with feedback for the user.
     *
     * @param message Explanation of the execution failure.
     */
    public CommandException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code CommandException} with the specified detail {@code message} and {@code cause}.
     *
     * @param message Explanation of the execution failure.
     * @param cause Underlying error that prevented execution.
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}

package seedu.address.logic.commands.exceptions;

/**
 * Reports a failed save and whether the completed command remains applied in memory.
 * The UI uses this distinction to avoid submitting a destructive command twice when a user retries.
 */
public class SaveFailureException extends CommandException {

    private final boolean changeApplied;

    /**
     * Creates a save failure with recovery guidance and the outcome of any in-memory rollback.
     *
     * @param message Explanation of the storage failure and how to recover.
     * @param cause Underlying storage error.
     * @param changeApplied Whether the command remains applied and its input should be cleared.
     */
    public SaveFailureException(String message, Throwable cause, boolean changeApplied) {
        super(message, cause);
        this.changeApplied = changeApplied;
    }

    /**
     * Returns whether the failed command remains applied in memory and must not be offered for resubmission.
     */
    public boolean isChangeApplied() {
        return changeApplied;
    }
}

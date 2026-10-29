package seedu.address.logic;

import static java.util.Objects.requireNonNull;

/**
 * The rules for answering the confirmation prompt of {@code delete} and {@code clear}.
 * Only {@link #WORD} confirms. A near miss keeps the confirmation pending, and any other input cancels it.
 */
public final class Confirmation {

    /** The only input that confirms a pending deletion or clear. */
    public static final String WORD = "yes";

    /** Explains cancellation without treating near misses as cancelling input. */
    public static final String CANCEL_INSTRUCTION =
            "type anything else to cancel (except yes or y in any letter case)";

    /** Shown for a near miss such as {@code y} or {@code YES}, which keeps the confirmation pending. */
    public static final String REMINDER = "To confirm, type " + WORD
            + " exactly, in lowercase, and press Enter, or " + CANCEL_INSTRUCTION + ".";

    /** Appended to a cancellation when the cancelling input was a command, which is not run. */
    public static final String INPUT_NOT_RUN = "\n\"%1$s\" was not run. Enter it again to run it.";

    /** The short form of {@link #WORD}, which is treated as a near miss rather than a cancellation. */
    private static final String SHORT_WORD = "y";

    private Confirmation() {
        // Prevents instantiation of this utility class.
    }

    /**
     * Returns true if {@code input} looks like an attempt to confirm but is not exactly {@link #WORD}.
     */
    public static boolean isNearMiss(String input) {
        requireNonNull(input);
        return !input.equals(WORD) && (input.equalsIgnoreCase(WORD) || input.equalsIgnoreCase(SHORT_WORD));
    }
}

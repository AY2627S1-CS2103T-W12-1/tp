package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Deletes the member named in a {@code delete} confirmation prompt after the user confirms it.
 * The member is stored instead of the index, so the confirmed deletion removes the member the prompt named.
 */
public class ConfirmDeleteCommand extends Command {

    /** The only input that confirms a pending deletion. Near misses keep it pending; any other input cancels it. */
    public static final String CONFIRMATION_WORD = "yes";

    public static final String MESSAGE_CANCELLED = "Deletion cancelled. No member was removed.";

    /** Shown for a near miss such as {@code y} or {@code YES}, which keeps the confirmation pending. */
    public static final String MESSAGE_CONFIRMATION_REMINDER = "To confirm, type " + CONFIRMATION_WORD
            + " exactly, in lowercase, and press Enter. To cancel, type no.";

    /** Appended to a cancellation when the cancelling input was a command, which is not run. */
    public static final String MESSAGE_INPUT_NOT_RUN = "\n\"%1$s\" was not run. Enter it again to run it.";

    public static final String MESSAGE_MEMBER_NOT_FOUND =
            "The member to delete is no longer in the roster. No member was removed.";

    private final Person target;

    /**
     * Creates a command that deletes {@code target} once the user confirms the deletion.
     */
    public ConfirmDeleteCommand(Person target) {
        this.target = requireNonNull(target);
    }

    /**
     * Returns true if {@code input} looks like an attempt to confirm but is not exactly the confirmation word.
     */
    public static boolean isNearMiss(String input) {
        return !input.equals(CONFIRMATION_WORD)
                && (input.equalsIgnoreCase(CONFIRMATION_WORD) || input.equalsIgnoreCase("y"));
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (!model.getAddressBook().getPersonList().contains(target)) {
            throw new CommandException(MESSAGE_MEMBER_NOT_FOUND);
        }

        model.deletePerson(target);
        return new CommandResult(String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS, Messages.format(target)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ConfirmDeleteCommand otherCommand)) {
            return false;
        }

        return target.equals(otherCommand.target);
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("target", target)
                .toString();
    }
}

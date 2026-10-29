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

    public static final String MESSAGE_CANCELLED = "Deletion cancelled. No member was removed.";

    public static final String MESSAGE_MEMBER_NOT_FOUND =
            "The member to delete is no longer in the roster. No member was removed.";

    private final Person target;

    /**
     * Creates a command that deletes {@code target} once the user confirms the deletion.
     */
    public ConfirmDeleteCommand(Person target) {
        this.target = requireNonNull(target);
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

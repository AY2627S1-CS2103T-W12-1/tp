package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Confirmation;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Asks the user to confirm deleting a person identified using its displayed index from the address book.
 * The deletion itself runs only if the user confirms it with the next input.
 */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Deletes the member identified by the index number used in the displayed member list "
            + "after you confirm the deletion.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";

    public static final String MESSAGE_CONFIRM_DELETE = "Delete member %1$d, %2$s?\nTags: %3$s\n"
            + "Type " + Confirmation.WORD
            + " and press Enter to confirm, or type no to cancel the deletion.";

    public static final String MESSAGE_DELETE_PERSON_SUCCESS = "Deleted member\n%1$s";

    private final Index targetIndex;

    public DeleteCommand(Index targetIndex) {
        this.targetIndex = targetIndex;
    }

    /**
     * Returns true because asking for confirmation neither changes records nor saves the roster.
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();

        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person personToDelete = lastShownList.get(targetIndex.getZeroBased());
        String prompt = String.format(MESSAGE_CONFIRM_DELETE, targetIndex.getOneBased(), personToDelete.getName(),
                Messages.formatTags(personToDelete));
        return new CommandResult(prompt, new ConfirmDeleteCommand(personToDelete));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DeleteCommand otherDeleteCommand)) {
            return false;
        }

        return targetIndex.equals(otherDeleteCommand.targetIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .toString();
    }
}

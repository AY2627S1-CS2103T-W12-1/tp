package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;

/**
 * Removes the complete roster, including members hidden by a search or filter.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE =
            "Invalid command format. Usage: clear. This command removes all members.";
    public static final String MESSAGE_SUCCESS = "Cleared %d members. The address book is now empty.";
    public static final String MESSAGE_SINGLE = "Cleared 1 member. The address book is now empty.";
    public static final String MESSAGE_EMPTY = "The address book is already empty. No changes were made.";
    public static final String MESSAGE_SAVE_FAILURE =
            "Unable to clear the address book because the changes could not be saved. No members were removed.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        int count = model.getAddressBook().getPersonList().size();
        // LogicManager resets the view only after saving, preserving its predicate for a failed-clear rollback.
        model.setAddressBook(new AddressBook());
        String feedback = switch (count) {
            case 0 -> MESSAGE_EMPTY;
            case 1 -> MESSAGE_SINGLE;
            default -> String.format(MESSAGE_SUCCESS, count);
        };
        return new CommandResult(feedback);
    }
}

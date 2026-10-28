package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;

/**
 * Removes the complete roster, including members hidden by a search or filter.
 * LogicManager requires confirmation before executing this command on a non-empty roster.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE =
            "Invalid command format. Usage: clear. This command removes all members.";
    public static final String MESSAGE_CONFIRMATION =
            "Clear the entire roster? Total members: %d (including anyone hidden by searches or filters).\n"
            + "Type y and press Enter to confirm. Any other input cancels.";
    public static final String MESSAGE_CANCELLED = "Clear cancelled. No members were removed.";
    public static final String MESSAGE_SUCCESS = "Cleared %d members. The roster is now empty.";
    public static final String MESSAGE_SINGLE = "Cleared 1 member. The roster is now empty.";
    public static final String MESSAGE_EMPTY = "The roster is already empty. No changes were made.";
    public static final String MESSAGE_SAVE_FAILURE =
            "Unable to clear the roster because the changes could not be saved. No members were removed. "
            + "Run clear again to retry.";

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

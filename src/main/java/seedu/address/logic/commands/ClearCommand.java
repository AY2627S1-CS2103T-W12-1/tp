package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;

/**
 * Clears the address book.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE = "clear: Permanently removes every member. No arguments are accepted.";
    public static final String MESSAGE_SUCCESS = "Cleared %1$d member(s).";
    public static final String MESSAGE_EMPTY = "No members to clear.";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        int removedCount = model.getAddressBook().getPersonList().size();
        model.setAddressBook(new AddressBook());
        return new CommandResult(removedCount == 0 ? MESSAGE_EMPTY : String.format(MESSAGE_SUCCESS, removedCount));
    }
}

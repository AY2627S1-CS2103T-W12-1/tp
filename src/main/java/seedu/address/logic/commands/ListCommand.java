package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import seedu.address.model.Model;

/**
 * Restores every member in stored order and reports the complete roster size.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";
    public static final String MESSAGE_USAGE = "Invalid command format. Usage: list";
    public static final String MESSAGE_SUCCESS = "Showing %d members.";
    public static final String MESSAGE_SINGLE = "Showing 1 member.";
    public static final String MESSAGE_EMPTY = "No members in the address book.";

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        model.setNameSorting(false);
        int count = model.getFilteredPersonList().size();
        String feedback = switch (count) {
            case 0 -> MESSAGE_EMPTY;
            case 1 -> MESSAGE_SINGLE;
            default -> String.format(MESSAGE_SUCCESS, count);
        };
        return new CommandResult(feedback);
    }
}

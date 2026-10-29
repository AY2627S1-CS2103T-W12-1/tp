package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.Model;

/**
 * Sorts the complete stored roster alphabetically by name, ignoring letter case.
 */
public class SortCommand extends Command {
    public static final String COMMAND_WORD = "sort";
    public static final String MESSAGE_USAGE = "Invalid command format. Usage: sort";
    public static final String MESSAGE_SUCCESS = "Sorted all %d members by name (A to Z).";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.sortPersonsByName();
        return new CommandResult(String.format(MESSAGE_SUCCESS, model.getAddressBook().getPersonList().size()));
    }
}

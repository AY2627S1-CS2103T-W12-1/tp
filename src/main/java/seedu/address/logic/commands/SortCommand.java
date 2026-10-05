package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;

import seedu.address.model.Model;

/**
 * Sorts the displayed members alphabetically by name, ignoring letter case.
 */
public class SortCommand extends Command {
    public static final String COMMAND_WORD = "sort";
    public static final String MESSAGE_USAGE = "Invalid command format. Usage: sort";
    public static final String MESSAGE_SUCCESS = "Sorted displayed members by name (A to Z).";

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.sortFilteredPersonList(Comparator.comparing(person -> person.getName().fullName,
                String.CASE_INSENSITIVE_ORDER));
        return new CommandResult(MESSAGE_SUCCESS);
    }
}

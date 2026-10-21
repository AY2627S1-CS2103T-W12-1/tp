package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonSelectionPredicate;

/**
 * Shows only the members at the specified indexes in the current displayed list.
 */
public class ShowCommand extends Command {
    public static final String COMMAND_WORD = "show";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Shows only the members at the specified numbers in the current list.\n"
            + "Parameters: INDEX [MORE_INDEXES]... (positive integers)\n"
            + "Example: " + COMMAND_WORD + " 1 3";
    public static final String MESSAGE_SINGLE = "Showing 1 member.";
    public static final String MESSAGE_MULTIPLE = "Showing %d members.";

    private final List<Index> targetIndexes;

    /**
     * Creates a command with at least one displayed index.
     */
    public ShowCommand(List<Index> targetIndexes) {
        requireNonNull(targetIndexes);
        if (targetIndexes.isEmpty()) {
            throw new IllegalArgumentException("At least one index is required");
        }
        this.targetIndexes = List.copyOf(targetIndexes);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> displayed = model.getFilteredPersonList();
        for (Index index : targetIndexes) {
            if (index.getZeroBased() >= displayed.size()) {
                throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
            }
        }

        List<Person> selected = targetIndexes.stream()
                .map(index -> displayed.get(index.getZeroBased()))
                .toList();
        model.filterFilteredPersonList(new PersonSelectionPredicate(selected));
        int count = model.getFilteredPersonList().size();
        return new CommandResult(count == 1 ? MESSAGE_SINGLE : String.format(MESSAGE_MULTIPLE, count));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ShowCommand otherShowCommand
                && targetIndexes.equals(otherShowCommand.targetIndexes);
    }

    @Override
    public int hashCode() {
        return targetIndexes.hashCode();
    }
}

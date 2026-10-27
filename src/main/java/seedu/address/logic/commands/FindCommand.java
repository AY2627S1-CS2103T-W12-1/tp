package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.Model;
import seedu.address.model.person.NameContainsKeywordsPredicate;

/**
 * Narrows the displayed list to persons whose names contain any of the argument keywords.
 * Keyword matching is case insensitive and accepts partial name words.
 */
public class FindCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds displayed members whose names contain any of "
            + "the specified keywords (case-insensitive, partial words allowed) "
            + "and displays them as a list with index numbers.\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...\n"
            + "Example: " + COMMAND_WORD + " alice bob charlie";
    public static final String MESSAGE_SINGLE = "1 member listed.";
    public static final String MESSAGE_MULTIPLE = "%d members listed.";

    private final NameContainsKeywordsPredicate predicate;

    public FindCommand(NameContainsKeywordsPredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.filterFilteredPersonListByName(predicate);
        int count = model.getFilteredPersonList().size();
        return new CommandResult(count == 1 ? MESSAGE_SINGLE : String.format(MESSAGE_MULTIPLE, count));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof FindCommand otherFindCommand)) {
            return false;
        }

        return predicate.equals(otherFindCommand.predicate);
    }

    @Override
    public int hashCode() {
        return predicate.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}

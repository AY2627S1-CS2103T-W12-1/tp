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
    public static final String MESSAGE_ALREADY_ACTIVE = "This name search is already active. ";
    public static final String MESSAGE_UNCHANGED = "Results unchanged. ";

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
        int previousCount = model.getFilteredPersonList().size();
        boolean added = model.filterFilteredPersonListByName(predicate);
        int count = model.getFilteredPersonList().size();
        String feedback = count == 1 ? MESSAGE_SINGLE : String.format(MESSAGE_MULTIPLE, count);
        if (!added) {
            return new CommandResult(MESSAGE_ALREADY_ACTIVE + feedback);
        }
        // A cumulative search can only remove members, so equal counts mean the view is unchanged.
        return new CommandResult((count == previousCount ? MESSAGE_UNCHANGED : "") + feedback);
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

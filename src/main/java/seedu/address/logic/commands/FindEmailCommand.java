package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.Model;
import seedu.address.model.person.EmailContainsKeywordsPredicate;

/**
 * Lists members whose email contains any given keyword, ignoring case.
 * A new email search replaces previous name searches and tag filters without changing stored records.
 */
public class FindEmailCommand extends Command {
    public static final String COMMAND_WORD = "findemail";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Finds members whose emails contain any specified keyword (case-insensitive).\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...\n"
            + "Example: " + COMMAND_WORD + " @example.com alice";
    public static final String MESSAGE_SINGLE = "1 member listed.";
    public static final String MESSAGE_MULTIPLE = "%d members listed.";

    private final EmailContainsKeywordsPredicate predicate;

    /**
     * Creates a command that searches emails using {@code predicate}.
     *
     * @throws NullPointerException If {@code predicate} is null.
     */
    public FindEmailCommand(EmailContainsKeywordsPredicate predicate) {
        this.predicate = requireNonNull(predicate);
    }

    /**
     * Returns true because searching neither changes records nor saves the roster.
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Searches the full roster, replacing existing filters and preserving the active display ordering.
     */
    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(predicate);
        int count = model.getFilteredPersonList().size();
        return new CommandResult(count == 1 ? MESSAGE_SINGLE : String.format(MESSAGE_MULTIPLE, count));
    }

    /**
     * Returns whether another command searches with the same predicate.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof FindEmailCommand otherCommand)) {
            return false;
        }

        return predicate.equals(otherCommand.predicate);
    }

    /**
     * Returns a hash code based on this command's predicate.
     */
    @Override
    public int hashCode() {
        return predicate.hashCode();
    }

    /**
     * Returns the predicate used by this command for diagnostic output.
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).add("predicate", predicate).toString();
    }
}

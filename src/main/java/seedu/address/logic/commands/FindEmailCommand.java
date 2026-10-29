package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.Model;
import seedu.address.model.person.EmailContainsKeywordsPredicate;

/**
 * Lists members whose email contains any given keyword, ignoring case.
 * Each email search narrows the displayed list without changing stored records or earlier conditions.
 */
public class FindEmailCommand extends Command {
    public static final String COMMAND_WORD = "findemail";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Narrows displayed members to emails containing any specified keyword (case-insensitive).\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...\n"
            + "Example: " + COMMAND_WORD + " @example.com alice";
    public static final String MESSAGE_SINGLE = "1 member listed.";
    public static final String MESSAGE_MULTIPLE = "%d members listed.";
    public static final String MESSAGE_ALREADY_ACTIVE = "This email search is already active. ";
    public static final String MESSAGE_UNCHANGED = "Results unchanged. ";

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
     * Adds an email condition, preserving existing filters, selections and active display ordering.
     */
    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        int previousCount = model.getFilteredPersonList().size();
        boolean added = model.filterFilteredPersonListByEmail(predicate);
        int count = model.getFilteredPersonList().size();
        String feedback = count == 1 ? MESSAGE_SINGLE : String.format(MESSAGE_MULTIPLE, count);
        if (!added) {
            return new CommandResult(MESSAGE_ALREADY_ACTIVE + feedback);
        }
        // A cumulative search can only remove members, so equal counts mean the view is unchanged.
        return new CommandResult((count == previousCount ? MESSAGE_UNCHANGED : "") + feedback);
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

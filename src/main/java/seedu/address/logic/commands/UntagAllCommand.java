package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Removes one tag from every member in the currently displayed list.
 */
public class UntagAllCommand extends Command {
    public static final String COMMAND_WORD = "untagall";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Removes one tag from every member in the current list. "
            + "Members who do not have the tag are skipped.\n"
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/committee";
    public static final String MESSAGE_SUCCESS = "Removed tag \"%1$s\" from %2$d member(s).";
    public static final String MESSAGE_NOT_TAGGED = " %1$d did not have this tag.";
    public static final String MESSAGE_EMPTY_LIST = "There are no members in the current list to update.";

    private static final Logger logger = LogsCenter.getLogger(UntagAllCommand.class);

    private final Tag tag;

    /**
     * Creates a command that removes {@code tag} from every displayed member.
     */
    public UntagAllCommand(Tag tag) {
        this.tag = requireNonNull(tag);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        // Copies the displayed members first, as removing a filtered tag can hide members from the displayed list.
        List<Person> targets = new ArrayList<>(model.getFilteredPersonList());
        if (targets.isEmpty()) {
            logger.info("untagall rejected: no members in the current list");
            throw new CommandException(MESSAGE_EMPTY_LIST);
        }

        int untaggedCount = 0;
        for (Person target : targets) {
            if (target.getTags().contains(tag)) {
                model.setPerson(target, createUntaggedPerson(target));
                untaggedCount++;
            }
        }

        int skippedCount = targets.size() - untaggedCount;
        logger.info(String.format("untagall t/%s: updated %d of %d displayed member(s), skipped %d",
                tag.tagName, untaggedCount, targets.size(), skippedCount));
        String message = String.format(MESSAGE_SUCCESS, tag.tagName, untaggedCount);
        if (skippedCount > 0) {
            message += String.format(MESSAGE_NOT_TAGGED, skippedCount);
        }
        return new CommandResult(message);
    }

    /**
     * Returns a copy of {@code person} with {@code tag} removed from the existing tags.
     */
    private Person createUntaggedPerson(Person person) {
        Set<Tag> updatedTags = new HashSet<>(person.getTags());
        updatedTags.remove(tag);
        return new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                updatedTags);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof UntagAllCommand otherUntagAllCommand)) {
            return false;
        }

        return tag.equals(otherUntagAllCommand.tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("tag", tag)
                .toString();
    }
}

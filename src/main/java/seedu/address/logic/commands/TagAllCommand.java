package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Adds one tag to every member in the currently displayed list.
 */
public class TagAllCommand extends Command {
    public static final String COMMAND_WORD = "tagall";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Adds one tag to every member in the current list. "
            + "Members who already have the tag are skipped.\n"
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/committee";
    public static final String MESSAGE_SUCCESS = "Added tag \"%1$s\" to %2$d member(s).";
    public static final String MESSAGE_ALREADY_TAGGED = " %1$d already had this tag.";
    public static final String MESSAGE_EMPTY_LIST = "There are no members in the current list to update.";

    private final Tag tag;

    /**
     * Creates a command that adds {@code tag} to every displayed member.
     */
    public TagAllCommand(Tag tag) {
        this.tag = requireNonNull(tag);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        // Copies the displayed members first, as updating a member refreshes the displayed list.
        List<Person> targets = new ArrayList<>(model.getFilteredPersonList());
        if (targets.isEmpty()) {
            throw new CommandException(MESSAGE_EMPTY_LIST);
        }

        int taggedCount = 0;
        for (Person target : targets) {
            if (!target.getTags().contains(tag)) {
                model.setPerson(target, createTaggedPerson(target));
                taggedCount++;
            }
        }

        int skippedCount = targets.size() - taggedCount;
        String message = String.format(MESSAGE_SUCCESS, tag.tagName, taggedCount);
        if (skippedCount > 0) {
            message += String.format(MESSAGE_ALREADY_TAGGED, skippedCount);
        }
        return new CommandResult(message);
    }

    /**
     * Returns a copy of {@code person} with {@code tag} added to the existing tags.
     */
    private Person createTaggedPerson(Person person) {
        Set<Tag> updatedTags = new HashSet<>(person.getTags());
        updatedTags.add(tag);
        return new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                updatedTags);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof TagAllCommand otherTagAllCommand)) {
            return false;
        }

        return tag.equals(otherTagAllCommand.tag);
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

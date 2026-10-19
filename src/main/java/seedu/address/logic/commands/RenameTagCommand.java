package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Renames one tag for every member in the roster, including members hidden by a search or filter.
 */
public class RenameTagCommand extends Command {
    public static final String COMMAND_WORD = "renametag";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Renames a tag for every member in the roster, including hidden members.\n"
            + "Parameters: t/CURRENT_TAG t/NEW_TAG\n"
            + "Example: " + COMMAND_WORD + " t/batch2026 t/alumni";
    public static final String MESSAGE_SUCCESS = "Renamed tag \"%1$s\" to \"%2$s\" for %3$d member(s).";
    public static final String MESSAGE_MERGED = " %1$d already had \"%2$s\".";
    public static final String MESSAGE_TAG_NOT_FOUND = "No member has the tag \"%1$s\".";

    private static final Logger logger = LogsCenter.getLogger(RenameTagCommand.class);

    private final Tag oldTag;
    private final Tag newTag;

    /**
     * Creates a command that replaces {@code oldTag} with {@code newTag} for every member who has {@code oldTag}.
     */
    public RenameTagCommand(Tag oldTag, Tag newTag) {
        requireAllNonNull(oldTag, newTag);
        this.oldTag = oldTag;
        this.newTag = newTag;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        // Copies the targets first, as updating a member changes the roster being read.
        List<Person> targets = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getTags().contains(oldTag))
                .toList();
        if (targets.isEmpty()) {
            logger.info(String.format("renametag rejected: no member has tag %s", oldTag.tagName));
            throw new CommandException(String.format(MESSAGE_TAG_NOT_FOUND, oldTag.tagName));
        }

        int mergedCount = 0;
        for (Person target : targets) {
            if (target.getTags().contains(newTag)) {
                mergedCount++;
            }
            model.setPerson(target, createRenamedPerson(target));
        }

        logger.info(String.format("renametag t/%s t/%s: updated %d member(s), %d already had the new tag",
                oldTag.tagName, newTag.tagName, targets.size(), mergedCount));
        String message = String.format(MESSAGE_SUCCESS, oldTag.tagName, newTag.tagName, targets.size());
        if (mergedCount > 0) {
            message += String.format(MESSAGE_MERGED, mergedCount, newTag.tagName);
        }
        return new CommandResult(message);
    }

    /**
     * Returns a copy of {@code person} with {@code oldTag} replaced by {@code newTag}.
     */
    private Person createRenamedPerson(Person person) {
        Set<Tag> updatedTags = new HashSet<>(person.getTags());
        updatedTags.remove(oldTag);
        updatedTags.add(newTag);
        return new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                updatedTags);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof RenameTagCommand otherRenameTagCommand)) {
            return false;
        }

        return oldTag.equals(otherRenameTagCommand.oldTag) && newTag.equals(otherRenameTagCommand.newTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldTag, newTag);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("oldTag", oldTag)
                .add("newTag", newTag)
                .toString();
    }
}

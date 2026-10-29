package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.ArrayList;
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
 * Renames one tag for every member in the currently displayed list who has it.
 */
public class RenameTagCommand extends Command {
    public static final String COMMAND_WORD = "renametag";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Renames a tag for every member in the current list who has it. "
            + "Hidden members are not changed.\n"
            + "Parameters: t/CURRENT_TAG t/NEW_TAG\n"
            + "Example: " + COMMAND_WORD + " t/batch2026 t/alumni";
    public static final String MESSAGE_SUCCESS = "Renamed tag \"%1$s\" to \"%2$s\" for %3$d member(s).";
    public static final String MESSAGE_MERGED = " %1$d already had \"%2$s\".";
    public static final String MESSAGE_TAG_NOT_FOUND = "No member in the current list has the tag \"%1$s\".";
    public static final String MESSAGE_ALREADY_SPELLED =
            "Every displayed member with this tag already uses \"%1$s\". No changes were made.";
    public static final String MESSAGE_SAME_TAG = "The new tag must be different from the current tag.";

    private static final Logger logger = LogsCenter.getLogger(RenameTagCommand.class);

    private final Tag oldTag;
    private final Tag newTag;

    /**
     * Creates a command that replaces {@code oldTag} with {@code newTag} for every displayed member
     * who has {@code oldTag}.
     */
    public RenameTagCommand(Tag oldTag, Tag newTag) {
        requireAllNonNull(oldTag, newTag);
        this.oldTag = oldTag;
        this.newTag = newTag;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (oldTag.tagName.equals(newTag.tagName)) {
            throw new CommandException(MESSAGE_SAME_TAG);
        }
        // Copies the targets first, as updating a member refreshes the displayed list.
        List<Person> targets = model.getFilteredPersonList().stream()
                .filter(person -> person.getTags().contains(oldTag))
                .toList();
        if (targets.isEmpty()) {
            logger.info(String.format("renametag rejected: no displayed member has tag %s", oldTag.tagName));
            throw new CommandException(String.format(MESSAGE_TAG_NOT_FOUND, oldTag.tagName));
        }

        boolean isCaseOnlyRename = oldTag.equals(newTag);
        int renamedCount = 0;
        int mergedCount = 0;
        List<Person> renamedPersons = new ArrayList<>();
        for (Person target : targets) {
            if (isCaseOnlyRename && target.getTags().stream()
                    .anyMatch(tag -> tag.tagName.equals(newTag.tagName))) {
                continue;
            }
            if (!isCaseOnlyRename && target.getTags().contains(newTag)) {
                mergedCount++;
            }
            Person renamedPerson = createRenamedPerson(target);
            model.setPerson(target, renamedPerson);
            renamedPersons.add(renamedPerson);
            renamedCount++;
        }

        logger.info(String.format("renametag t/%s t/%s: updated %d member(s), %d already had the new tag",
                oldTag.tagName, newTag.tagName, renamedCount, mergedCount));
        if (renamedCount == 0) {
            return new CommandResult(String.format(MESSAGE_ALREADY_SPELLED, newTag.tagName));
        }
        String message = String.format(MESSAGE_SUCCESS, oldTag.tagName, newTag.tagName, renamedCount);
        if (mergedCount > 0) {
            message += String.format(MESSAGE_MERGED, mergedCount, newTag.tagName);
        }
        if (!model.getFilteredPersonList().containsAll(renamedPersons)) {
            message += FilterCommand.describeHiddenUpdates(model);
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

        return oldTag.tagName.equals(otherRenameTagCommand.oldTag.tagName)
                && newTag.tagName.equals(otherRenameTagCommand.newTag.tagName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldTag.tagName, newTag.tagName);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("oldTag", oldTag)
                .add("newTag", newTag)
                .toString();
    }
}

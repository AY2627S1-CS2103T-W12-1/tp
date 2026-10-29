package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Adds or removes one tag for every member in the currently displayed list.
 * Members whose tags would not change are skipped.
 */
public abstract class BulkTagCommand extends Command {
    public static final String MESSAGE_EMPTY_LIST = "There are no members in the current list to update. "
            + "Run list to show everyone, or use find or filter to choose a group.";

    protected final Tag tag;

    private final Logger logger = LogsCenter.getLogger(getClass());
    private final String commandWord;
    private final String successMessage;
    private final String skippedMessage;

    /**
     * Creates a command that updates {@code tag} for every displayed member.
     *
     * @param tag The tag to add or remove.
     * @param commandWord The command word, used in log messages.
     * @param successMessage Format of the feedback, given the tag name and the number of updated members.
     * @param skippedMessage Format appended to the feedback, given the number of skipped members.
     */
    protected BulkTagCommand(Tag tag, String commandWord, String successMessage, String skippedMessage) {
        requireAllNonNull(tag, commandWord, successMessage, skippedMessage);
        this.tag = tag;
        this.commandWord = commandWord;
        this.successMessage = successMessage;
        this.skippedMessage = skippedMessage;
    }

    /**
     * Returns true if {@code person} needs {@code tag} added or removed.
     */
    protected abstract boolean needsUpdate(Person person);

    /**
     * Adds {@code tag} to, or removes it from, {@code tags}.
     */
    protected abstract void updateTags(Set<Tag> tags);

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> targets = getDisplayedMembers(model);

        List<Person> updatedPersons = new ArrayList<>();
        for (Person target : targets) {
            if (needsUpdate(target)) {
                Person updatedPerson = createUpdatedPerson(target);
                model.setPerson(target, updatedPerson);
                updatedPersons.add(updatedPerson);
            }
        }

        int skippedCount = targets.size() - updatedPersons.size();
        logger.info(String.format("%s t/%s: updated %d of %d displayed member(s), skipped %d",
                commandWord, tag.tagName, updatedPersons.size(), targets.size(), skippedCount));
        return new CommandResult(createFeedback(model, updatedPersons, skippedCount));
    }

    /**
     * Returns a copy of the displayed members, as updating a member refreshes the displayed list.
     *
     * @throws CommandException If no member is displayed.
     */
    private List<Person> getDisplayedMembers(Model model) throws CommandException {
        List<Person> displayedMembers = new ArrayList<>(model.getFilteredPersonList());
        if (displayedMembers.isEmpty()) {
            logger.info(commandWord + " rejected: no members in the current list");
            throw new CommandException(MESSAGE_EMPTY_LIST);
        }
        return displayedMembers;
    }

    /**
     * Returns a copy of {@code person} with {@code tag} added or removed.
     */
    private Person createUpdatedPerson(Person person) {
        Set<Tag> updatedTags = new HashSet<>(person.getTags());
        updateTags(updatedTags);
        return TagUpdateUtil.withTags(person, updatedTags);
    }

    /**
     * Returns the feedback for updating {@code updatedPersons} and skipping {@code skippedCount} members.
     * It also explains when the active filters now hide some of the updated members.
     */
    private String createFeedback(Model model, List<Person> updatedPersons, int skippedCount) {
        String feedback = String.format(successMessage, tag.tagName, updatedPersons.size());
        if (skippedCount > 0) {
            feedback += String.format(skippedMessage, skippedCount);
        }
        if (updatedPersons.isEmpty()) {
            return feedback + Messages.MESSAGE_NO_CHANGES;
        }
        return feedback + TagUpdateUtil.describeHiddenUpdates(model, updatedPersons);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // Tagging and untagging with the same tag are different commands.
        if (other == null || other.getClass() != getClass()) {
            return false;
        }

        return tag.tagName.equals(((BulkTagCommand) other).tag.tagName);
    }

    @Override
    public int hashCode() {
        return tag.tagName.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("tag", tag)
                .toString();
    }
}

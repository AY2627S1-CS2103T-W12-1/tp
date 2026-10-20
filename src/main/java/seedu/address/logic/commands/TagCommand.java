package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/** Adds one tag to a member identified by their displayed index. */
public class TagCommand extends Command {
    public static final String COMMAND_WORD = "tag";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Adds one tag to the member at the displayed index, retaining their other tags.\n"
            + "Parameters: INDEX (positive integer) t/TAG\n"
            + "Example: " + COMMAND_WORD + " 1 t/committee";
    public static final String MESSAGE_SUCCESS = "Added tag \"%1$s\" to member %2$d.\n%3$s";
    public static final String MESSAGE_ALREADY_TAGGED = "Member %1$d already has tag \"%2$s\". No changes were made.";

    private final Index targetIndex;
    private final Tag tag;

    /** Creates a command to add {@code tag} to the member at {@code targetIndex}. */
    public TagCommand(Index targetIndex, Tag tag) {
        this.targetIndex = requireNonNull(targetIndex);
        this.tag = requireNonNull(tag);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> displayed = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= displayed.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person target = displayed.get(targetIndex.getZeroBased());
        if (target.getTags().contains(tag)) {
            return new CommandResult(String.format(MESSAGE_ALREADY_TAGGED, targetIndex.getOneBased(), tag.tagName));
        }

        Set<Tag> updatedTags = new HashSet<>(target.getTags());
        updatedTags.add(tag);
        Person updated = new Person(target.getName(), target.getPhone(), target.getEmail(), target.getAddress(),
                updatedTags);
        model.setPerson(target, updated);
        String sortedTags = updated.getTags().stream().map(memberTag -> memberTag.tagName)
                .sorted().collect(Collectors.joining(", "));
        String summary = "Name: " + updated.getName() + "\nTags: " + sortedTags;
        return new CommandResult(String.format(MESSAGE_SUCCESS, tag.tagName, targetIndex.getOneBased(),
                summary));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof TagCommand otherTagCommand
                && targetIndex.equals(otherTagCommand.targetIndex) && tag.equals(otherTagCommand.tag);
    }

    @Override
    public int hashCode() {
        return 31 * targetIndex.getZeroBased() + tag.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetIndex", targetIndex).add("tag", tag).toString();
    }
}

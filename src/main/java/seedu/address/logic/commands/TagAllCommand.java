package seedu.address.logic.commands;

import java.util.Set;

import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Adds one tag to every member in the currently displayed list.
 */
public class TagAllCommand extends BulkTagCommand {
    public static final String COMMAND_WORD = "tagall";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Adds one tag to every member in the current list. "
            + "Members who already have the tag are skipped.\n"
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/committee";
    public static final String MESSAGE_SUCCESS = "Added tag \"%1$s\" to %2$d member(s).";
    public static final String MESSAGE_ALREADY_TAGGED = " %1$d already had this tag (any letter case).";

    /**
     * Creates a command that adds {@code tag} to every displayed member.
     */
    public TagAllCommand(Tag tag) {
        super(tag, COMMAND_WORD, MESSAGE_SUCCESS, MESSAGE_ALREADY_TAGGED);
    }

    @Override
    protected boolean needsUpdate(Person person) {
        return !person.getTags().contains(tag);
    }

    @Override
    protected void updateTags(Set<Tag> tags) {
        boolean isAdded = tags.add(tag);
        assert isAdded : "tagall updates only members who do not have the tag";
    }
}

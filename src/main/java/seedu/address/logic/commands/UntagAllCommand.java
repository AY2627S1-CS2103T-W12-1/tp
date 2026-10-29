package seedu.address.logic.commands;

import java.util.Set;

import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Removes one tag from every member in the currently displayed list.
 */
public class UntagAllCommand extends BulkTagCommand {
    public static final String COMMAND_WORD = "untagall";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Removes one tag from every member in the current list. "
            + "Members who do not have the tag are skipped.\n"
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/committee";
    public static final String MESSAGE_SUCCESS = "Removed tag \"%1$s\" (any letter case) from %2$d member(s).";
    public static final String MESSAGE_NOT_TAGGED = " %1$d did not have this tag.";

    /**
     * Creates a command that removes {@code tag} from every displayed member.
     */
    public UntagAllCommand(Tag tag) {
        super(tag, COMMAND_WORD, MESSAGE_SUCCESS, MESSAGE_NOT_TAGGED);
    }

    @Override
    protected boolean needsUpdate(Person person) {
        return person.getTags().contains(tag);
    }

    @Override
    protected void updateTags(Set<Tag> tags) {
        boolean isRemoved = tags.remove(tag);
        assert isRemoved : "untagall updates only members who have the tag";
    }
}

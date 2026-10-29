package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import seedu.address.commons.core.LogsCenter;
import seedu.address.model.Model;
import seedu.address.model.tag.Tag;

/**
 * Narrows the displayed members to those with the specified tag.
 */
public class FilterCommand extends Command {
    public static final String COMMAND_WORD = "filter";
    public static final String MESSAGE_USAGE = "filter: Shows currently displayed members with one tag.\n"
            + "Parameters: t/TAG\nExample: filter t/committee";
    public static final String MESSAGE_SUCCESS = "%d member(s) listed with %s %s.";
    public static final String MESSAGE_ALREADY_FILTERED = "Tag \"%s\" is already an active filter. "
            + "No changes were made.\n";

    private static final Logger logger = LogsCenter.getLogger(FilterCommand.class);

    private final Tag tag;

    /**
     * Creates a command that filters the displayed members by {@code tag}.
     */
    public FilterCommand(Tag tag) {
        this.tag = requireNonNull(tag);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        Tag existingTag = model.getActiveTagFilters().stream().filter(tag::equals).findFirst().orElse(null);
        if (existingTag == null) {
            model.filterFilteredPersonListByTag(tag);
        }
        List<Tag> activeTags = model.getActiveTagFilters();
        String tagNames = activeTags.stream()
                .map(activeTag -> "\"" + activeTag.tagName + "\"")
                .collect(Collectors.joining(", "));
        String tagLabel = activeTags.size() == 1 ? "tag" : "tags";
        int displayedCount = model.getFilteredPersonList().size();
        logger.info(String.format("filter t/%s: %d member(s) displayed; active tag filters (%d): %s; "
                + "name searches are not included", tag.tagName, displayedCount, activeTags.size(), tagNames));
        String feedback = String.format(MESSAGE_SUCCESS, displayedCount, tagLabel, tagNames);
        if (existingTag != null) {
            feedback = String.format(MESSAGE_ALREADY_FILTERED, existingTag.tagName) + feedback;
        }
        return new CommandResult(feedback);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof FilterCommand otherFilterCommand && tag.equals(otherFilterCommand.tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }
}

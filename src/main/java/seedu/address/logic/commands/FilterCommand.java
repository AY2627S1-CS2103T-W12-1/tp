package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import seedu.address.commons.core.LogsCenter;
import seedu.address.model.ActiveFilter;
import seedu.address.model.Model;
import seedu.address.model.tag.Tag;

/**
 * Narrows the displayed members to those with at least one of the specified tags.
 * Tags given in the same command are alternatives (OR); each separate command narrows the list further (AND).
 */
public class FilterCommand extends Command {
    public static final String COMMAND_WORD = "filter";
    public static final String MESSAGE_USAGE = "filter: Shows currently displayed members with any of the "
            + "specified tags.\nParameters: t/TAG [t/MORE_TAGS]...\nExample: filter t/committee t/alumni";
    public static final String MESSAGE_SUCCESS = "%d member(s) listed with %s %s.";
    public static final String MESSAGE_LISTED = "%d member(s) listed.";
    public static final String MESSAGE_RUN_LIST = " Run list to show everyone.";
    public static final String MESSAGE_ALREADY_FILTERED = "Tag \"%s\" is already an active filter. "
            + "No changes were made.\n";
    public static final String MESSAGE_ALREADY_FILTERED_ANY = "A filter for the tags %s is already active. "
            + "No changes were made.\n";

    private static final Logger logger = LogsCenter.getLogger(FilterCommand.class);

    /** The alternative tags, without repeats and in the order they were typed. */
    private final Set<Tag> tags;

    /**
     * Creates a command that filters the displayed members by {@code tag}.
     */
    public FilterCommand(Tag tag) {
        this(List.of(tag));
    }

    /**
     * Creates a command that shows members who have at least one of {@code tags}. Repeated tags count once.
     *
     * @throws IllegalArgumentException If {@code tags} is empty.
     */
    public FilterCommand(Collection<Tag> tags) {
        requireNonNull(tags);
        this.tags = new LinkedHashSet<>(List.copyOf(tags));
        if (this.tags.isEmpty()) {
            throw new IllegalArgumentException("At least one tag is required");
        }
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        List<Tag> requestedTags = List.copyOf(tags);
        boolean isAdded = model.filterFilteredPersonListByTags(requestedTags);
        List<Tag> activeTags = model.getActiveTagFilters();
        String typedArguments = requestedTags.stream().map(tag -> "t/" + tag.tagName)
                .collect(Collectors.joining(" "));
        logger.info(String.format("filter %s: %d member(s) displayed; active tag filters (%d): %s; "
                + "name searches are not included", typedArguments,
                model.getFilteredPersonList().size(), activeTags.size(), formatTagNames(activeTags)));
        String feedback = describeDisplayedMembers(model);
        if (!isAdded) {
            feedback = describeAlreadyActive(model, requestedTags) + feedback;
        }
        return new CommandResult(feedback);
    }

    /**
     * Describes the displayed member count and, when tag filters are active, those tags,
     * for example {@code 2 member(s) listed with tag "friends".} Alternatives within one filter are joined
     * with " / ", for example {@code 3 member(s) listed with tags "friends" / "owesMoney", "year1".}
     */
    public static String describeDisplayedMembers(Model model) {
        requireNonNull(model);
        int displayedCount = model.getFilteredPersonList().size();
        List<Tag> activeTags = model.getActiveTagFilters();
        if (activeTags.isEmpty()) {
            return String.format(MESSAGE_LISTED, displayedCount);
        }
        String tagLabel = activeTags.size() == 1 ? "tag" : "tags";
        String conditions = model.getActiveFilters().stream()
                .filter(filter -> filter.getKind() == ActiveFilter.Kind.TAG)
                .map(filter -> formatAlternatives(filter.getTags()))
                .collect(Collectors.joining(", "));
        return String.format(MESSAGE_SUCCESS, displayedCount, tagLabel, conditions);
    }

    /**
     * Describes the existing filter for {@code requestedTags}, using the spelling it was created with.
     */
    private static String describeAlreadyActive(Model model, List<Tag> requestedTags) {
        List<Tag> existingTags = model.getActiveFilters().stream()
                .filter(filter -> filter.hasSameTags(requestedTags))
                .findFirst()
                .map(ActiveFilter::getTags)
                .orElse(requestedTags);
        if (existingTags.size() == 1) {
            return String.format(MESSAGE_ALREADY_FILTERED, existingTags.getFirst().tagName);
        }
        return String.format(MESSAGE_ALREADY_FILTERED_ANY, formatAlternatives(existingTags));
    }

    /** Quotes each tag name and joins them with " / ", as they are in one filter. */
    private static String formatAlternatives(List<Tag> alternatives) {
        return alternatives.stream()
                .map(activeTag -> "\"" + activeTag.tagName + "\"")
                .collect(Collectors.joining(ActiveFilter.ALTERNATIVES_SEPARATOR));
    }

    private static String formatTagNames(List<Tag> tags) {
        return tags.stream()
                .map(activeTag -> "\"" + activeTag.tagName + "\"")
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object other) {
        return other == this
                || other instanceof FilterCommand otherFilterCommand && tags.equals(otherFilterCommand.tags);
    }

    @Override
    public int hashCode() {
        return tags.hashCode();
    }
}

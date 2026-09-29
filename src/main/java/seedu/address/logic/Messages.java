package seedu.address.logic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.address.logic.parser.Prefix;
import seedu.address.model.person.Person;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command. Type help for available commands.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format.\n\n%1$s";
    public static final String MESSAGE_INVALID_PERSON_DISPLAYED_INDEX =
            "No member has that displayed index. Use list, then choose a number shown beside a member.";
    public static final String MESSAGE_PERSONS_LISTED_OVERVIEW = "Members found: %1$d";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Formats member details as labelled rows, with tags in a stable alphabetical order.
     */
    public static String format(Person person) {
        String tags = person.getTags().stream()
                .map(tag -> tag.tagName)
                .sorted()
                .collect(Collectors.joining(", "));
        return "Name: " + person.getName()
                + "\nPhone: " + person.getPhone()
                + "\nEmail: " + person.getEmail()
                + "\nAddress: " + person.getAddress()
                + "\nTags: " + (tags.isEmpty() ? "(none)" : tags);
    }

}

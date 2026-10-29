package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Helpers shared by the commands that update the tags of the displayed members.
 */
final class TagUpdateUtil {

    private TagUpdateUtil() {
        // Prevents instantiation of this utility class.
    }

    /**
     * Returns a copy of {@code person} with {@code tags} as its tags.
     */
    static Person withTags(Person person, Set<Tag> tags) {
        requireNonNull(person);
        requireNonNull(tags);
        return new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(), tags);
    }

    /**
     * Returns feedback that explains why updated members disappeared from the displayed list,
     * or an empty string if all of {@code updatedPersons} are still displayed.
     */
    static String describeHiddenUpdates(Model model, List<Person> updatedPersons) {
        requireNonNull(model);
        if (model.getFilteredPersonList().containsAll(updatedPersons)) {
            return "";
        }
        return "\n" + FilterCommand.describeDisplayedMembers(model) + FilterCommand.MESSAGE_RUN_LIST;
    }
}

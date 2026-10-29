package seedu.address.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * A utility class for building the expected model of tag commands.
 */
public class TagTestUtil {

    /**
     * Adds the tag named {@code tagName} to every member displayed in {@code model} who does not have it.
     */
    public static void addTagToDisplayed(Model model, String tagName) {
        updateTagNames(model, model.getFilteredPersonList(), tagNames -> {
            if (!tagNames.contains(tagName)) {
                tagNames.add(tagName);
            }
        });
    }

    /**
     * Removes the tag named {@code tagName} from every member displayed in {@code model}.
     */
    public static void removeTagFromDisplayed(Model model, String tagName) {
        updateTagNames(model, model.getFilteredPersonList(), tagNames -> tagNames.remove(tagName));
    }

    /**
     * Replaces the tag named {@code oldName} with {@code newName} for every member stored in {@code model},
     * keeping one copy of {@code newName}.
     */
    public static void renameTagForAll(Model model, String oldName, String newName) {
        updateTagNames(model, model.getAddressBook().getPersonList(), tagNames -> {
            if (tagNames.remove(oldName) && !tagNames.contains(newName)) {
                tagNames.add(newName);
            }
        });
    }

    /**
     * Replaces each of {@code persons} in {@code model} whose tag names {@code update} changes.
     */
    private static void updateTagNames(Model model, List<Person> persons, Consumer<List<String>> update) {
        // Copies the members first, as replacing a member changes the list it came from.
        for (Person person : new ArrayList<>(persons)) {
            List<String> originalTagNames = person.getTags().stream().map(tag -> tag.tagName).toList();
            List<String> updatedTagNames = new ArrayList<>(originalTagNames);
            update.accept(updatedTagNames);
            if (!updatedTagNames.equals(originalTagNames)) {
                Person updatedPerson = new PersonBuilder(person)
                        .withTags(updatedTagNames.toArray(new String[0]))
                        .build();
                model.setPerson(person, updatedPerson);
            }
        }
    }
}

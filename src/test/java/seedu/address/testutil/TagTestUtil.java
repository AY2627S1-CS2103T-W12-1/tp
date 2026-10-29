package seedu.address.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import seedu.address.logic.commands.Command;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;

/**
 * A utility class for building the expected model of tag commands.
 */
public final class TagTestUtil {

    private TagTestUtil() {
        // Prevents instantiation of this utility class.
    }

    /**
     * Returns a model of the typical members with all of them displayed.
     */
    public static Model createTypicalModel() {
        return new ModelManager(getTypicalAddressBook(), new UserPrefs());
    }

    /**
     * Returns a model of the typical members, displaying only those that match {@code displayed}.
     */
    public static Model createTypicalModel(Predicate<Person> displayed) {
        Model model = createTypicalModel();
        model.updateFilteredPersonList(displayed);
        return model;
    }

    /**
     * Asserts that {@code command} succeeds as described by {@link CommandTestUtil#assertCommandSuccess}
     * and logs exactly {@code expectedLog} at the info level.
     */
    public static void assertCommandSuccessWithLog(Command command, Model model, String expectedMessage,
            Model expectedModel, String expectedLog) {
        try (LogCapture logs = new LogCapture(command.getClass())) {
            assertCommandSuccess(command, model, expectedMessage, expectedModel);
            assertEquals(List.of(expectedLog), logs.getInfoMessages());
        }
    }

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

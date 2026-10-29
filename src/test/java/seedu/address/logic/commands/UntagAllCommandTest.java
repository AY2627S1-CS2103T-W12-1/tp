package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.LogCapture;
import seedu.address.testutil.PersonBuilder;

public class UntagAllCommandTest {
    private static final Predicate<Person> HAS_FRIENDS_TAG = person ->
            person.getTags().contains(new Tag("friends"));
    private static final Predicate<Person> IS_BENSON = person -> person.isSamePerson(BENSON);

    private final Tag committee = new Tag("committee");
    private final Tag friends = new Tag("friends");

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new UntagAllCommand(null));
    }

    @Test
    public void isReadOnly_dataChangingCommand_returnsFalse() {
        assertFalse(new UntagAllCommand(committee).isReadOnly());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new UntagAllCommand(committee).execute(null));
    }

    @Test
    public void execute_someDisplayedMembersHaveTag_removesTagAndSkipsOthers() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        removeTagFromAll(expectedModel, "friends");
        String expectedMessage = "Removed tag \"friends\" (any letter case)"
                + " from 3 member(s). 4 did not have this tag.";

        try (LogCapture logs = new LogCapture(UntagAllCommand.class)) {
            assertCommandSuccess(new UntagAllCommand(friends), model, expectedMessage, expectedModel);
            assertEquals(List.of("untagall t/friends: updated 3 of 7 displayed member(s), skipped 4"),
                    logs.getInfoMessages());
        }
        Person updatedBenson = new PersonBuilder(BENSON).withTags("owesMoney").build();
        assertTrue(model.getFilteredPersonList().contains(updatedBenson));
    }

    @Test
    public void execute_noDisplayedMemberHasTag_changesNothing() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        String expectedMessage = "Removed tag \"committee\" (any letter case)"
                + " from 0 member(s). 7 did not have this tag.";

        try (LogCapture logs = new LogCapture(UntagAllCommand.class)) {
            assertCommandSuccess(new UntagAllCommand(committee), model, expectedMessage, expectedModel);
            assertEquals(List.of("untagall t/committee: updated 0 of 7 displayed member(s), skipped 7"),
                    logs.getInfoMessages());
        }
    }

    @Test
    public void execute_removeTagUsedByFilter_hidesUpdatedMembersAndCountsOriginalTargets() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(HAS_FRIENDS_TAG);
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(HAS_FRIENDS_TAG);
        removeTagFromAll(expectedModel, "friends");
        String expectedMessage = "Removed tag \"friends\" (any letter case) from 3 member(s).";

        try (LogCapture logs = new LogCapture(UntagAllCommand.class)) {
            assertCommandSuccess(new UntagAllCommand(friends), model, expectedMessage, expectedModel);
            // Counts the members displayed before the update, although the filter now hides them all.
            assertEquals(List.of("untagall t/friends: updated 3 of 3 displayed member(s), skipped 0"),
                    logs.getInfoMessages());
        }
        // The filter stays applied, so the updated members are hidden but still stored.
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(7, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_filteredList_updatesOnlyDisplayedMembers() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(IS_BENSON);
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(IS_BENSON);
        removeTagFromAll(expectedModel, "friends");
        String expectedMessage = "Removed tag \"friends\" (any letter case) from 1 member(s).";

        assertCommandSuccess(new UntagAllCommand(friends), model, expectedMessage, expectedModel);
        assertTrue(model.getAddressBook().getPersonList().contains(ALICE));
        assertTrue(model.getAddressBook().getPersonList().contains(DANIEL));
    }

    @Test
    public void execute_emptyDisplayedList_throwsCommandException() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> false);

        try (LogCapture logs = new LogCapture(UntagAllCommand.class)) {
            assertCommandFailure(new UntagAllCommand(friends), model, UntagAllCommand.MESSAGE_EMPTY_LIST);
            assertEquals(List.of("untagall rejected: no members in the current list"), logs.getInfoMessages());
        }
    }

    @Test
    public void equals_sameAndDifferentTags_returnsCorrectResult() {
        UntagAllCommand command = new UntagAllCommand(committee);

        assertTrue(command.equals(command));
        assertTrue(command.equals(new UntagAllCommand(new Tag("committee"))));
        assertEquals(command.hashCode(), new UntagAllCommand(new Tag("committee")).hashCode());
        assertFalse(command.equals(new UntagAllCommand(friends)));
        assertFalse(command.equals(new TagAllCommand(committee)));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toString_validCommand_containsTag() {
        UntagAllCommand command = new UntagAllCommand(committee);
        String expected = UntagAllCommand.class.getCanonicalName() + "{tag=" + committee + "}";
        assertEquals(expected, command.toString());
    }

    /**
     * Removes the tag named {@code tagName} from every member displayed in {@code model}.
     */
    private static void removeTagFromAll(Model model, String tagName) {
        List<Person> displayedPersons = new ArrayList<>(model.getFilteredPersonList());
        for (Person person : displayedPersons) {
            List<String> tagNames = new ArrayList<>(person.getTags().stream()
                    .map(tag -> tag.tagName)
                    .toList());
            if (tagNames.remove(tagName)) {
                Person untaggedPerson = new PersonBuilder(person)
                        .withTags(tagNames.toArray(new String[0]))
                        .build();
                model.setPerson(person, untaggedPerson);
            }
        }
    }
}

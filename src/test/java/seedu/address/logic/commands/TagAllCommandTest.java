package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TagTestUtil.addTagToDisplayed;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

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

public class TagAllCommandTest {
    private static final Predicate<Person> HAS_FRIENDS_TAG = person ->
            person.getTags().contains(new Tag("friends"));

    private final Tag committee = new Tag("committee");
    private final Tag friends = new Tag("friends");

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagAllCommand(null));
    }

    @Test
    public void isReadOnly_dataChangingCommand_returnsFalse() {
        assertFalse(new TagAllCommand(committee).isReadOnly());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagAllCommand(committee).execute(null));
    }

    @Test
    public void execute_noDisplayedMemberHasTag_addsTagToAll() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        addTagToDisplayed(expectedModel, "committee");
        String expectedMessage = "Added tag \"committee\" to 7 member(s).";

        assertCommandSuccess(new TagAllCommand(committee), model, expectedMessage, expectedModel);
        Person updatedBenson = new PersonBuilder(BENSON)
                .withTags("owesMoney", "friends", "committee")
                .build();
        assertTrue(model.getFilteredPersonList().contains(updatedBenson));
    }

    @Test
    public void execute_someDisplayedMembersHaveTag_skipsThem() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        addTagToDisplayed(expectedModel, "friends");
        String expectedMessage = "Added tag \"friends\" to 4 member(s). 3 already had this tag.";

        try (LogCapture logs = new LogCapture(TagAllCommand.class)) {
            assertCommandSuccess(new TagAllCommand(friends), model, expectedMessage, expectedModel);
            assertEquals(List.of("tagall t/friends: updated 4 of 7 displayed member(s), skipped 3"),
                    logs.getInfoMessages());
        }
        assertTrue(model.getFilteredPersonList().contains(ALICE));
    }

    @Test
    public void execute_allDisplayedMembersHaveTag_changesNothing() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.filterFilteredPersonList(HAS_FRIENDS_TAG);
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.filterFilteredPersonList(HAS_FRIENDS_TAG);
        String expectedMessage = "Added tag \"friends\" to 0 member(s). 3 already had this tag."
                + " No changes were made.";

        try (LogCapture logs = new LogCapture(TagAllCommand.class)) {
            assertCommandSuccess(new TagAllCommand(friends), model, expectedMessage, expectedModel);
            assertEquals(List.of("tagall t/friends: updated 0 of 3 displayed member(s), skipped 3"),
                    logs.getInfoMessages());
        }
    }

    @Test
    public void execute_filteredList_updatesOnlyDisplayedMembersAndKeepsFilter() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.filterFilteredPersonList(HAS_FRIENDS_TAG);
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.filterFilteredPersonList(HAS_FRIENDS_TAG);
        addTagToDisplayed(expectedModel, "committee");
        String expectedMessage = "Added tag \"committee\" to 3 member(s).";

        assertCommandSuccess(new TagAllCommand(committee), model, expectedMessage, expectedModel);
        assertEquals(3, model.getFilteredPersonList().size());
        assertTrue(model.getAddressBook().getPersonList().contains(CARL));
        assertFalse(model.getAddressBook().getPersonList().contains(DANIEL));
    }

    @Test
    public void execute_emptyDisplayedList_throwsCommandException() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> false);

        try (LogCapture logs = new LogCapture(TagAllCommand.class)) {
            assertCommandFailure(new TagAllCommand(committee), model, TagAllCommand.MESSAGE_EMPTY_LIST);
            assertEquals(List.of("tagall rejected: no members in the current list"), logs.getInfoMessages());
        }
    }

    @Test
    public void equals_sameAndDifferentTags_returnsCorrectResult() {
        TagAllCommand command = new TagAllCommand(committee);

        assertTrue(command.equals(command));
        assertTrue(command.equals(new TagAllCommand(new Tag("committee"))));
        assertEquals(command.hashCode(), new TagAllCommand(new Tag("committee")).hashCode());
        assertFalse(command.equals(new TagAllCommand(friends)));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toString_validCommand_containsTag() {
        TagAllCommand command = new TagAllCommand(committee);
        String expected = TagAllCommand.class.getCanonicalName() + "{tag=" + committee + "}";
        assertEquals(expected, command.toString());
    }
}

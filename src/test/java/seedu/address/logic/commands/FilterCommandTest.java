package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.tag.Tag;

public class FilterCommandTest {
    private final Tag friends = new Tag("friends");

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCommand(null));
    }

    @Test
    public void isReadOnly_returnsTrue() {
        assertTrue(new FilterCommand(friends).isReadOnly());
    }

    @Test
    public void execute_matchingTag_narrowsDisplayedList() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

        CommandResult result = new FilterCommand(friends).execute(model);

        assertEquals("3 member(s) listed with tag \"friends\".", result.getFeedbackToUser());
        assertEquals(List.of(ALICE, BENSON, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void execute_existingFilter_narrowsCurrentList() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> !person.equals(DANIEL));

        CommandResult result = new FilterCommand(friends).execute(model);

        assertEquals("2 member(s) listed with tag \"friends\".", result.getFeedbackToUser());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCommand(friends).execute(null));
    }

    @Test
    public void equals_andHashCode_matchTags() {
        FilterCommand command = new FilterCommand(friends);
        FilterCommand sameTag = new FilterCommand(new Tag("friends"));

        assertTrue(command.equals(command));
        assertEquals(command, sameTag);
        assertEquals(command.hashCode(), sameTag.hashCode());
        assertFalse(command.equals(new FilterCommand(new Tag("owesMoney"))));
        assertFalse(command.equals(null));
        assertFalse(command.equals("filter"));
    }
}

package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.NameContainsKeywordsPredicate;

public class ConfirmDeleteCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_memberInRoster_deletesThatMember() {
        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS, Messages.format(BENSON));
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.deletePerson(BENSON);

        assertCommandSuccess(new ConfirmDeleteCommand(BENSON), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_memberHiddenFromCurrentList_stillDeletesNamedMember() {
        // The prompt names a member, so a later view change must not redirect the deletion to another member.
        model.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of("Alice")));
        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS, Messages.format(BENSON));
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of("Alice")));
        expectedModel.deletePerson(BENSON);

        assertCommandSuccess(new ConfirmDeleteCommand(BENSON), model, expectedMessage, expectedModel);
        assertTrue(model.getAddressBook().getPersonList().contains(ALICE));
    }

    @Test
    public void execute_memberNoLongerInRoster_throwsCommandException() {
        model.deletePerson(BENSON);

        assertCommandFailure(new ConfirmDeleteCommand(BENSON), model, ConfirmDeleteCommand.MESSAGE_MEMBER_NOT_FOUND);
    }

    @Test
    public void isReadOnly_confirmedDeletion_returnsFalse() {
        assertFalse(new ConfirmDeleteCommand(ALICE).isReadOnly());
    }

    @Test
    public void equals() {
        ConfirmDeleteCommand deleteAlice = new ConfirmDeleteCommand(ALICE);

        assertTrue(deleteAlice.equals(deleteAlice));
        assertTrue(deleteAlice.equals(new ConfirmDeleteCommand(ALICE)));
        assertEquals(deleteAlice.hashCode(), new ConfirmDeleteCommand(ALICE).hashCode());
        assertFalse(deleteAlice.equals(1));
        assertFalse(deleteAlice.equals(null));
        assertFalse(deleteAlice.equals(new ConfirmDeleteCommand(BENSON)));
    }

    @Test
    public void toStringMethod() {
        String expected = ConfirmDeleteCommand.class.getCanonicalName() + "{target=" + ALICE + "}";
        assertEquals(expected, new ConfirmDeleteCommand(ALICE).toString());
    }
}

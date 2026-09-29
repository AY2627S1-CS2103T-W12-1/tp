package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;

/** Tests whole-roster clearing and feedback, including records hidden from the current view. */
public class ClearCommandTest {

    @Test
    public void execute_emptyAddressBook_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_EMPTY, expectedModel);
    }

    @Test
    public void execute_nonEmptyAddressBook_success() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.setAddressBook(new AddressBook());

        String expectedMessage = String.format(ClearCommand.MESSAGE_SUCCESS,
                model.getAddressBook().getPersonList().size());
        assertCommandSuccess(new ClearCommand(), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_filteredAddressBook_countsHiddenMembers() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        int total = model.getAddressBook().getPersonList().size();
        Person shown = model.getAddressBook().getPersonList().getFirst();
        model.updateFilteredPersonList(shown::equals);
        assertEquals(1, model.getFilteredPersonList().size());

        Model expectedModel = new ModelManager();
        assertCommandSuccess(new ClearCommand(), model,
                String.format(ClearCommand.MESSAGE_SUCCESS, total), expectedModel);
    }

}

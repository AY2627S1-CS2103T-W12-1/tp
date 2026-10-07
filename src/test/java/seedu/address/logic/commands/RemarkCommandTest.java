package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

/** Contains integration tests and unit tests for {@link RemarkCommand}. */
public class RemarkCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_addRemark_success() {
        Remark remark = new Remark("Likes swimming.");
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, remark);
        var person = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        var editedPerson = new PersonBuilder(person).withRemark(remark.value).build();
        Model expectedModel = new ModelManager(new AddressBook(model.getAddressBook()), new UserPrefs());
        expectedModel.setPerson(person, editedPerson);

        assertCommandSuccess(command, model,
                String.format(RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS, Messages.format(editedPerson)), expectedModel);
        assertTrue(model.getAddressBook().getPersonList().contains(editedPerson));
    }

    @Test
    public void execute_removeRemark_success() {
        Model modelWithRemark = new ModelManager(new AddressBook(model.getAddressBook()), new UserPrefs());
        var person = modelWithRemark.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        var remarkedPerson = new PersonBuilder(person).withRemark("Temporary").build();
        modelWithRemark.setPerson(person, remarkedPerson);
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        var clearedPerson = new PersonBuilder(remarkedPerson).withRemark("").build();
        Model expectedModel = new ModelManager(new AddressBook(modelWithRemark.getAddressBook()), new UserPrefs());
        expectedModel.setPerson(remarkedPerson, clearedPerson);

        assertCommandSuccess(command, modelWithRemark,
                String.format(RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS, Messages.format(clearedPerson)),
                expectedModel);
    }

    @Test
    public void execute_invalidIndex_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new RemarkCommand(outOfBoundIndex, new Remark("Remark")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        RemarkCommand standardCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Remark"));
        assertTrue(standardCommand.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Remark"))));
        assertTrue(standardCommand.equals(standardCommand));
        assertFalse(standardCommand.equals(null));
        assertFalse(standardCommand.equals(new ClearCommand()));
        assertFalse(standardCommand.equals(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("Remark"))));
        assertFalse(standardCommand.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Other"))));
    }
}

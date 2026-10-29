package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.EditPersonDescriptorBuilder;
import seedu.address.testutil.PersonBuilder;

public class ShowCommandTest {
    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_multipleIndexes_showsSelectedMembersInDisplayedOrder() throws CommandException {
        CommandResult result = new ShowCommand(List.of(index(4), index(1), index(3), index(3))).execute(model);

        assertEquals(ShowCommand.MESSAGE_MULTIPLE.formatted(3), result.getFeedbackToUser());
        assertEquals(List.of(ALICE, CARL, DANIEL), model.getFilteredPersonList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
    }

    @Test
    public void execute_existingView_usesCurrentIndexesAndCanNarrowAgain() throws CommandException {
        model.updateFilteredPersonList(person -> !person.equals(ALICE));

        new ShowCommand(List.of(index(1), index(3))).execute(model);
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());

        CommandResult result = new ShowCommand(List.of(index(2))).execute(model);
        assertEquals(ShowCommand.MESSAGE_SINGLE, result.getFeedbackToUser());
        assertEquals(List.of(DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void execute_sortedView_usesDisplayedOrder() throws CommandException {
        AddressBook book = new AddressBook();
        book.addPerson(DANIEL);
        book.addPerson(CARL);
        book.addPerson(BENSON);
        book.addPerson(ALICE);
        Model sortedModel = new ModelManager(book, new UserPrefs());
        sortedModel.sortPersonsByName();

        new ShowCommand(List.of(index(1), index(3))).execute(sortedModel);

        assertEquals(List.of(ALICE, CARL), sortedModel.getFilteredPersonList());
    }

    @Test
    public void execute_bulkTagChanges_keepsOnlySelectedMembersVisible() throws CommandException {
        new ShowCommand(List.of(index(1), index(3))).execute(model);
        Tag tag = new Tag("selected");

        new TagAllCommand(tag).execute(model);

        Person taggedAlice = new PersonBuilder(ALICE).withTags("friends", "selected").build();
        Person taggedCarl = new PersonBuilder(CARL).withTags("selected").build();
        assertEquals(List.of(taggedAlice, taggedCarl), model.getFilteredPersonList());
        assertEquals(BENSON, model.getAddressBook().getPersonList().get(1));

        new UntagAllCommand(tag).execute(model);

        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
    }

    @Test
    public void execute_contactEditAfterRepeatedShow_keepsSelectionAcrossEveryPredicate() throws CommandException {
        new ShowCommand(List.of(index(1), index(3))).execute(model);
        new ShowCommand(List.of(index(2))).execute(model);

        new EditCommand(index(1), new EditPersonDescriptorBuilder().withPhone("91234567").build()).execute(model);

        Person editedCarl = new PersonBuilder(CARL).withPhone("91234567").build();
        assertEquals(List.of(editedCarl), model.getFilteredPersonList());
        assertEquals(editedCarl, model.getAddressBook().getPersonList().get(2));
        assertEquals(ALICE, model.getAddressBook().getPersonList().getFirst());
    }

    @Test
    public void execute_contactEdit_doesNotSelectAnotherMemberWithTheSameName() throws CommandException {
        Person sameNameMember = new PersonBuilder(ALICE).withPhone("90000001").build();
        AddressBook book = new AddressBook();
        book.addPerson(ALICE);
        book.addPerson(sameNameMember);
        Model sameNameModel = new ModelManager(book, new UserPrefs());
        new ShowCommand(List.of(index(1))).execute(sameNameModel);

        new EditCommand(index(1), new EditPersonDescriptorBuilder().withPhone("90000002").build())
                .execute(sameNameModel);

        assertEquals(List.of(new PersonBuilder(ALICE).withPhone("90000002").build()),
                sameNameModel.getFilteredPersonList());
        assertEquals(sameNameMember, sameNameModel.getAddressBook().getPersonList().get(1));
    }

    @Test
    public void execute_invalidIndex_doesNotChangeView() {
        ShowCommand command = new ShowCommand(List.of(index(1), index(8)));
        List<?> before = List.copyOf(model.getFilteredPersonList());

        CommandException exception = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, exception.getMessage());
        assertEquals(before, model.getFilteredPersonList());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ShowCommand(List.of(index(1))).execute(null));
    }

    @Test
    public void isReadOnly_returnsTrue() {
        assertTrue(new ShowCommand(List.of(index(1))).isReadOnly());
    }

    private Index index(int oneBased) {
        return Index.fromOneBased(oneBased);
    }
}

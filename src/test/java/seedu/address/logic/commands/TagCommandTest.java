package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class TagCommandTest {
    private final Tag committee = new Tag("committee");

    @Test
    public void execute_filteredList_updatesOnlySelectedMemberAndPreservesOtherTags() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        new FilterCommand(new Tag("friends")).execute(model);

        Person hidden = model.getAddressBook().getPersonList().get(2);
        String feedback = new TagCommand(INDEX_SECOND_PERSON, committee).execute(model).getFeedbackToUser();

        Person updatedBenson = new PersonBuilder(BENSON).withTags("friends", "owesMoney", "committee").build();
        assertEquals(List.of(ALICE, updatedBenson, model.getFilteredPersonList().get(2)),
                model.getFilteredPersonList());
        assertSame(hidden, model.getAddressBook().getPersonList().get(2));
        assertEquals(String.format(TagCommand.MESSAGE_SUCCESS, "committee", 2, Messages.format(updatedBenson)),
                feedback);
    }

    @Test
    public void execute_existingTag_reportsNoOpAndKeepsMember() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Person original = model.getFilteredPersonList().get(0);

        String feedback = new TagCommand(INDEX_FIRST_PERSON, new Tag("friends"))
                .execute(model).getFeedbackToUser();

        assertSame(original, model.getFilteredPersonList().get(0));
        assertEquals(String.format(TagCommand.MESSAGE_ALREADY_TAGGED, 1, "friends"), feedback);
    }

    @Test
    public void execute_showSelection_preservesSelectedMember() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        new ShowCommand(List.of(INDEX_SECOND_PERSON)).execute(model);

        new TagCommand(INDEX_FIRST_PERSON, committee).execute(model);

        assertEquals(1, model.getFilteredPersonList().size());
        assertTrue(model.getFilteredPersonList().get(0).getTags().contains(committee));
    }

    @Test
    public void execute_invalidDisplayedIndex_failsWithoutChanges() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        assertCommandFailure(new TagCommand(Index.fromOneBased(8), committee), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals_sameIndexAndTag_matches() {
        TagCommand command = new TagCommand(INDEX_FIRST_PERSON, committee);
        assertFalse(command.isReadOnly());
        assertEquals(command, new TagCommand(Index.fromOneBased(1), new Tag("committee")));
        assertEquals(command.hashCode(), new TagCommand(INDEX_FIRST_PERSON, committee).hashCode());
        assertFalse(command.equals(new TagCommand(INDEX_SECOND_PERSON, committee)));
    }
}

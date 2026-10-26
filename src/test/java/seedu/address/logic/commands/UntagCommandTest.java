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
import static seedu.address.testutil.TypicalPersons.DANIEL;
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

public class UntagCommandTest {
    private final Tag friends = new Tag("friends");

    @Test
    public void execute_filteredList_updatesOnlySelectedMemberAndPreservesOtherTags() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        new FilterCommand(friends).execute(model);
        Person hidden = model.getAddressBook().getPersonList().get(2);

        String feedback = new UntagCommand(INDEX_SECOND_PERSON, friends).execute(model).getFeedbackToUser();

        Person updatedBenson = new PersonBuilder(BENSON).withTags("owesMoney").build();
        assertEquals(List.of(ALICE, DANIEL), model.getFilteredPersonList());
        assertTrue(model.getAddressBook().getPersonList().contains(updatedBenson));
        assertSame(hidden, model.getAddressBook().getPersonList().get(2));
        assertEquals(String.format(UntagCommand.MESSAGE_SUCCESS, "friends", 2,
                "Name: " + updatedBenson.getName() + "\nTags: owesMoney"),
                feedback);
    }

    @Test
    public void execute_missingTag_reportsNoOpAndKeepsMember() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Person original = model.getFilteredPersonList().get(0);

        String feedback = new UntagCommand(INDEX_FIRST_PERSON, new Tag("committee"))
                .execute(model).getFeedbackToUser();

        assertSame(original, model.getFilteredPersonList().get(0));
        assertEquals(String.format(UntagCommand.MESSAGE_NOT_TAGGED, 1, "committee"), feedback);
    }

    @Test
    public void execute_showSelection_preservesSelectedMember() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        new ShowCommand(List.of(INDEX_SECOND_PERSON)).execute(model);

        new UntagCommand(INDEX_FIRST_PERSON, friends).execute(model);

        assertEquals(1, model.getFilteredPersonList().size());
        assertFalse(model.getFilteredPersonList().get(0).getTags().contains(friends));
    }

    @Test
    public void execute_invalidDisplayedIndex_failsWithoutChanges() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        assertCommandFailure(new UntagCommand(Index.fromOneBased(8), friends), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals_sameIndexAndTag_matches() {
        UntagCommand command = new UntagCommand(INDEX_FIRST_PERSON, friends);
        assertFalse(command.isReadOnly());
        assertEquals(command, new UntagCommand(Index.fromOneBased(1), new Tag("friends")));
        assertEquals(command.hashCode(), new UntagCommand(INDEX_FIRST_PERSON, friends).hashCode());
        assertFalse(command.equals(new UntagCommand(INDEX_SECOND_PERSON, friends)));
    }
}

package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TagTestUtil.addTagToDisplayed;
import static seedu.address.testutil.TagTestUtil.assertCommandSuccessWithLog;
import static seedu.address.testutil.TagTestUtil.createTypicalModel;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.DANIEL;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class TagAllCommandTest extends BulkTagCommandTest<TagAllCommand> {

    @Test
    public void execute_noDisplayedMemberHasTag_addsTagToAll() {
        Model model = createTypicalModel();
        Model expectedModel = createTypicalModel();
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
        Model model = createTypicalModel();
        Model expectedModel = createTypicalModel();
        addTagToDisplayed(expectedModel, "friends");
        String expectedMessage = "Added tag \"friends\" to 4 member(s). 3 already had this tag (any letter case).";

        assertCommandSuccessWithLog(new TagAllCommand(friends), model, expectedMessage, expectedModel,
                "tagall t/friends: updated 4 of 7 displayed member(s), skipped 3");
        assertTrue(model.getFilteredPersonList().contains(ALICE));
    }

    @Test
    public void execute_allDisplayedMembersHaveTag_changesNothing() {
        Model model = createTypicalModel(HAS_FRIENDS_TAG);
        Model expectedModel = createTypicalModel(HAS_FRIENDS_TAG);
        String expectedMessage = "Added tag \"friends\" to 0 member(s). 3 already had this tag (any letter case)."
                + " No changes were made.";

        assertCommandSuccessWithLog(new TagAllCommand(friends), model, expectedMessage, expectedModel,
                "tagall t/friends: updated 0 of 3 displayed member(s), skipped 3");
    }

    @Test
    public void execute_filteredList_updatesOnlyDisplayedMembersAndKeepsFilter() {
        Model model = createTypicalModel(HAS_FRIENDS_TAG);
        Model expectedModel = createTypicalModel(HAS_FRIENDS_TAG);
        addTagToDisplayed(expectedModel, "committee");
        String expectedMessage = "Added tag \"committee\" to 3 member(s).";

        assertCommandSuccess(new TagAllCommand(committee), model, expectedMessage, expectedModel);
        assertEquals(3, model.getFilteredPersonList().size());
        assertTrue(model.getAddressBook().getPersonList().contains(CARL));
        assertFalse(model.getAddressBook().getPersonList().contains(DANIEL));
    }

    @Override
    protected TagAllCommand createCommand(Tag tag) {
        return new TagAllCommand(tag);
    }

    @Override
    protected BulkTagCommand createOtherCommand(Tag tag) {
        return new UntagAllCommand(tag);
    }

    @Override
    protected String getCommandWord() {
        return TagAllCommand.COMMAND_WORD;
    }
}

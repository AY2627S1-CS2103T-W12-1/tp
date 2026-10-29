package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TagTestUtil.assertCommandSuccessWithLog;
import static seedu.address.testutil.TagTestUtil.createTypicalModel;
import static seedu.address.testutil.TagTestUtil.removeTagFromDisplayed;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.DANIEL;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class UntagAllCommandTest extends BulkTagCommandTest<UntagAllCommand> {
    private static final Predicate<Person> IS_BENSON = person -> person.isSamePerson(BENSON);

    @Test
    public void execute_someDisplayedMembersHaveTag_removesTagAndSkipsOthers() {
        Model model = createTypicalModel();
        Model expectedModel = createTypicalModel();
        removeTagFromDisplayed(expectedModel, "friends");
        String expectedMessage = "Removed tag \"friends\" (any letter case)"
                + " from 3 member(s). 4 did not have this tag.";

        assertCommandSuccessWithLog(new UntagAllCommand(friends), model, expectedMessage, expectedModel,
                "untagall t/friends: updated 3 of 7 displayed member(s), skipped 4");
        Person updatedBenson = new PersonBuilder(BENSON).withTags("owesMoney").build();
        assertTrue(model.getFilteredPersonList().contains(updatedBenson));
    }

    @Test
    public void execute_noDisplayedMemberHasTag_changesNothing() {
        Model model = createTypicalModel();
        Model expectedModel = createTypicalModel();
        String expectedMessage = "Removed tag \"committee\" (any letter case)"
                + " from 0 member(s). 7 did not have this tag. No changes were made.";

        assertCommandSuccessWithLog(new UntagAllCommand(committee), model, expectedMessage, expectedModel,
                "untagall t/committee: updated 0 of 7 displayed member(s), skipped 7");
    }

    @Test
    public void execute_removeTagUsedByFilter_hidesUpdatedMembersAndCountsOriginalTargets() {
        Model model = createTypicalModel(HAS_FRIENDS_TAG);
        Model expectedModel = createTypicalModel(HAS_FRIENDS_TAG);
        removeTagFromDisplayed(expectedModel, "friends");
        String expectedMessage = "Removed tag \"friends\" (any letter case) from 3 member(s).\n"
                + "0 member(s) listed. Run list to show everyone.";

        // Counts the members displayed before the update, although the filter now hides them all.
        assertCommandSuccessWithLog(new UntagAllCommand(friends), model, expectedMessage, expectedModel,
                "untagall t/friends: updated 3 of 3 displayed member(s), skipped 0");
        // The filter stays applied, so the updated members are hidden but still stored.
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(7, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_filteredList_updatesOnlyDisplayedMembers() {
        Model model = createTypicalModel(IS_BENSON);
        Model expectedModel = createTypicalModel(IS_BENSON);
        removeTagFromDisplayed(expectedModel, "friends");
        String expectedMessage = "Removed tag \"friends\" (any letter case) from 1 member(s).";

        assertCommandSuccess(new UntagAllCommand(friends), model, expectedMessage, expectedModel);
        assertTrue(model.getAddressBook().getPersonList().contains(ALICE));
        assertTrue(model.getAddressBook().getPersonList().contains(DANIEL));
    }

    @Override
    protected UntagAllCommand createCommand(Tag tag) {
        return new UntagAllCommand(tag);
    }

    @Override
    protected BulkTagCommand createOtherCommand(Tag tag) {
        return new TagAllCommand(tag);
    }

    @Override
    protected String getCommandWord() {
        return UntagAllCommand.COMMAND_WORD;
    }
}

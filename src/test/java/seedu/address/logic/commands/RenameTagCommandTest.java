package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TagTestUtil.createTypicalModel;
import static seedu.address.testutil.TagTestUtil.renameTagForAll;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class RenameTagCommandTest {
    private final Tag friends = new Tag("friends");
    private final Tag buddies = new Tag("buddies");
    private final Tag owesMoney = new Tag("owesMoney");

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RenameTagCommand(null, buddies));
        assertThrows(NullPointerException.class, () -> new RenameTagCommand(friends, null));
    }

    @Test
    public void isReadOnly_dataChangingCommand_returnsFalse() {
        assertFalse(new RenameTagCommand(friends, buddies).isReadOnly());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RenameTagCommand(friends, buddies).execute(null));
    }

    @Test
    public void execute_tagUsedByMembers_renamesTagAndKeepsOtherTags() {
        Model model = createTypicalModel();
        Model expectedModel = createTypicalModel();
        renameTagForAll(expectedModel, "friends", "buddies");
        String expectedMessage = "Renamed tag \"friends\" to \"buddies\" for 3 member(s).";

        assertCommandSuccess(new RenameTagCommand(friends, buddies), model, expectedMessage, expectedModel);
        Person renamedBenson = new PersonBuilder(BENSON).withTags("owesMoney", "buddies").build();
        assertTrue(model.getAddressBook().getPersonList().contains(renamedBenson));
    }

    @Test
    public void execute_memberAlreadyHasNewTag_keepsOneCopyAndReportsMerge() {
        Model model = createTypicalModel();
        Model expectedModel = createTypicalModel();
        renameTagForAll(expectedModel, "owesMoney", "friends");
        String expectedMessage = "Renamed tag \"owesMoney\" to \"friends\" for 1 member(s). "
                + "1 already had \"friends\".";

        assertCommandSuccess(new RenameTagCommand(owesMoney, friends), model, expectedMessage, expectedModel);
        Person mergedBenson = new PersonBuilder(BENSON).withTags("friends").build();
        assertTrue(model.getAddressBook().getPersonList().contains(mergedBenson));
    }

    @Test
    public void execute_memberHasNewTagInOtherCase_usesTypedSpelling() throws Exception {
        Model model = createTypicalModel();
        model.setPerson(BENSON, new PersonBuilder(BENSON).withTags("owesMoney", "FRIENDS").build());

        new RenameTagCommand(owesMoney, friends).execute(model);

        Person renamedBenson = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.isSamePerson(BENSON)).findFirst().orElseThrow();
        assertEquals(List.of("friends"), renamedBenson.getTags().stream().map(tag -> tag.tagName).toList());
    }

    @Test
    public void execute_membersHiddenBySearch_renamesOnlyDisplayedMembers() {
        Model model = createTypicalModel();
        model.updateFilteredPersonList(person -> person.isSamePerson(ALICE));
        Model expectedModel = createTypicalModel();
        expectedModel.updateFilteredPersonList(person -> person.isSamePerson(ALICE));
        expectedModel.setPerson(ALICE, new PersonBuilder(ALICE).withTags("buddies").build());
        String expectedMessage = "Renamed tag \"friends\" to \"buddies\" for 1 member(s).";

        assertCommandSuccess(new RenameTagCommand(friends, buddies), model, expectedMessage, expectedModel);
        assertTrue(model.getAddressBook().getPersonList().contains(BENSON));
        // The search stays applied.
        assertEquals(1, model.getFilteredPersonList().size());
    }

    @Test
    public void execute_tagOnlyOnHiddenMembers_throwsCommandException() {
        Model model = createTypicalModel();
        model.updateFilteredPersonList(person -> person.isSamePerson(CARL));

        assertCommandFailure(new RenameTagCommand(friends, buddies), model,
                String.format(RenameTagCommand.MESSAGE_TAG_NOT_FOUND, "friends"));
    }

    @Test
    public void execute_sameTag_throwsCommandExceptionAndPreservesFilteredRoster() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.filterFilteredPersonListByTag(friends);

        assertCommandFailure(new RenameTagCommand(friends, new Tag("friends")), model,
                RenameTagCommand.MESSAGE_SAME_TAG);
        assertEquals(List.of(friends), model.getActiveTagFilters());
    }

    @Test
    public void execute_caseOnlyRename_standardizesSpellingWithoutFalseMergeCount() throws Exception {
        Model model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alice").withTags("committee").build());
        model.addPerson(new PersonBuilder().withName("Bob").withTags("Committee").build());
        model.filterFilteredPersonListByTag(new Tag("COMMITTEE"));

        String feedback = new RenameTagCommand(new Tag("COMMITTEE"), new Tag("Committee"))
                .execute(model).getFeedbackToUser();

        assertEquals("Renamed tag \"COMMITTEE\" to \"Committee\" for 1 member(s).", feedback);
        assertEquals(2, model.getFilteredPersonList().size());
        assertTrue(model.getAddressBook().getPersonList().stream().allMatch(person ->
                person.getTags().stream().anyMatch(tag -> tag.tagName.equals("Committee"))));
    }

    @Test
    public void execute_caseOnlyRenameAlreadySpelled_reportsNoChanges() throws Exception {
        Model model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alice").withTags("Committee").build());
        model.addPerson(new PersonBuilder().withName("Bob").withTags("Committee").build());
        AddressBook before = new AddressBook(model.getAddressBook());

        String feedback = new RenameTagCommand(new Tag("committee"), new Tag("Committee"))
                .execute(model).getFeedbackToUser();

        assertEquals("Every displayed member with this tag already uses \"Committee\". No changes were made.",
                feedback);
        assertEquals(before, model.getAddressBook());
    }

    @Test
    public void execute_renamingActiveFilterTag_keepsFilterAndHidesRenamedMembers() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.filterFilteredPersonListByTag(friends);
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.filterFilteredPersonListByTag(friends);
        renameTagForAll(expectedModel, "friends", "buddies");

        assertCommandSuccess(new RenameTagCommand(friends, buddies), model,
                "Renamed tag \"friends\" to \"buddies\" for 3 member(s).\n"
                + "0 member(s) listed with tag \"friends\". Run list to show everyone.", expectedModel);
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(friends), model.getActiveTagFilters());
    }

    @Test
    public void execute_tagNotUsed_throwsCommandException() {
        Model model = createTypicalModel();

        assertCommandFailure(new RenameTagCommand(new Tag("committee"), buddies), model,
                String.format(RenameTagCommand.MESSAGE_TAG_NOT_FOUND, "committee"));
    }

    @Test
    public void execute_tagDiffersOnlyInCase_renamesMatchingMembers() {
        Model model = createTypicalModel();
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        renameTagForAll(expectedModel, "friends", "buddies");

        assertCommandSuccess(new RenameTagCommand(new Tag("Friends"), buddies), model,
                "Renamed tag \"Friends\" to \"buddies\" for 3 member(s).", expectedModel);
    }

    @Test
    public void equals_sameAndDifferentTags_returnsCorrectResult() {
        RenameTagCommand command = new RenameTagCommand(friends, buddies);

        assertTrue(command.equals(command));
        assertTrue(command.equals(new RenameTagCommand(new Tag("friends"), new Tag("buddies"))));
        assertEquals(command.hashCode(), new RenameTagCommand(new Tag("friends"), new Tag("buddies")).hashCode());
        assertFalse(command.equals(new RenameTagCommand(buddies, friends)));
        assertFalse(command.equals(new RenameTagCommand(friends, owesMoney)));
        assertFalse(command.equals(new RenameTagCommand(new Tag("Friends"), buddies)));
        assertFalse(command.equals(new RenameTagCommand(friends, new Tag("Buddies"))));
        assertFalse(command.equals(new TagAllCommand(friends)));
        assertFalse(command.equals(null));
    }

    @Test
    public void toString_validCommand_containsBothTags() {
        RenameTagCommand command = new RenameTagCommand(friends, buddies);
        String expected = RenameTagCommand.class.getCanonicalName() + "{oldTag=" + friends + ", newTag=" + buddies
                + "}";
        assertEquals(expected, command.toString());
    }
}

package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

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
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        renameTagForAll(expectedModel, "friends", "buddies");
        String expectedMessage = "Renamed tag \"friends\" to \"buddies\" for 3 member(s).";

        assertCommandSuccess(new RenameTagCommand(friends, buddies), model, expectedMessage, expectedModel);
        Person renamedBenson = new PersonBuilder(BENSON).withTags("owesMoney", "buddies").build();
        assertTrue(model.getAddressBook().getPersonList().contains(renamedBenson));
    }

    @Test
    public void execute_memberAlreadyHasNewTag_keepsOneCopyAndReportsMerge() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        renameTagForAll(expectedModel, "owesMoney", "friends");
        String expectedMessage = "Renamed tag \"owesMoney\" to \"friends\" for 1 member(s). "
                + "1 already had \"friends\".";

        assertCommandSuccess(new RenameTagCommand(owesMoney, friends), model, expectedMessage, expectedModel);
        Person mergedBenson = new PersonBuilder(BENSON).withTags("friends").build();
        assertTrue(model.getAddressBook().getPersonList().contains(mergedBenson));
    }

    @Test
    public void execute_membersHiddenBySearch_renamesWholeRoster() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> person.isSamePerson(ALICE));
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(person -> person.isSamePerson(ALICE));
        renameTagForAll(expectedModel, "friends", "buddies");
        String expectedMessage = "Renamed tag \"friends\" to \"buddies\" for 3 member(s).";

        assertCommandSuccess(new RenameTagCommand(friends, buddies), model, expectedMessage, expectedModel);
        assertTrue(model.getAddressBook().getPersonList().stream()
                .noneMatch(person -> person.getTags().contains(friends)));
        // The search stays applied.
        assertEquals(1, model.getFilteredPersonList().size());
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
    public void execute_renamingActiveFilterTag_keepsFilterAndHidesRenamedMembers() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.filterFilteredPersonListByTag(friends);
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.filterFilteredPersonListByTag(friends);
        renameTagForAll(expectedModel, "friends", "buddies");

        assertCommandSuccess(new RenameTagCommand(friends, buddies), model,
                "Renamed tag \"friends\" to \"buddies\" for 3 member(s).", expectedModel);
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(friends), model.getActiveTagFilters());
    }

    @Test
    public void execute_tagNotUsed_throwsCommandException() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

        assertCommandFailure(new RenameTagCommand(new Tag("committee"), buddies), model,
                String.format(RenameTagCommand.MESSAGE_TAG_NOT_FOUND, "committee"));
    }

    @Test
    public void execute_tagDiffersOnlyInCase_throwsCommandException() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

        assertCommandFailure(new RenameTagCommand(new Tag("Friends"), buddies), model,
                String.format(RenameTagCommand.MESSAGE_TAG_NOT_FOUND, "Friends"));
    }

    @Test
    public void equals_sameAndDifferentTags_returnsCorrectResult() {
        RenameTagCommand command = new RenameTagCommand(friends, buddies);

        assertTrue(command.equals(command));
        assertTrue(command.equals(new RenameTagCommand(new Tag("friends"), new Tag("buddies"))));
        assertEquals(command.hashCode(), new RenameTagCommand(new Tag("friends"), new Tag("buddies")).hashCode());
        assertFalse(command.equals(new RenameTagCommand(buddies, friends)));
        assertFalse(command.equals(new RenameTagCommand(friends, owesMoney)));
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

    /**
     * Replaces the tag named {@code oldName} with {@code newName} for every member stored in {@code model}.
     */
    private static void renameTagForAll(Model model, String oldName, String newName) {
        List<Person> storedPersons = new ArrayList<>(model.getAddressBook().getPersonList());
        for (Person person : storedPersons) {
            List<String> tagNames = new ArrayList<>(person.getTags().stream()
                    .map(tag -> tag.tagName)
                    .toList());
            if (tagNames.remove(oldName)) {
                if (!tagNames.contains(newName)) {
                    tagNames.add(newName);
                }
                Person renamedPerson = new PersonBuilder(person)
                        .withTags(tagNames.toArray(new String[0]))
                        .build();
                model.setPerson(person, renamedPerson);
            }
        }
    }
}

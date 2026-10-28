package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.GuiSettings;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonSelectionPredicate;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.AddressBookBuilder;
import seedu.address.testutil.PersonBuilder;

public class ModelManagerTest {

    private ModelManager modelManager = new ModelManager();

    @Test
    public void hashCode_equalModelsWithDifferentFilterImplementations_retrieveTheSameMapEntry() {
        AddressBook roster = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        ModelManager first = new ModelManager(roster, new UserPrefs());
        ModelManager second = new ModelManager(roster, new UserPrefs());
        first.filterFilteredPersonListByName(new NameContainsKeywordsPredicate(List.of("Alice")));
        second.updateFilteredPersonList(person -> person.equals(ALICE));
        assertEquals(first, second);
        Map<ModelManager, String> models = new HashMap<>();
        models.put(first, "filtered");
        assertEquals("filtered", models.get(second));
        assertEquals("filtered", models.put(second, "replaced"));
        assertEquals(1, models.size());
    }

    @Test
    public void hashCode_changedBeforeInsertion_usesCurrentModelState() {
        ModelManager changed = new ModelManager();
        changed.hashCode();
        changed.addPerson(ALICE);
        changed.setGuiSettings(new GuiSettings(900, 600, 30, 40));
        ModelManager equal = new ModelManager(changed.getAddressBook(), changed.getUserPrefs());
        Map<ModelManager, String> models = new HashMap<>();
        models.put(changed, "updated");
        assertEquals("updated", models.get(equal));
    }

    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new AddressBook(), new AddressBook(modelManager.getAddressBook()));
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new AddressBook(), userPrefs);
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // Modifying userPrefs should not modify modelManager's userPrefs
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null));
    }

    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4);
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasPerson(null));
    }

    @Test
    public void hasPerson_personNotInAddressBook_returnsFalse() {
        assertFalse(modelManager.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personInAddressBook_returnsTrue() {
        modelManager.addPerson(ALICE);
        assertTrue(modelManager.hasPerson(ALICE));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredPersonList().remove(0));
    }

    @Test
    public void filterFilteredPersonList_narrowsExistingView() {
        modelManager.addPerson(ALICE);
        modelManager.addPerson(BENSON);
        modelManager.addPerson(CARL);
        modelManager.updateFilteredPersonList(person -> !person.equals(CARL));

        modelManager.filterFilteredPersonList(person -> person.getTags().contains(new Tag("owesMoney")));
        assertEquals(List.of(BENSON), modelManager.getFilteredPersonList());

        modelManager.filterFilteredPersonList(person -> person.getTags().contains(new Tag("friends")));
        assertEquals(List.of(BENSON), modelManager.getFilteredPersonList());

        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        assertEquals(List.of(ALICE, BENSON, CARL), modelManager.getFilteredPersonList());
    }

    @Test
    public void filterFilteredPersonList_noExistingPredicate_appliesFilter() {
        AddressBook addressBook = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        modelManager = new ModelManager(addressBook, new UserPrefs());

        modelManager.filterFilteredPersonList(person -> person.equals(BENSON));

        assertEquals(List.of(BENSON), modelManager.getFilteredPersonList());
    }

    @Test
    public void filterFilteredPersonList_manyFilters_doesNotOverflowStack() {
        for (int i = 0; i < 12_000; i++) {
            modelManager.filterFilteredPersonList(person -> true);
        }

        modelManager.setAddressBook(new AddressBookBuilder().withPerson(ALICE).build());

        assertEquals(List.of(ALICE), modelManager.getFilteredPersonList());
    }

    @Test
    public void filterFilteredPersonList_nullPredicate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.filterFilteredPersonList(null));
    }

    @Test
    public void setPerson_duplicateReplacement_keepsSelectedMemberAndRosterUnchanged() {
        AddressBook original = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        modelManager = new ModelManager(original, new UserPrefs());
        modelManager.filterFilteredPersonList(new PersonSelectionPredicate(List.of(ALICE)));

        assertThrows(DuplicatePersonException.class, () -> modelManager.setPerson(ALICE, BENSON));

        assertEquals(List.of(ALICE), modelManager.getFilteredPersonList());
        assertEquals(original, modelManager.getAddressBook());
        modelManager.setPerson(ALICE, new PersonBuilder(ALICE).withPhone("90000001").build());
        assertEquals(List.of(new PersonBuilder(ALICE).withPhone("90000001").build()),
                modelManager.getFilteredPersonList());
    }

    @Test
    public void removeActiveFilters_preservesOtherFiltersAndRestoresMatchingMembers() {
        AddressBook addressBook = new AddressBookBuilder()
                .withPerson(ALICE).withPerson(BENSON).withPerson(CARL).build();
        modelManager = new ModelManager(addressBook, new UserPrefs());
        NameContainsKeywordsPredicate nameFilter = new NameContainsKeywordsPredicate(List.of("Alice", "Benson"));
        Tag friends = new Tag("friends");
        Tag owesMoney = new Tag("owesMoney");

        modelManager.updateFilteredPersonList(nameFilter);
        modelManager.filterFilteredPersonListByTag(friends);
        modelManager.filterFilteredPersonListByTag(owesMoney);
        assertEquals(List.of("Name: Alice Benson", "Tag: friends", "Tag: owesMoney"),
                modelManager.getActiveFilters().stream().map(ActiveFilter::getLabel).toList());
        assertEquals(List.of(friends, owesMoney), modelManager.getActiveTagFilters());

        modelManager.removeActiveFilter(modelManager.getActiveFilters().get(1));
        assertEquals(List.of(owesMoney), modelManager.getActiveTagFilters());
        assertEquals(List.of(BENSON), modelManager.getFilteredPersonList());

        modelManager.removeActiveFilter(modelManager.getActiveFilters().getFirst());
        assertEquals(List.of("Tag: owesMoney"),
                modelManager.getActiveFilters().stream().map(ActiveFilter::getLabel).toList());
        assertEquals(List.of(BENSON), modelManager.getFilteredPersonList());

        modelManager.removeActiveFilter(modelManager.getActiveFilters().getFirst());
        assertEquals(List.of(ALICE, BENSON, CARL), modelManager.getFilteredPersonList());
    }

    @Test
    public void showFilter_labelTracksEditedAndDeletedSelectedMembers() {
        AddressBook addressBook = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        modelManager = new ModelManager(addressBook, new UserPrefs());
        modelManager.filterFilteredPersonList(new PersonSelectionPredicate(List.of(ALICE, BENSON)));
        ActiveFilter showFilter = modelManager.getActiveFilters().getFirst();
        assertEquals("Show: 2 selected", showFilter.getLabel());

        Person renamedAlice = new PersonBuilder(ALICE).withName("Alice Renamed").build();
        modelManager.setPerson(ALICE, renamedAlice);
        assertEquals("Alice Renamed, Benson Meier", showFilter.getShowSelectionNames());

        modelManager.deletePerson(BENSON);
        assertEquals("Show: 1 selected", showFilter.getLabel());
        assertEquals("Alice Renamed", showFilter.getShowSelectionNames());

        modelManager.deletePerson(renamedAlice);
        assertEquals(List.of(showFilter), modelManager.getActiveFilters());
        assertEquals("Show: 0 selected", showFilter.getLabel());
        assertEquals("No selected members remain", showFilter.getShowSelectionNames());
        assertEquals(List.of(), modelManager.getFilteredPersonList());
    }

    @Test
    public void duplicateShowSelection_doesNotAddAnotherFilter() {
        AddressBook addressBook = new AddressBookBuilder()
                .withPerson(ALICE).withPerson(BENSON).withPerson(CARL).build();
        modelManager = new ModelManager(addressBook, new UserPrefs());
        modelManager.filterFilteredPersonList(new PersonSelectionPredicate(List.of(ALICE, BENSON)));

        modelManager.filterFilteredPersonList(new PersonSelectionPredicate(List.of(BENSON, ALICE)));
        assertEquals(1, modelManager.getActiveFilters().size());
        assertEquals(List.of(ALICE, BENSON), modelManager.getFilteredPersonList());

        modelManager.filterFilteredPersonList(new PersonSelectionPredicate(List.of(ALICE)));
        assertEquals(2, modelManager.getActiveFilters().size());
        assertEquals(List.of(ALICE), modelManager.getFilteredPersonList());
    }

    @Test
    public void updateFilteredPersonList_clearsDisplayedFilters() {
        modelManager.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of("Alice")));
        modelManager.filterFilteredPersonListByTag(new Tag("friends"));

        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        assertEquals(List.of(), modelManager.getActiveFilters());
    }

    @Test
    public void multipleNameFilters_narrowResultsAndCanBeRemovedIndividually() {
        AddressBook addressBook = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        modelManager = new ModelManager(addressBook, new UserPrefs());

        modelManager.filterFilteredPersonListByName(new NameContainsKeywordsPredicate(List.of("Alice", "Benson")));
        modelManager.filterFilteredPersonListByTag(new Tag("friends"));
        modelManager.filterFilteredPersonListByName(new NameContainsKeywordsPredicate(List.of("Meier")));

        assertEquals(List.of("Name: Alice Benson", "Tag: friends", "Name: Meier"),
                modelManager.getActiveFilters().stream().map(ActiveFilter::getLabel).toList());
        assertEquals(List.of(BENSON), modelManager.getFilteredPersonList());

        modelManager.removeActiveFilter(modelManager.getActiveFilters().getLast());
        assertEquals(List.of(ALICE, BENSON), modelManager.getFilteredPersonList());
    }

    @Test
    public void setNameSorting_extraSpaces_ignoresSpacingWithoutChangingRecords() {
        Person later = new PersonBuilder(ALICE).withName("Alice  Zoe ").build();
        Person earlier = new PersonBuilder(BENSON).withName("alice Bob").build();
        modelManager.addPerson(later);
        modelManager.addPerson(earlier);

        modelManager.setNameSorting(true);

        assertEquals(List.of(earlier, later), modelManager.getFilteredPersonList());
        assertEquals("Alice  Zoe ", later.getName().fullName);
        assertEquals(List.of(later, earlier), modelManager.getAddressBook().getPersonList());
        modelManager.setNameSorting(false);
        assertEquals(List.of(later, earlier), modelManager.getFilteredPersonList());
    }

    @Test
    public void equals() {
        AddressBook addressBook = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        AddressBook differentAddressBook = new AddressBook();
        UserPrefs userPrefs = new UserPrefs();

        // same values -> returns true
        modelManager = new ModelManager(addressBook, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(addressBook, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // same object -> returns true
        assertTrue(modelManager.equals(modelManager));

        // null -> returns false
        assertFalse(modelManager.equals(null));

        // different types -> returns false
        assertFalse(modelManager.equals(5));

        // different addressBook -> returns false
        assertFalse(modelManager.equals(new ModelManager(differentAddressBook, userPrefs)));

        // different filteredList -> returns false
        String[] keywords = ALICE.getName().fullName.split("\\s+");
        modelManager.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(addressBook, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(addressBook, differentUserPrefs)));
    }
}

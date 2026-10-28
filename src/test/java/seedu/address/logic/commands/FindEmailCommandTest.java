package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
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
import seedu.address.model.person.EmailContainsKeywordsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests cumulative email searches, feedback, and preservation of stored records and display sorting.
 */
public class FindEmailCommandTest {
    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
    private final Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void constructor_nullPredicate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FindEmailCommand(null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FindEmailCommand(predicate("alice")).execute(null));
    }

    @Test
    public void isReadOnly_emailSearch_returnsTrue() {
        assertTrue(new FindEmailCommand(predicate("alice")).isReadOnly());
    }

    @Test
    public void execute_noMatchingEmail_showsEmptyList() {
        EmailContainsKeywordsPredicate predicate = predicate("missing@example.org");
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(new FindEmailCommand(predicate), model, "0 members listed.", expectedModel);
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_oneMatchingEmail_usesSingularFeedback() {
        EmailContainsKeywordsPredicate predicate = predicate("ALICE@");
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(new FindEmailCommand(predicate), model, "1 member listed.", expectedModel);
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
    }

    @Test
    public void execute_multipleKeywords_usesOrMatchingAndPluralFeedback() {
        EmailContainsKeywordsPredicate predicate = predicate("john", "heinz");
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(new FindEmailCommand(predicate), model, "2 members listed.", expectedModel);
        assertEquals(List.of(BENSON, CARL), model.getFilteredPersonList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
    }

    @Test
    public void execute_existingNameAndTagFilters_preservesConditionsAndDoesNotRestoreHiddenMembers() {
        model.filterFilteredPersonListByName(new NameContainsKeywordsPredicate(List.of("Alice")));
        model.filterFilteredPersonListByTag(new Tag("friends"));
        assertEquals(List.of(ALICE), model.getFilteredPersonList());

        CommandResult result = new FindEmailCommand(predicate("heinz")).execute(model);

        assertEquals("0 members listed.", result.getFeedbackToUser());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(new Tag("friends")), model.getActiveTagFilters());
        assertEquals(List.of("Name: Alice", "Tag: friends", "Email: heinz"),
                model.getActiveFilters().stream().map(filter -> filter.getLabel()).toList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
    }

    @Test
    public void execute_committeeThenRepeatedEmailSearches_narrowsAndRemovesOnlyChosenCondition() {
        Person david = new PersonBuilder().withName("David Li").withEmail("lidavid@example.com")
                .withTags("Committee", "family").build();
        Person irfan = new PersonBuilder().withName("Irfan Ibrahim").withEmail("irfan@school.edu")
                .withTags("classmates", "committee").build();
        Person zara = new PersonBuilder().withName("Zara Ali").withEmail("zara@example.com")
                .withTags("committee", "year-1").build();
        Person alex = new PersonBuilder().withName("Alex Yeoh").withEmail("alexyeoh@example.com")
                .withTags("friends").build();
        AddressBook roster = new AddressBook();
        roster.setPersons(List.of(david, irfan, zara, alex));
        model.setAddressBook(roster);
        model.filterFilteredPersonListByTag(new Tag("committee"));

        new FindEmailCommand(predicate("EXAMPLE")).execute(model);
        assertEquals(List.of(david, zara), model.getFilteredPersonList());
        new FindEmailCommand(predicate("zara")).execute(model);
        assertEquals(List.of(zara), model.getFilteredPersonList());
        assertEquals(List.of("Tag: committee", "Email: EXAMPLE", "Email: zara"),
                model.getActiveFilters().stream().map(filter -> filter.getLabel()).toList());

        model.removeActiveFilter(model.getActiveFilters().getLast());
        assertEquals(List.of(david, zara), model.getFilteredPersonList());
        model.removeActiveFilter(model.getActiveFilters().getFirst());
        assertEquals(List.of(david, zara, alex), model.getFilteredPersonList());
        new ListCommand().execute(model);
        assertTrue(model.getActiveFilters().isEmpty());
        assertEquals(List.of(david, irfan, zara, alex), model.getFilteredPersonList());
        assertEquals(roster, model.getAddressBook());
    }

    @Test
    public void execute_nameTagAndEmailInAnyOrder_returnsSameIntersection() throws Exception {
        List<Command> commands = List.of(
                new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Benson"))),
                new FilterCommand(new Tag("friends")), new FindEmailCommand(predicate("john", "heinz")));
        for (List<Integer> order : List.of(List.of(0, 1, 2), List.of(0, 2, 1), List.of(1, 0, 2),
                List.of(1, 2, 0), List.of(2, 0, 1), List.of(2, 1, 0))) {
            Model current = new ModelManager(getTypicalAddressBook(), new UserPrefs());
            for (int index : order) {
                commands.get(index).execute(current);
            }
            assertEquals(List.of(BENSON), current.getFilteredPersonList(), order.toString());
            assertEquals(3, current.getActiveFilters().size());
        }
    }

    @Test
    public void execute_identicalEmailSearches_removingOneRetainsTheOther() {
        new FindEmailCommand(predicate("alice")).execute(model);
        new FindEmailCommand(predicate("alice")).execute(model);
        assertEquals(2, model.getActiveFilters().size());

        model.removeActiveFilter(model.getActiveFilters().getFirst());
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
        assertEquals(1, model.getActiveFilters().size());
        model.removeActiveFilter(model.getActiveFilters().getFirst());
        assertEquals(getTypicalAddressBook().getPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void execute_activeSorting_preservesAlphabeticalDisplayAndStoredOrder() {
        Person zara = new PersonBuilder().withName("Zara Tan").withEmail("zara@example.com").build();
        Person bob = new PersonBuilder().withName("Bob Tan").withEmail("bob@example.com").build();
        Person alice = new PersonBuilder().withName("Alice Tan").withEmail("alice@example.com").build();
        AddressBook roster = new AddressBook();
        roster.setPersons(List.of(zara, bob, alice));
        model.setAddressBook(roster);
        model.setNameSorting(true);

        CommandResult result = new FindEmailCommand(predicate("@example.com")).execute(model);

        assertEquals("3 members listed.", result.getFeedbackToUser());
        assertEquals(List.of(alice, bob, zara), model.getFilteredPersonList());
        assertEquals(List.of(zara, bob, alice), model.getAddressBook().getPersonList());
    }

    @Test
    public void equals_sameAndDifferentPredicates_returnsCorrectResult() {
        FindEmailCommand command = new FindEmailCommand(predicate("alice"));
        FindEmailCommand copy = new FindEmailCommand(predicate("alice"));
        assertEquals(command, command);
        assertEquals(command, copy);
        assertEquals(command.hashCode(), copy.hashCode());
        assertFalse(command.equals(new FindEmailCommand(predicate("bob"))));
        assertFalse(command.equals(new FindCommand(new NameContainsKeywordsPredicate(List.of("alice")))));
        assertFalse(command.equals(null));
    }

    /**
     * Creates the email predicate used by a command under test.
     */
    private EmailContainsKeywordsPredicate predicate(String... keywords) {
        return new EmailContainsKeywordsPredicate(List.of(keywords));
    }
}

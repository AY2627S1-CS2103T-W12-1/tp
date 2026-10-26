package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.AddressBookParser;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/** Verifies stable displayed indexes when sorted members compare equal by normalized name. */
public class SortOrderIntegrationTest {
    private final AddressBookParser parser = new AddressBookParser();
    private ModelManager model;

    @Test
    public void execute_contactEditAmongEqualNames_preservesStoredTieOrder() throws Exception {
        startWithNames("Alice", "Alice", "Alice", "Alice");
        execute("sort");

        execute("edit 1 p/999");

        assertDisplayOrder(0, 1, 2, 3);
        assertStoredOrder(0, 1, 2, 3);
    }

    @Test
    public void execute_individualTagChangesAmongEqualNames_preservesStoredTieOrder() throws Exception {
        startWithNames("Alice", "Alice", "Alice", "Alice");
        execute("sort");

        execute("tag 1 t/new");
        assertDisplayOrder(0, 1, 2, 3);
        execute("untag 1 t/old");
        assertDisplayOrder(0, 1, 2, 3);
        assertStoredOrder(0, 1, 2, 3);
    }

    @Test
    public void execute_bulkTagChangesAmongEqualNames_preservesStoredTieOrder() throws Exception {
        startWithNames("Alice", "Alice");
        execute("sort");

        execute("tagall t/new");
        assertDisplayOrder(0, 1);
        execute("untagall t/old");
        assertDisplayOrder(0, 1);
        assertStoredOrder(0, 1);
    }

    @Test
    public void execute_renameTagAmongEqualNames_preservesStoredTieOrder() throws Exception {
        startWithNames("Alice", "Alice");
        execute("sort");

        execute("renametag t/old t/new");

        assertDisplayOrder(0, 1);
        assertStoredOrder(0, 1);
    }

    @Test
    public void execute_normalizedEqualNames_preservesCaseSpacingAndStoredTieOrder() throws Exception {
        List<String> names = List.of("Alice Tan", "aLiCe  tAn", "ALICE TAN ", "alice tan");
        startWithNames(names.toArray(String[]::new));
        execute("sort");
        assertDisplayOrder(0, 1, 2, 3);

        execute("edit 1 a/New address");

        assertDisplayOrder(0, 1, 2, 3);
        assertEquals(names, model.getAddressBook().getPersonList().stream()
                .map(person -> person.getName().fullName).toList());
    }

    @Test
    public void execute_filteredShowSelection_preservesTieOrderDuringReplacementAndRestoration() throws Exception {
        startWithNames("Zoe", "Alice", "Alice", "Alice", "Alice", "Bob");
        execute("sort");
        execute("find Alice");
        execute("show 1 2 4");
        assertDisplayOrder(1, 2, 4);

        execute("tagall t/new");
        assertDisplayOrder(1, 2, 4);
        assertEquals(1, model.getActiveShowSelectionCount());
        assertEquals(1, model.getActiveFilters().size());
        model.clearShowSelections();
        assertDisplayOrder(1, 2, 3, 4);
        model.removeActiveFilter(model.getActiveFilters().getFirst());
        assertDisplayOrder(1, 2, 3, 4, 5, 0);
        assertStoredOrder(0, 1, 2, 3, 4, 5);
    }

    @Test
    public void execute_addIntoEqualNameGroup_placesNewMemberAfterStoredPeers() throws Exception {
        startWithNames("Bob", "Alice", "Alice", "Charlie");
        execute("sort");

        model.addPerson(member("aLiCe", 4));

        assertDisplayOrder(1, 2, 4, 0, 3);
        assertStoredOrder(0, 1, 2, 3, 4);
    }

    @Test
    public void execute_renameIntoEqualNameGroup_usesStoredPositionInsteadOfPreviousSortedPosition() throws Exception {
        startWithNames("Bob", "Alice", "Alice", "Zoe");
        execute("sort");

        execute("edit 3 n/ALICE");
        assertDisplayOrder(0, 1, 2, 3);
        execute("edit 4 n/alice");
        assertDisplayOrder(0, 1, 2, 3);
        execute("list");
        assertDisplayOrder(0, 1, 2, 3);
        execute("sort");
        assertDisplayOrder(0, 1, 2, 3);
    }

    @Test
    public void execute_replaceRosterWhileSorting_preservesReplacementRosterTieOrder() throws Exception {
        startWithNames("Alice", "Alice", "Alice", "Alice");
        execute("sort");
        AddressBook replacement = new AddressBook();
        replacement.setPersons(List.of(member("Alice", 3), member("Alice", 0), member("Alice", 2)));

        model.setAddressBook(replacement);
        execute("tag 1 t/new");

        assertDisplayOrder(3, 0, 2);
        assertStoredOrder(3, 0, 2);
    }

    private void startWithNames(String... names) {
        AddressBook roster = new AddressBook();
        for (int index = 0; index < names.length; index++) {
            roster.addPerson(member(names[index], index));
        }
        model = new ModelManager(roster, new UserPrefs());
    }

    /** Distinct emails identify tied members without depending on mutable phone, address or tag values. */
    private Person member(String name, int identifier) {
        return new PersonBuilder().withName(name).withEmail("member" + identifier + "@example.com")
                .withTags("old").build();
    }

    private void execute(String command) throws Exception {
        parser.parseCommand(command).execute(model);
    }

    private void assertDisplayOrder(Integer... identifiers) {
        assertEquals(List.of(identifiers).stream().map(id -> "member" + id + "@example.com").toList(),
                model.getFilteredPersonList().stream().map(person -> person.getEmail().value).toList());
    }

    private void assertStoredOrder(Integer... identifiers) {
        assertEquals(List.of(identifiers).stream().map(id -> "member" + id + "@example.com").toList(),
                model.getAddressBook().getPersonList().stream().map(person -> person.getEmail().value).toList());
    }
}

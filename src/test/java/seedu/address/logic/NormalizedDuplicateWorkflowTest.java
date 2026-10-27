package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks normalized duplicate input through commands, persistence and CSV import. */
public class NormalizedDuplicateWorkflowTest {
    @TempDir
    public Path directory;

    private Path roster;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() {
        roster = directory.resolve("roster.json");
        model = new ModelManager();
        logic = logicFor(model);
    }

    @Test
    public void add_nameVariants_rejectedWithoutChangingRosterViewOrFile() throws Exception {
        logic.execute("add n/Alice Tan p/123 e/alice@example.com a/Home");
        Person original = model.getAddressBook().getPersonList().getFirst();
        logic.execute("find Alice");
        var filters = model.getActiveFilters();
        byte[] saved = Files.readAllBytes(roster);

        for (String name : List.of("alice tan", "Alice  Tan", "ALICE   TAN")) {
            CommandException error = assertThrows(CommandException.class, () ->
                    logic.execute("add n/" + name + " p/123 e/alice@example.com a/Home t/new"));

            assertEquals(AddCommand.MESSAGE_DUPLICATE_PERSON, error.getMessage());
            assertTrue(error.getMessage().contains("name case and repeated spaces"));
            assertEquals(List.of(original), model.getAddressBook().getPersonList());
            assertEquals(List.of(original), model.getFilteredPersonList());
            assertEquals(filters, model.getActiveFilters());
            assertArrayEquals(saved, Files.readAllBytes(roster));
        }
    }

    @Test
    public void add_sameNormalizedNameWithDifferentContactFields_allowedAndRetainsFormatting() throws Exception {
        logic.execute("add n/Alice Tan p/123 e/alice@example.com a/Home");
        logic.execute("add n/ALICE  TAN p/456 e/alice@example.com a/Home");
        logic.execute("add n/alice tan p/123 e/another@example.com a/Home");
        logic.execute("add n/Alice   Tan p/123 e/alice@example.com a/Elsewhere");

        assertEquals(List.of("Alice Tan", "ALICE  TAN", "alice tan", "Alice   Tan"),
                model.getAddressBook().getPersonList().stream().map(person -> person.getName().fullName).toList());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow());
    }

    @Test
    public void edit_nameVariantOfAnotherMember_rejectedWithoutSaving() throws Exception {
        logic.execute("add n/Alice Tan p/123 e/alice@example.com a/Home");
        logic.execute("add n/Bob Tan p/123 e/alice@example.com a/Home");
        AddressBook before = new AddressBook(model.getAddressBook());
        byte[] saved = Files.readAllBytes(roster);

        assertThrows(CommandException.class, () -> logic.execute("edit 2 n/ALICE  TAN"));

        assertEquals(before, model.getAddressBook());
        assertArrayEquals(saved, Files.readAllBytes(roster));
    }

    @Test
    public void edit_ownNameFormatting_allowedAndRetainedAfterReload() throws Exception {
        logic.execute("add n/Alice Tan p/123 e/alice@example.com a/Home");

        logic.execute("edit 1 n/ALICE  TAN");

        assertEquals("ALICE  TAN", model.getAddressBook().getPersonList().getFirst().getName().fullName);
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow());
    }

    @Test
    public void import_nameVariants_skipsExistingAndEarlierRowsWithoutMergingTags() throws Exception {
        logic.execute("add n/Alice Tan p/123 e/alice@example.com a/Home t/old");
        Path csv = directory.resolve("member list.csv");
        Files.writeString(csv, "name,phone,email,address,tags\n"
                + "ALICE  TAN,123,alice@example.com,Home,replacement\n"
                + "BoB  Ray,456,bob@example.com,Elsewhere,first\n"
                + "bob ray,456,bob@example.com,Elsewhere,second\n"
                + "alice tan,789,alice@example.com,Home,new\n");

        var result = logic.execute("import p/\"" + csv + "\"");

        assertTrue(result.getFeedbackToUser().startsWith("Imported 2 member(s)"));
        assertTrue(result.getFeedbackToUser().endsWith("Skipped 2 duplicate record(s)."));
        List<Person> people = model.getAddressBook().getPersonList();
        assertEquals(List.of("Alice Tan", "BoB  Ray", "alice tan"),
                people.stream().map(person -> person.getName().fullName).toList());
        assertEquals(List.of("old"), people.get(0).getTags().stream().map(tag -> tag.tagName).toList());
        assertEquals(List.of("first"), people.get(1).getTags().stream().map(tag -> tag.tagName).toList());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow());
    }

    @Test
    public void existingNameVariants_loadAndAllowTagEditsWithoutMergingRecords() throws Exception {
        Person first = new PersonBuilder().withName("Alice Tan").build();
        Person second = new PersonBuilder(first).withName("alice  tan").build();
        AddressBook previous = new AddressBook();
        previous.addPerson(first);
        previous.addPerson(second);
        new JsonAddressBookStorage(roster).saveAddressBook(previous);
        model = new ModelManager(new JsonAddressBookStorage(roster).readAddressBook().orElseThrow(), new UserPrefs());
        logic = logicFor(model);

        logic.execute("edit 2 t/legacy");

        assertEquals(List.of("Alice Tan", "alice  tan"),
                model.getAddressBook().getPersonList().stream().map(person -> person.getName().fullName).toList());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(roster).readAddressBook().orElseThrow());
        Person anotherVariant = new PersonBuilder(first).withName("ALICE TAN").build();
        assertThrows(CommandException.class, () -> new AddCommand(anotherVariant).execute(model));
    }

    @Test
    public void duplicateComparison_usesRootLocaleAndPreservesOtherContactDifferences() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person iris = new PersonBuilder().withName("IRIS TAN").build();
            Person variant = new PersonBuilder(iris).withName("iris  tan").withTags("new").build();

            assertTrue(iris.isDuplicateContactOf(variant));
            assertTrue(variant.isDuplicateContactOf(iris));
            assertFalse(iris.isSamePerson(variant));
            assertFalse(iris.equals(variant));
            assertFalse(iris.isDuplicateContactOf(null));
            assertFalse(iris.isDuplicateContactOf(new PersonBuilder(iris).withPhone("999").build()));
            assertFalse(iris.isDuplicateContactOf(new PersonBuilder(iris).withEmail("other@example.com").build()));
            assertFalse(iris.isDuplicateContactOf(new PersonBuilder(iris).withAddress("Another address").build()));
            assertFalse(iris.isDuplicateContactOf(new PersonBuilder(iris).withName("IRISTAN").build()));
            assertEquals("IRIS TAN", iris.getName().fullName);
            assertEquals("iris  tan", variant.getName().fullName);
        } finally {
            Locale.setDefault(previous);
        }
    }

    private LogicManager logicFor(ModelManager sessionModel) {
        return new LogicManager(sessionModel, new StorageManager(new JsonAddressBookStorage(roster),
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));
    }
}

package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.AddressBookParser;
import seedu.address.model.ActiveFilter;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/** Exercises repeated and unchanged searches through the parser and model for both search commands. */
public class SearchFeedbackTest {
    private final Person andy = new PersonBuilder().withName("Andy Jones").withEmail("andy@example.com").build();
    private final Person bob = new PersonBuilder().withName("Bob Lee").withEmail("bob@school.edu").build();
    private Model model = createModel();

    @Test
    public void execute_repeatedSearch_keepsOriginalChipAndExplainsRepeat() throws Exception {
        for (String command : List.of("find", "findemail")) {
            model = createModel();
            assertEquals("1 member listed.", execute(command + " andy"));
            ActiveFilter original = model.getActiveFilters().getFirst();
            for (String query : List.of("andy", "ANDY", "Andy andy")) {
                assertEquals(alreadyActive(command) + "1 member listed.", execute(command + " " + query));
                assertEquals(List.of(original), model.getActiveFilters());
                assertSame(original, model.getActiveFilters().getFirst());
                assertEquals(List.of(andy), model.getFilteredPersonList());
            }
        }
    }

    @Test
    public void execute_reorderedAlternatives_doesNotDuplicateChip() throws Exception {
        for (String command : List.of("find", "findemail")) {
            model = createModel();
            assertEquals("Results unchanged. 2 members listed.", execute(command + " andy bob"));
            assertEquals(alreadyActive(command) + "2 members listed.", execute(command + " BOB ANDY andy"));
            assertEquals(1, model.getActiveFilters().size());
            assertEquals(List.of(andy, bob), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_differentQuerySameResults_retainsBothAccurateConditions() throws Exception {
        for (String command : List.of("find", "findemail")) {
            model = createModel();
            execute(command + " andy");
            assertEquals("Results unchanged. 1 member listed.", execute(command + " and"));
            String prefix = command.equals("find") ? "Name: " : "Email: ";
            assertEquals(List.of(prefix + "andy", prefix + "and"),
                    model.getActiveFilters().stream().map(ActiveFilter::getLabel).toList());

            Person andrew = new PersonBuilder(andy).withName("Andrew Jones").withEmail("andrew@example.com").build();
            model.setPerson(andy, andrew);
            assertEquals(List.of(), model.getFilteredPersonList());
            model.removeActiveFilter(model.getActiveFilters().getFirst());
            assertEquals(List.of(andrew), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_emptyView_distinguishesNewConditionFromRepeat() throws Exception {
        for (String command : List.of("find", "findemail")) {
            model = createModel();
            assertEquals("0 members listed.", execute(command + " absent"));
            assertEquals("Results unchanged. 0 members listed.", execute(command + " missing"));
            assertEquals(alreadyActive(command) + "0 members listed.", execute(command + " ABSENT"));
            assertEquals(2, model.getActiveFilters().size());
        }
    }

    @Test
    public void execute_removedOrResetSearch_canBeAddedAgain() throws Exception {
        for (String command : List.of("find", "findemail")) {
            model = createModel();
            execute(command + " andy");
            model.removeActiveFilter(model.getActiveFilters().getFirst());
            assertEquals("1 member listed.", execute(command + " ANDY"));
            execute("list");
            assertEquals("1 member listed.", execute(command + " andy"));
            assertEquals(1, model.getActiveFilters().size());
        }
    }

    @Test
    public void execute_sameKeywordsInOtherSearchKind_keepsBothConditions() throws Exception {
        for (String command : List.of("find", "findemail")) {
            model = createModel();
            execute(command + " andy");
            String other = command.equals("find") ? "findemail" : "find";
            assertEquals("Results unchanged. 1 member listed.", execute(other + " andy"));
            assertEquals(2, model.getActiveFilters().size());
        }
    }

    @Test
    public void execute_unicodeNameCaseVariants_usesTheNamesMatchingRules() throws Exception {
        Person member = new PersonBuilder(andy).withName("İpek Σίγμα").build();
        model.setPerson(andy, member);
        execute("find İPEK Σ");

        assertEquals("This name search is already active. 1 member listed.", execute("find ipek ς"));
        assertEquals(1, model.getActiveFilters().size());
        assertEquals(List.of(member), model.getFilteredPersonList());
    }

    /** Creates an isolated synthetic roster with one match for each query. */
    private Model createModel() {
        AddressBook roster = new AddressBook();
        roster.setPersons(List.of(andy, bob));
        return new ModelManager(roster, new UserPrefs());
    }

    /** Runs a typed command through the real parser and returns its feedback. */
    private String execute(String input) throws Exception {
        return new AddressBookParser().parseCommand(input).execute(model).getFeedbackToUser();
    }

    /** Supplies the user-facing repeat message for the command under test. */
    private String alreadyActive(String command) {
        return command.equals("find")
                ? "This name search is already active. " : "This email search is already active. ";
    }
}

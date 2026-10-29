package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.LogCapture;

public class FilterCommandTest {
    private final Tag friends = new Tag("friends");

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCommand(null));
    }

    @Test
    public void isReadOnly_returnsTrue() {
        assertTrue(new FilterCommand(friends).isReadOnly());
    }

    @Test
    public void execute_matchingTag_narrowsDisplayedList() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

        try (LogCapture logs = new LogCapture(FilterCommand.class)) {
            CommandResult result = new FilterCommand(friends).execute(model);

            assertEquals("3 member(s) listed with tag \"friends\".", result.getFeedbackToUser());
            assertEquals(List.of("filter t/friends: 3 member(s) displayed; active tag filters (1): \"friends\"; "
                            + "name searches are not included"),
                    logs.getInfoMessages());
        }
        assertEquals(List.of(ALICE, BENSON, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void execute_absentTag_logsZeroDisplayedMembers() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

        try (LogCapture logs = new LogCapture(FilterCommand.class)) {
            CommandResult result = new FilterCommand(new Tag("missing")).execute(model);

            assertEquals("0 member(s) listed with tag \"missing\".", result.getFeedbackToUser());
            assertEquals(List.of("filter t/missing: 0 member(s) displayed; active tag filters (1): \"missing\"; "
                            + "name searches are not included"),
                    logs.getInfoMessages());
        }
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_stackedAndRepeatedTags_logsDistinctActiveTagCount() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        FilterCommand friendsCommand = new FilterCommand(friends);
        FilterCommand owesMoneyCommand = new FilterCommand(new Tag("owesMoney"));

        try (LogCapture logs = new LogCapture(FilterCommand.class)) {
            friendsCommand.execute(model);
            CommandResult stackedResult = owesMoneyCommand.execute(model);
            CommandResult repeatedResult = owesMoneyCommand.execute(model);

            assertEquals("1 member(s) listed with tags \"friends\", \"owesMoney\".",
                    stackedResult.getFeedbackToUser());
            assertEquals("Tag \"owesMoney\" is already an active filter. No changes were made.\n"
                    + stackedResult.getFeedbackToUser(), repeatedResult.getFeedbackToUser());
            assertEquals(List.of(
                    "filter t/friends: 3 member(s) displayed; active tag filters (1): \"friends\"; "
                            + "name searches are not included",
                    "filter t/owesMoney: 1 member(s) displayed; active tag filters (2): \"friends\", "
                            + "\"owesMoney\"; name searches are not included",
                    "filter t/owesMoney: 1 member(s) displayed; active tag filters (2): \"friends\", "
                            + "\"owesMoney\"; name searches are not included"),
                    logs.getInfoMessages());
        }
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void execute_repeatedTagWithDifferentCase_reportsActiveSpellingAndPreservesView() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        new FilterCommand(friends).execute(model);
        new FilterCommand(new Tag("owesMoney")).execute(model);
        List<?> filtersBefore = model.getActiveFilters();
        List<?> displayedBefore = List.copyOf(model.getFilteredPersonList());

        CommandResult result = new FilterCommand(new Tag("OWESMONEY")).execute(model);

        assertEquals("Tag \"owesMoney\" is already an active filter. No changes were made.\n"
                + "1 member(s) listed with tags \"friends\", \"owesMoney\".", result.getFeedbackToUser());
        assertEquals(filtersBefore, model.getActiveFilters());
        assertEquals(displayedBefore, model.getFilteredPersonList());
    }

    @Test
    public void execute_existingFilter_narrowsCurrentList() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> !person.equals(DANIEL));

        try (LogCapture logs = new LogCapture(FilterCommand.class)) {
            CommandResult result = new FilterCommand(friends).execute(model);

            assertEquals("2 member(s) listed with tag \"friends\".", result.getFeedbackToUser());
            assertEquals(List.of("filter t/friends: 2 member(s) displayed; active tag filters (1): \"friends\"; "
                            + "name searches are not included"),
                    logs.getInfoMessages());
        }
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
    }

    @Test
    public void execute_afterNameSearch_logsTagScopeWithoutChangingSearch() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice"))).execute(model);

        try (LogCapture logs = new LogCapture(FilterCommand.class)) {
            CommandResult result = new FilterCommand(friends).execute(model);

            assertEquals("1 member(s) listed with tag \"friends\".", result.getFeedbackToUser());
            assertEquals(List.of("filter t/friends: 1 member(s) displayed; active tag filters (1): \"friends\"; "
                            + "name searches are not included"),
                    logs.getInfoMessages());
        }
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCommand(friends).execute(null));
    }

    @Test
    public void equals_andHashCode_matchTags() {
        FilterCommand command = new FilterCommand(friends);
        FilterCommand sameTag = new FilterCommand(new Tag("friends"));

        assertTrue(command.equals(command));
        assertEquals(command, sameTag);
        assertEquals(command.hashCode(), sameTag.hashCode());
        assertFalse(command.equals(new FilterCommand(new Tag("owesMoney"))));
        assertFalse(command.equals(null));
        assertFalse(command.equals("filter"));
    }
}

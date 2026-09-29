package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.HelpCommand.SHOWING_HELP_MESSAGE;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.NameContainsKeywordsPredicate;

/**
 * Verifies that offline help selects its topic without changing member data or the active search.
 */
public class HelpCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
    private final Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_help_success() {
        CommandResult expectedCommandResult = new CommandResult(SHOWING_HELP_MESSAGE, true, false);
        assertCommandSuccess(new HelpCommand(), model, expectedCommandResult, expectedModel);
        assertTrue(new HelpCommand().isReadOnly());
    }

    @Test
    public void execute_topic_preservesSearchAndData() {
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of("Alice"));
        model.updateFilteredPersonList(predicate);
        expectedModel.updateFilteredPersonList(predicate);
        CommandResult expected = new CommandResult("Opened help for edit.", true, false, "edit");
        assertCommandSuccess(new HelpCommand("edit"), model, expected, expectedModel);
        assertEquals(expectedModel.getFilteredPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void constructor_invalidTopic_rejectsInput() {
        assertThrows(NullPointerException.class, () -> new HelpCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new HelpCommand("filter"));
    }

    @Test
    public void equals_topic_comparesSelectedCommand() {
        assertEquals(new HelpCommand(), new HelpCommand());
        assertEquals(new HelpCommand("edit"), new HelpCommand("edit"));
        assertNotEquals(new HelpCommand(), new HelpCommand("edit"));
        assertNotEquals(new HelpCommand("add"), new HelpCommand("edit"));
    }
}

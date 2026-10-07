package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Prevents the offline guide from omitting a supported command or publishing an invalid example.
 */
public class CommandHelpTest {

    @Test
    public void catalogue_supportedCommands_coversEveryAvailableCommand() {
        Set<String> keywords = Arrays.stream(CommandHelp.values()).map(CommandHelp::getKeyword)
                .collect(Collectors.toSet());
        assertEquals(Set.of(AddCommand.COMMAND_WORD, EditCommand.COMMAND_WORD, DeleteCommand.COMMAND_WORD,
                ClearCommand.COMMAND_WORD, FindCommand.COMMAND_WORD, ListCommand.COMMAND_WORD,
                HelpCommand.COMMAND_WORD, ExitCommand.COMMAND_WORD, ExportCommand.COMMAND_WORD,
                SortCommand.COMMAND_WORD, RemarkCommand.COMMAND_WORD), keywords);
        assertEquals(CommandHelp.values().length, keywords.size());
    }

    @Test
    public void catalogue_examples_parseAsDocumentedCommands() throws ParseException {
        AddressBookParser parser = new AddressBookParser();
        for (CommandHelp entry : CommandHelp.values()) {
            Command command = parser.parseCommand(entry.getExample());
            assertEquals(entry.getKeyword(), entry.getExample().split(" ")[0]);
            assertEquals(entry.getKeyword() + "command", command.getClass().getSimpleName().toLowerCase());
            assertFalse(entry.getPurpose().isBlank());
            assertFalse(entry.getExpectedResult().isBlank());
            assertFalse(entry.getErrors().isBlank());
        }
    }

    @Test
    public void find_unavailableTopic_hasNoHelpEntry() {
        assertTrue(CommandHelp.find("help").isPresent());
        assertTrue(CommandHelp.find("tagall").isEmpty());
        assertTrue(CommandHelp.find("filter").isEmpty());
        assertTrue(CommandHelp.find("EXIT").isEmpty());
    }
}

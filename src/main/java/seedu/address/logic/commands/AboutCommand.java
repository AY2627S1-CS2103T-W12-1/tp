package seedu.address.logic.commands;

import seedu.address.model.Model;

/**
 * Displays information about the application.
 */
public class AboutCommand extends Command {

    public static final String COMMAND_WORD = "about";

    public static final String MESSAGE_SUCCESS = "AddressBook-Level3 (AB3) is a desktop address book application.";

    @Override
    public CommandResult execute(Model model) {
        return new CommandResult(MESSAGE_SUCCESS);
    }
}

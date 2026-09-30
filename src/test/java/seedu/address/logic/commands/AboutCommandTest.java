package seedu.address.logic.commands;

import static seedu.address.logic.commands.AboutCommand.MESSAGE_SUCCESS;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;

public class AboutCommandTest {
    private final Model model = new ModelManager();
    private final Model expectedModel = new ModelManager();

    @Test
    public void execute_about_success() {
        CommandResult expectedCommandResult = new CommandResult(MESSAGE_SUCCESS);
        assertCommandSuccess(new AboutCommand(), model, expectedCommandResult, expectedModel);
    }
}

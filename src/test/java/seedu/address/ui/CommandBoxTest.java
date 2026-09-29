package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.testutil.JavaFxTestUtil;

/** Checks that retrying input cannot repeat changes that were already applied before a save failure. */
public class CommandBoxTest {
    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void enter_successClearsInputAndBlankDoesNotExecute() throws Exception {
        onJavaFxThread(() -> {
            AtomicInteger executions = new AtomicInteger();
            TextField field = field(new CommandBox(command -> {
                executions.incrementAndGet();
                assertEquals("list", command);
                return new CommandResult("Showing 1 member.");
            }));
            field.fireEvent(new ActionEvent());
            assertEquals(0, executions.get());
            field.setText("list");
            field.fireEvent(new ActionEvent());
            assertEquals("", field.getText());
            assertEquals(1, executions.get());
        });
    }

    @Test
    public void enter_appliedSaveFailure_clearsInputAndPreventsRepeatedDeletion() throws Exception {
        onJavaFxThread(() -> {
            AtomicInteger executions = new AtomicInteger();
            TextField field = field(new CommandBox(command -> {
                executions.incrementAndGet();
                throw new SaveFailureException("Unsaved deletion", new IOException(), true);
            }));
            field.setText("delete 1");
            field.fireEvent(new ActionEvent());
            assertEquals("", field.getText());
            assertTrue(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            field.fireEvent(new ActionEvent());
            assertEquals(1, executions.get());
            field.setText("list");
            assertFalse(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
        });
    }

    @Test
    public void enter_rejectedOrRolledBackCommand_preservesEditableInput() throws Exception {
        onJavaFxThread(() -> {
            CommandBox.CommandExecutor[] failures = {
                command -> {
                    throw new ParseException("Invalid syntax");
                },
                command -> {
                    throw new CommandException("Invalid member");
                },
                command -> {
                    throw new SaveFailureException("Clear rolled back", new IOException(), false);
                }
            };
            for (CommandBox.CommandExecutor failure : failures) {
                TextField field = field(new CommandBox(failure));
                field.setText("clear extra");
                field.fireEvent(new ActionEvent());
                field.fireEvent(new ActionEvent());
                assertEquals("clear extra", field.getText());
                assertEquals(1, field.getStyleClass().stream().filter(CommandBox.ERROR_STYLE_CLASS::equals).count());
                field.setText("clear");
                assertFalse(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            }
        });
    }

    private TextField field(CommandBox box) {
        return (TextField) box.getRoot().lookup("#commandTextField");
    }
}

package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
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
                field.setText("clear /all");
                assertFalse(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            }
        });
    }

    @Test
    public void paste_tabOrInternalLineBreak_rejectsWholePaste() throws Exception {
        onJavaFxThread(() -> {
            CommandBox box = new CommandBox(command -> new CommandResult("Done."));
            TextField field = field(box);
            Label warning = warning(box);
            // EP: tabs and line breaks at command, prefix and keyword boundaries, inside values, and in replies.
            for (String pasted : List.of("add\tn/Charlie p/123 e/charlie@example.com",
                    "add n/Charlie\tp/123\te/charlie@example.com", "find Alice\tBob",
                    "add n/Charlie p/123\t456 e/charlie@example.com", "y\tes", "find Alice\nBob", "y\r\nes",
                    "\nlist", "list\u0000", "list\u007f")) {
                field.clear();
                field.replaceSelection(pasted);
                assertEquals("", field.getText());
                assertTrue(warning.isVisible());
                assertEquals(CommandBox.PASTE_REJECTED_MESSAGE, warning.getText());
            }
        });
    }

    @Test
    public void paste_rejectedReplyWhileConfirming_blocksEnterUntilReplyIsTyped() throws Exception {
        onJavaFxThread(() -> {
            List<String> executed = new ArrayList<>();
            CommandBox box = new CommandBox(command -> {
                executed.add(command);
                return new CommandResult("Done.");
            });
            TextField field = field(box);
            box.setAwaitingConfirmation(true);
            field.replaceSelection("y\tes");
            assertEquals("", field.getText());
            // An empty Enter would otherwise cancel the pending command, so this only passes while Enter is blocked.
            field.fireEvent(new ActionEvent());
            assertEquals(List.of(), executed);
            assertTrue(warning(box).isVisible());

            field.replaceSelection("no");
            assertFalse(warning(box).isVisible());
            field.fireEvent(new ActionEvent());
            assertEquals(List.of("no"), executed);
        });
    }

    @Test
    public void paste_rejectedPartialReplacement_keepsEarlierCommandUnsubmittedUntilEdited() throws Exception {
        onJavaFxThread(() -> {
            List<String> executed = new ArrayList<>();
            CommandBox box = new CommandBox(command -> {
                executed.add(command);
                return new CommandResult("Done.");
            });
            TextField field = field(box);
            field.setText("delete 1");
            field.selectRange(7, 8);
            field.replaceSelection("\t2");
            assertEquals("delete 1", field.getText());
            // Moving the caret is not an edit, so the earlier command must stay blocked.
            field.positionCaret(0);
            field.fireEvent(new ActionEvent());
            assertEquals(List.of(), executed);
            assertTrue(warning(box).isVisible());

            field.positionCaret(field.getLength());
            field.replaceSelection("0");
            assertFalse(warning(box).isVisible());
            field.fireEvent(new ActionEvent());
            assertEquals(List.of("delete 10"), executed);
        });
    }

    @Test
    public void paste_trailingLineBreakAtEnd_isAcceptedWithoutJoiningWords() throws Exception {
        onJavaFxThread(() -> {
            CommandBox box = new CommandBox(command -> new CommandResult("Done."));
            TextField field = field(box);
            field.replaceSelection("find Alice Bob\r\n");
            assertEquals("find Alice Bob", field.getText());
            assertFalse(warning(box).isVisible());

            // The same line break before existing text would join two words, so it is rejected.
            field.setText("find  Bob");
            field.positionCaret(5);
            field.replaceSelection("Alice\n");
            assertEquals("find  Bob", field.getText());
            assertTrue(warning(box).isVisible());
        });
    }

    private Label warning(CommandBox box) {
        return (Label) box.getRoot().lookup("#pasteWarning");
    }

    private TextField field(CommandBox box) {
        return (TextField) box.getRoot().lookup("#commandTextField");
    }
}

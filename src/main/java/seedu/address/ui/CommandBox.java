package seedu.address.ui;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import seedu.address.logic.Confirmation;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    public static final String CONFIRMATION_STYLE_CLASS = "confirmation-pending";
    public static final String DEFAULT_PROMPT = "Enter command here...";
    public static final String CONFIRMATION_PROMPT = "Type " + Confirmation.WORD
            + " and press Enter to confirm, or no to cancel";
    private static final String FXML = "CommandBox.fxml";

    private final CommandExecutor commandExecutor;
    private boolean isAwaitingConfirmation;

    @FXML
    private TextField commandTextField;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}.
     */
    public CommandBox(CommandExecutor commandExecutor) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
    }

    /** Returns keyboard focus to the command field after roster inspection. */
    public void focusInput() {
        commandTextField.requestFocus();
    }

    /**
     * Highlights the command box and changes its hint while the next input confirms or cancels a destructive command.
     */
    public void setAwaitingConfirmation(boolean isAwaitingConfirmation) {
        this.isAwaitingConfirmation = isAwaitingConfirmation;
        ObservableList<String> styleClass = commandTextField.getStyleClass();
        styleClass.remove(CONFIRMATION_STYLE_CLASS);
        if (isAwaitingConfirmation) {
            styleClass.add(CONFIRMATION_STYLE_CLASS);
        }
        commandTextField.setPromptText(isAwaitingConfirmation ? CONFIRMATION_PROMPT : DEFAULT_PROMPT);
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        if (commandText.isEmpty() && !isAwaitingConfirmation) {
            return;
        }

        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (SaveFailureException e) {
            if (e.isChangeApplied()) {
                // Repeating an already-applied delete or edit could affect a different displayed member.
                commandTextField.clear();
            }
            setStyleToIndicateCommandFailure();
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see seedu.address.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}

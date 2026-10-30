package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandHelp;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.ExportCommand;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ActiveFilter;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    public static final String UNSAVED_CHANGE_GUIDANCE =
            "\n\nThe change is visible in this session but is not saved. The previous saved roster is unchanged.\n"
            + "Fix the storage problem, then check the list and use edit with an existing field value to retry saving. "
            + "If the roster is empty, retry with clear. Exiting loses unsaved changes.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;

    /** The destructive command awaiting confirmation by the next input, or null if none is pending. */
    private Command pendingConfirmation;

    /** The prompt that named the pending action and its scope, repeated after a near-miss reply. */
    private String pendingConfirmationPrompt;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("User command received");

        if (pendingConfirmation != null) {
            // Ignore boundary spaces without changing confirmation case or interior text.
            return handleConfirmationInput(commandText.replaceAll(
                    "\\A[\\p{javaWhitespace}\\p{Zs}]+|[\\p{javaWhitespace}\\p{Zs}]+\\z", ""));
        }

        Command command = addressBookParser.parseCommand(commandText);
        if (command instanceof ClearCommand clear && clear.prepareTargets(model) > 0) {
            // Capture the scope before asking; later filter changes cannot expand the deletion.
            String prompt = clear.getConfirmationMessage();
            requestConfirmation(command, prompt);
            return new CommandResult(prompt);
        }
        CommandResult commandResult = executeAndSave(command);
        commandResult.getPendingDeletion()
                .ifPresent(deletion -> requestConfirmation(deletion, commandResult.getFeedbackToUser()));
        return commandResult;
    }

    /**
     * Waits for the next input to confirm {@code command}, which {@code prompt} has described to the user.
     */
    private void requestConfirmation(Command command, String prompt) {
        pendingConfirmation = command;
        pendingConfirmationPrompt = prompt;
    }

    /**
     * Runs the pending destructive command if {@code input} is the confirmation word. A near miss keeps
     * the confirmation pending, and any other input cancels it without being run as a command.
     */
    private CommandResult handleConfirmationInput(String input) throws CommandException {
        assert pendingConfirmation != null : "Confirmation input is handled only while a command is pending";
        if (Confirmation.isNearMiss(input)) {
            logger.info("Inexact confirmation word received; the confirmation stays pending");
            return new CommandResult(Confirmation.REMINDER
                    + "\n\n" + pendingConfirmationPrompt);
        }
        Command confirmation = pendingConfirmation;
        // Any other input ends the confirmation, so a later "yes" cannot repeat a destructive action.
        pendingConfirmation = null;
        if (!input.equals(Confirmation.WORD)) {
            logger.info("Confirmation cancelled; the cancelling input was not run as a command");
            return new CommandResult(createCancellationMessage(confirmation, input));
        }
        logger.info("Destructive command confirmed");
        return executeAndSave(confirmation);
    }

    /**
     * Returns the message for cancelling {@code confirmation}. If {@code input} starts with a command word,
     * the message also says that the command was not run.
     */
    private static String createCancellationMessage(Command confirmation, String input) {
        String message = confirmation instanceof ClearCommand
                ? ClearCommand.MESSAGE_CANCELLED
                : ConfirmDeleteCommand.MESSAGE_CANCELLED;
        String firstWord = input.split("\\s+")[0];
        if (CommandHelp.find(firstWord).isPresent()) {
            message += String.format(Confirmation.INPUT_NOT_RUN, input);
        }
        return message;
    }

    @Override
    public boolean isAwaitingConfirmation() {
        return pendingConfirmation != null;
    }

    /**
     * Executes {@code command} and saves the roster unless the command is read-only.
     */
    private CommandResult executeAndSave(Command command) throws CommandException {
        CommandResult commandResult;
        AddressBook beforeClear = command instanceof ClearCommand ? new AddressBook(model.getAddressBook()) : null;
        commandResult = command instanceof ExportCommand exportCommand
                ? exportCommand.execute(model, storage.getAddressBookFilePath(), storage.getUserPrefsFilePath())
                : command.execute(model);

        if (command.isReadOnly()) {
            return commandResult;
        }

        try {
            storage.saveAddressBook(model.getAddressBook());
        } catch (IOException ioe) {
            logger.warning("Could not save roster after " + command.getClass().getSimpleName()
                    + " (" + ioe.getClass().getSimpleName() + ")");
            if (beforeClear != null) {
                // Clear changes only the records, so restoring them also restores the prior filtered view.
                model.setAddressBook(beforeClear);
                String errorFormat = ioe instanceof AccessDeniedException
                        ? FILE_OPS_PERMISSION_ERROR_FORMAT : FILE_OPS_ERROR_FORMAT;
                String details = ioe.getMessage() == null ? ioe.toString() : ioe.getMessage();
                throw new SaveFailureException(ClearCommand.MESSAGE_SAVE_FAILURE + "\n"
                        + String.format(errorFormat, details), ioe, false);
            }
            if (command instanceof ImportCommand) {
                String details = ioe.getMessage() == null ? ioe.toString() : ioe.getMessage();
                throw new SaveFailureException(String.format(ImportCommand.MESSAGE_SAVE_FAILURE, details)
                        + UNSAVED_CHANGE_GUIDANCE, ioe, true);
            }
            String errorFormat = ioe instanceof AccessDeniedException
                    ? FILE_OPS_PERMISSION_ERROR_FORMAT : FILE_OPS_ERROR_FORMAT;
            throw new SaveFailureException(String.format(errorFormat, ioe.getMessage())
                    + UNSAVED_CHANGE_GUIDANCE, ioe, true);
        }

        if (command instanceof ClearCommand clear) {
            clear.finishSaving(model);
        }
        return commandResult;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public List<ActiveFilter> getActiveFilters() {
        return model.getActiveFilters();
    }

    @Override
    public void removeActiveFilter(ActiveFilter filter) {
        model.removeActiveFilter(filter);
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}

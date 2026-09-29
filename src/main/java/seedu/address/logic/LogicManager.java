package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
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

    public static final String CLEAR_ROLLBACK_GUIDANCE =
            "\n\nNo members were removed. The roster and current list were restored; the saved file is unchanged.\n"
            + "Fix the storage problem, then retry clear if you still want to remove every member.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;

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
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command = addressBookParser.parseCommand(commandText);
        AddressBook beforeClear = command instanceof ClearCommand ? new AddressBook(model.getAddressBook()) : null;
        commandResult = command.execute(model);

        if (command.isReadOnly()) {
            return commandResult;
        }

        try {
            storage.saveAddressBook(model.getAddressBook());
        } catch (IOException ioe) {
            boolean changeApplied = beforeClear == null;
            if (beforeClear != null) {
                // Clear changes only the records, so restoring them also restores the prior filtered view.
                model.setAddressBook(beforeClear);
            }
            String errorFormat = ioe instanceof AccessDeniedException
                    ? FILE_OPS_PERMISSION_ERROR_FORMAT : FILE_OPS_ERROR_FORMAT;
            String guidance = changeApplied ? UNSAVED_CHANGE_GUIDANCE : CLEAR_ROLLBACK_GUIDANCE;
            throw new SaveFailureException(String.format(errorFormat, ioe.getMessage()) + guidance, ioe, changeApplied);
        }

        return commandResult;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
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

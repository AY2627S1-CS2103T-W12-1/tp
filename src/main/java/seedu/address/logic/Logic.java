package seedu.address.logic;

import java.util.List;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ActiveFilter;
import seedu.address.model.person.Person;

/**
 * API of the Logic component
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /**
     * Returns true if the next input confirms or cancels a pending deletion instead of running as a command.
     */
    boolean isAwaitingConfirmation();

    /** Returns an unmodifiable view of the filtered list of persons */
    ObservableList<Person> getFilteredPersonList();

    /** Returns removable display conditions in application order. */
    List<ActiveFilter> getActiveFilters();

    /** Returns the number of active {@code show} selections. */
    int getActiveShowSelectionCount();

    /** Clears all {@code show} selections without changing other filters or saved data. */
    void clearShowSelections();

    /** Removes one active filter without changing the saved roster. */
    void removeActiveFilter(ActiveFilter filter);

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}

package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import seedu.address.logic.Confirmation;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Removes displayed members, or the complete roster when explicitly requested.
 * LogicManager requires confirmation before executing this command for a non-empty target set.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE =
            "Invalid command format. Usage: clear [/all]. Use /all to remove hidden members too.";
    public static final String MESSAGE_CONFIRMATION =
            "Clear the entire roster? Total members: %d (including anyone hidden by searches or filters).\n"
            + "Type " + Confirmation.WORD
            + " and press Enter to confirm, or " + Confirmation.CANCEL_INSTRUCTION + ".";
    public static final String MESSAGE_CANCELLED = "Clear cancelled. No members were removed.";
    public static final String MESSAGE_SUCCESS = "Cleared %d members. The roster is now empty.";
    public static final String MESSAGE_SINGLE = "Cleared 1 member. The roster is now empty.";
    public static final String MESSAGE_EMPTY = "The roster is already empty. No changes were made.";
    public static final String MESSAGE_SAVE_FAILURE =
            "Unable to clear the roster because the changes could not be saved. No members were removed.";

    public static final String MESSAGE_DISPLAY_CONFIRMATION =
            "Clear the %d displayed member(s)? Hidden members will be kept. Use clear /all to remove everyone.\n"
            + "Type " + Confirmation.WORD
            + " and press Enter to confirm, or " + Confirmation.CANCEL_INSTRUCTION + ".";
    public static final String MESSAGE_DISPLAY_SUCCESS =
            "Cleared %d displayed member(s). %d member(s) remain in the roster.";
    public static final String MESSAGE_NO_DISPLAYED =
            "No members in the displayed list. Run list to show everyone, or clear /all to remove the entire roster.";

    private final boolean all;
    /** The exact members named by the prompt, retained even if a filter is removed while waiting. */
    private List<Person> targets;

    /** Creates a command that clears only displayed members. */
    public ClearCommand() {
        this(false);
    }

    /** Creates a command with an explicit whole-roster or displayed-list scope. */
    public ClearCommand(boolean all) {
        this.all = all;
    }

    /** Captures the targets before confirmation and returns their count. */
    public int prepareTargets(Model model) throws CommandException {
        if (targets == null) {
            targets = List.copyOf(all ? model.getAddressBook().getPersonList() : model.getFilteredPersonList());
        }
        if (targets.isEmpty() && !model.getAddressBook().getPersonList().isEmpty()) {
            throw new CommandException(MESSAGE_NO_DISPLAYED);
        }
        return targets.size();
    }

    /** Returns a confirmation prompt naming the scope and captured count. */
    public String getConfirmationMessage() {
        return String.format(all ? MESSAGE_CONFIRMATION : MESSAGE_DISPLAY_CONFIRMATION, targets.size());
    }

    /** Updates selection descriptions only after saving, preserving rollback state until then. */
    public void finishSaving(Model model) {
        if (all) {
            model.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);
        } else {
            model.removeDeletedPersonsFromSelections(targets);
        }
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        int count = prepareTargets(model);
        AddressBook remaining = new AddressBook();
        remaining.setPersons(model.getAddressBook().getPersonList().stream()
                .filter(person -> !targets.contains(person)).toList());
        // Do not mutate selection predicates before saving: a failed clear must restore the original view.
        model.setAddressBook(remaining);
        if (!remaining.getPersonList().isEmpty()) {
            return new CommandResult(String.format(MESSAGE_DISPLAY_SUCCESS, count, remaining.getPersonList().size()));
        }
        String feedback = switch (count) {
            case 0 -> MESSAGE_EMPTY;
            case 1 -> MESSAGE_SINGLE;
            default -> String.format(MESSAGE_SUCCESS, count);
        };
        return new CommandResult(feedback);
    }
}

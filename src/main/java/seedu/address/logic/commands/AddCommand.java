package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Adds a person to the address book.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = "Add a member\n"
            + "Name: " + PREFIX_NAME + "NAME\n"
            + "Phone: " + PREFIX_PHONE + "PHONE\n"
            + "Email: " + PREFIX_EMAIL + "EMAIL\n"
            + "Address: " + PREFIX_ADDRESS + "ADDRESS\n"
            + "Tags (optional): " + PREFIX_TAG + "TAG ...\n\n"
            + "Example (enter as one command):\n"
            + COMMAND_WORD + " " + PREFIX_NAME + "John Doe " + PREFIX_PHONE + "98765432 "
            + PREFIX_EMAIL + "john@example.com " + PREFIX_ADDRESS + "Clementi Avenue 2 "
            + PREFIX_TAG + "committee\n\n"
            + "Type help add for field rules and more guidance.";

    public static final String MESSAGE_SUCCESS = "Added member\n%1$s";
    public static final String MESSAGE_DUPLICATE_PERSON =
            "A member with these contact details already exists. Use edit to update that member.";

    private final Person toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Person}
     */
    public AddCommand(Person person) {
        requireNonNull(person);
        toAdd = person;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        if (model.hasPerson(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_PERSON);
        }

        model.addPerson(toAdd);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.format(toAdd)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}

package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TagTestUtil.createTypicalModel;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.LogCapture;

/**
 * Tests shared by the bulk tag commands {@code tagall} and {@code untagall}.
 *
 * @param <T> The command under test.
 */
public abstract class BulkTagCommandTest<T extends BulkTagCommand> {
    protected static final Predicate<Person> HAS_FRIENDS_TAG = person ->
            person.getTags().contains(new Tag("friends"));

    protected final Tag committee = new Tag("committee");
    protected final Tag friends = new Tag("friends");

    /** Returns the command under test, for {@code tag}. */
    protected abstract T createCommand(Tag tag);

    /** Returns the other bulk tag command, for {@code tag}. */
    protected abstract BulkTagCommand createOtherCommand(Tag tag);

    /** Returns the command word of the command under test. */
    protected abstract String getCommandWord();

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> createCommand(null));
    }

    @Test
    public void isReadOnly_dataChangingCommand_returnsFalse() {
        assertFalse(createCommand(committee).isReadOnly());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> createCommand(committee).execute(null));
    }

    @Test
    public void execute_emptyDisplayedList_throwsCommandException() {
        Model model = createTypicalModel(person -> false);

        try (LogCapture logs = new LogCapture(createCommand(committee).getClass())) {
            assertCommandFailure(createCommand(friends), model, BulkTagCommand.MESSAGE_EMPTY_LIST);
            assertEquals(List.of(getCommandWord() + " rejected: no members in the current list"),
                    logs.getInfoMessages());
        }
    }

    @Test
    public void equals_sameAndDifferentTags_returnsCorrectResult() {
        T command = createCommand(committee);

        assertTrue(command.equals(command));
        assertTrue(command.equals(createCommand(new Tag("committee"))));
        assertEquals(command.hashCode(), createCommand(new Tag("committee")).hashCode());
        assertFalse(command.equals(createCommand(friends)));
        assertFalse(command.equals(createCommand(new Tag("Committee"))));
        assertFalse(command.equals(createOtherCommand(committee)));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toString_validCommand_containsTag() {
        T command = createCommand(committee);
        String expected = command.getClass().getCanonicalName() + "{tag=" + committee + "}";
        assertEquals(expected, command.toString());
    }
}

package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Keeps a displayed selection attached to its members when their records are replaced. */
public class PersonSelectionPredicate implements Predicate<Person> {
    private final Set<Person> selectedPersons;

    /** Creates a selection containing exactly the supplied members, without duplicate entries. */
    public PersonSelectionPredicate(Collection<Person> selectedPersons) {
        requireAllNonNull(selectedPersons);
        this.selectedPersons = new HashSet<>(selectedPersons);
    }

    @Override
    public boolean test(Person person) {
        return selectedPersons.contains(person);
    }

    /** Returns the members still selected by this show command. */
    public Set<Person> getSelectedPersons() {
        return Set.copyOf(selectedPersons);
    }

    /** Drops a deleted member from the selection. */
    public void removePerson(Person person) {
        selectedPersons.remove(person);
    }

    /**
     * Tracks a successful record replacement, returning whether the replaced member was selected.
     * This must be called only after the roster accepts the replacement.
     */
    public boolean replacePerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);
        if (!selectedPersons.remove(target)) {
            return false;
        }
        selectedPersons.add(editedPerson);
        return true;
    }
}

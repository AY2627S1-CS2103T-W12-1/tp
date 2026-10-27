package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.EmailContainsKeywordsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonSelectionPredicate;
import seedu.address.model.tag.Tag;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;
    private final UserPrefs userPrefs;
    private final FilteredList<Person> filteredPersons;
    /** Extra predicates used internally by commands and tests, without removable UI boxes. */
    private final List<Predicate<Person>> additionalFilters = new ArrayList<>();
    /** Removable name, email and tag conditions, kept in the order they were applied. */
    private final List<ActiveFilter> activeFilters = new ArrayList<>();
    /** Live display ordering; the underlying roster retains its stored order. */
    private final SortedList<Person> sortedPersons;

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing model with " + addressBook.getPersonList().size() + " member(s)");

        this.addressBook = new AddressBook(addressBook);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
        sortedPersons = new SortedList<>(filteredPersons);
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        addressBook.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        addressBook.setPerson(target, editedPerson);
        boolean selectionChanged = false;
        for (Predicate<Person> filter : additionalFilters) {
            if (filter instanceof PersonSelectionPredicate selection && selection.replacePerson(target, editedPerson)) {
                selectionChanged = true;
            }
        }
        if (selectionChanged) {
            refreshFilteredPersons();
        }
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code addressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return sortedPersons;
    }

    @Override
    public void setNameSorting(boolean enabled) {
        sortedPersons.setComparator(enabled
                ? (first, second) -> {
                    String firstName = first.getName().fullName.trim().replaceAll(" +", " ");
                    String secondName = second.getName().fullName.trim().replaceAll(" +", " ");
                    return firstName.compareToIgnoreCase(secondName);
                }
                : null);
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        additionalFilters.clear();
        activeFilters.clear();
        if (predicate instanceof NameContainsKeywordsPredicate namePredicate) {
            activeFilters.add(ActiveFilter.createNameFilter(namePredicate));
        } else if (predicate instanceof EmailContainsKeywordsPredicate emailPredicate) {
            activeFilters.add(ActiveFilter.createEmailFilter(emailPredicate));
        } else {
            additionalFilters.add(predicate);
        }
        refreshFilteredPersons();
    }

    @Override
    public void filterFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        additionalFilters.add(predicate);
        refreshFilteredPersons();
    }

    @Override
    public void filterFilteredPersonListByName(NameContainsKeywordsPredicate predicate) {
        requireNonNull(predicate);
        activeFilters.add(ActiveFilter.createNameFilter(predicate));
        refreshFilteredPersons();
    }

    @Override
    public void filterFilteredPersonListByTag(Tag tag) {
        requireNonNull(tag);
        if (!getActiveTagFilters().contains(tag)) {
            activeFilters.add(ActiveFilter.createTagFilter(tag));
            refreshFilteredPersons();
        }
    }

    @Override
    public List<Tag> getActiveTagFilters() {
        return activeFilters.stream().filter(filter -> filter.getKind() == ActiveFilter.Kind.TAG)
                .map(ActiveFilter::getTag).toList();
    }

    @Override
    public List<ActiveFilter> getActiveFilters() {
        return List.copyOf(activeFilters);
    }

    @Override
    public int getActiveShowSelectionCount() {
        return (int) additionalFilters.stream().filter(PersonSelectionPredicate.class::isInstance).count();
    }

    @Override
    public void clearShowSelections() {
        if (additionalFilters.removeIf(PersonSelectionPredicate.class::isInstance)) {
            refreshFilteredPersons();
        }
    }

    @Override
    public void removeActiveFilter(ActiveFilter filter) {
        requireNonNull(filter);
        if (activeFilters.remove(filter)) {
            refreshFilteredPersons();
        }
    }

    private void refreshFilteredPersons() {
        filteredPersons.setPredicate(person -> {
            for (Predicate<Person> filter : additionalFilters) {
                if (!filter.test(person)) {
                    return false;
                }
            }
            for (ActiveFilter filter : activeFilters) {
                if (!filter.test(person)) {
                    return false;
                }
            }
            return true;
        });
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return addressBook.equals(otherModelManager.addressBook)
                && userPrefs.equals(otherModelManager.userPrefs)
                && sortedPersons.equals(otherModelManager.sortedPersons);
    }

    /** Hashes the current state used by equality; mutable model state is never cached. */
    @Override
    public int hashCode() {
        return Objects.hash(addressBook, userPrefs, sortedPersons);
    }

}

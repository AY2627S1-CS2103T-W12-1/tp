package seedu.address.model;

import java.util.List;
import java.util.function.Predicate;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.person.EmailContainsKeywordsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true */
    Predicate<Person> PREDICATE_SHOW_ALL_PERSONS = unused -> true;

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces address book data with the data in {@code addressBook}.
     */
    void setAddressBook(ReadOnlyAddressBook addressBook);

    /** Returns the AddressBook */
    ReadOnlyAddressBook getAddressBook();

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book.
     */
    boolean hasPerson(Person person);

    /**
     * Deletes the given person.
     * The person must exist in the address book.
     */
    void deletePerson(Person target);

    /**
     * Adds the given person.
     * {@code person} must not already exist in the address book.
     */
    void addPerson(Person person);

    /**
     * Replaces the given person {@code target} with {@code editedPerson}.
     * {@code target} must exist in the address book.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the address book.
     */
    void setPerson(Person target, Person editedPerson);

    /** Returns an unmodifiable view of the filtered person list */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Sets whether displayed persons are sorted by name, ignoring letter case.
     * The filter and stored roster order are unchanged.
     *
     * @param enabled True for alphabetical order, false for stored order.
     */
    void setNameSorting(boolean enabled);

    /**
     * Updates the filter of the filtered person list to filter by the given {@code predicate}.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredPersonList(Predicate<Person> predicate);

    /**
     * Narrows the currently displayed person list using the given {@code predicate}.
     *
     * @throws NullPointerException if {@code predicate} is null.
     */
    void filterFilteredPersonList(Predicate<Person> predicate);

    /**
     * Adds a separately removable name search unless an equivalent search is already active.
     *
     * @return True if added; false for a repeat, ignoring keyword case, repetition and order.
     */
    boolean filterFilteredPersonListByName(NameContainsKeywordsPredicate predicate);

    /**
     * Adds a separately removable email search unless an equivalent search is already active.
     *
     * @return True if added; false for a repeat, ignoring keyword case, repetition and order.
     */
    boolean filterFilteredPersonListByEmail(EmailContainsKeywordsPredicate predicate);

    /**
     * Narrows the displayed list to persons with {@code tag} and records the active tag filter.
     */
    void filterFilteredPersonListByTag(Tag tag);

    /**
     * Adds one removable tag condition that a person satisfies by having any one of {@code tags}.
     * Earlier conditions still apply, so this narrows the list further.
     *
     * @return True if added; false if a condition over exactly the same tags is already active.
     * @throws IllegalArgumentException If {@code tags} is empty.
     */
    boolean filterFilteredPersonListByTags(List<Tag> tags);

    /** Returns the tags of every active tag condition, in the order they were applied. */
    List<Tag> getActiveTagFilters();

    /** Returns removable display conditions in the order they were applied. */
    List<ActiveFilter> getActiveFilters();

    /** Removes exactly one active filter while retaining the others. */
    void removeActiveFilter(ActiveFilter filter);
}

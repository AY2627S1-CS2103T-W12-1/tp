package seedu.address.model;

import java.util.function.Predicate;

import seedu.address.model.person.EmailContainsKeywordsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonSelectionPredicate;
import seedu.address.model.tag.Tag;

/** One removable condition in the displayed member list. */
public final class ActiveFilter {
    /** The kind of condition shown in the active filter row. */
    public enum Kind {
        NAME, EMAIL, TAG, SHOW
    }

    private final Kind kind;
    private final String label;
    private final Predicate<Person> predicate;
    private final Tag tag;

    private ActiveFilter(Kind kind, String label, Predicate<Person> predicate, Tag tag) {
        this.kind = kind;
        this.label = label;
        this.predicate = predicate;
        this.tag = tag;
    }

    /** Creates a name condition whose keywords are alternatives within this one search. */
    public static ActiveFilter createNameFilter(NameContainsKeywordsPredicate predicate) {
        return new ActiveFilter(Kind.NAME, "Name: " + String.join(" ", predicate.getKeywords()), predicate, null);
    }

    /** Creates a condition matching any of the given email fragments. */
    public static ActiveFilter createEmailFilter(EmailContainsKeywordsPredicate predicate) {
        return new ActiveFilter(Kind.EMAIL, "Email: " + String.join(" ", predicate.getKeywords()), predicate, null);
    }

    /** Creates a condition requiring the given tag. */
    public static ActiveFilter createTagFilter(Tag tag) {
        return new ActiveFilter(Kind.TAG, "Tag: " + tag.tagName, person -> person.getTags().contains(tag), tag);
    }

    /** Creates one removable condition for a single show command. */
    public static ActiveFilter createShowFilter(PersonSelectionPredicate predicate) {
        return new ActiveFilter(Kind.SHOW, null, predicate, null);
    }

    public Kind getKind() {
        return kind;
    }

    public String getLabel() {
        if (kind == Kind.SHOW) {
            int count = ((PersonSelectionPredicate) predicate).getSelectedPersons().size();
            return "Show: " + count + " selected";
        }
        return label;
    }

    /** Returns the selected names for a show condition's tooltip. */
    public String getShowSelectionNames() {
        if (kind != Kind.SHOW) {
            throw new IllegalStateException("Only show conditions have selected members");
        }
        return ((PersonSelectionPredicate) predicate).getSelectedPersons().stream()
                .map(person -> person.getName().fullName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((first, second) -> first + ", " + second)
                .orElse("No selected members remain");
    }

    /** Whether this show condition already selects the same members. */
    boolean hasSameShowSelection(PersonSelectionPredicate selection) {
        return kind == Kind.SHOW && ((PersonSelectionPredicate) predicate).getSelectedPersons()
                .equals(selection.getSelectedPersons());
    }

    /** Keeps a show condition attached to a member after its record is edited. */
    boolean replaceSelectedPerson(Person target, Person editedPerson) {
        return kind == Kind.SHOW && ((PersonSelectionPredicate) predicate).replacePerson(target, editedPerson);
    }

    /** Removes a deleted member from a show condition's description. */
    void removeSelectedPerson(Person person) {
        if (kind == Kind.SHOW) {
            PersonSelectionPredicate selection = (PersonSelectionPredicate) predicate;
            selection.removePerson(person);
        }
    }

    /** Returns whether the member satisfies this condition. */
    public boolean test(Person person) {
        return predicate.test(person);
    }

    /** Returns the tag for a tag condition, or null for a search condition. */
    Tag getTag() {
        return tag;
    }
}

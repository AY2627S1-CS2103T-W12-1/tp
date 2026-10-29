package seedu.address.model;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import seedu.address.model.person.EmailContainsKeywordsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonSelectionPredicate;
import seedu.address.model.tag.Tag;

/** One removable condition in the displayed member list. */
public final class ActiveFilter {
    /** Joins the alternatives of one condition in its label, since a member needs only one of them. */
    public static final String ALTERNATIVES_SEPARATOR = " / ";

    /** The kind of condition shown in the active filter row. */
    public enum Kind {
        NAME, EMAIL, TAG, SHOW
    }

    private final Kind kind;
    private final String label;
    private final Predicate<Person> predicate;
    /** The tags of a tag condition, where a member needs any one of them; empty for other kinds. */
    private final List<Tag> tags;
    private final Set<String> searchKeywords;

    private ActiveFilter(Kind kind, String label, Predicate<Person> predicate, List<Tag> tags,
            List<String> keywords) {
        this.kind = kind;
        this.label = label;
        this.predicate = predicate;
        this.tags = tags;
        searchKeywords = normalizeSearchKeywords(kind, keywords);
    }

    /** Uses the same case rules as each search predicate, without changing its displayed keywords. */
    private static Set<String> normalizeSearchKeywords(Kind kind, List<String> keywords) {
        if (kind == Kind.NAME) {
            Set<String> normalized = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            normalized.addAll(keywords);
            return Collections.unmodifiableSet(normalized);
        }
        return keywords.stream().map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Creates a name condition whose keywords are alternatives within this one search. */
    public static ActiveFilter createNameFilter(NameContainsKeywordsPredicate predicate) {
        String label = "Name: " + String.join(ALTERNATIVES_SEPARATOR, predicate.getKeywords());
        return new ActiveFilter(Kind.NAME, label, predicate, List.of(), predicate.getKeywords());
    }

    /** Creates a condition matching any of the given email fragments. */
    public static ActiveFilter createEmailFilter(EmailContainsKeywordsPredicate predicate) {
        String label = "Email: " + String.join(ALTERNATIVES_SEPARATOR, predicate.getKeywords());
        return new ActiveFilter(Kind.EMAIL, label, predicate, List.of(), predicate.getKeywords());
    }

    /** Creates a condition requiring the given tag. */
    public static ActiveFilter createTagFilter(Tag tag) {
        return createTagFilter(List.of(tag));
    }

    /**
     * Creates one condition that a member satisfies by having at least one of {@code tags}.
     * Separate tag conditions still all have to hold, so this is where OR applies within a single command.
     *
     * @throws IllegalArgumentException If {@code tags} is empty.
     */
    public static ActiveFilter createTagFilter(List<Tag> tags) {
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("A tag condition needs at least one tag");
        }
        List<Tag> copiedTags = List.copyOf(tags);
        String tagNames = copiedTags.stream().map(tag -> tag.tagName)
                .collect(Collectors.joining(ALTERNATIVES_SEPARATOR));
        return new ActiveFilter(Kind.TAG, "Tag: " + tagNames,
                person -> copiedTags.stream().anyMatch(person.getTags()::contains), copiedTags, List.of());
    }

    /** Whether this is a tag condition over exactly the given tags, ignoring their order. */
    public boolean hasSameTags(List<Tag> otherTags) {
        return kind == Kind.TAG && Set.copyOf(tags).equals(Set.copyOf(otherTags));
    }

    /** Compares searches of the same kind, ignoring keyword case, repetition and order. */
    public boolean hasSameSearch(ActiveFilter other) {
        return (kind == Kind.NAME || kind == Kind.EMAIL)
                && kind == other.kind && searchKeywords.equals(other.searchKeywords);
    }

    /** Creates one removable condition for a single show command. */
    public static ActiveFilter createShowFilter(PersonSelectionPredicate predicate) {
        return new ActiveFilter(Kind.SHOW, null, predicate, List.of(), List.of());
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

    /** Returns the alternative tags of a tag condition, or an empty list for any other condition. */
    public List<Tag> getTags() {
        return tags;
    }
}

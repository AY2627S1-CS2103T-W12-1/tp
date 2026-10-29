package seedu.address.model;

import java.util.function.Predicate;

import seedu.address.model.person.EmailContainsKeywordsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/** One removable name, email or tag condition in the displayed member list. */
public final class ActiveFilter {
    /** The kind of condition shown in the active filter row. */
    public enum Kind {
        NAME, EMAIL, TAG
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
        String label = "Name: " + String.join(" ", predicate.getKeywords());
        return new ActiveFilter(Kind.NAME, label, predicate, null);
    }

    /** Creates a condition matching any of the given email fragments. */
    public static ActiveFilter createEmailFilter(EmailContainsKeywordsPredicate predicate) {
        return new ActiveFilter(Kind.EMAIL, "Email: " + String.join(" ", predicate.getKeywords()), predicate, null);
    }

    /** Creates a condition requiring the given tag. */
    public static ActiveFilter createTagFilter(Tag tag) {
        return new ActiveFilter(Kind.TAG, "Tag: " + tag.tagName, person -> person.getTags().contains(tag), tag);
    }

    public Kind getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
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

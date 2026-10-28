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
    private final Set<String> searchKeywords;

    private ActiveFilter(Kind kind, String label, Predicate<Person> predicate, Tag tag, List<String> keywords) {
        this.kind = kind;
        this.label = label;
        this.predicate = predicate;
        this.tag = tag;
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
        return new ActiveFilter(Kind.NAME, "Name: " + String.join(" ", predicate.getKeywords()),
                predicate, null, predicate.getKeywords());
    }

    /** Creates a condition matching any of the given email fragments. */
    public static ActiveFilter createEmailFilter(EmailContainsKeywordsPredicate predicate) {
        return new ActiveFilter(Kind.EMAIL, "Email: " + String.join(" ", predicate.getKeywords()),
                predicate, null, predicate.getKeywords());
    }

    /** Creates a condition requiring the given tag. */
    public static ActiveFilter createTagFilter(Tag tag) {
        return new ActiveFilter(Kind.TAG, "Tag: " + tag.tagName,
                person -> person.getTags().contains(tag), tag, List.of());
    }

    /** Compares searches of the same kind, ignoring keyword case, repetition and order. */
    public boolean hasSameSearch(ActiveFilter other) {
        return kind != Kind.TAG && kind == other.kind && searchKeywords.equals(other.searchKeywords);
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

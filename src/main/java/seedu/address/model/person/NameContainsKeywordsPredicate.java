package seedu.address.model.person;

import java.text.Normalizer;
import java.util.List;
import java.util.function.Predicate;

import seedu.address.commons.util.StringUtil;
import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person}'s {@code Name} contains any keyword as a complete or partial word,
 * ignoring case and canonical Unicode representation.
 */
public class NameContainsKeywordsPredicate implements Predicate<Person> {
    private final List<String> keywords;

    /** Creates a name predicate with an immutable copy of the search keywords. */
    public NameContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = List.copyOf(keywords);
    }

    /** Returns the words used by the current name search. */
    public List<String> getKeywords() {
        return keywords;
    }

    @Override
    public boolean test(Person person) {
        String name = Normalizer.normalize(person.getName().fullName, Normalizer.Form.NFC);
        return keywords.stream().anyMatch(keyword -> StringUtil.containsPartialWordIgnoreCase(
                name, Normalizer.normalize(keyword, Normalizer.Form.NFC)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof NameContainsKeywordsPredicate otherNameContainsKeywordsPredicate)) {
            return false;
        }

        return keywords.equals(otherNameContainsKeywordsPredicate.keywords);
    }

    @Override
    public int hashCode() {
        return keywords.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}

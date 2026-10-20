package seedu.address.model.person;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests whether a person's email contains any search keyword, ignoring case.
 * Keywords are literal substrings and are never interpreted as regular expressions.
 */
public class EmailContainsKeywordsPredicate implements Predicate<Person> {
    private final List<String> keywords;

    /**
     * Creates an email predicate with an immutable copy of {@code keywords}.
     *
     * @param keywords Non-null search terms; each term is supplied without surrounding whitespace.
     * @throws NullPointerException If the list or any keyword is null.
     */
    public EmailContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = List.copyOf(keywords);
    }

    /** Returns the immutable search terms for the active filter label. */
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * Returns whether at least one keyword occurs in {@code person}'s email.
     * Case conversion uses a fixed locale so matching is independent of the computer's locale.
     */
    @Override
    public boolean test(Person person) {
        String email = person.getEmail().value.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(keyword -> email.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns whether another predicate has the same keyword sequence.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof EmailContainsKeywordsPredicate otherPredicate)) {
            return false;
        }

        return keywords.equals(otherPredicate.keywords);
    }

    /**
     * Returns a hash code based on the keyword sequence.
     */
    @Override
    public int hashCode() {
        return keywords.hashCode();
    }

    /**
     * Returns the keywords used by this predicate for diagnostic output.
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}

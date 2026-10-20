package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

/**
 * Tests literal email matching independently of other member fields and the default locale.
 */
public class EmailContainsKeywordsPredicateTest {
    @Test
    public void constructor_nullListOrKeyword_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new EmailContainsKeywordsPredicate(null));
        List<String> keywords = new ArrayList<>();
        keywords.add(null);
        assertThrows(NullPointerException.class, () -> new EmailContainsKeywordsPredicate(keywords));
    }

    @Test
    public void constructor_mutatedSourceList_keepsOriginalKeywords() {
        List<String> keywords = new ArrayList<>(List.of("alice"));
        EmailContainsKeywordsPredicate predicate = new EmailContainsKeywordsPredicate(keywords);
        keywords.clear();
        keywords.add("bob");

        assertTrue(predicate.test(new PersonBuilder().withEmail("alice@example.com").build()));
        assertFalse(predicate.test(new PersonBuilder().withEmail("bob@example.com").build()));
    }

    @Test
    public void test_emailContainsSubstring_ignoresCaseAndWordBoundaries() {
        EmailContainsKeywordsPredicate predicate = new EmailContainsKeywordsPredicate(List.of("NETT"));
        assertTrue(predicate.test(new PersonBuilder().withEmail("annette@example.com").build()));
    }

    @Test
    public void test_anyKeywordMatches_returnsTrue() {
        EmailContainsKeywordsPredicate predicate = new EmailContainsKeywordsPredicate(List.of("missing", "@EXAMPLE"));
        assertTrue(predicate.test(new PersonBuilder().withEmail("alice@example.com").build()));
    }

    @Test
    public void test_emailPunctuation_matchesLiterally() {
        Person person = new PersonBuilder().withEmail("alice+club@example.com").build();
        assertTrue(new EmailContainsKeywordsPredicate(List.of("+club@")).test(person));
        assertTrue(new EmailContainsKeywordsPredicate(List.of(".com")).test(person));
        assertFalse(new EmailContainsKeywordsPredicate(List.of(".*")).test(person));
        assertFalse(new EmailContainsKeywordsPredicate(List.of("alice|bob")).test(person));
    }

    @Test
    public void test_turkishDefaultLocale_matchesWithRootLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            EmailContainsKeywordsPredicate predicate = new EmailContainsKeywordsPredicate(List.of("INFO"));
            assertTrue(predicate.test(new PersonBuilder().withEmail("info@EXAMPLE.COM").build()));
            assertTrue(new EmailContainsKeywordsPredicate(List.of("info"))
                    .test(new PersonBuilder().withEmail("INFO@example.com").build()));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void test_keywordsOnlyMatchOtherFields_returnsFalse() {
        Person person = new PersonBuilder().withName("Needle Member").withPhone("91234567")
                .withEmail("mailbox@example.com").withAddress("Needle Road").withTags("needle").build();
        EmailContainsKeywordsPredicate predicate =
                new EmailContainsKeywordsPredicate(List.of("needle", "91234567", "Road"));
        assertFalse(predicate.test(person));
    }

    @Test
    public void test_emptyOrUnmatchedKeywords_returnsFalse() {
        Person person = new PersonBuilder().withEmail("alice@example.com").build();
        assertFalse(new EmailContainsKeywordsPredicate(List.of()).test(person));
        assertFalse(new EmailContainsKeywordsPredicate(List.of("alicia", "example.org")).test(person));
    }

    @Test
    public void equals_sameAndDifferentKeywords_returnsCorrectResult() {
        EmailContainsKeywordsPredicate predicate = new EmailContainsKeywordsPredicate(List.of("alice", "@example"));
        EmailContainsKeywordsPredicate copy = new EmailContainsKeywordsPredicate(List.of("alice", "@example"));
        assertEquals(predicate, predicate);
        assertEquals(predicate, copy);
        assertEquals(predicate.hashCode(), copy.hashCode());
        assertFalse(predicate.equals(new EmailContainsKeywordsPredicate(List.of("bob"))));
        assertFalse(predicate.equals(new NameContainsKeywordsPredicate(List.of("alice", "@example"))));
        assertFalse(predicate.equals(null));
    }
}

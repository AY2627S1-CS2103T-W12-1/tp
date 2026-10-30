package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.Duration;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Tests email syntax and responsiveness for large inputs. */
public class EmailTest {

    @Test
    public void isValidEmail_shortHyphenatedDomain_returnsTrue() {
        assertTrue(Email.isValidEmail("alice@b-c"));
    }

    @Test
    public void isValidEmail_longInvalidDomain_finishesPromptly() {
        String email = "alice@" + "a".repeat(20000) + "!";
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertFalse(Email.isValidEmail(email)));
    }

    @Test
    public void isValidEmail_longSeparatedParts_finishesPromptly() {
        String local = "a.".repeat(10000) + "a";
        String hyphenatedDomain = "a-".repeat(10000) + "aa";
        String dottedDomain = "a.".repeat(10000) + "aa";
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertTrue(Email.isValidEmail(local + "@example.com"));
            assertTrue(Email.isValidEmail("alice@" + hyphenatedDomain));
            assertTrue(Email.isValidEmail("alice@" + dottedDomain));
            assertFalse(Email.isValidEmail(local + ".@example.com"));
            assertFalse(Email.isValidEmail("alice@" + hyphenatedDomain + "!"));
            assertFalse(Email.isValidEmail("alice@" + dottedDomain + "!"));
        });
    }

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    public void constructor_invalidEmail_throwsIllegalArgumentException() {
        String invalidEmail = "";
        assertThrows(IllegalArgumentException.class, () -> new Email(invalidEmail));
    }

    @Test
    public void isValidEmail() {
        // null email
        assertThrows(NullPointerException.class, () -> Email.isValidEmail(null));

        // blank email
        assertFalse(Email.isValidEmail("")); // empty string
        assertFalse(Email.isValidEmail(" ")); // spaces only

        // missing parts
        assertFalse(Email.isValidEmail("@example.com")); // missing local part
        assertFalse(Email.isValidEmail("peterjackexample.com")); // missing '@' symbol
        assertFalse(Email.isValidEmail("peterjack@")); // missing domain name

        // invalid parts
        assertFalse(Email.isValidEmail("peterjack@-")); // invalid domain name
        assertFalse(Email.isValidEmail("peterjack@exam_ple.com")); // underscore in domain name
        assertFalse(Email.isValidEmail("peter jack@example.com")); // spaces in local part
        assertFalse(Email.isValidEmail("peterjack@exam ple.com")); // spaces in domain name
        assertFalse(Email.isValidEmail(" peterjack@example.com")); // leading space
        assertFalse(Email.isValidEmail("peterjack@example.com ")); // trailing space
        assertFalse(Email.isValidEmail("peterjack@@example.com")); // double '@' symbol
        assertFalse(Email.isValidEmail("peter@jack@example.com")); // '@' symbol in local part
        assertTrue(Email.isValidEmail("-peterjack@example.com"));
        assertTrue(Email.isValidEmail("peterjack-@example.com"));
        assertFalse(Email.isValidEmail("peter..jack@example.com")); // local part has two consecutive periods
        assertFalse(Email.isValidEmail("peterjack@example@com")); // '@' symbol in domain name
        assertFalse(Email.isValidEmail("peterjack@.example.com")); // domain name starts with a period
        assertFalse(Email.isValidEmail("peterjack@example.com.")); // domain name ends with a period
        assertFalse(Email.isValidEmail("peterjack@-example.com")); // domain name starts with a hyphen
        assertFalse(Email.isValidEmail("peterjack@example.com-")); // domain name ends with a hyphen
        assertFalse(Email.isValidEmail("peterjack@example.c")); // top level domain has less than two chars

        // valid email
        assertTrue(Email.isValidEmail("PeterJack_1190@example.com")); // underscore in local part
        assertTrue(Email.isValidEmail("PeterJack.1190@example.com")); // period in local part
        assertTrue(Email.isValidEmail("PeterJack+1190@example.com")); // '+' symbol in local part
        assertTrue(Email.isValidEmail("PeterJack-1190@example.com")); // hyphen in local part
        assertTrue(Email.isValidEmail("a@bc")); // minimal
        assertTrue(Email.isValidEmail("test@localhost")); // alphabets only
        assertTrue(Email.isValidEmail("123@145")); // numeric local part and domain name
        assertTrue(Email.isValidEmail("a1+be.d@example1.com")); // mixture of alphanumeric and special characters
        assertTrue(Email.isValidEmail("peter_jack@very-very-very-long-example.com")); // long domain name
        assertTrue(Email.isValidEmail("if.you.dream.it_you.can.do.it@example.com")); // long local part
        assertTrue(Email.isValidEmail("e1234567@u.nus.edu")); // more than one period in domain
    }

    @Test
    public void isValidEmail_separatorAndAsciiBoundaries_matchDocumentedRules() {
        for (String separator : new String[] {"+", "_", ".", "'", "-"}) {
            assertTrue(Email.isValidEmail("alice" + separator + "tan@example.com"));
            for (String secondSeparator : new String[] {"+", "_", ".", "'", "-"}) {
                assertTrue(Email.isValidEmail("alice" + separator + secondSeparator + "tan@example.com")
                        == !(separator.equals(".") && secondSeparator.equals(".")));
            }
        }
        assertTrue(Email.isValidEmail("'alice@example.com"));
        assertTrue(Email.isValidEmail("alice'@example.com"));
        assertFalse(Email.isValidEmail("alice@o'connor.com"));
        assertTrue(Email.isValidEmail("alice@a-b.co"));
        assertTrue(Email.isValidEmail("alice@a--b.co"));
        assertFalse(Email.isValidEmail("alice@a..co"));
        assertTrue(Email.isValidEmail("alice__tan@xn--bcher-kva.example"));
        assertTrue(Email.isValidEmail("alice._tan@example.com"));
        assertTrue(Email.isValidEmail("_alice@example.com"));
        assertFalse(Email.isValidEmail(".alice@example.com"));
        assertFalse(Email.isValidEmail("alice.@example.com"));
        assertFalse(Email.isValidEmail("alice@xn--.example"));
        assertFalse(Email.isValidEmail("álîce@example.com"));
        assertFalse(Email.isValidEmail("alice@éxample.com"));
        assertFalse(Email.isValidEmail("alice@１２.com"));
    }

    @Test
    public void isValidEmail_randomUnicode_isRejectedReproducibly() {
        long seed = 0x359_356_10_000L;
        Random random = new Random(seed);
        for (int i = 0; i < 10_000; i++) {
            int codePoint;
            do {
                codePoint = 0x80 + random.nextInt(Character.MAX_CODE_POINT - 0x7f);
            } while (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE);
            String unicode = new String(Character.toChars(codePoint));
            String email = random.nextBoolean()
                    ? "alice" + unicode + "@example.com" : "alice@example" + unicode + ".com";
            assertFalse(Email.isValidEmail(email), "seed=" + seed + ", case=" + i + ", input=" + email);
        }
    }

    @Test
    public void equals() {
        Email email = new Email("valid@email");

        // same values -> returns true
        assertTrue(email.equals(new Email("valid@email")));

        // same object -> returns true
        assertTrue(email.equals(email));

        // null -> returns false
        assertFalse(email.equals(null));

        // different types -> returns false
        assertFalse(email.equals(5.0f));

        // different values -> returns false
        assertFalse(email.equals(new Email("other.valid@email")));
    }
    @Test
    public void isValidEmail_expandedBoundariesAndLongHyphens() {
        for (String punctuation : new String[] {"+", "_", "'", "-"}) {
            assertTrue(Email.isValidEmail(punctuation + "alice@example.com"));
            assertTrue(Email.isValidEmail("alice" + punctuation + "@example.com"));
            assertTrue(Email.isValidEmail("alice." + punctuation + "tan@example.com"));
        }
        assertTrue(Email.isValidEmail("alice@example.xn--p1ai"));
        assertFalse(Email.isValidEmail("alice@-xn--p1ai.example"));
        assertFalse(Email.isValidEmail("alice@xn--p1ai-.example"));
        assertFalse(Email.isValidEmail("alice@example..xn--p1ai"));
        String label = "xn" + "-".repeat(20000) + "p1ai";
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertTrue(Email.isValidEmail("alice@example." + label));
            assertFalse(Email.isValidEmail("alice@example." + label + "-"));
        });
    }
}

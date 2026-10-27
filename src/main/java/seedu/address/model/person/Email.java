package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's email in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    public static final String MESSAGE_CONSTRAINTS = "Emails must use the format local-part@domain.\n"
            + "1. Use ASCII letters (A-Z, a-z) and digits (0-9). The local-part must start and end with a letter "
            + "or digit; single + _ . apostrophe or - separators may appear between letters or digits. "
            + "Consecutive separators are not allowed.\n"
            + "2. Separate domain labels with single periods. Each label must start and end with a letter or digit; "
            + "single hyphens may appear between letters or digits. Consecutive hyphens are not allowed.\n"
            + "3. The final domain label must contain at least 2 characters.";
    private static final String SPECIAL_CHARACTERS = "+_.'-";
    // Possessive repetition avoids backtracking and stack growth on long inputs. Each group starts
    // with a separator absent from the following alphanumeric run, so no valid match needs backtracking.
    private static final String ALPHANUMERIC_NO_UNDERSCORE = "[^\\W_]++"; // alphanumeric characters except underscore
    private static final String LOCAL_PART_REGEX = "^" + ALPHANUMERIC_NO_UNDERSCORE + "(?:[" + SPECIAL_CHARACTERS + "]"
            + ALPHANUMERIC_NO_UNDERSCORE + ")*+";
    private static final String DOMAIN_PART_REGEX = ALPHANUMERIC_NO_UNDERSCORE
            + "(?:-" + ALPHANUMERIC_NO_UNDERSCORE + ")*+";
    // Check the label length separately to avoid nested repetition and excessive backtracking.
    private static final String DOMAIN_LAST_PART_REGEX = "(?=[^.]{2,}$)" + DOMAIN_PART_REGEX + "$";
    private static final String DOMAIN_REGEX = "(?:" + DOMAIN_PART_REGEX + "\\.)*+" + DOMAIN_LAST_PART_REGEX;
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@" + DOMAIN_REGEX;

    public final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = email;
    }

    /**
     * Returns true if a given string is a valid email.
     */
    public static boolean isValidEmail(String test) {
        return test.matches(VALIDATION_REGEX);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

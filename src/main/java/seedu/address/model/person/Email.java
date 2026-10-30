package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's email in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    public static final String MESSAGE_CONSTRAINTS = "Emails must use the format local-part@domain.\n"
            + "1. The local part uses ASCII letters, digits, +, _, apostrophe or -, with single periods "
            + "between nonempty parts.\n"
            + "2. Domain labels use ASCII letters, digits and internal hyphens, separated by single periods.\n"
            + "3. The final domain label must contain at least 2 characters.";
    // Possessive runs prevent backtracking and stack growth on long inputs.
    private static final String LOCAL_ATOM = "[A-Za-z0-9+_'-]++";
    private static final String LOCAL_PART_REGEX = "^" + LOCAL_ATOM + "(?:\\." + LOCAL_ATOM + ")*+";
    private static final String DOMAIN_PART_REGEX = "[A-Za-z0-9]++(?:-++[A-Za-z0-9]++)*+";
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

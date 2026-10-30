package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.regex.Pattern;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Names must start with a letter or ASCII digit and contain only Unicode letters, combining marks, "
                    + "ASCII digits, spaces, periods, slashes, hyphens and apostrophes";
    public static final String VALIDATION_REGEX = "[\\p{L}0-9][\\p{L}\\p{M}0-9 ./'\u2019-]*";

    /** Shared spacing rule for new-contact duplicate matching and stored-roster sorting. */
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

    public final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = name;
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        return test.matches(VALIDATION_REGEX);
    }


    /** Returns a spacing-normalized comparison value without changing spelling or letter case. */
    public String getComparisonKey() {
        return WHITESPACE.matcher(fullName).replaceAll(" ").trim();
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}

package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's address in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidAddress(String)}.
 * An empty value represents an address that was not provided.
 */
public class Address {

    public static final String MESSAGE_CONSTRAINTS = "Addresses should not be blank when provided";

    /*
     * A non-empty address must not start with whitespace.
     */
    public static final String VALIDATION_REGEX = "[^\\s].*";

    public static final Address EMPTY_ADDRESS = new Address("");

    public final String value;

    /**
     * Constructs an {@code Address}.
     *
     * @param address A valid address, or an empty string when no address was provided.
     */
    public Address(String address) {
        requireNonNull(address);
        checkArgument(isValidAddress(address), MESSAGE_CONSTRAINTS);
        value = address;
    }

    /**
     * Returns true if a given string is a valid address.
     */
    public static boolean isValidAddress(String test) {
        return test.isEmpty() || test.matches(VALIDATION_REGEX);
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
        if (!(other instanceof Address otherAddress)) {
            return false;
        }

        return value.equals(otherAddress.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

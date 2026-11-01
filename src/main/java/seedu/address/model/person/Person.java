package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: details are present and not null, field values are validated, immutable.
 */
public class Person {

    // Identity fields
    private final Name name;
    private final Phone phone;
    private final Email email;

    // Data fields
    private final Address address;
    private final Set<Tag> tags = new HashSet<>();

    /**
     * Every field must be present and not null.
     */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        requireAllNonNull(name, phone, email, address, tags);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.tags.addAll(tags);
    }

    public Name getName() {
        return name;
    }

    public Phone getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if both persons have the same contact fields.
     * Tags do not form part of a person's identity.
     * Surrounding whitespace is ignored; case and internal spacing remain significant.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getName().fullName.trim().equals(getName().fullName.trim())
                && otherPerson.getPhone().value.trim().equals(getPhone().value.trim())
                && otherPerson.getEmail().value.trim().equals(getEmail().value.trim())
                && otherPerson.getAddress().value.trim().equals(getAddress().value.trim());
    }

    /**
     * Checks duplicate contact input without changing stored names or the legacy storage identity rule.
     * Name case and repeated whitespace are ignored; phone spacing and email-domain case are ignored.
     * Other contact fields are trimmed and compared exactly.
     * Tags are ignored. Existing JSON records continue to use {@link #isSamePerson(Person)} when loading.
     */
    public boolean isDuplicateContactOf(Person otherPerson) {
        return otherPerson != null
                && name.getComparisonKey().equalsIgnoreCase(otherPerson.getName().getComparisonKey())
                && normalizePhone(phone.value).equals(normalizePhone(otherPerson.getPhone().value))
                && normalizeEmail(email.value).equals(normalizeEmail(otherPerson.getEmail().value))
                && address.value.trim().equals(otherPerson.getAddress().value.trim());
    }

    /** Removes ASCII spaces while retaining a phone number's optional leading plus. */
    private static String normalizePhone(String value) {
        return value.trim().replace(" ", "");
    }

    /** Compares the email local part exactly and folds only the domain's ASCII case. */
    private static String normalizeEmail(String value) {
        String trimmed = value.trim();
        int separator = trimmed.lastIndexOf('@');
        String localPart = trimmed.substring(0, separator);
        String domain = trimmed.substring(separator + 1).toLowerCase(Locale.ROOT);
        return localPart + "@" + domain;
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && phone.equals(otherPerson.phone)
                && email.equals(otherPerson.email)
                && address.equals(otherPerson.address)
                && tags.equals(otherPerson.tags);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, address, tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("address", address)
                .add("tags", tags)
                .toString();
    }

}

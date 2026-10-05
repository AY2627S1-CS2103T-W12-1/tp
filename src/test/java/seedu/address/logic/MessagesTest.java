package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Checks that command feedback preserves complete member details in readable rows.
 */
public class MessagesTest {

    @Test
    public void format_multipleTags_displaysLabelledRowsInStableOrder() {
        Person member = new PersonBuilder().withName("Alice Tan").withPhone("91234567")
                .withEmail("alice@example.com").withAddress("10 Clementi Road, Unit 123")
                .withTags("year1", "committee").build();
        assertEquals("Name: Alice Tan\nPhone: 91234567\nEmail: alice@example.com"
                + "\nAddress: 10 Clementi Road, Unit 123\nTags: committee, year1", Messages.format(member));
    }

    @Test
    public void format_noTagsAndLongAddress_preservesDetailsWithoutEllipsis() {
        String address = "A long address " + "with additional directions ".repeat(40);
        Person member = new PersonBuilder().withName("Alice Tan").withPhone("91234567")
                .withEmail("alice@example.com").withAddress(address).withTags().build();
        assertEquals("Name: Alice Tan\nPhone: 91234567\nEmail: alice@example.com"
                + "\nAddress: " + address + "\nTags: (none)", Messages.format(member));
    }
}

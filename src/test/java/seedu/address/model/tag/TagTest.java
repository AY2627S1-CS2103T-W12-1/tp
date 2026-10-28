package seedu.address.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void equalityAndHashCode_ignoreLetterCase() {
        Tag tag = new Tag("committee");
        assertTrue(tag.equals(tag));
        assertEquals(tag, new Tag("committee"));
        assertEquals(tag.hashCode(), new Tag("committee").hashCode());
        assertEquals(tag, new Tag("Committee"));
        assertEquals(tag, new Tag("COMMITTEE"));
        assertEquals(tag.hashCode(), new Tag("Committee").hashCode());
        assertEquals("committee", tag.tagName);
        assertEquals("Committee", new Tag("Committee").tagName);
        assertFalse(tag.equals(new Tag("committees")));
        assertFalse(tag.equals(null));
        assertFalse(tag.equals("committee"));
    }

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        String invalidTagName = "";
        assertThrows(IllegalArgumentException.class, () -> new Tag(invalidTagName));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));

        assertFalse(Tag.isValidTagName(""));
        assertFalse(Tag.isValidTagName("a".repeat(31)));
        assertFalse(Tag.isValidTagName("-committee"));
        assertFalse(Tag.isValidTagName("committee-"));
        assertFalse(Tag.isValidTagName("committee--member"));
        assertFalse(Tag.isValidTagName("committee member"));
        assertFalse(Tag.isValidTagName("committee/member"));
        assertFalse(Tag.isValidTagName("café"));

        assertTrue(Tag.isValidTagName("a"));
        assertTrue(Tag.isValidTagName("Year1"));
        assertTrue(Tag.isValidTagName("year-1"));
        assertTrue(Tag.isValidTagName("committee-member"));
        assertTrue(Tag.isValidTagName("a".repeat(30)));
        assertTrue(Tag.isValidTagName("a".repeat(28) + "-1"));
    }

}

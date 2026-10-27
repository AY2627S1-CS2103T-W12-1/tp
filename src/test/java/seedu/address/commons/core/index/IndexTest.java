package seedu.address.commons.core.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Tests index conversion boundaries and equality without accepting invalid input through overflow. */
public class IndexTest {

    @Test
    public void fromOneBased_minimumInteger_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromOneBased(Integer.MIN_VALUE));
    }

    @Test
    public void fromZeroBased_maximumInteger_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromZeroBased(Integer.MAX_VALUE));
    }

    @Test
    public void conversion_largestRepresentableIndex_preservesBothBases() {
        Index largest = Index.fromOneBased(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE - 1, largest.getZeroBased());
        assertEquals(Integer.MAX_VALUE, largest.getOneBased());
        assertEquals(largest, Index.fromZeroBased(Integer.MAX_VALUE - 1));
    }

    @Test
    public void createOneBasedIndex() {
        // invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromOneBased(0));

        // check equality using the same base
        assertEquals(1, Index.fromOneBased(1).getOneBased());
        assertEquals(5, Index.fromOneBased(5).getOneBased());

        // convert from one-based index to zero-based index
        assertEquals(0, Index.fromOneBased(1).getZeroBased());
        assertEquals(4, Index.fromOneBased(5).getZeroBased());
    }

    @Test
    public void createZeroBasedIndex() {
        // invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromZeroBased(-1));

        // check equality using the same base
        assertEquals(0, Index.fromZeroBased(0).getZeroBased());
        assertEquals(5, Index.fromZeroBased(5).getZeroBased());

        // convert from zero-based index to one-based index
        assertEquals(1, Index.fromZeroBased(0).getOneBased());
        assertEquals(6, Index.fromZeroBased(5).getOneBased());
    }

    @Test
    public void equals() {
        final Index fifthPersonIndex = Index.fromOneBased(5);

        // same values -> returns true
        assertTrue(fifthPersonIndex.equals(Index.fromOneBased(5)));
        assertTrue(fifthPersonIndex.equals(Index.fromZeroBased(4)));

        // same object -> returns true
        assertTrue(fifthPersonIndex.equals(fifthPersonIndex));

        // null -> returns false
        assertFalse(fifthPersonIndex.equals(null));

        // different types -> returns false
        assertFalse(fifthPersonIndex.equals(5.0f));

        // different index -> returns false
        assertFalse(fifthPersonIndex.equals(Index.fromOneBased(1)));
    }

    @Test
    public void hashCode_equivalentBases_retrieveTheSameMapEntry() {
        Map<Index, String> entries = new HashMap<>();
        entries.put(Index.fromOneBased(1), "first");
        entries.put(Index.fromOneBased(Integer.MAX_VALUE), "last");
        assertEquals("first", entries.get(Index.fromZeroBased(0)));
        assertEquals("last", entries.get(Index.fromZeroBased(Integer.MAX_VALUE - 1)));
        assertEquals("first", entries.put(Index.fromZeroBased(0), "updated"));
        assertEquals(2, entries.size());
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromZeroBased(0);
        String expected = Index.class.getCanonicalName() + "{zeroBasedIndex=" + index.getZeroBased() + "}";
        assertEquals(expected, index.toString());
    }
}

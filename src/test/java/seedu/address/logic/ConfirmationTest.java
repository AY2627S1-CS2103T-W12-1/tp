package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ConfirmationTest {

    @Test
    public void isNearMiss_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Confirmation.isNearMiss(null));
    }

    @Test
    public void isNearMiss_shortFormOrOtherLetterCase_returnsTrue() {
        for (String input : new String[] {"y", "Y", "YES", "Yes", "yEs", "yeS"}) {
            assertTrue(Confirmation.isNearMiss(input), input);
        }
    }

    @Test
    public void isNearMiss_exactWordOrOtherInput_returnsFalse() {
        for (String input : new String[] {Confirmation.WORD, "", "n", "no", "ye", "yess", "yes please", "y es"}) {
            assertFalse(Confirmation.isNearMiss(input), input);
        }
    }
}

package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.GuiSettings;

public class UserPrefsTest {

    @Test
    public void copyAndEquality_preserveSettingsWithoutSharingMutablePreferences() {
        UserPrefs original = new UserPrefs();
        UserPrefs copy = new UserPrefs(original);
        assertTrue(original.equals(original));
        assertFalse(original.equals(null));
        assertFalse(original.equals("preferences"));
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        copy.setGuiSettings(new GuiSettings(900, 650, 20, 30));
        assertFalse(original.equals(copy));
        assertEquals(new GuiSettings(), original.getGuiSettings());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        UserPrefs userPref = new UserPrefs();
        assertThrows(NullPointerException.class, () -> userPref.setGuiSettings(null));
    }

}

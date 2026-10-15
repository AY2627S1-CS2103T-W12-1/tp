package seedu.address.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class GuiSettingsTest {
    @Test
    public void equalityAndHashCode_compareDimensionsAndPosition() {
        GuiSettings settings = new GuiSettings(900, 650, 20, 30);
        assertEquals(settings, new GuiSettings(900, 650, 20, 30));
        assertEquals(settings.hashCode(), new GuiSettings(900, 650, 20, 30).hashCode());
        assertFalse(settings.equals(null));
        assertFalse(settings.equals("settings"));
        assertFalse(settings.equals(new GuiSettings(901, 650, 20, 30)));
        assertFalse(settings.equals(new GuiSettings(900, 651, 20, 30)));
        assertFalse(settings.equals(new GuiSettings(900, 650, 21, 30)));
    }

    @Test
    public void toStringMethod() {
        GuiSettings guiSettings = new GuiSettings();
        String expected = GuiSettings.class.getCanonicalName() + "{windowWidth=" + guiSettings.getWindowWidth()
                + ", windowHeight=" + guiSettings.getWindowHeight() + ", windowCoordinates="
                + guiSettings.getWindowCoordinates() + "}";
        assertEquals(expected, guiSettings.toString());
    }
}

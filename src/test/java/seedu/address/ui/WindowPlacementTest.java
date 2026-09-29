package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.geometry.Rectangle2D;
import seedu.address.commons.core.GuiSettings;

/**
 * Tests display-boundary recovery without starting the JavaFX application thread.
 */
public class WindowPlacementTest {

    private static final Rectangle2D PRIMARY = new Rectangle2D(0, 24, 1920, 1056);

    @Test
    public void restore_visibleWindow_preservesPlacement() {
        GuiSettings saved = new GuiSettings(800, 600, 120, 80);
        assertEquals(new Rectangle2D(120, 80, 800, 600), restore(saved, List.of(PRIMARY)));
    }

    @Test
    public void restore_disconnectedDisplay_centersOnPrimary() {
        GuiSettings saved = new GuiSettings(800, 600, 2200, 100);
        assertEquals(new Rectangle2D(560, 252, 800, 600), restore(saved, List.of(PRIMARY)));
    }

    @Test
    public void restore_negativeCoordinateDisplay_preservesPlacement() {
        Rectangle2D leftDisplay = new Rectangle2D(-1440, 0, 1440, 900);
        GuiSettings saved = new GuiSettings(800, 600, -1300, 100);
        assertEquals(new Rectangle2D(-1300, 100, 800, 600), restore(saved, List.of(PRIMARY, leftDisplay)));
    }

    @Test
    public void restore_partialOverlap_keepsWindowWithinVisualBounds() {
        GuiSettings saved = new GuiSettings(800, 600, -100, -50);
        assertEquals(new Rectangle2D(0, 24, 800, 600), restore(saved, List.of(PRIMARY)));
    }

    @Test
    public void restore_oversizedWindow_fitsAvailableDisplay() {
        GuiSettings saved = new GuiSettings(2400, 1400, 100, 100);
        assertEquals(PRIMARY, restore(saved, List.of(PRIMARY)));
    }

    @Test
    public void restore_invalidDimensions_usesDefaultSize() {
        GuiSettings saved = new GuiSettings(Double.NaN, Double.POSITIVE_INFINITY, 120, 80);
        assertEquals(new Rectangle2D(120, 80, 740, 600), restore(saved, List.of(PRIMARY)));
        GuiSettings negative = new GuiSettings(-1, 0, 120, 80);
        assertEquals(new Rectangle2D(120, 80, 740, 600), restore(negative, List.of(PRIMARY)));
    }

    @Test
    public void restore_displayBelowMinimumSize_fitsAvailableDisplay() {
        Rectangle2D smallDisplay = new Rectangle2D(0, 0, 400, 500);
        assertEquals(smallDisplay, restore(new GuiSettings(), List.of(smallDisplay)));
    }

    @Test
    public void restore_firstLaunch_centersDefaultWindow() {
        assertEquals(new Rectangle2D(590, 252, 740, 600), restore(new GuiSettings(), List.of(PRIMARY)));
    }

    private Rectangle2D restore(GuiSettings settings, List<Rectangle2D> screens) {
        return WindowPlacement.restore(settings, screens, 450, 600);
    }
}

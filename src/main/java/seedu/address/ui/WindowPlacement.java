package seedu.address.ui;

import static seedu.address.commons.util.AppUtil.checkArgument;

import java.awt.Point;
import java.util.List;

import javafx.geometry.Rectangle2D;
import seedu.address.commons.core.GuiSettings;

/**
 * Restores saved window geometry within the currently available displays, without depending on a running UI.
 */
public final class WindowPlacement {

    private WindowPlacement() {}

    /**
     * Keeps a valid saved placement, or fits it to the display containing most of the saved window.
     * The first display is the fallback when the saved display has been disconnected.
     *
     * @param settings Previously saved window size and optional position.
     * @param screens Available screen visual bounds, with the primary display first.
     * @param minimumWidth Preferred minimum window width, reduced only if the display is smaller.
     * @param minimumHeight Preferred minimum window height, reduced only if the display is smaller.
     * @return A rectangle fully contained in one available display's usable area.
     */
    public static Rectangle2D restore(GuiSettings settings, List<Rectangle2D> screens,
            double minimumWidth, double minimumHeight) {
        checkArgument(!screens.isEmpty(), "At least one screen is required.");
        GuiSettings defaults = new GuiSettings();
        double width = validSize(settings.getWindowWidth(), defaults.getWindowWidth());
        double height = validSize(settings.getWindowHeight(), defaults.getWindowHeight());
        Point position = settings.getWindowCoordinates();
        Rectangle2D selected = screens.get(0);
        double largestOverlap = 0;
        if (position != null) {
            Rectangle2D saved = new Rectangle2D(position.getX(), position.getY(), width, height);
            for (Rectangle2D screen : screens) {
                double overlap = overlapArea(saved, screen);
                if (overlap > largestOverlap) {
                    selected = screen;
                    largestOverlap = overlap;
                }
            }
        }

        width = Math.min(Math.max(width, minimumWidth), selected.getWidth());
        height = Math.min(Math.max(height, minimumHeight), selected.getHeight());
        double x = selected.getMinX() + (selected.getWidth() - width) / 2;
        double y = selected.getMinY() + (selected.getHeight() - height) / 2;
        if (position != null && largestOverlap > 0) {
            x = clamp(position.getX(), selected.getMinX(), selected.getMaxX() - width);
            y = clamp(position.getY(), selected.getMinY(), selected.getMaxY() - height);
        }
        return new Rectangle2D(x, y, width, height);
    }

    private static double validSize(double saved, double fallback) {
        return Double.isFinite(saved) && saved > 0 ? saved : fallback;
    }

    private static double overlapArea(Rectangle2D first, Rectangle2D second) {
        double width = Math.max(0, Math.min(first.getMaxX(), second.getMaxX())
                - Math.max(first.getMinX(), second.getMinX()));
        double height = Math.max(0, Math.min(first.getMaxY(), second.getMaxY())
                - Math.max(first.getMinY(), second.getMinY()));
        return width * height;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}

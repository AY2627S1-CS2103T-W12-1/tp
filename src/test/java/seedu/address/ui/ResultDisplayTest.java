package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.TextArea;
import seedu.address.testutil.JavaFxTestUtil;

/** Verifies that long command feedback wraps and new results start at the beginning. */
public class ResultDisplayTest {
    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void feedback_newResult_preservesAllTextAndResetsScroll() throws Exception {
        onJavaFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            TextArea area = (TextArea) display.getRoot().lookup("#resultDisplay");
            assertTrue(area.getText().contains("Type help"));
            assertTrue(area.isWrapText());
            assertFalse(area.isEditable());
            display.setFeedbackToUser("Long line\n".repeat(100));
            area.positionCaret(area.getLength());
            area.setScrollTop(500);
            String next = "Name: Alice Tan\nAddress: " + "Long address ".repeat(50);
            display.setFeedbackToUser(next);
            assertEquals(next, area.getText());
            assertEquals(0, area.getCaretPosition());
            assertEquals(0, area.getScrollTop());
        });
    }
}

package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** Checks that selected member tags remain readable when the details column is narrow. */
public class MemberDetailsPanelTest {

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void showPerson_nullPerson_throwsNullPointerException() throws Exception {
        onJavaFxThread(() -> {
            MemberDetailsPanel panel = new MemberDetailsPanel();
            assertThrows(NullPointerException.class, () -> panel.showPerson(null, 1));
            // A rejected call must not reveal any details.
            assertFalse(panel.getRoot().lookup("#detailsScroll").isVisible());
        });
    }

    @Test
    public void showPerson_nameWithCombiningMark_showsComposedName() throws Exception {
        onJavaFxThread(() -> {
            MemberDetailsPanel panel = new MemberDetailsPanel();
            panel.showPerson(new PersonBuilder().withName("Aléx").build(), 1);
            // The ScrollPane content is only reachable by lookup once the panel is in a styled scene.
            StackPane container = new StackPane(panel.getRoot());
            new Scene(container, 430, 260);
            container.applyCss();
            container.layout();
            assertEquals("Aléx", ((Label) panel.getRoot().lookup("#name")).getText());
        });
    }

    @Test
    public void selectedMember_longestWideTag_wrapsWithinNarrowAndResizedDetails() throws Exception {
        onJavaFxThread(() -> {
            String tagName = "W".repeat(30);
            MemberDetailsPanel panel = new MemberDetailsPanel();
            panel.showPerson(new PersonBuilder().withTags(tagName).build(), 1);
            Region root = panel.getRoot();
            StackPane container = new StackPane(root);
            Scene scene = new Scene(container, 430, 260);
            scene.getStylesheets().add(getClass().getResource("/view/LightTheme.css").toExternalForm());
            for (double width : new double[]{430, 300, 190}) {
                container.resize(width, 260);
                container.applyCss();
                container.layout();
                ScrollPane scroll = (ScrollPane) root.lookup("#detailsScroll");
                scroll.setVvalue(1);
                // Refresh the viewport offset after the selected tag reflows at its new width.
                scroll.requestLayout();
                scroll.layout();
                Label tag = (Label) ((FlowPane) root.lookup("#tags")).getChildren().getFirst();
                Bounds bounds = tag.localToScene(tag.getBoundsInLocal());
                Bounds viewport = scroll.lookup(".viewport").localToScene(
                        scroll.lookup(".viewport").getBoundsInLocal());
                assertEquals(tagName, tag.getText());
                Text renderedText = (Text) tag.lookup(".text");
                assertEquals(tagName, renderedText.getText());
                assertTrue(tag.getHeight() >= renderedText.getBoundsInLocal().getHeight());
                assertEquals(tagName, tag.getTooltip().getText());
                assertTrue(bounds.getMinX() >= viewport.getMinX() - 1);
                assertTrue(bounds.getMaxX() <= viewport.getMaxX() + 1, "Tag must fit in the details column");
                if (width <= 300) {
                    assertTrue(tag.getHeight() > 2 * tag.getFont().getSize(), "The long tag should wrap");
                }
            }
            panel.clear();
            assertTrue(((FlowPane) root.lookup("#tags")).getChildren().isEmpty());
            assertFalse(root.lookup("#detailsScroll").isVisible());
        });
    }
}

package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.address.logic.commands.CommandHelp;
import seedu.address.testutil.JavaFxTestUtil;

/** Exercises the actual FXML guide, navigation, responsive layout, and keyboard controls. */
public class HelpWindowTest {
    private HelpWindow help;

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @BeforeEach
    public void setUp() throws Exception {
        onJavaFxThread(() -> {
            Stage stage = new Stage();
            stage.setOpacity(0);
            help = new HelpWindow(stage);
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        onJavaFxThread(help::hide);
    }

    @Test
    public void overview_allCommands_showsExamplesAndReflows() throws Exception {
        onJavaFxThread(() -> {
            help.show();
            Parent root = layout(980, 740);
            assertTrue(help.isShowing());
            assertEquals(Set.of("list", "sort", "find", "add", "edit", "delete", "clear", "help", "exit"),
                    root.lookupAll(".overview-keyword").stream()
                            .map(node -> ((Button) node).getText()).collect(Collectors.toSet()));
            assertEquals(4, root.lookupAll(".overview-category").size());
            Set<String> examples = root.lookupAll(".overview-example").stream()
                    .map(node -> ((Label) node).getText()).collect(Collectors.toSet());
            for (CommandHelp entry : CommandHelp.values()) {
                assertTrue(examples.contains(entry.getExample()), entry.getKeyword());
            }
            GridPane grid = (GridPane) ((VBox) root.lookup("#commandCards")).getChildren().getFirst();
            assertEquals(2, grid.getColumnConstraints().size());
            layout(600, 740);
            assertEquals(1, grid.getColumnConstraints().size());
            assertEquals(4, grid.getChildren().size());
            assertFalse(root.lookup("#allCommandsButton").isManaged());
        });
    }

    @Test
    public void details_eachTopic_showsCompleteContentAndReturnsToOverview() throws Exception {
        onJavaFxThread(() -> {
            for (CommandHelp entry : CommandHelp.values()) {
                help.show();
                Parent root = layout(980, 740);
                root.lookupAll(".overview-keyword").stream().map(node -> (Button) node)
                        .filter(button -> button.getText().equals(entry.getKeyword())).findFirst().orElseThrow().fire();
                root = layout(600, 740);
                assertEquals("TrackCall · Help: " + entry.getKeyword(), help.getRoot().getTitle());
                Set<String> labels = root.lookupAll(".label").stream().map(node -> ((Label) node).getText())
                        .collect(Collectors.toSet());
                String[] content = {entry.getCategory(), entry.getTitle(), entry.getSyntax(),
                    entry.getExample(), entry.getPurpose(), entry.getExpectedResult(), entry.getNotes(),
                    entry.getErrors()};
                for (String text : content) {
                    assertTrue(labels.contains(text), entry.getKeyword() + ": " + text);
                }
                assertTrue(root.lookup("#allCommandsButton").isVisible());
                Button overview = (Button) root.lookup("#allCommandsButton");
                overview.fire();
                assertEquals(CommandHelp.values().length, layout(980, 740).lookupAll(".overview-keyword").size());
            }
        });
    }

    @Test
    public void keyboard_navigationScrollsAndEscapeClosesOnlyGuide() throws Exception {
        onJavaFxThread(() -> {
            help.show("add");
            Parent root = layout(600, 440);
            ScrollPane pane = (ScrollPane) root.lookup("#commandScrollPane");
            press(KeyCode.END);
            assertEquals(1, pane.getVvalue());
            press(KeyCode.UP);
            assertTrue(pane.getVvalue() < 1);
            press(KeyCode.HOME);
            assertEquals(0, pane.getVvalue());
            press(KeyCode.DOWN);
            assertTrue(pane.getVvalue() > 0);
            press(KeyCode.PAGE_DOWN);
            double afterDown = pane.getVvalue();
            press(KeyCode.PAGE_UP);
            assertTrue(pane.getVvalue() < afterDown);
            double beforeOtherKey = pane.getVvalue();
            press(KeyCode.A);
            assertEquals(beforeOtherKey, pane.getVvalue());
            press(KeyCode.ESCAPE);
            assertFalse(help.isShowing());
            help.show();
            assertTrue(help.isShowing());
        });
    }

    /** Applies CSS and layout at a deterministic size without relying on native window-manager timings. */
    private Parent layout(double width, double height) {
        Region root = (Region) help.getRoot().getScene().getRoot();
        root.resize(width, height);
        root.applyCss();
        root.layout();
        return root;
    }

    private void press(KeyCode code) {
        Event.fireEvent(help.getRoot().getScene(),
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }
}

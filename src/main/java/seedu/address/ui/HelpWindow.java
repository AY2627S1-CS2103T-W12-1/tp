package seedu.address.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.address.logic.commands.CommandHelp;

/**
 * Resizable, keyboard-accessible command guide whose content is bundled with the application.
 */
public class HelpWindow extends UiPart<Stage> {

    private static final String FXML = "HelpWindow.fxml";
    private static final double TWO_COLUMN_MINIMUM_WIDTH = 740;

    private final List<VBox> overviewCategories = new ArrayList<>();
    private GridPane overviewGrid;
    private int overviewColumns;

    @FXML
    private Label guideSubtitle;
    @FXML
    private Button allCommandsButton;
    @FXML
    private ScrollPane commandScrollPane;
    @FXML
    private VBox commandCards;

    /**
     * Creates the offline guide using the supplied window.
     */
    public HelpWindow(Stage root) {
        super(FXML, root);
        root.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        commandScrollPane.viewportBoundsProperty().addListener((observable, previous, current) ->
                layoutOverview(current.getWidth()));
    }

    /**
     * Creates the offline guide in its own window.
     */
    public HelpWindow() {
        this(new Stage());
    }

    /**
     * Shows all commands and focuses the guide's keyboard-scrollable content.
     */
    public void show() {
        show("");
    }

    /**
     * Shows the selected command, or all commands when the topic is empty.
     */
    public void show(String topic) {
        populateCards(topic);
        if (!getRoot().isShowing()) {
            getRoot().show();
            getRoot().centerOnScreen();
        }
        focus();
        Platform.runLater(() -> {
            commandScrollPane.setVvalue(0);
            commandScrollPane.requestFocus();
        });
    }

    /**
     * Returns whether the guide is visible.
     */
    public boolean isShowing() {
        return getRoot().isShowing();
    }

    /**
     * Hides the guide without closing the main application.
     */
    @FXML
    public void hide() {
        getRoot().hide();
    }

    /**
     * Restores a minimized guide and brings it to the foreground.
     */
    public void focus() {
        getRoot().setIconified(false);
        getRoot().toFront();
        getRoot().requestFocus();
    }

    @FXML
    private void showAllCommands() {
        show();
    }

    private void populateCards(String topic) {
        commandCards.getChildren().clear();
        overviewGrid = null;
        overviewCategories.clear();
        boolean showAll = topic.isEmpty();
        allCommandsButton.setVisible(!showAll);
        allCommandsButton.setManaged(!showAll);
        guideSubtitle.setText(showAll ? "Choose a command, or type help COMMAND, for syntax and details."
                : "A closer look at " + topic + ". Use All commands to return to the complete guide.");
        getRoot().setTitle(showAll ? "TrackCall · Command guide" : "TrackCall · Help: " + topic);
        if (showAll) {
            createOverview();
        } else {
            CommandHelp entry = CommandHelp.find(topic).orElseThrow();
            commandCards.getChildren().addAll(label(entry.getCategory(), "category-title"), createCard(entry));
        }
    }

    /**
     * Groups the available commands into four compact, keyboard-accessible category cards.
     */
    private void createOverview() {
        overviewGrid = new GridPane();
        overviewGrid.setHgap(14);
        overviewGrid.setVgap(14);
        overviewColumns = 0;
        String previousCategory = "";
        VBox category = null;
        for (CommandHelp entry : CommandHelp.values()) {
            if (!entry.getCategory().equals(previousCategory)) {
                category = new VBox(14);
                category.getStyleClass().add("overview-category");
                category.getChildren().add(label(entry.getCategory(), "overview-category-title"));
                overviewCategories.add(category);
                previousCategory = entry.getCategory();
            }
            category.getChildren().add(createOverviewRow(entry));
        }
        commandCards.getChildren().add(overviewGrid);
        layoutOverview(commandScrollPane.getViewportBounds().getWidth());
    }

    private VBox createOverviewRow(CommandHelp entry) {
        Button details = new Button(entry.getKeyword());
        details.getStyleClass().add("overview-keyword");
        details.setAccessibleText("Help for " + entry.getKeyword());
        details.setMinWidth(Region.USE_PREF_SIZE);
        details.setOnAction(event -> show(entry.getKeyword()));
        HBox heading = new HBox(10, details, label(entry.getTitle(), "overview-command-title"));
        heading.getStyleClass().add("command-heading");
        return new VBox(6, heading, label(entry.getExample(), "overview-example"));
    }

    /**
     * Reflows the overview without recreating buttons when the window crosses the width threshold.
     */
    private void layoutOverview(double viewportWidth) {
        if (overviewGrid == null) {
            return;
        }
        int columns = viewportWidth >= TWO_COLUMN_MINIMUM_WIDTH ? 2 : 1;
        if (columns == overviewColumns) {
            return;
        }
        overviewColumns = columns;
        overviewGrid.getChildren().clear();
        overviewGrid.getColumnConstraints().clear();
        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / columns);
            overviewGrid.getColumnConstraints().add(constraints);
        }
        for (int index = 0; index < overviewCategories.size(); index++) {
            overviewGrid.add(overviewCategories.get(index), index % columns, index / columns);
        }
    }

    private VBox createCard(CommandHelp entry) {
        VBox card = new VBox(10);
        card.getStyleClass().add("command-card");
        Label keyword = label(entry.getKeyword(), "command-keyword");
        Label title = label(entry.getTitle(), "command-title");
        HBox heading = new HBox(12, keyword, title);
        heading.getStyleClass().add("command-heading");
        card.getChildren().addAll(heading, label(entry.getPurpose(), "command-purpose"),
                labelledText("SYNTAX", entry.getSyntax(), "command-code"),
                labelledText("EXAMPLE", entry.getExample(), "command-code"),
                label(entry.getExpectedResult(), "example-result"),
                label(entry.getNotes(), "command-notes"),
                labelledText("IF SOMETHING GOES WRONG", entry.getErrors(), "command-notes"));
        return card;
    }

    private VBox labelledText(String caption, String content, String contentStyle) {
        return new VBox(5, label(caption, "field-caption"), label(content, contentStyle));
    }

    private Label label(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.getStyleClass().add(style);
        return label;
    }

    /**
     * Keeps scrolling available even when a toolbar button has keyboard focus.
     */
    private void handleKeyPress(KeyEvent event) {
        double scrollableHeight = Math.max(1,
                commandCards.getHeight() - commandScrollPane.getViewportBounds().getHeight());
        double line = 42 / scrollableHeight;
        double page = commandScrollPane.getViewportBounds().getHeight() * 0.85 / scrollableHeight;
        switch (event.getCode()) {
            case ESCAPE:
                hide();
                break;
            case DOWN:
                scrollBy(line);
                break;
            case UP:
                scrollBy(-line);
                break;
            case PAGE_DOWN:
                scrollBy(page);
                break;
            case PAGE_UP:
                scrollBy(-page);
                break;
            case HOME:
                commandScrollPane.setVvalue(0);
                break;
            case END:
                commandScrollPane.setVvalue(1);
                break;
            default:
                return;
        }
        event.consume();
    }

    private void scrollBy(double amount) {
        commandScrollPane.setVvalue(Math.max(0, Math.min(1, commandScrollPane.getVvalue() + amount)));
    }
}

package seedu.address.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.FilterCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ActiveFilter;
import seedu.address.model.person.Person;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";
    private static final String REMOVE_BUTTON_TEXT = "×";
    private static final double DIVIDER_POSITION = 0.22;
    private static final int FILTER_CHIP_SPACING = 6;

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private MemberDetailsPanel memberDetailsPanel;
    private CommandBox commandBox;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;

    /** The latest confirmation prompt, shown again if a filter is removed before the user answers it. */
    private String pendingConfirmationPrompt = "";

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private FlowPane activeFiltersPane;

    @FXML
    private ScrollPane activeFiltersScroll;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private Label rosterHeading;

    @FXML
    private StackPane memberDetailsPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    @FXML
    private SplitPane mainSplit;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
        helpWindow.getRoot().initOwner(primaryStage);
        helpWindow.getRoot().setOnHidden(event -> {
            if (primaryStage.isShowing()) {
                primaryStage.requestFocus();
            }
        });
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        memberDetailsPanel = new MemberDetailsPanel();
        memberDetailsPlaceholder.getChildren().add(memberDetailsPanel.getRoot());

        personListPanel = new PersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());
        personListPanel.setOnSelectionChanged(this::showSelectedMember);
        logic.getFilteredPersonList().addListener((ListChangeListener<Person>) change -> refreshRosterHeading());
        refreshRosterHeading();

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
        mainSplit.setDividerPositions(DIVIDER_POSITION);
        refreshActiveFilters();
        primaryStage.getScene().focusOwnerProperty().addListener((observable, previous, focused) -> {
            if (focused != null && activeFiltersPane.getChildren().contains(focused.getParent())) {
                scrollFilterIntoView(focused.getParent());
            }
        });

        primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F2) {
                personListPanel.focusList();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE
                    && (personListPanel.isListFocused() || personListPanel.getSelectedPerson() != null)) {
                personListPanel.clearSelection();
                commandBox.focusInput();
                event.consume();
            }
        });
    }

    /** Keeps the detail view tied to a deliberate, currently displayed selection. */
    private void showSelectedMember() {
        if (personListPanel.getSelectedPerson() == null || personListPanel.getSelectedIndex() < 0) {
            memberDetailsPanel.clear();
        } else {
            int displayedNumber = personListPanel.getSelectedIndex() + 1;
            memberDetailsPanel.showPerson(personListPanel.getSelectedPerson(), displayedNumber);
        }
    }

    /** Keeps the roster heading aligned with the number of members currently displayed. */
    private void refreshRosterHeading() {
        int count = logic.getFilteredPersonList().size();
        rosterHeading.setText("ROSTER · " + count + " shown");
    }

    /** Rebuilds the filter and show boxes for the displayed list. */
    private void refreshActiveFilters() {
        int previousCount = activeFiltersPane.getChildren().size();
        activeFiltersPane.getChildren().clear();
        for (ActiveFilter filter : logic.getActiveFilters()) {
            activeFiltersPane.getChildren().add(createFilterChip(filter));
        }
        boolean hasFilters = !activeFiltersPane.getChildren().isEmpty();
        activeFiltersPane.setVisible(hasFilters);
        activeFiltersPane.setManaged(hasFilters);
        activeFiltersScroll.setVisible(hasFilters);
        activeFiltersScroll.setManaged(hasFilters);
        if (hasFilters) {
            // Font metrics differ across platforms; reserve enough height for one complete styled row.
            activeFiltersScroll.applyCss();
            double rowHeight = activeFiltersPane.getChildren().stream()
                    .mapToDouble(chip -> ((Region) chip).prefHeight(-1)).max().orElse(0);
            activeFiltersScroll.setMinHeight(rowHeight);
        }
        if (activeFiltersPane.getChildren().size() > previousCount) {
            Node newestChip = activeFiltersPane.getChildren().getLast();
            // Wait for wrapping to settle without taking keyboard focus away from command entry.
            Platform.runLater(() -> scrollFilterIntoView(newestChip));
        }
    }

    /** Creates a box whose X removes exactly the condition represented by {@code filter}. */
    private HBox createFilterChip(ActiveFilter filter) {
        boolean isShow = filter.getKind() == ActiveFilter.Kind.SHOW;
        String showNames = isShow ? filter.getShowSelectionNames() : "";
        String removeDescription = isShow ? "Remove this show selection" : "Remove " + filter.getLabel();
        Label label = new Label(filter.getLabel());
        label.setTooltip(new Tooltip(isShow ? "Selected members: " + showNames : filter.getLabel()));
        Button removeButton = new Button(REMOVE_BUTTON_TEXT);
        removeButton.getStyleClass().add("filter-chip-remove");
        removeButton.setAccessibleText(removeDescription);
        if (!isShow) {
            removeButton.setTooltip(new Tooltip(removeDescription));
        }
        removeButton.setOnAction(event -> {
            personListPanel.clearSelection();
            logic.removeActiveFilter(filter);
            showChipRemoval(isShow ? "Removed show selection." : "Removed " + filter.getLabel() + ".");
        });
        HBox chip = new HBox(FILTER_CHIP_SPACING, label, removeButton);
        chip.getStyleClass().add("filter-chip");
        chip.getStyleClass().add("filter-chip-" + filter.getKind().name().toLowerCase(java.util.Locale.ROOT));
        configureFilterChip(chip, label, removeButton);
        return chip;
    }

    /** Keeps long conditions compact while leaving their removal control reachable. */
    private void configureFilterChip(HBox chip, Label label, Button removeButton) {
        label.setMinWidth(0);
        HBox.setHgrow(label, Priority.ALWAYS);
        removeButton.setMinWidth(Region.USE_PREF_SIZE);
        chip.maxWidthProperty().bind(activeFiltersPane.widthProperty());
    }

    /** Scrolls a focused or newly added condition into view after its FlowPane has wrapped. */
    private void scrollFilterIntoView(Node chip) {
        if (chip.getParent() != activeFiltersPane) {
            return;
        }
        activeFiltersScroll.applyCss();
        activeFiltersScroll.requestLayout();
        activeFiltersScroll.layout();
        double viewportHeight = activeFiltersScroll.getViewportBounds().getHeight();
        double scrollableHeight = activeFiltersPane.getHeight() - viewportHeight;
        if (scrollableHeight <= 0) {
            return;
        }
        Bounds bounds = chip.getBoundsInParent();
        double offset = activeFiltersScroll.getVvalue() * scrollableHeight;
        if (bounds.getMinY() < offset) {
            activeFiltersScroll.setVvalue(bounds.getMinY() / scrollableHeight);
        } else if (bounds.getMaxY() > offset + viewportHeight) {
            activeFiltersScroll.setVvalue((bounds.getMaxY() - viewportHeight) / scrollableHeight);
        }
    }

    /**
     * Refreshes the filter controls and shows removal feedback with the updated member count.
     * Restores focus after a focused chip is removed and repeats a pending confirmation prompt if needed.
     *
     * @param message Description of the removal to include in the feedback.
     */
    private void showChipRemoval(String message) {
        Node focused = primaryStage.getScene().getFocusOwner();
        int focusedChipIndex = focused == null ? -1 : activeFiltersPane.getChildren().indexOf(focused.getParent());
        refreshActiveFilters();
        if (focusedChipIndex >= 0) {
            if (activeFiltersPane.getChildren().isEmpty()) {
                commandBox.focusInput();
            } else {
                int nextIndex = Math.min(focusedChipIndex, activeFiltersPane.getChildren().size() - 1);
                HBox nextChip = (HBox) activeFiltersPane.getChildren().get(nextIndex);
                nextChip.getChildren().get(1).requestFocus();
            }
        }
        String feedback = message + " "
                + String.format(FilterCommand.MESSAGE_LISTED, logic.getFilteredPersonList().size());
        if (logic.isAwaitingConfirmation()) {
            // Removing a view restriction keeps confirmation pending, so repeat the action and its scope.
            feedback += "\n\n" + pendingConfirmationPrompt;
        }
        resultDisplay.setFeedbackToUser(feedback);
    }

    /**
     * Restores the saved bounds while keeping the window accessible after display changes.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        List<Rectangle2D> screens = new ArrayList<>();
        Screen primaryScreen = Screen.getPrimary();
        screens.add(primaryScreen.getVisualBounds());
        for (Screen screen : Screen.getScreens()) {
            if (!screen.equals(primaryScreen)) {
                screens.add(screen.getVisualBounds());
            }
        }
        Rectangle2D placement = WindowPlacement.restore(guiSettings, screens,
                primaryStage.getMinWidth(), primaryStage.getMinHeight());
        primaryStage.setMinWidth(Math.min(primaryStage.getMinWidth(), placement.getWidth()));
        primaryStage.setMinHeight(Math.min(primaryStage.getMinHeight(), placement.getHeight()));
        primaryStage.setWidth(placement.getWidth());
        primaryStage.setHeight(placement.getHeight());
        primaryStage.setX(placement.getMinX());
        primaryStage.setY(placement.getMinY());
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        helpWindow.show();
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        personListPanel.clearSelection();
        boolean wasAwaitingConfirmation = logic.isAwaitingConfirmation();
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Command completed successfully");
            resultDisplay.setFeedbackToUser(hideContactDetails(commandResult.getFeedbackToUser()));
            // A near-miss reply keeps the original prompt, so later filter removals still name the action.
            if (logic.isAwaitingConfirmation() && !wasAwaitingConfirmation) {
                pendingConfirmationPrompt = commandResult.getFeedbackToUser();
            }

            if (commandResult.isShowHelp()) {
                helpWindow.show(commandResult.getHelpTopic());
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("Command rejected or could not be saved");
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        } finally {
            // A failed save can retain a model change that also reset its filters.
            refreshActiveFilters();
            commandBox.setAwaitingConfirmation(logic.isAwaitingConfirmation());
        }
    }

    /** Keeps command confirmations from revealing contact fields outside the selected-member panel. */
    private String hideContactDetails(String feedback) {
        if (!feedback.startsWith("Added member\n")
                && !feedback.startsWith("Updated member\n")
                && !feedback.startsWith("Deleted member\n")) {
            return feedback;
        }
        return feedback.lines()
                .filter(line -> !line.startsWith("Phone:") && !line.startsWith("Email:")
                        && !line.startsWith("Address:"))
                .collect(Collectors.joining("\n"));
    }
}

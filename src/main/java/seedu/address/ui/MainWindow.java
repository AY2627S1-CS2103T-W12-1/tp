package seedu.address.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
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

    /** The latest deletion prompt, shown again if a filter is removed before the user answers it. */
    private String pendingDeletionPrompt = "";

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private FlowPane activeFiltersPane;

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
        mainSplit.setDividerPositions(0.22);
        refreshActiveFilters();

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
            memberDetailsPanel.showPerson(personListPanel.getSelectedPerson(), personListPanel.getSelectedIndex() + 1);
        }
    }

    /** Keeps the roster heading aligned with the number of members currently displayed. */
    private void refreshRosterHeading() {
        int count = logic.getFilteredPersonList().size();
        rosterHeading.setText("ROSTER · " + count + " shown");
    }

    /** Rebuilds the filter and show boxes for the displayed list. */
    private void refreshActiveFilters() {
        activeFiltersPane.getChildren().clear();
        for (ActiveFilter filter : logic.getActiveFilters()) {
            activeFiltersPane.getChildren().add(createFilterChip(filter));
        }
        int showSelectionCount = logic.getActiveShowSelectionCount();
        if (showSelectionCount > 0) {
            activeFiltersPane.getChildren().add(createShowIndicator(showSelectionCount));
        }
        boolean hasFilters = !activeFiltersPane.getChildren().isEmpty();
        activeFiltersPane.setVisible(hasFilters);
        activeFiltersPane.setManaged(hasFilters);
    }

    /** Shows that {@code show} still narrows the roster and lets the user clear every show selection. */
    private HBox createShowIndicator(int showSelectionCount) {
        Label label = new Label("Show: active");
        label.setAccessibleText("Show selection active");
        String times = showSelectionCount == 1 ? "once" : showSelectionCount + " times";
        label.setTooltip(new Tooltip("Show has been applied " + times + ". X removes all show selections."));
        Button removeButton = new Button("×");
        removeButton.getStyleClass().add("filter-chip-remove");
        removeButton.setAccessibleText("Remove all show selections");
        removeButton.setTooltip(new Tooltip("Remove all show selections"));
        removeButton.setOnAction(event -> {
            personListPanel.clearSelection();
            logic.clearShowSelections();
            showChipRemoval("Removed all show selections.");
        });
        HBox chip = new HBox(6, label, removeButton);
        chip.getStyleClass().addAll("filter-chip", "filter-chip-show");
        return chip;
    }

    /** Creates a box whose X removes exactly the condition represented by {@code filter}. */
    private HBox createFilterChip(ActiveFilter filter) {
        String removeDescription = "Remove " + filter.getLabel();
        Label label = new Label(filter.getLabel());
        Button removeButton = new Button("×");
        removeButton.getStyleClass().add("filter-chip-remove");
        removeButton.setAccessibleText(removeDescription);
        removeButton.setTooltip(new Tooltip(removeDescription));
        removeButton.setOnAction(event -> {
            personListPanel.clearSelection();
            logic.removeActiveFilter(filter);
            showChipRemoval("Removed " + filter.getLabel() + ".");
        });
        HBox chip = new HBox(6, label, removeButton);
        chip.getStyleClass().add("filter-chip");
        chip.getStyleClass().add("filter-chip-" + filter.getKind().name().toLowerCase(java.util.Locale.ROOT));
        return chip;
    }

    private void showChipRemoval(String message) {
        refreshActiveFilters();
        String feedback = message + " " + logic.getFilteredPersonList().size() + " member(s) listed.";
        if (logic.isAwaitingConfirmation()) {
            // Removing a view restriction keeps the deletion pending, so the member it names must stay visible.
            feedback += "\n\n" + pendingDeletionPrompt;
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
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Command completed successfully");
            resultDisplay.setFeedbackToUser(hideContactDetails(commandResult.getFeedbackToUser()));
            if (logic.isAwaitingConfirmation()) {
                pendingDeletionPrompt = commandResult.getFeedbackToUser();
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

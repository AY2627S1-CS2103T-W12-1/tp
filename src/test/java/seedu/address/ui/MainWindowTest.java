package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.CommandHelp;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** Tests command entry through the real UI, logic and disposable storage, including help and save recovery. */
public class MainWindowTest {
    @TempDir
    public Path temporaryFolder;

    private MainWindow window;
    private ModelManager model;
    private TextField input;
    private TextArea feedback;

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @BeforeEach
    public void setUp() throws Exception {
        onJavaFxThread(() -> {
            model = new ModelManager();
            model.addPerson(new PersonBuilder().withName("Alice Tan").build());
            model.addPerson(new PersonBuilder().withName("Bob Lee").build());
            Path dataFile = temporaryFolder.resolve("members.json");
            StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
            Stage stage = new Stage();
            stage.setOpacity(0);
            window = new MainWindow(stage, new LogicManager(model, storage), dataFile);
            window.show();
            window.fillInnerParts();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            input = (TextField) stage.getScene().lookup("#commandTextField");
            feedback = (TextArea) stage.getScene().lookup("#resultDisplay");
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        onJavaFxThread(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    public void enter_contactCommands_routineLogsOmitMemberValuesAndAddressClearPersists() throws Exception {
        List<String> messages = new ArrayList<>();
        Handler recorder = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        Logger logger = Logger.getLogger("ab3");
        Level previous = logger.getLevel();
        logger.addHandler(recorder);
        logger.setLevel(Level.ALL);
        try {
            onJavaFxThread(() -> {
                enter("add n/Private Synthetic p/90009999 e/private@example.com a/Private Road");
                enter("edit 3 a/");
                enter("private@example.com");
            });
        } finally {
            logger.setLevel(previous);
            logger.removeHandler(recorder);
        }
        String logs = String.join("\n", messages);
        assertTrue(logs.contains("Command completed successfully"));
        for (String value : List.of("Private Synthetic", "90009999", "private@example.com", "Private Road")) {
            assertFalse(logs.contains(value), value + " appeared in diagnostic logs");
        }
        Person saved = new JsonAddressBookStorage(temporaryFolder.resolve("members.json"))
                .readAddressBook().orElseThrow().getPersonList().get(2);
        assertEquals("", saved.getAddress().value);
    }

    @Test
    public void help_commandTopicAndF1_navigateOfflineWithoutSlash() throws Exception {
        onJavaFxThread(() -> {
            enter("help add");
            Stage guide = guide();
            assertEquals(window.getPrimaryStage(), guide.getOwner());
            assertEquals("TrackCall · Help: add", guide.getTitle());
            assertEquals("Opened help for add.", feedback.getText());
            Event.fireEvent(guide.getScene(), key(KeyCode.ESCAPE));
            assertFalse(guide.isShowing());
            assertTrue(window.getPrimaryStage().isShowing());
            enter("/help");
            assertEquals(Messages.MESSAGE_UNKNOWN_COMMAND, feedback.getText());
            assertEquals("/help", input.getText());
            Event.fireEvent(input, key(KeyCode.F1));
            guide = guide();
            assertEquals("TrackCall · Command guide", guide.getTitle());
            guide.getScene().getRoot().applyCss();
            assertEquals(CommandHelp.values().length,
                    guide.getScene().getRoot().lookupAll(".overview-keyword").size());
            enter("exit");
            assertFalse(guide.isShowing());
            assertFalse(window.getPrimaryStage().isShowing());
        });
        assertFalse(Files.exists(temporaryFolder.resolve("members.json")));
    }

    @Test
    public void enter_failedDeleteAndClear_doNotRepeatOrLoseOtherMembers() throws Exception {
        Files.createDirectory(temporaryFolder.resolve("members.json"));
        onJavaFxThread(() -> {
            enter("find Alice");
            enter("delete 1");
            enter("yes");
            assertTrue(feedback.getText().contains("not saved"));
            assertEquals("", input.getText());
            assertEquals("Bob Lee", model.getAddressBook().getPersonList().getFirst().getName().fullName);
            input.fireEvent(new ActionEvent());
            assertEquals(1, model.getAddressBook().getPersonList().size());
            enter("list");
            assertEquals("Showing 1 member.", feedback.getText());
            enter("clear");
            enter("yes");
            assertTrue(feedback.getText().startsWith(ClearCommand.MESSAGE_SAVE_FAILURE));
            assertTrue(feedback.getText().contains("Could not save data"));
            assertEquals("yes", input.getText());
            assertEquals(1, model.getFilteredPersonList().size());
        });
    }

    @Test
    public void enter_blankReplyCancelsDeletionAndLaterConfirmationCannotDelete() throws Exception {
        onJavaFxThread(() -> {
            enter("edit 1 p/90000001");
            enter("edit 2 p/90000002");
        });
        Path dataFile = temporaryFolder.resolve("members.json");
        Path backupFile = temporaryFolder.resolve("members.json.bak");
        String savedRoster = Files.readString(dataFile);
        String savedBackup = Files.readString(backupFile);
        var rosterModified = Files.getLastModifiedTime(dataFile);
        var backupModified = Files.getLastModifiedTime(backupFile);

        onJavaFxThread(() -> {
            enter("delete 1");
            assertEquals("", input.getText());
            input.fireEvent(new ActionEvent());
            assertEquals(ConfirmDeleteCommand.MESSAGE_CANCELLED, feedback.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.DEFAULT_PROMPT, input.getPromptText());
            assertDisplayedNames("Alice Tan", "Bob Lee");
            enter("yes");
            assertEquals(Messages.MESSAGE_UNKNOWN_COMMAND, feedback.getText());
            assertEquals(2, model.getAddressBook().getPersonList().size());
            input.clear();
            input.fireEvent(new ActionEvent());
            assertEquals(Messages.MESSAGE_UNKNOWN_COMMAND, feedback.getText());
        });
        assertEquals(savedRoster, Files.readString(dataFile));
        assertEquals(savedBackup, Files.readString(backupFile));
        assertEquals(rosterModified, Files.getLastModifiedTime(dataFile));
        assertEquals(backupModified, Files.getLastModifiedTime(backupFile));
    }

    @Test
    public void enter_clearAwaitingConfirmation_preservesFilteredRosterUntilConfirmed() throws Exception {
        onJavaFxThread(() -> {
            enter("find Alice");
            enter("clear");
            assertEquals(String.format(ClearCommand.MESSAGE_CONFIRMATION, 2), feedback.getText());
            assertTrue(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.CONFIRMATION_PROMPT, input.getPromptText());
            assertDisplayedNames("Alice Tan");
            assertEquals(2, model.getAddressBook().getPersonList().size());
            assertFalse(Files.exists(temporaryFolder.resolve("members.json")));

            input.fireEvent(new ActionEvent());
            assertEquals(ClearCommand.MESSAGE_CANCELLED, feedback.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.DEFAULT_PROMPT, input.getPromptText());
            assertDisplayedNames("Alice Tan");
            assertEquals(2, model.getAddressBook().getPersonList().size());

            enter("clear");
            enter("yes");
            assertEquals("Cleared 2 members. The roster is now empty.", feedback.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertDisplayedNames();
        });
        assertTrue(new JsonAddressBookStorage(temporaryFolder.resolve("members.json"))
                .readAddressBook().orElseThrow().getPersonList().isEmpty());
    }

    @Test
    public void enter_deleteAwaitingConfirmation_highlightsCommandBoxUntilAnswered() throws Exception {
        onJavaFxThread(() -> {
            enter("delete 1");
            assertTrue(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.CONFIRMATION_PROMPT, input.getPromptText());
            assertEquals(2, model.getAddressBook().getPersonList().size());

            enter("list");
            assertEquals(ConfirmDeleteCommand.MESSAGE_CANCELLED + "\n\"list\" was not run. Enter it again to run it.",
                    feedback.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.DEFAULT_PROMPT, input.getPromptText());
            assertEquals(2, model.getAddressBook().getPersonList().size());

            enter("delete 1");
            enter("yes");
            assertTrue(feedback.getText().startsWith("Deleted member"));
            assertFalse(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(1, model.getAddressBook().getPersonList().size());
        });
    }

    @Test
    public void removeFilterChip_deleteAwaitingConfirmation_keepsPromptVisible() throws Exception {
        onJavaFxThread(() -> {
            enter("find Alice");
            enter("delete 1");

            assertFirstChipRemovalKeepsPrompt("Removed Name: Alice. 2 member(s) listed.",
                    "Delete member 1, Alice Tan?");
            enter("yes");
            assertDisplayedNames("Bob Lee");
        });
    }

    @Test
    public void removeFilterChip_afterNearMissReply_keepsOriginalPromptAndTarget() throws Exception {
        onJavaFxThread(() -> {
            enter("find Bob");
            enter("delete 1");
            enter("Y");
            assertTrue(feedback.getText().startsWith("To confirm, type yes exactly"));
            assertTrue(feedback.getText().contains("Delete member 1, Bob Lee?"));

            assertFirstChipRemovalKeepsPrompt("Removed Name: Bob. 2 member(s) listed.", "Delete member 1, Bob Lee?");
            enter("yes");
            assertDisplayedNames("Alice Tan");
        });
    }

    @Test
    public void enter_failedAddSave_refreshesFilterChipsToMatchRetainedRoster() throws Exception {
        Files.createDirectory(temporaryFolder.resolve("members.json"));
        onJavaFxThread(() -> {
            enter("find Alice");
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
            assertEquals(List.of("Name: Alice"), chipLabels(chips));
            assertDisplayedNames("Alice Tan");

            enter("add n/Carol Wong p/90000002 e/carol@example.com a/Test Street");

            assertTrue(feedback.getText().contains("not saved"));
            assertEquals(List.of(), model.getActiveFilters());
            assertEquals(List.of(), chipLabels(chips));
            assertFalse(chips.isVisible());
            assertFalse(chips.isManaged());
            assertDisplayedNames("Alice Tan", "Bob Lee", "Carol Wong");
        });
    }

    @Test
    public void emailFilterChips_retainEarlierConditionsAndRemoveOnlyChosenSearchWithoutSaving() throws Exception {
        onJavaFxThread(() -> {
            enter("edit 1 e/alice@example.com t/committee");
            enter("edit 2 e/bob@school.edu t/committee");
        });
        Path dataFile = temporaryFolder.resolve("members.json");
        String savedRoster = Files.readString(dataFile);
        onJavaFxThread(() -> {
            enter("sort");
            enter("find Alice Bob");
            enter("filter t/committee");
            enter("show 1");
            enter("findemail @example.com @school.edu");
            enter("findemail alice");
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
            assertEquals(List.of("Name: Alice or Bob", "Tag: committee", "Show: 1 selected",
                    "Email: @example.com or @school.edu", "Email: alice"), chipLabels(chips));
            List<String> originalLabels = chipLabels(chips);
            enter("find BOB ALICE alice");
            assertEquals("This name search is already active. 1 member listed.", feedback.getText());
            assertEquals(originalLabels, chipLabels(chips));
            enter("findemail ALICE");
            assertEquals("This email search is already active. 1 member listed.", feedback.getText());
            assertEquals(originalLabels, chipLabels(chips));
            assertTrue(chips.isVisible());
            assertTrue(((HBox) chips.getChildren().get(3)).getStyleClass().contains("filter-chip-email"));
            assertDisplayedNames("Alice Tan");

            enter("findemail bob");
            assertDisplayedNames();
            removeChip(chips, 5);
            assertDisplayedNames("Alice Tan");
            removeChip(chips, 4);
            assertEquals(List.of("Name: Alice or Bob", "Tag: committee", "Show: 1 selected",
                    "Email: @example.com or @school.edu"), chipLabels(chips));
            assertDisplayedNames("Alice Tan");
            removeChip(chips, 3);
            assertDisplayedNames("Alice Tan");
            assertEquals(List.of("Name: Alice or Bob", "Tag: committee", "Show: 1 selected"), chipLabels(chips));
            enter("list");
            assertEquals(List.of(), chipLabels(chips));
            assertDisplayedNames("Alice Tan", "Bob Lee");
        });
        assertEquals(savedRoster, Files.readString(dataFile));
    }

    @Test
    public void showIndicatorAndRosterHeading_followDisplayedViewAndReset() throws Exception {
        onJavaFxThread(() -> {
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
            Label heading = (Label) window.getPrimaryStage().getScene().lookup("#rosterHeading");
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertFalse(chips.isVisible());

            enter("show 1 2");
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertEquals(List.of("Show: 2 selected"), chipLabels(chips));
            assertEquals(2, ((HBox) chips.getChildren().getFirst()).getChildren().size());
            Label showLabel = (Label) ((HBox) chips.getChildren().getFirst()).getChildren().getFirst();
            Button showRemove = (Button) ((HBox) chips.getChildren().getFirst()).getChildren().get(1);
            assertEquals("Selected members: Alice Tan, Bob Lee",
                    showLabel.getTooltip().getText());
            assertNull(showRemove.getTooltip());
            assertEquals("Remove this show selection", showRemove.getAccessibleText());

            enter("show 1");
            assertEquals("ROSTER · 1 shown", heading.getText());
            assertEquals(List.of("Show: 2 selected", "Show: 1 selected"), chipLabels(chips));
            enter("show 1");
            assertEquals(List.of("Show: 2 selected", "Show: 1 selected"), chipLabels(chips));
            removeChip(chips, 1);
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertEquals(List.of("Show: 2 selected"), chipLabels(chips));
            removeChip(chips, 0);
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertFalse(chips.isVisible());

            enter("show 1");
            enter("find Bob");
            assertEquals("ROSTER · 0 shown", heading.getText());
            assertEquals(List.of("Show: 1 selected", "Name: Bob"), chipLabels(chips));
            removeChip(chips, 0);
            assertEquals("ROSTER · 1 shown", heading.getText());
            assertEquals(List.of("Name: Bob"), chipLabels(chips));
            assertDisplayedNames("Bob Lee");

            enter("list");
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertFalse(chips.isVisible());
        });
    }

    @Test
    public void showChip_afterLastSelectedMemberDeleted_keepsEmptySelectionUntilRemoved() throws Exception {
        onJavaFxThread(() -> {
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
            enter("show 1");
            enter("delete 1");
            enter("yes");

            assertEquals(List.of("Show: 0 selected"), chipLabels(chips));
            Label showLabel = (Label) ((HBox) chips.getChildren().getFirst()).getChildren().getFirst();
            assertEquals("Selected members: No selected members remain", showLabel.getTooltip().getText());
            assertDisplayedNames();

            removeChip(chips, 0);
            assertDisplayedNames("Bob Lee");
        });
    }

    @Test
    public void showChips_keepCommandOrderAsFiltersChange() throws Exception {
        onJavaFxThread(() -> {
            Person alice = model.getAddressBook().getPersonList().getFirst();
            model.setPerson(alice, new PersonBuilder(alice).withTags("committee").build());
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");

            enter("show 1");
            enter("show 1");
            enter("find Alice");
            enter("filter t/committee");
            assertEquals(List.of("Show: 1 selected", "Name: Alice", "Tag: committee"),
                    chipLabels(chips));
            assertDisplayedNames("Alice Tan");

            removeChip(chips, 1);
            assertEquals(List.of("Show: 1 selected", "Tag: committee"), chipLabels(chips));
            removeChip(chips, 0);
            assertEquals(List.of("Tag: committee"), chipLabels(chips));

            enter("show 1");
            assertEquals(List.of("Tag: committee", "Show: 1 selected"), chipLabels(chips));

            enter("find Alice");
            assertEquals(List.of("Tag: committee", "Show: 1 selected", "Name: Alice"), chipLabels(chips));
            removeChip(chips, 0);
            assertEquals(List.of("Show: 1 selected", "Name: Alice"), chipLabels(chips));

            enter("list");
            enter("show 1");
            enter("filter t/committee");
            enter("find Alice");
            assertEquals(List.of("Show: 1 selected", "Tag: committee", "Name: Alice"), chipLabels(chips));
        });
    }

    @Test
    public void activeFilterChips_removeIndividualFiltersAndUpdateDisplayedMembers() throws Exception {
        onJavaFxThread(() -> {
            Person alice = model.getAddressBook().getPersonList().get(0);
            Person bob = model.getAddressBook().getPersonList().get(1);
            model.setPerson(alice, new PersonBuilder(alice).withTags("classA", "team").build());
            model.setPerson(bob, new PersonBuilder(bob).withTags("classA").build());
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
            assertFalse(chips.isVisible());

            enter("find Alice Bob");
            enter("filter t/classA");
            enter("find Tan");
            enter("filter t/team");
            assertEquals(List.of("Name: Alice or Bob", "Tag: classA", "Name: Tan", "Tag: team"), chipLabels(chips));
            assertTrue(((HBox) chips.getChildren().get(0)).getStyleClass().contains("filter-chip-name"));
            assertTrue(((HBox) chips.getChildren().get(1)).getStyleClass().contains("filter-chip-tag"));
            assertDisplayedNames("Alice Tan");

            ListView<?> roster = (ListView<?>) window.getPrimaryStage().getScene().lookup("#personListView");
            ScrollPane details = (ScrollPane) window.getPrimaryStage().getScene().lookup("#detailsScroll");
            roster.getSelectionModel().select(0);
            assertTrue(details.isVisible());
            removeChip(chips, 3);
            assertFalse(details.isVisible());
            assertEquals(-1, roster.getSelectionModel().getSelectedIndex());
            assertEquals(List.of("Name: Alice or Bob", "Tag: classA", "Name: Tan"), chipLabels(chips));
            assertDisplayedNames("Alice Tan");
            removeChip(chips, 2);
            assertEquals(List.of("Name: Alice or Bob", "Tag: classA"), chipLabels(chips));
            assertDisplayedNames("Alice Tan", "Bob Lee");
            removeChip(chips, 0);
            assertEquals(List.of("Tag: classA"), chipLabels(chips));
            assertDisplayedNames("Alice Tan", "Bob Lee");
            removeChip(chips, 0);
            assertFalse(chips.isVisible());
            assertFalse(chips.isManaged());
            assertDisplayedNames("Alice Tan", "Bob Lee");

            enter("find Bob");
            assertEquals(List.of("Name: Bob"), chipLabels(chips));
            enter("list");
            assertFalse(chips.isVisible());
        });
    }

    @Test
    public void activeFilterChips_longCondition_keepsRemovalVisibleWithFullTooltip() throws Exception {
        resizeWindow(853, 480);
        onJavaFxThread(() -> {
            String condition = "Alice Benjamin Charlotte Danielle Elizabeth Francesca Gabriella Harrison Isabella "
                    + "Jonathan Katherine Leonardo Madeleine Nathaniel Olivia Penelope Quentin Rebecca "
                    + "Sebastian Theodore";
            enter("find " + condition);
            layoutWindow();
            ScrollPane scroll = (ScrollPane) window.getPrimaryStage().getScene().lookup("#activeFiltersScroll");
            FlowPane chips = (FlowPane) scroll.getContent();
            HBox chip = (HBox) chips.getChildren().getFirst();
            Label label = (Label) chip.getChildren().getFirst();
            Button removeButton = (Button) chip.getChildren().get(1);
            assertEquals("Name: " + condition.replace(" ", " or "), label.getText());
            assertEquals(label.getText(), label.getTooltip().getText());
            assertTrue(label.getWidth() < label.prefWidth(-1), "The long label should be ellipsized");
            assertInside(removeButton, scroll.lookup(".viewport"));
            assertInScene(removeButton);
            removeButton.fire();
            assertDisplayedNames("Alice Tan", "Bob Lee");
            assertFalse(scroll.isManaged());
        });
        assertFalse(Files.exists(temporaryFolder.resolve("members.json")));
    }

    @Test
    public void activeFilterChips_manyConditions_keepRosterVisibleAndScrollForKeyboardFocus() throws Exception {
        resizeWindow(853, 480);
        onJavaFxThread(() -> {
            for (int index = 0; index < 22; index++) {
                enter("find Alice Group" + index);
            }
            layoutWindow();
        });
        onJavaFxThread(() -> {
            layoutWindow();
            Stage stage = window.getPrimaryStage();
            ScrollPane scroll = (ScrollPane) stage.getScene().lookup("#activeFiltersScroll");
            FlowPane chips = (FlowPane) scroll.getContent();
            assertEquals(22, chips.getChildren().size());
            assertTrue(chips.getHeight() > scroll.getViewportBounds().getHeight());
            assertTrue(scroll.getHeight() <= 80);
            assertInScene(stage.getScene().lookup("#mainSplit"));
            assertInScene(stage.getScene().lookup("#statusbarPlaceholder"));
            assertTrue(stage.getScene().lookup("#personListPanelPlaceholder").getLayoutBounds().getHeight() >= 90);
            assertInside(chips.getChildren().getLast(), scroll.lookup(".viewport"));
            assertDisplayedNames("Alice Tan");

            scroll.setVvalue(0);
            Button lastRemove = (Button) ((HBox) chips.getChildren().getLast()).getChildren().get(1);
            lastRemove.requestFocus();
            layoutWindow();
            assertInside(lastRemove, scroll.lookup(".viewport"));
            assertTrue(scroll.getVvalue() > 0);

            Button firstRemove = (Button) ((HBox) chips.getChildren().getFirst()).getChildren().get(1);
            firstRemove.requestFocus();
            layoutWindow();
            assertInside(firstRemove, scroll.lookup(".viewport"));
            assertEquals(0, scroll.getVvalue(), 0.01);
            scroll.requestFocus();
            Event.fireEvent(scroll, key(KeyCode.DOWN));
            assertTrue(scroll.getVvalue() > 0, "The filter area must support keyboard scrolling");
            firstRemove.requestFocus();
            for (int index = 1; index < 22; index++) {
                Event.fireEvent(stage.getScene().getFocusOwner(), key(KeyCode.TAB));
            }
            layoutWindow();
            assertEquals(lastRemove, stage.getScene().getFocusOwner());
            assertInside(lastRemove, scroll.lookup(".viewport"));

            firstRemove.requestFocus();
            firstRemove.fire();
            layoutWindow();
            assertEquals(21, chips.getChildren().size());
            assertEquals(((HBox) chips.getChildren().getFirst()).getChildren().get(1),
                    stage.getScene().getFocusOwner());
            assertDisplayedNames("Alice Tan");
        });
    }

    @Test
    public void activeFilterChips_removeLastCondition_keepsFocusedSuccessorVisibleAfterLayout() throws Exception {
        resizeWindow(450, 450);
        onJavaFxThread(() -> {
            for (int index = 0; index < 30; index++) {
                enter("find Alice Group" + index);
            }
            layoutWindow();
            ScrollPane scroll = (ScrollPane) window.getPrimaryStage().getScene().lookup("#activeFiltersScroll");
            FlowPane chips = (FlowPane) scroll.getContent();
            Button lastRemove = (Button) ((HBox) chips.getChildren().getLast()).getChildren().get(1);
            lastRemove.requestFocus();
            lastRemove.fire();
        });
        awaitLayoutPulses();
        onJavaFxThread(() -> {
            ScrollPane scroll = (ScrollPane) window.getPrimaryStage().getScene().lookup("#activeFiltersScroll");
            FlowPane chips = (FlowPane) scroll.getContent();
            assertEquals(29, chips.getChildren().size());
            Node focused = window.getPrimaryStage().getScene().getFocusOwner();
            assertEquals(((HBox) chips.getChildren().getLast()).getChildren().get(1), focused);
            assertInside(focused, scroll.lookup(".viewport"));
            assertDisplayedNames("Alice Tan");
        });
    }

    @Test
    public void activeFilterChips_tallerFontMetrics_keepsWholeRowsAndMainPanelsVisible() throws Exception {
        Path largerFontStylesheet = temporaryFolder.resolve("larger-filter-font.css");
        Files.writeString(largerFontStylesheet, ".filter-chip-remove { -fx-font-size: 24px; }");
        onJavaFxThread(() -> window.getPrimaryStage().getScene().getStylesheets()
                .add(largerFontStylesheet.toUri().toString()));
        for (int[] size : new int[][]{{853, 480}, {450, 450}}) {
            resizeWindow(size[0], size[1]);
            onJavaFxThread(() -> {
                enter("list");
                for (int index = 0; index < 22; index++) {
                    enter("find Alice Group" + index);
                }
                layoutWindow();
            });
            awaitLayoutPulses();
            onJavaFxThread(() -> {
                Stage stage = window.getPrimaryStage();
                ScrollPane scroll = (ScrollPane) stage.getScene().lookup("#activeFiltersScroll");
                FlowPane chips = (FlowPane) scroll.getContent();
                HBox lastChip = (HBox) chips.getChildren().getLast();
                assertTrue(lastChip.getHeight() > 35, "Exercise a row taller than the original fixed minimum");
                assertTrue(scroll.getViewportBounds().getHeight() >= lastChip.getHeight());
                assertInside(lastChip, scroll.lookup(".viewport"));
                assertInScene(stage.getScene().lookup("#mainSplit"));
                assertInScene(stage.getScene().lookup("#statusbarPlaceholder"));
                Button firstRemove = (Button) ((HBox) chips.getChildren().getFirst()).getChildren().get(1);
                firstRemove.requestFocus();
                layoutWindow();
                assertInside(chips.getChildren().getFirst(), scroll.lookup(".viewport"));
                Button lastRemove = (Button) lastChip.getChildren().get(1);
                lastRemove.requestFocus();
                layoutWindow();
                assertInside(lastChip, scroll.lookup(".viewport"));
                lastRemove.fire();
            });
            awaitLayoutPulses();
            onJavaFxThread(() -> {
                ScrollPane scroll = (ScrollPane) window.getPrimaryStage().getScene().lookup("#activeFiltersScroll");
                FlowPane chips = (FlowPane) scroll.getContent();
                assertInside(chips.getChildren().getLast(), scroll.lookup(".viewport"));
                assertDisplayedNames("Alice Tan");
            });
        }
    }

    @Test
    public void activeFilterChips_decoratedMinimumViewport_keepsPanelsInsideScene() throws Exception {
        Path largerFontStylesheet = temporaryFolder.resolve("decorated-minimum-filter-font.css");
        Files.writeString(largerFontStylesheet, ".filter-chip-remove { -fx-font-size: 40px; }");
        onJavaFxThread(() -> {
            Stage stage = window.getPrimaryStage();
            stage.getScene().getStylesheets().add(largerFontStylesheet.toUri().toString());
            // Exercise the 434 x 411 content area of the decorated 450 x 450 Windows CI stage.
            double frameWidth = stage.getWidth() - stage.getScene().getWidth();
            double frameHeight = stage.getHeight() - stage.getScene().getHeight();
            stage.setMinWidth(0);
            stage.setMinHeight(0);
            stage.setWidth(434 + frameWidth);
            stage.setHeight(411 + frameHeight);
        });
        awaitLayoutPulses();
        onJavaFxThread(() -> {
            assertEquals(434, window.getPrimaryStage().getScene().getWidth(), 1);
            assertEquals(411, window.getPrimaryStage().getScene().getHeight(), 1);
            for (int index = 0; index < 22; index++) {
                enter("find Alice Group" + index);
            }
            layoutWindow();
        });
        awaitLayoutPulses();
        onJavaFxThread(() -> {
            Stage stage = window.getPrimaryStage();
            ScrollPane scroll = (ScrollPane) stage.getScene().lookup("#activeFiltersScroll");
            FlowPane chips = (FlowPane) scroll.getContent();
            assertInside(chips.getChildren().getLast(), scroll.lookup(".viewport"));
            assertInScene(stage.getScene().lookup("#mainSplit"));
            assertInScene(stage.getScene().lookup("#statusbarPlaceholder"));
            assertTrue(feedback.getHeight() >= 40, "Feedback must retain space for readable text");
            assertTrue(stage.getScene().lookup("#personListView").getLayoutBounds().getHeight() >= 60,
                    "The roster must retain space for a member row");
        });
    }

    /** Resizes on a separate JavaFX turn so the native window can update its scene dimensions. */
    private void resizeWindow(double width, double height) throws Exception {
        onJavaFxThread(() -> {
            window.getPrimaryStage().setWidth(width);
            window.getPrimaryStage().setHeight(height);
            window.getPrimaryStage().requestFocus();
        });
        awaitLayoutPulses();
    }

    /** Waits for native scene changes and responsive layouts to reach the next rendered frame. */
    private void awaitLayoutPulses() throws Exception {
        CountDownLatch layoutReady = new CountDownLatch(1);
        onJavaFxThread(() -> {
            new AnimationTimer() {
                private int pulses;

                @Override
                public void handle(long now) {
                    if (++pulses >= 2) {
                        stop();
                        layoutReady.countDown();
                    }
                }
            }.start();
        });
        assertTrue(layoutReady.await(5, TimeUnit.SECONDS), "The resized window did not receive a layout pulse");
    }

    /** Applies styles and layout before checking the visible positions of controls. */
    private void layoutWindow() {
        window.getPrimaryStage().getScene().getRoot().applyCss();
        window.getPrimaryStage().getScene().getRoot().layout();
    }

    /** Checks against the window viewport, including when children extend the root's bounds. */
    private void assertInScene(Node control) {
        Bounds bounds = control.localToScene(control.getBoundsInLocal());
        assertTrue(bounds.getMinX() >= -1, bounds.toString());
        assertTrue(bounds.getMinY() >= -1, bounds.toString());
        assertTrue(bounds.getMaxX() <= control.getScene().getWidth() + 1, bounds.toString());
        assertTrue(bounds.getMaxY() <= control.getScene().getHeight() + 1, bounds.toString());
    }

    /** Checks that the entire control is reachable within the supplied visible region. */
    private void assertInside(Node control, Node region) {
        Bounds controlBounds = control.localToScene(control.getBoundsInLocal());
        Bounds regionBounds = region.localToScene(region.getBoundsInLocal());
        assertTrue(controlBounds.getMinX() >= regionBounds.getMinX() - 1, controlBounds + " inside " + regionBounds);
        assertTrue(controlBounds.getMaxX() <= regionBounds.getMaxX() + 1, controlBounds + " inside " + regionBounds);
        assertTrue(controlBounds.getMinY() >= regionBounds.getMinY() - 1, controlBounds + " inside " + regionBounds);
        assertTrue(controlBounds.getMaxY() <= regionBounds.getMaxY() + 1, controlBounds + " inside " + regionBounds);
    }

    @Test
    public void enter_allCommands_roundTripsGroupChangesThroughRealUiAndStorage() throws Exception {
        Path exported = temporaryFolder.resolve("member copy.csv");
        onJavaFxThread(() -> {
            enter("help");
            Stage guide = guide();
            assertTrue(guide.isShowing());
            Event.fireEvent(guide.getScene(), key(KeyCode.ESCAPE));
            enter("clear");
            enter("yes");
            enter("add n/Zara Tan p/90000001 e/zara@example.com a/Test Road t/committee");
            enter("add n/bob Lee p/90000002 e/bob@example.com a/Test Road t/year1");
            enter("add n/Alice Tan p/90000003 e/alice@example.com a/Test Road t/committee");
            enter("sort");
            assertDisplayedNames("Alice Tan", "bob Lee", "Zara Tan");
            enter("find Zara Alice");
            enter("filter t/committee");
            assertDisplayedNames("Alice Tan", "Zara Tan");
            enter("tagall t/year2");
            assertEquals("Added tag \"year2\" to 2 member(s).", feedback.getText());
            enter("untagall t/committee");
            assertEquals("Removed tag \"committee\" (any letter case) from 2 member(s).\n"
                    + "0 member(s) listed with tag \"committee\". Run list to show everyone.", feedback.getText());
            assertDisplayedNames();
            enter("list");
            assertDisplayedNames("Zara Tan", "bob Lee", "Alice Tan");
            enter("edit 1 p/90000004");
            enter("export " + exported);
            assertTrue(feedback.getText().startsWith("Exported 3 members to "));
            enter("delete 2");
            enter("yes");
            assertDisplayedNames("Zara Tan", "Alice Tan");
            enter("clear");
            enter("yes");
            enter("import p/\"" + exported + "\"");
            assertTrue(feedback.getText().startsWith("Imported 3 member(s)"));
            assertDisplayedNames("Zara Tan", "bob Lee", "Alice Tan");
            assertEquals("90000004", model.getFilteredPersonList().getFirst().getPhone().value);
            assertTrue(model.getFilteredPersonList().getFirst().getTags().stream()
                    .anyMatch(tag -> tag.tagName.equals("year2")));
            assertTrue(model.getFilteredPersonList().getFirst().getTags().stream()
                    .noneMatch(tag -> tag.tagName.equals("committee")));
            enter("find Alice");
            enter("clear");
            enter("yes");
            assertEquals("Cleared 3 members. The roster is now empty.", feedback.getText());
            enter("list");
            assertEquals("No members in the roster.", feedback.getText());
            enter("exit");
            assertFalse(window.getPrimaryStage().isShowing());
        });
        assertEquals(4, Files.readAllLines(exported).size());
        assertTrue(new JsonAddressBookStorage(temporaryFolder.resolve("members.json"))
                .readAddressBook().orElseThrow().getPersonList().isEmpty());
    }

    @Test
    public void privacyPanel_revealsOnlySelectedMemberAndClearsAfterCommand() throws Exception {
        onJavaFxThread(() -> {
            ListView<?> list = (ListView<?>) window.getPrimaryStage().getScene().lookup("#personListView");
            ScrollPane details = (ScrollPane) window.getPrimaryStage().getScene().lookup("#detailsScroll");
            Label phone = (Label) window.getPrimaryStage().getScene().lookup("#phone");
            Label email = (Label) window.getPrimaryStage().getScene().lookup("#email");
            Label address = (Label) window.getPrimaryStage().getScene().lookup("#address");

            assertFalse(details.isVisible());
            assertEquals("", phone.getText());
            list.getSelectionModel().select(0);
            assertTrue(details.isVisible());
            assertEquals(model.getFilteredPersonList().getFirst().getPhone().value, phone.getText());
            assertEquals(model.getFilteredPersonList().getFirst().getEmail().value, email.getText());
            assertEquals(model.getFilteredPersonList().getFirst().getAddress().value, address.getText());

            Button copyPhone = (Button) window.getPrimaryStage().getScene().lookup("#copyPhoneButton");
            copyPhone.fire();
            assertEquals(phone.getText(), Clipboard.getSystemClipboard().getString());

            enter("edit 1 p/90000001");
            assertFalse(details.isVisible());
            assertEquals("", phone.getText());
            assertEquals(-1, list.getSelectionModel().getSelectedIndex());
            assertTrue(feedback.getText().contains("Updated member"));
            assertFalse(feedback.getText().contains("90000001"));
            assertFalse(feedback.getText().contains("Email:"));
            assertFalse(feedback.getText().contains("Address:"));

            enter("add n/Carol Wong p/90000002 e/carol@example.com a/Test Street");
            assertTrue(feedback.getText().contains("Added member"));
            assertFalse(feedback.getText().contains("90000002"));
            assertFalse(feedback.getText().contains("carol@example.com"));

            enter("delete 3");
            assertTrue(feedback.getText().startsWith("Delete member 3, Carol Wong?"));
            assertFalse(feedback.getText().contains("90000002"));
            assertFalse(feedback.getText().contains("carol@example.com"));
            enter("yes");
            assertTrue(feedback.getText().contains("Deleted member"));
            assertFalse(feedback.getText().contains("90000002"));
        });
    }

    @Test
    public void copyAddressButton_onlyEnabledWhenSelectedMemberHasAddress() throws Exception {
        onJavaFxThread(() -> {
            ListView<?> list = (ListView<?>) window.getPrimaryStage().getScene().lookup("#personListView");
            Label address = (Label) window.getPrimaryStage().getScene().lookup("#address");
            Button copyAddress = (Button) window.getPrimaryStage().getScene().lookup("#copyAddressButton");
            Button copyPhone = (Button) window.getPrimaryStage().getScene().lookup("#copyPhoneButton");

            assertTrue(copyAddress.isDisabled());
            enter("add n/Carol Wong p/90000002 e/carol@example.com");
            list.getSelectionModel().select(2);
            assertEquals("(none)", address.getText());
            assertTrue(copyAddress.isDisabled());

            Clipboard clipboard = Clipboard.getSystemClipboard();
            Map<DataFormat, Object> originalClipboardContent = new HashMap<>();
            for (DataFormat format : clipboard.getContentTypes()) {
                originalClipboardContent.put(format, clipboard.getContent(format));
            }
            try {
                copyPhone.fire();
                String previousClipboardText = clipboard.getString();
                assertEquals("90000002", previousClipboardText);
                copyAddress.fire();
                assertEquals(previousClipboardText, clipboard.getString());

                enter("edit 3 a/123 Test Road");
                list.getSelectionModel().select(2);
                assertEquals("123 Test Road", address.getText());
                assertFalse(copyAddress.isDisabled());
                copyAddress.fire();
                assertEquals("123 Test Road", clipboard.getString());
            } finally {
                clipboard.setContent(originalClipboardContent);
            }
        });
    }

    @Test
    public void enter_tagWithDetailsHidden_reportsOnlyNameAndTagsAndSavesMember() throws Exception {
        Person original = new PersonBuilder().withName("Alice Tan").withPhone("90000123")
                .withEmail("alice@example.com").withAddress("12 Test Road").withTags("zebra", "friends").build();
        Person expected = new PersonBuilder(original).withTags("zebra", "friends", "committee").build();
        onJavaFxThread(() -> {
            model.setPerson(model.getAddressBook().getPersonList().getFirst(), original);
            ScrollPane details = (ScrollPane) window.getPrimaryStage().getScene().lookup("#detailsScroll");
            assertFalse(details.isVisible());

            enter("tag 1 t/committee");

            assertFalse(details.isVisible());
            assertTrue(feedback.getText().contains("Name: Alice Tan"));
            assertTrue(feedback.getText().contains("Tags: committee, friends, zebra"));
            for (String hidden : List.of("Phone:", "Email:", "Address:", original.getPhone().value,
                    original.getEmail().value, original.getAddress().value)) {
                assertFalse(feedback.getText().contains(hidden), hidden);
            }
            assertEquals(expected, model.getAddressBook().getPersonList().getFirst());
        });
        assertEquals(expected, new JsonAddressBookStorage(temporaryFolder.resolve("members.json"))
                .readAddressBook().orElseThrow().getPersonList().getFirst());
    }

    @Test
    public void enter_untagWithDetailsHidden_reportsOnlyNameAndTagsAndSavesMember() throws Exception {
        Person original = new PersonBuilder().withName("Alice Tan").withPhone("90000123")
                .withEmail("alice@example.com").withAddress("12 Test Road")
                .withTags("zebra", "committee", "friends").build();
        onJavaFxThread(() -> {
            model.setPerson(model.getAddressBook().getPersonList().getFirst(), original);
            ScrollPane details = (ScrollPane) window.getPrimaryStage().getScene().lookup("#detailsScroll");
            assertFalse(details.isVisible());

            enter("untag 1 t/committee");

            assertFalse(details.isVisible());
            assertTrue(feedback.getText().contains("Name: Alice Tan"));
            assertTrue(feedback.getText().contains("Tags: friends, zebra"));
            assertContactDetailsHidden(original);

            enter("untag 1 t/friends");
            enter("untag 1 t/zebra");

            assertFalse(details.isVisible());
            assertTrue(feedback.getText().contains("Name: Alice Tan"));
            assertTrue(feedback.getText().contains("Tags: (none)"));
            assertContactDetailsHidden(original);
        });
        Person expected = new PersonBuilder(original).withTags().build();
        assertEquals(expected, model.getAddressBook().getPersonList().getFirst());
        assertEquals(expected, new JsonAddressBookStorage(temporaryFolder.resolve("members.json"))
                .readAddressBook().orElseThrow().getPersonList().getFirst());
    }

    private void assertContactDetailsHidden(Person person) {
        for (String hidden : List.of("Phone:", "Email:", "Address:", person.getPhone().value,
                person.getEmail().value, person.getAddress().value)) {
            assertFalse(feedback.getText().contains(hidden), hidden);
        }
    }

    @Test
    public void privacyPanel_escapeFromCopyControl_hidesDetails() throws Exception {
        onJavaFxThread(() -> {
            ListView<?> list = (ListView<?>) window.getPrimaryStage().getScene().lookup("#personListView");
            list.getSelectionModel().select(1);
            ScrollPane details = (ScrollPane) window.getPrimaryStage().getScene().lookup("#detailsScroll");
            assertTrue(details.isVisible());
            Button copyButton = (Button) window.getPrimaryStage().getScene().lookup("#copyEmailButton");
            Event.fireEvent(copyButton, key(KeyCode.ESCAPE));
            assertEquals(-1, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void privacyPanel_smallWindow_keepsDetailsScrollable() throws Exception {
        onJavaFxThread(() -> {
            Stage stage = window.getPrimaryStage();
            stage.setWidth(450);
            stage.setHeight(450);
        });
        onJavaFxThread(() -> {
            Stage stage = window.getPrimaryStage();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
            list.getSelectionModel().select(0);
            stage.getScene().getRoot().layout();
            ScrollPane details = (ScrollPane) stage.getScene().lookup("#detailsScroll");
            assertTrue(details.isVisible());
            assertTrue(details.getWidth() > 0);
            assertTrue(details.getHeight() > 0);
            details.setVvalue(1);
            Label address = (Label) stage.getScene().lookup("#address");
            assertEquals(model.getFilteredPersonList().getFirst().getAddress().value,
                    address.getText());
        });
    }

    /** Checks the actual list bound to the displayed member cards. */
    private void assertDisplayedNames(String... expectedNames) {
        ListView<?> displayed = (ListView<?>) window.getPrimaryStage().getScene().lookup("#personListView");
        assertEquals(List.of(expectedNames), displayed.getItems().stream()
                .map(person -> ((Person) person).getName().fullName).toList());
    }

    private List<String> chipLabels(FlowPane chips) {
        return chips.getChildren().stream()
                .map(chip -> ((Label) ((HBox) chip).getChildren().get(0)).getText()).toList();
    }

    /** Returns the pane that holds the active filter boxes. */
    private FlowPane getFilterChips() {
        return (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
    }

    /**
     * Removes the first filter box while a deletion awaits confirmation, then checks that the feedback reports
     * {@code removal} and still names the member in {@code prompt}, and that the prompt stays pending.
     */
    private void assertFirstChipRemovalKeepsPrompt(String removal, String prompt) {
        removeChip(getFilterChips(), 0);

        assertTrue(feedback.getText().startsWith(removal));
        assertTrue(feedback.getText().contains(prompt));
        assertTrue(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
    }

    private void removeChip(FlowPane chips, int index) {
        HBox chip = (HBox) chips.getChildren().get(index);
        Button removeButton = (Button) chip.getChildren().get(1);
        removeButton.fire();
    }

    private void enter(String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }

    private Stage guide() {
        Stage guide = Window.getWindows().stream().filter(candidate -> candidate instanceof Stage)
                .map(candidate -> (Stage) candidate).filter(candidate -> candidate != window.getPrimaryStage())
                .findFirst().orElseThrow();
        guide.setOpacity(0);
        return guide;
    }

    private KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }
}

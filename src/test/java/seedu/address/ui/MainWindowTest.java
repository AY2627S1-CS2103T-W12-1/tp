package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
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
            enter("y");
            assertTrue(feedback.getText().contains("not saved"));
            assertEquals("", input.getText());
            assertEquals("Bob Lee", model.getAddressBook().getPersonList().getFirst().getName().fullName);
            input.fireEvent(new ActionEvent());
            assertEquals(1, model.getAddressBook().getPersonList().size());
            enter("list");
            assertEquals("Showing 1 member.", feedback.getText());
            enter("clear");
            assertEquals(ClearCommand.MESSAGE_SAVE_FAILURE, feedback.getText());
            assertEquals("clear", input.getText());
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
            enter("y");
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
    public void enter_deleteAwaitingConfirmation_highlightsCommandBoxUntilAnswered() throws Exception {
        onJavaFxThread(() -> {
            enter("delete 1");
            assertTrue(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.CONFIRMATION_PROMPT, input.getPromptText());
            assertEquals(2, model.getAddressBook().getPersonList().size());

            enter("list");
            assertEquals(ConfirmDeleteCommand.MESSAGE_CANCELLED, feedback.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            assertEquals(CommandBox.DEFAULT_PROMPT, input.getPromptText());
            assertEquals(2, model.getAddressBook().getPersonList().size());

            enter("delete 1");
            enter("y");
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
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");

            removeChip(chips, 0);

            assertTrue(feedback.getText().startsWith("Removed Name: Alice. 2 member(s) listed."));
            assertTrue(feedback.getText().contains("Delete member Alice Tan?"));
            assertTrue(input.getStyleClass().contains(CommandBox.CONFIRMATION_STYLE_CLASS));
            enter("y");
            assertDisplayedNames("Bob Lee");
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
    public void emailFilterChip_replacesSelectionAndCanBeRemovedWithoutSaving() throws Exception {
        onJavaFxThread(() -> {
            enter("edit 1 e/alice@example.com t/committee");
            enter("edit 2 e/bob@school.edu t/committee");
        });
        Path dataFile = temporaryFolder.resolve("members.json");
        String savedRoster = Files.readString(dataFile);
        onJavaFxThread(() -> {
            enter("sort");
            enter("find Bob");
            enter("filter t/committee");
            enter("show 1");
            enter("findemail @example.com");
            FlowPane chips = (FlowPane) window.getPrimaryStage().getScene().lookup("#activeFiltersPane");
            assertEquals(List.of("Email: @example.com"), chipLabels(chips));
            assertTrue(chips.isVisible());
            assertTrue(((HBox) chips.getChildren().getFirst()).getStyleClass().contains("filter-chip-email"));
            assertDisplayedNames("Alice Tan");

            enter("filter t/committee");
            enter("find Bob");
            assertDisplayedNames();
            removeChip(chips, 2);
            assertDisplayedNames("Alice Tan");
            assertEquals(List.of("Email: @example.com", "Tag: committee"), chipLabels(chips));
            removeChip(chips, 0);
            assertEquals(List.of("Tag: committee"), chipLabels(chips));
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

            enter("show 1");
            assertEquals("ROSTER · 1 shown", heading.getText());
            assertEquals(List.of("Show: active"), chipLabels(chips));
            assertEquals(2, ((HBox) chips.getChildren().getFirst()).getChildren().size());
            Label showLabel = (Label) ((HBox) chips.getChildren().getFirst()).getChildren().getFirst();
            assertEquals("Show has been applied once. X removes all show selections.",
                    showLabel.getTooltip().getText());

            enter("show 1");
            showLabel = (Label) ((HBox) chips.getChildren().getFirst()).getChildren().getFirst();
            assertEquals("Show has been applied 2 times. X removes all show selections.",
                    showLabel.getTooltip().getText());
            removeChip(chips, 0);
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertFalse(chips.isVisible());

            enter("show 1");
            enter("find Bob");
            assertEquals("ROSTER · 0 shown", heading.getText());
            assertEquals(List.of("Name: Bob", "Show: active"), chipLabels(chips));
            removeChip(chips, 1);
            assertEquals("ROSTER · 1 shown", heading.getText());
            assertEquals(List.of("Name: Bob"), chipLabels(chips));
            assertDisplayedNames("Bob Lee");

            enter("list");
            assertEquals("ROSTER · 2 shown", heading.getText());
            assertFalse(chips.isVisible());
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
            assertEquals(List.of("Name: Alice Bob", "Tag: classA", "Name: Tan", "Tag: team"), chipLabels(chips));
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
            assertEquals(List.of("Name: Alice Bob", "Tag: classA", "Name: Tan"), chipLabels(chips));
            assertDisplayedNames("Alice Tan");
            removeChip(chips, 2);
            assertEquals(List.of("Name: Alice Bob", "Tag: classA"), chipLabels(chips));
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
    public void enter_allCommands_roundTripsGroupChangesThroughRealUiAndStorage() throws Exception {
        Path exported = temporaryFolder.resolve("member copy.csv");
        onJavaFxThread(() -> {
            enter("help");
            Stage guide = guide();
            assertTrue(guide.isShowing());
            Event.fireEvent(guide.getScene(), key(KeyCode.ESCAPE));
            enter("clear");
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
            assertEquals("Removed tag \"committee\" from 2 member(s).", feedback.getText());
            assertDisplayedNames();
            enter("list");
            assertDisplayedNames("Zara Tan", "bob Lee", "Alice Tan");
            enter("edit 1 p/90000004");
            enter("export " + exported);
            assertTrue(feedback.getText().startsWith("Exported 3 members to "));
            enter("delete 2");
            enter("y");
            assertDisplayedNames("Zara Tan", "Alice Tan");
            enter("clear");
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
            assertTrue(feedback.getText().startsWith("Delete member Carol Wong?"));
            assertFalse(feedback.getText().contains("90000002"));
            assertFalse(feedback.getText().contains("carol@example.com"));
            enter("y");
            assertTrue(feedback.getText().contains("Deleted member"));
            assertFalse(feedback.getText().contains("90000002"));
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

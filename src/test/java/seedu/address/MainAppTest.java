package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.tag.Tag;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;
import seedu.address.ui.UiManager;

/** Exercises startup and recovery against disposable files without exiting the shared JavaFX toolkit. */
public class MainAppTest {
    @TempDir
    public Path directory;

    private MainApp app;
    private JsonAddressBookStorage roster;
    private JsonUserPrefsStorage prefs;
    private boolean opened;

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @BeforeEach
    public void setUp() {
        roster = new JsonAddressBookStorage(directory.resolve("members.json"));
        prefs = new JsonUserPrefsStorage(directory.resolve("preferences.json"));
        app = new MainApp();
        app.storage = new StorageManager(roster, prefs);
        app.model = new ModelManager();
        app.ui = unused -> opened = true;
    }

    @AfterEach
    public void closeWindows() throws Exception {
        onJavaFxThread(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    public void init_wiresComponentsToSuppliedFiles_andRetainsSavedPreferences() throws Exception {
        UserPrefs saved = new UserPrefs();
        saved.setGuiSettings(new GuiSettings(900, 650, 20, 30));
        prefs.saveUserPrefs(saved);

        app.init(prefs.getUserPrefsFilePath(), roster.getAddressBookFilePath());

        assertEquals(saved, app.model.getUserPrefs());
        assertEquals(roster.getAddressBookFilePath(), app.storage.getAddressBookFilePath());
        assertEquals(prefs.getUserPrefsFilePath(), app.storage.getUserPrefsFilePath());
        assertInstanceOf(UiManager.class, app.ui);
        app.logic.execute("add n/Alice p/12345678 e/alice@example.com a/Main Street");
        assertEquals(app.model.getAddressBook(), roster.readAddressBook().orElseThrow());
    }

    @Test
    public void recovery_originalChangesDuringDialog_rejectsReplacementAndReportsOwnedError() throws Exception {
        Files.writeString(roster.getAddressBookFilePath(), "damaged roster");
        onJavaFxThread(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane()));
            Platform.runLater(() -> {
                assertDoesNotThrow(() -> Files.writeString(roster.getAddressBookFilePath(), "changed original"));
                answerDialog("OK");
            });
            answerDialog("Start empty");
            assertFalse(app.recoverRoster(owner, "Invalid JSON data"));
        });

        assertEquals("changed original", Files.readString(roster.getAddressBookFilePath()));
        assertPreservedCopy();
    }

    @Test
    public void start_validAndEmptyRosters_loadsExactlySavedMembersWithoutWriting() throws Exception {
        for (AddressBook saved : List.of(getTypicalAddressBook(), new AddressBook())) {
            roster.saveAddressBook(saved);
            byte[] before = Files.readAllBytes(roster.getAddressBookFilePath());
            opened = false;

            onJavaFxThread(() -> app.start(new Stage()));

            assertTrue(opened);
            assertEquals(saved, app.model.getAddressBook());
            assertArrayEquals(before, Files.readAllBytes(roster.getAddressBookFilePath()));
        }
    }

    @Test
    public void start_missingRoster_loadsSamplesWithoutCreatingFile() throws Exception {
        onJavaFxThread(() -> app.start(new Stage()));

        assertTrue(opened);
        assertEquals(SampleDataUtil.getSampleAddressBook(), app.model.getAddressBook());
        assertFalse(Files.exists(roster.getAddressBookFilePath()));
    }

    @Test
    public void start_damagedRoster_restoresBackupBeforeOpeningUi() throws Exception {
        roster.saveAddressBook(getTypicalAddressBook());
        roster.saveAddressBook(new AddressBook());
        Files.writeString(roster.getAddressBookFilePath(), "damaged roster");

        onJavaFxThread(() -> {
            answerDialog("Restore backup");
            app.start(new Stage());
        });

        assertTrue(opened);
        assertEquals(getTypicalAddressBook(), app.model.getAddressBook());
        assertEquals(getTypicalAddressBook(), roster.readAddressBook().orElseThrow());
        assertEquals(getTypicalAddressBook(), roster.readBackup().orElseThrow());
        assertPreservedCopy();
    }

    @Test
    public void start_invalidBackup_explicitlyStartsEmptyWithoutReplacingBackup() throws Exception {
        Files.writeString(roster.getAddressBookFilePath(), "damaged roster");
        Path backup = JsonAddressBookStorage.getBackupFilePath(roster.getAddressBookFilePath());
        Files.writeString(backup, "damaged backup");

        onJavaFxThread(() -> {
            answerDialog("Start empty");
            app.start(new Stage());
        });

        assertTrue(opened);
        assertEquals(new AddressBook(), roster.readAddressBook().orElseThrow());
        assertEquals("damaged backup", Files.readString(backup));
        assertPreservedCopy();
    }

    @Test
    public void start_afterEmptyRecovery_externalEditIsRefusedBeforeFirstSave() throws Exception {
        assertOutsideEditProtectedAfterRecovery("Start empty", new AddressBook());
    }

    @Test
    public void start_afterBackupRecovery_externalEditIsRefusedBeforeFirstSave() throws Exception {
        assertOutsideEditProtectedAfterRecovery("Restore backup", getTypicalAddressBook());
    }

    /** Checks the actual startup path without reading through session storage before its first save. */
    private void assertOutsideEditProtectedAfterRecovery(String choice, AddressBook recovered) throws Exception {
        Path file = roster.getAddressBookFilePath();
        JsonAddressBookStorage externalWriter = new JsonAddressBookStorage(file);
        externalWriter.saveAddressBook(getTypicalAddressBook(), JsonAddressBookStorage.getBackupFilePath(file));
        Files.writeString(file, "damaged roster");
        onJavaFxThread(() -> {
            answerDialog(choice);
            app.start(new Stage());
        });
        assertEquals(recovered, app.model.getAddressBook());
        AddressBook outside = new AddressBook();
        outside.addPerson(new PersonBuilder().withName("Outside Editor").build());
        externalWriter.saveAddressBook(outside);
        byte[] outsideBytes = Files.readAllBytes(file);
        byte[] backupBytes = Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file));

        IOException failure = assertThrows(IOException.class, () ->
                app.storage.saveAddressBook(app.model.getAddressBook()));

        assertEquals(JsonAddressBookStorage.MESSAGE_CHANGED_OUTSIDE, failure.getMessage());
        assertArrayEquals(outsideBytes, Files.readAllBytes(file));
        assertArrayEquals(backupBytes, Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file)));
        app.storage.saveAddressBook(app.model.getAddressBook());
        assertEquals(recovered, new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        assertArrayEquals(outsideBytes, Files.readAllBytes(JsonAddressBookStorage.getBackupFilePath(file)));
    }

    @Test
    public void start_invalidTag_showsReasonInRecoveryDialog() throws Exception {
        Files.writeString(roster.getAddressBookFilePath(), "{\"persons\":[{\"name\":\"Alice\",\"phone\":\"123\","
                + "\"email\":\"alice@example.com\",\"address\":\"\",\"tags\":[\"committee!\"]}]}");
        AtomicReference<String> shownText = new AtomicReference<>();

        onJavaFxThread(() -> {
            Platform.runLater(() -> shownText.set(findDialogPane().getContentText()));
            answerDialog("Start empty");
            app.start(new Stage());
        });

        assertTrue(opened);
        assertTrue(shownText.get().startsWith("Reason: " + Tag.MESSAGE_CONSTRAINTS + "\n\n"));
    }

    @Test
    public void recovery_closeWithoutBackup_preservesOriginalAndDoesNotOpenUi() throws Exception {
        Files.writeString(roster.getAddressBookFilePath(), "damaged roster");
        onJavaFxThread(() -> {
            answerDialog("Close");
            assertFalse(app.recoverRoster(new Stage(), "Invalid JSON data"));
        });

        assertFalse(opened);
        assertEquals("damaged roster", Files.readString(roster.getAddressBookFilePath()));
        assertPreservedCopy();
    }

    @Test
    public void recovery_preservationFailure_reportsFailureAndLeavesOriginalAlone() throws Exception {
        Files.createDirectory(roster.getAddressBookFilePath());
        onJavaFxThread(() -> {
            answerDialog("OK");
            assertFalse(app.recoverRoster(new Stage(), "Invalid JSON data"));
        });

        assertFalse(opened);
        assertTrue(Files.isDirectory(roster.getAddressBookFilePath()));
    }

    @Test
    public void initPrefs_missingInvalidAndValidFiles_returnsAndSavesExpectedSettings() throws Exception {
        assertEquals(new UserPrefs(), app.initPrefs(prefs));
        Files.writeString(prefs.getUserPrefsFilePath(), "invalid preferences");
        assertEquals(new UserPrefs(), app.initPrefs(prefs));
        UserPrefs saved = new UserPrefs();
        saved.setGuiSettings(new GuiSettings(900, 650, 20, 30));
        prefs.saveUserPrefs(saved);

        assertEquals(saved, app.initPrefs(prefs));
        assertEquals(saved, prefs.readUserPrefs().orElseThrow());
    }

    @Test
    public void preferences_unwritableDestination_doesNotAbortStartupOrShutdown() throws Exception {
        Files.createDirectory(prefs.getUserPrefsFilePath());

        assertEquals(new UserPrefs(), app.initPrefs(prefs));
        assertDoesNotThrow(app::stop);
        assertTrue(Files.isDirectory(prefs.getUserPrefsFilePath()));
    }

    @Test
    public void stop_savesPreferencesWithoutCreatingRoster() throws Exception {
        app.model.setGuiSettings(new GuiSettings(900, 650, 20, 30));
        app.stop();

        assertEquals(app.model.getUserPrefs(), prefs.readUserPrefs().orElseThrow());
        assertFalse(Files.exists(roster.getAddressBookFilePath()));
    }

    private void assertPreservedCopy() throws Exception {
        try (var files = Files.list(directory)) {
            List<Path> copies = files.filter(path -> path.getFileName().toString().contains(".corrupt-")).toList();
            assertEquals(1, copies.size());
            assertEquals("damaged roster", Files.readString(copies.getFirst()));
        }
    }

    private static DialogPane findDialogPane() {
        return (DialogPane) Window.getWindows().stream()
                .filter(window -> window.getScene().getRoot() instanceof DialogPane)
                .findFirst().orElseThrow().getScene().getRoot();
    }

    private void answerDialog(String label) {
        Platform.runLater(() -> {
            DialogPane pane = findDialogPane();
            var button = pane.getButtonTypes().stream().filter(type -> type.getText().equals(label))
                    .findFirst().orElseThrow();
            Button control = (Button) pane.lookupButton(button);
            control.fire();
        });
    }
}

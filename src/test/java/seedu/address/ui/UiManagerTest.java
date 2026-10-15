package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.logic.LogicManager;
import seedu.address.model.ModelManager;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.JavaFxTestUtil;

/** Verifies the real application window, icon, and owned message dialogs. */
public class UiManagerTest {
    @TempDir
    public Path directory;

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @AfterEach
    public void closeWindows() throws Exception {
        onJavaFxThread(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    public void startAndAlert_createsUsableWindowAndOwnedDialog() throws Exception {
        onJavaFxThread(() -> {
            Path dataFile = directory.resolve("members.json");
            var storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                    new JsonUserPrefsStorage(directory.resolve("preferences.json")));
            var manager = new UiManager(new LogicManager(new ModelManager(), storage), dataFile);
            Stage stage = new Stage();
            stage.setOpacity(0);
            manager.start(stage);
            assertTrue(stage.isShowing());
            assertFalse(stage.getIcons().isEmpty());
            assertTrue(stage.getIcons().stream().noneMatch(Image::isError));
            assertNotNull(stage.getScene().lookup("#commandTextField"));
            FutureTask<Void> inspectDialog = new FutureTask<>(() -> {
                Window window = Window.getWindows().stream().filter(candidate -> candidate != stage)
                        .findFirst().orElseThrow();
                DialogPane pane = (DialogPane) window.getScene().getRoot();
                try {
                    assertEquals(stage, ((Stage) window).getOwner());
                    assertEquals("Storage problem", pane.getHeaderText());
                    assertEquals("Please retry.", pane.getContentText());
                    assertEquals(UiManager.ALERT_DIALOG_PANE_FIELD_ID, pane.getId());
                } finally {
                    Button control = (Button) pane.lookupButton(pane.getButtonTypes().getFirst());
                    control.fire();
                }
            }, null);
            Platform.runLater(inspectDialog);
            manager.showAlertDialogAndWait(Alert.AlertType.INFORMATION, "Notice", "Storage problem", "Please retry.");
            assertDoesNotThrow(() -> {
                inspectDialog.get();
            });
        });
    }
}

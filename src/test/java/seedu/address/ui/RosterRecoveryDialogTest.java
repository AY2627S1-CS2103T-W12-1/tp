package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.testutil.JavaFxTestUtil;

/** Checks safe defaults and the user's explicit choices in the real recovery dialog. */
public class RosterRecoveryDialogTest {
    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void recovery_noValidBackup_offersStartEmptyAndDefaultsToClose() throws Exception {
        onJavaFxThread(() -> {
            RosterRecoveryDialog alert = new RosterRecoveryDialog(new Stage(), Path.of("preserved.json"),
                    Path.of("members.json.bak"), false);
            assertTrue(alert.getContentText().contains("No valid backup"));
            assertTrue(alert.getContentText().contains("preserved.json"));
            assertEquals(List.of(ButtonData.OTHER, ButtonData.CANCEL_CLOSE), alert.getButtonTypes().stream()
                    .map(button -> button.getButtonData()).toList());
            alert.getButtonTypes().forEach(type -> {
                Button button = (Button) alert.getDialogPane().lookupButton(type);
                assertEquals(type.getButtonData() == ButtonData.CANCEL_CLOSE, button.isDefaultButton());
            });
        });
    }

    @Test
    public void recovery_validBackup_returnsEachExplicitChoice() throws Exception {
        onJavaFxThread(() -> {
            for (ButtonData button : List.of(ButtonData.YES, ButtonData.OTHER, ButtonData.CANCEL_CLOSE)) {
                RosterRecoveryDialog alert = new RosterRecoveryDialog(new Stage(),
                        Path.of("preserved.json"), Path.of("members.json.bak"), true);
                Platform.runLater(() -> alert.getButtonTypes().stream()
                        .filter(type -> type.getButtonData() == button).findFirst()
                        .ifPresent(type -> ((Button) alert.getDialogPane().lookupButton(type)).fire()));
                assertEquals(switch (button) {
                    case YES -> RosterRecoveryDialog.Choice.RESTORE_BACKUP;
                    case OTHER -> RosterRecoveryDialog.Choice.START_EMPTY;
                    default -> RosterRecoveryDialog.Choice.CLOSE;
                }, alert.showChoice());
            }
            assertFalse(Window.getWindows().stream().anyMatch(Window::isShowing));
        });
    }
}

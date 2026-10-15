package seedu.address.ui;

import java.nio.file.Path;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/** Presents an explicit recovery choice before an empty roster can be edited. */
public final class RosterRecoveryDialog {
    /** Available responses to an unreadable roster. */
    public enum Choice { RESTORE_BACKUP, START_EMPTY, CLOSE }

    private RosterRecoveryDialog() {
    }

    /** Offers restoration only when a validated backup is available. Closing the dialog leaves all files alone. */
    public static Choice show(Stage owner, Path preservedFile, Path backupFile, boolean hasValidBackup) {
        Alert alert = createAlert(owner, preservedFile, backupFile, hasValidBackup);
        return switch (alert.showAndWait().orElse(ButtonType.CANCEL).getButtonData()) {
            case YES -> Choice.RESTORE_BACKUP;
            case OTHER -> Choice.START_EMPTY;
            default -> Choice.CLOSE;
        };
    }

    /** Builds the recovery dialog with Close as its safe default action. */
    static Alert createAlert(Stage owner, Path preservedFile, Path backupFile, boolean hasValidBackup) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        // Startup recovery runs before the primary window has a scene (required by JavaFX 17's owner binding).
        if (owner.getScene() != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Recover member records");
        alert.setHeaderText("Your saved roster could not be loaded.");
        String backupDetails = hasValidBackup ? "\n\nRestore the previous saved roster from:\n" + backupFile
                : "\n\nNo valid backup is available.";
        alert.setContentText("The original file has been preserved at:\n" + preservedFile + backupDetails
                + "\n\nStart empty creates a new empty roster. Close leaves your files unchanged.");
        ButtonType close = new ButtonType("Close", ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(new ButtonType("Start empty", ButtonData.OTHER), close);
        if (hasValidBackup) {
            alert.getButtonTypes().addFirst(new ButtonType("Restore backup", ButtonData.YES));
        }
        for (ButtonType button : alert.getButtonTypes()) {
            Button control = (Button) alert.getDialogPane().lookupButton(button);
            control.setDefaultButton(button == close);
        }
        return alert;
    }
}

package seedu.address.ui;

import java.nio.file.Path;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/** Presents an explicit recovery choice before an empty roster can be edited. */
public final class RosterRecoveryDialog extends Alert {
    /** Available responses to an unreadable roster. */
    public enum Choice { RESTORE_BACKUP, START_EMPTY, CLOSE }

    /**
     * Builds the recovery dialog with Close as its safe default action.
     *
     * @param reason Why the roster could not be loaded, or {@code null} if no reason is known.
     */
    public RosterRecoveryDialog(Stage owner, String reason, Path preservedFile, Path backupFile,
            boolean hasValidBackup) {
        super(AlertType.WARNING);
        // Startup recovery runs before the primary window has a scene (required by JavaFX 17's owner binding).
        if (owner.getScene() != null) {
            initOwner(owner);
        }
        setTitle("Recover member records");
        setHeaderText("Your saved roster could not be loaded.");
        String backupDetails = hasValidBackup ? "\n\nRestore the previous saved roster from:\n" + backupFile
                : "\n\nNo valid backup is available.";
        boolean hasReason = reason != null && !reason.isBlank();
        String reasonDetails = hasReason ? "Reason: " + reason.strip() + "\n\n" : "";
        setContentText(reasonDetails + "The original file has been preserved at:\n" + preservedFile + backupDetails
                + "\n\nStart empty creates a new empty roster. Close leaves your files unchanged.");
        ButtonType close = new ButtonType("Close", ButtonData.CANCEL_CLOSE);
        getButtonTypes().setAll(new ButtonType("Start empty", ButtonData.OTHER), close);
        if (hasValidBackup) {
            getButtonTypes().addFirst(new ButtonType("Restore backup", ButtonData.YES));
        }
        for (ButtonType button : getButtonTypes()) {
            Button control = (Button) getDialogPane().lookupButton(button);
            control.setDefaultButton(button == close);
        }
    }

    /** Returns Close for cancellation or dismissal, without modifying any files. */
    public Choice showChoice() {
        return switch (showAndWait().orElse(ButtonType.CANCEL).getButtonData()) {
            case YES -> Choice.RESTORE_BACKUP;
            case OTHER -> Choice.START_EMPTY;
            default -> Choice.CLOSE;
        };
    }
}

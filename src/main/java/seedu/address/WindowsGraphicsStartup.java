package seedu.address;

import java.awt.GraphicsEnvironment;
import java.nio.charset.Charset;
import java.nio.file.Path;
import javax.swing.JOptionPane;

import seedu.address.commons.util.FileUtil;

/** Provides recovery guidance when the Windows graphics loader cannot use the application folder. */
final class WindowsGraphicsStartup {
    private static final String RECOVERY_MESSAGE = "TrackCall could not start its graphics system from this folder.\n"
            + "Move the entire TrackCall folder, including its data, to a writable location\n"
            + "with only basic Latin letters or digits in every folder name, then launch the JAR again.";

    private WindowsGraphicsStartup() {
    }

    /** Shows a message independently of JavaFX if its toolkit failed on an unrepresentable Windows path. */
    static void explainFailure(RuntimeException failure) {
        if (requiresRelocation(failure, System.getProperty("os.name"),
                Charset.forName(System.getProperty("native.encoding")), FileUtil.getHomeFolder())) {
            showRecoveryMessage();
        }
    }

    /** Distinguishes a path-related toolkit failure from unrelated errors and supported Unicode paths. */
    static boolean requiresRelocation(RuntimeException failure, String osName, Charset codePage, Path folder) {
        return "No toolkit found".equals(failure.getMessage()) && osName.startsWith("Windows")
                && !codePage.newEncoder().canEncode(folder.toString());
    }

    /** The console also gets the message when a graphical desktop is unavailable. */
    static void showRecoveryMessage() {
        System.err.println(RECOVERY_MESSAGE);
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, RECOVERY_MESSAGE, "TrackCall startup", JOptionPane.ERROR_MESSAGE);
        }
    }
}

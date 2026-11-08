package seedu.address;

import java.awt.GraphicsEnvironment;
import java.nio.charset.Charset;
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
        if (!"No toolkit found".equals(failure.getMessage()) || !System.getProperty("os.name").startsWith("Windows")) {
            return;
        }
        Charset codePage = Charset.forName(System.getProperty("native.encoding"));
        if (codePage.newEncoder().canEncode(FileUtil.getHomeFolder().toString())) {
            return;
        }
        System.err.println(RECOVERY_MESSAGE);
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, RECOVERY_MESSAGE, "TrackCall startup", JOptionPane.ERROR_MESSAGE);
        }
    }
}

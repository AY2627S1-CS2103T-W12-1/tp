package seedu.address;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.JOptionPane;

import javafx.application.Application;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.FileUtil;

/**
 * The main entry point to the application.
 *
 * This is a workaround for the following error when MainApp is made the
 * entry point of the application:
 *
 *     Error: JavaFX runtime components are missing, and are required to run this application
 *
 * The reason is that MainApp extends Application. In that case, the
 * LauncherHelper will check for the javafx.graphics module to be present
 * as a named module. We don't use JavaFX via the module system so it can't
 * find the javafx.graphics module, and so the launch is aborted.
 *
 * By having a separate main class (Main) that doesn't extend Application
 * to be the entry point of the application, we avoid this issue.
 */
public class Main {
    /** Prepares JavaFX files inside the application folder before starting its toolkit. */
    private static void prepareCache() throws IOException {
        Path cache = FileUtil.resolvePath(FileUtil.getHomeFolder().resolve(".javafx-cache"));
        if (Files.exists(cache)) {
            try (Stream<Path> paths = Files.walk(cache)) {
                for (Path path : paths.toList()) {
                    FileUtil.resolvePath(path);
                }
            }
        }
        Files.createDirectories(cache);
        System.setProperty("javafx.cachedir", cache.toString());
        // JavaFX also uses this folder if its primary cache is unavailable.
        System.setProperty("java.io.tmpdir", cache.toString());
        prepareMacLibraries(cache);
    }

    /**
     * Extracts the matching Mac libraries inside the JAR folder before JavaFX starts.
     * Windows and Linux continue using JavaFX's bundled resource loader.
     */
    private static void prepareMacLibraries(Path cache) throws IOException {
        if (!System.getProperty("os.name").startsWith("Mac")) {
            return;
        }
        String arch = System.getProperty("os.arch");
        String folder = arch.equals("aarch64") || arch.equals("arm64") ? "mac-arm64" : "mac-x64";
        Path nativeFolder = FileUtil.resolvePath(cache.resolve(folder));
        for (String library : List.of("glass", "javafx_iio", "javafx_font", "prism_common",
                "prism_es2", "decora_sse", "prism_sw")) {
            String filename = "lib" + library + ".dylib";
            try (InputStream input = Main.class.getResourceAsStream("/natives/" + folder + "/" + filename)) {
                if (input != null) {
                    Files.createDirectories(nativeFolder);
                    Files.copy(input, FileUtil.resolvePath(nativeFolder.resolve(filename)),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        System.setProperty("java.library.path", nativeFolder.toString());
    }

    /**
     * Launches TrackCall's JavaFX application after logging native-access guidance.
     *
     * @param args Command-line arguments forwarded to the application.
     */
    public static void main(String[] args) {
        launch(args, System::exit);
    }

    /** Starts TrackCall, requesting termination after any startup diagnostic has been presented. */
    static void launch(String[] args, IntConsumer exit) {
        try {
            prepareCache();
        } catch (IOException exception) {
            String message = "TrackCall could not prepare its startup files in:\n"
                    + FileUtil.getHomeFolder().resolve(".javafx-cache")
                    + "\n\nMake sure the application folder allows writing, or move the TrackCall folder "
                    + "to a writable location and restart.\n\nDetails: " + exception.getMessage();
            System.err.println(message);
            if (GraphicsEnvironment.isHeadless()) {
                exit.accept(1);
            } else {
                // The JavaFX toolkit is unavailable until these files are ready.
                EventQueue.invokeLater(() -> {
                    try {
                        JOptionPane.showMessageDialog(null, message, "TrackCall could not start",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        exit.accept(1);
                    }
                });
            }
            return;
        }

        Logger logger = LogsCenter.getLogger(Main.class);
        logger.warning("The warnings about a 'restricted method in java.lang.System' "
            + "and 'enabling native access' appearing below (if any) can be ignored.");
        Application.launch(MainApp.class, args);
    }
}

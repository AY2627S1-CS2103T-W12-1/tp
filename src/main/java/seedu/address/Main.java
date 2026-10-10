package seedu.address;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
    static {
        try {
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
        } catch (IOException e) {
            throw new IllegalStateException("Could not use the JavaFX cache inside the application folder.", e);
        }
    }

    private static Logger logger = LogsCenter.getLogger(Main.class);

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

    public static void main(String[] args) {

        logger.warning("The warnings about a 'restricted method in java.lang.System' "
            + "and 'enabling native access' appearing below (if any) can be ignored.");
        Application.launch(MainApp.class, args);
    }
}

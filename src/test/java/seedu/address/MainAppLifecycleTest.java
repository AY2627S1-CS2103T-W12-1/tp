package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.storage.AppInstanceLock;
import seedu.address.testutil.TestUtil;

/** Runs actual application shutdown in an isolated JVM and retains its separate JaCoCo execution data. */
public class MainAppLifecycleTest {
    @TempDir
    public Path directory;

    @Test
    public void startup_cancelRecovery_exitsWithoutOpeningOrReplacingRoster() throws Exception {
        Path roster = directory.resolve("data/addressbook.json");
        Files.createDirectories(roster.getParent());
        Files.writeString(roster, "damaged roster");
        runChild("cancel");
        assertTrue(Files.exists(directory.resolve("cancelled.txt")));
        assertEquals("damaged roster", Files.readString(roster));
    }

    @Test
    public void startup_secondInstance_showsMessageAndPreservesFirstSession() throws Exception {
        Path roster = directory.resolve("data/addressbook.json");
        Files.createDirectories(roster.getParent());
        Files.writeString(roster, "first session roster");
        Path preferences = directory.resolve("preferences.json");
        Files.writeString(preferences, "first session preferences");
        Path lockFile = directory.resolve(AppInstanceLock.FILE_NAME);
        try (AppInstanceLock firstSession = AppInstanceLock.acquire(lockFile)) {
            assertNotNull(firstSession);
            runChild("blocked");
            assertTrue(Files.exists(directory.resolve("blocked.txt")));
            assertEquals("first session roster", Files.readString(roster));
            assertEquals("first session preferences", Files.readString(preferences));
            assertThrows(IOException.class, () -> AppInstanceLock.acquire(lockFile),
                    "The original session must still own its lock");
        }
        try (AppInstanceLock restartedSession = AppInstanceLock.acquire(lockFile)) {
            assertNotNull(restartedSession, "The lock must be available after the first session closes");
        }
    }

    @Test
    public void startup_writableFolder_opensMainWindowFromProductionEntryPoint() throws Exception {
        runChild("welcome");
        assertTrue(Files.exists(directory.resolve("opened.txt")));
    }

    /** Runs one real JavaFX startup and requires a clean exit, retaining coverage from every child. */
    private void runChild(String scenario) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        Path coverage = Path.of(System.getProperty("test.lifecycleCoverageFile",
                directory.resolve("lifecycle.exec").toString()));
        List<String> agents = TestUtil.childCoverageArguments(coverage);
        command.addAll(agents);
        command.addAll(List.of("-cp", System.getProperty("test.runtimeClasspath",
                System.getProperty("java.class.path")), Launcher.class.getName(), scenario));
        Path log = directory.resolve(scenario + ".log");
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Startup did not exit: " + Files.readString(log));
            assertEquals(0, process.exitValue(), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        if (!agents.isEmpty()) {
            assertTrue(Files.exists(coverage), "The child JVM did not produce coverage data");
        }
    }

    /** Supplies a main class separate from Application, matching the production launcher arrangement. */
    public static class Launcher {
        public static void main(String[] args) {
            if (args[0].equals("welcome")) {
                observeWindow();
                Main.main(args);
                return;
            }
            Application.launch(args[0].equals("blocked") ? BlockedStartupApp.class : CancelRecoveryApp.class, args);
        }

        /** Closes a successful production launch after recording that its main window became visible. */
        private static void observeWindow() {
            Thread.startVirtualThread(() -> {
                try {
                    while (!Files.exists(Path.of("opened.txt"))) {
                        try {
                            Platform.runLater(() -> {
                                if (Window.getWindows().stream().anyMatch(window -> window instanceof Stage stage
                                        && stage.isShowing() && stage.getTitle().contains("TrackCall"))) {
                                    try {
                                        Files.writeString(Path.of("opened.txt"), "TrackCall opened");
                                    } catch (IOException e) {
                                        throw new IllegalStateException(e);
                                    }
                                    Platform.exit();
                                }
                            });
                        } catch (IllegalStateException notStarted) {
                            // The observer starts before JavaFX initializes its event queue.
                        }
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /** Checks that a refused second launch displays its reason before exiting without initializing storage. */
    public static class BlockedStartupApp extends MainApp {
        @Override
        public void start(Stage stage) {
            Platform.runLater(() -> {
                DialogPane pane = (DialogPane) Window.getWindows().getFirst().getScene().getRoot();
                assertTrue(pane.getScene().getWindow().isShowing());
                assertEquals(AppInstanceLock.MESSAGE_ALREADY_RUNNING, pane.getContentText());
                Button button = (Button) pane.lookupButton(pane.getButtonTypes().getFirst());
                button.fire();
            });
            super.start(stage);
            assertFalse(stage.isShowing());
            assertNull(storage);
            assertNull(model);
            try {
                Files.writeString(Path.of("blocked.txt"), "A visible message preceded clean shutdown");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Cancels the real startup dialog and records that no editable roster was opened. */
    public static class CancelRecoveryApp extends MainApp {
        @Override
        public void start(Stage stage) {
            Platform.runLater(() -> {
                DialogPane pane = (DialogPane) Window.getWindows().getFirst().getScene().getRoot();
                var close = pane.getButtonTypes().stream().filter(button -> button.getText().equals("Close"))
                        .findFirst().orElseThrow();
                Button button = (Button) pane.lookupButton(close);
                button.fire();
            });
            super.start(stage);
            assertFalse(stage.isShowing());
            try {
                Files.writeString(Path.of("cancelled.txt"), "No editable roster was opened");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
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

/** Runs actual application shutdown in an isolated JVM and retains its separate JaCoCo execution data. */
public class MainAppLifecycleTest {
    @TempDir
    public Path directory;

    @Test
    public void startup_cancelRecovery_exitsWithoutOpeningOrReplacingRoster() throws Exception {
        Path roster = directory.resolve("data/addressbook.json");
        Files.createDirectories(roster.getParent());
        Files.writeString(roster, "damaged roster");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        Path coverage = Path.of(System.getProperty("test.lifecycleCoverageFile",
                directory.resolve("lifecycle.exec").toString()));
        List<String> agents = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.startsWith("-javaagent:") && argument.contains("jacoco"))
                .map(argument -> argument.split("=", 2)[0] + "=destfile=" + coverage + ",append=false").toList();
        command.addAll(agents);
        command.addAll(List.of("-cp", System.getProperty("test.runtimeClasspath",
                System.getProperty("java.class.path")), Launcher.class.getName()));
        Path log = directory.resolve("startup.log");
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Startup cancellation did not exit");
            assertEquals(0, process.exitValue(), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        assertTrue(Files.exists(directory.resolve("cancelled.txt")), Files.readString(log));
        assertEquals("damaged roster", Files.readString(roster));
        if (!agents.isEmpty()) {
            assertTrue(Files.exists(coverage), "The child JVM did not produce coverage data");
        }
    }

    /** Supplies a main class separate from Application, matching the production launcher arrangement. */
    public static class Launcher {
        public static void main(String[] args) {
            Application.launch(CancelRecoveryApp.class, args);
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

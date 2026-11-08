package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.JDialog;
import javax.swing.Timer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.testutil.TestUtil;

/** Exercises the Windows recovery message and the actual packaged JavaFX native loader. */
public class WindowsGraphicsStartupTest {
    @TempDir
    public Path directory;

    @Test
    public void startup_windowsGraphicsFailure_explainsUnrepresentableFolderOnly() throws Exception {
        for (String folderName : List.of("TrackCall-東京", "TrackCall-café")) {
            Path folder = Files.createDirectory(directory.resolve(folderName));
            Path log = folder.resolve("launch.log");
            boolean showDialog = System.getProperty("os.name").startsWith("Windows")
                    && !GraphicsEnvironment.isHeadless() && folderName.contains("東京");
            List<String> command = new ArrayList<>();
            command.add(javaExecutable());
            Path coverage = Path.of(System.getProperty("test.lifecycleCoverageFile",
                    directory.resolve("lifecycle.exec").toString()));
            command.addAll(TestUtil.childCoverageArguments(coverage));
            command.addAll(List.of("-Djava.awt.headless=" + !showDialog, "-Djavafx.toolkit=MissingTestToolkit",
                    "-cp", System.getProperty("test.runtimeClasspath"), GraphicsFailureLauncher.class.getName()));
            Process process = new ProcessBuilder(command).directory(folder.toFile())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Startup did not finish");
                String output = Files.readString(log, StandardCharsets.ISO_8859_1);
                assertTrue(output.contains("No toolkit found"), output);
                assertTrue(process.exitValue() != 0, output);
                assertEquals(showDialog, output.contains("Displayed recovery dialog"), output);
                assertEquals(folderName.contains("東京"), output.contains("Move the entire TrackCall folder"), output);
            } finally {
                process.destroyForcibly();
            }
        }
    }

    /** Exercises the real JavaFX failure path using a fixed Windows code page on all test platforms. */
    public static class GraphicsFailureLauncher {
        /** Runs in a separate JVM to isolate toolkit selection and platform settings from other tests. */
        public static void main(String[] args) {
            System.setProperty("os.name", "Windows 11");
            System.setProperty("native.encoding", "windows-1252");
            if (!GraphicsEnvironment.isHeadless()) {
                Timer timer = new Timer(100, event -> {
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (window instanceof JDialog dialog && dialog.isShowing()
                                && "TrackCall startup".equals(dialog.getTitle())) {
                            System.out.println("Displayed recovery dialog");
                            dialog.dispose();
                            Timer dialogWatcher = (Timer) event.getSource();
                            dialogWatcher.stop();
                        }
                    }
                });
                timer.start();
            }
            Main.main(args);
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void packagedJar_windowsFolders_startsOrExplainsNativePathFailure() throws Exception {
        for (String folderName : List.of("TrackCall-control", "TrackCall-東京")) {
            Path folder = Files.createDirectory(directory.resolve(folderName));
            Files.copy(Path.of(System.getProperty("test.releaseJar")), folder.resolve("trackcall.jar"));
            Path log = folder.resolve("launch.log");
            // Suppress only the Swing recovery dialog. JavaFX still uses the real Windows graphics loader.
            Process process = new ProcessBuilder(javaExecutable(), "-Djava.awt.headless=true", "-jar", "trackcall.jar")
                    .directory(folder.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                while (process.isAlive() && System.nanoTime() < deadline
                        && !Files.readString(log, StandardCharsets.ISO_8859_1).contains("Starting UI...")) {
                    process.waitFor(100, TimeUnit.MILLISECONDS);
                }
                String output = Files.readString(log, StandardCharsets.ISO_8859_1);
                if (output.contains("Starting UI...")) {
                    System.out.println(folderName + ": initialized the Windows graphics toolkit");
                } else {
                    assertTrue(!Charset.forName(System.getProperty("native.encoding")).newEncoder()
                            .canEncode(folder.toString()), output);
                    assertTrue(output.contains("Move the entire TrackCall folder"), output);
                    assertTrue(process.waitFor(5, TimeUnit.SECONDS), output);
                    assertNotEquals(0, process.exitValue(), output);
                    System.out.println(folderName + ": reported the Windows folder recovery steps");
                }
            } finally {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    private String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}

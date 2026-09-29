package seedu.address.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests logger startup in a separate JVM so earlier tests cannot hide initialization failures. */
public class LogsCenterTest {

    @TempDir
    public Path testFolder;

    @Test
    public void initialize_unwritableLogFile_consoleLoggingStillWorks() throws Exception {
        // A directory at the log-file path gives a deterministic I/O failure on every OS.
        Files.createDirectory(testFolder.resolve("addressbook.log.0"));
        Path packageDirectory = Files.createDirectories(testFolder.resolve("seedu/address/commons/core"));
        Path probe = packageDirectory.resolve("LogProbe.java");
        Files.writeString(probe, """
                package seedu.address.commons.core;
                /** Launches logging before another test can initialize it. */
                class LogProbe {
                    /** Starts the isolated logging probe. */
                    public static void main(String[] args) {
                        LogsCenter.getLogger("probe").info("Console logging is available");
                    }
                }
                """);
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Path classes = Path.of(LogsCenter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path output = testFolder.resolve("probe-output.txt");
        Process process = new ProcessBuilder(java.toString(), "--class-path", classes.toString(), probe.toString())
                .directory(testFolder.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Logger initialization did not finish");
            String log = Files.readString(output);
            assertEquals(0, process.exitValue(), log);
            assertTrue(log.contains("Error adding file handler for logger"), log);
            assertTrue(log.contains("Console logging is available"), log);
        } finally {
            process.destroyForcibly();
        }
    }
}

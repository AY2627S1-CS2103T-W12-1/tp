package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests session exclusion and automatic operating-system cleanup after process termination. */
public class AppInstanceLockTest {
    @TempDir
    public Path folder;

    @Test
    public void acquire_secondSession_rejectedUntilFirstCloses() throws Exception {
        Path file = folder.resolve(".trackcall.lock");
        try (AppInstanceLock first = AppInstanceLock.acquire(file)) {
            assertNotNull(first);
            IOException failure = assertThrows(IOException.class, () -> AppInstanceLock.acquire(file));
            assertEquals(AppInstanceLock.MESSAGE_ALREADY_RUNNING, failure.getMessage());
        }
        try (AppInstanceLock reopened = AppInstanceLock.acquire(file)) {
            assertNotNull(reopened);
            assertTrue(Files.exists(file));
        }
    }

    @Test
    public void acquire_separateProcesses_rejectedAndReleasedAfterTermination() throws Exception {
        Path lockFile = folder.resolve("process.lock");
        Path readyFile = folder.resolve("ready");
        String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process holder = new ProcessBuilder(executable, "-cp", System.getProperty("test.runtimeClasspath"),
                LockHolder.class.getName(), lockFile.toString(), readyFile.toString())
                .redirectErrorStream(true).redirectOutput(folder.resolve("holder.log").toFile()).start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!Files.exists(readyFile) && holder.isAlive() && System.nanoTime() < deadline) {
                Thread.sleep(25);
            }
            assertTrue(Files.exists(readyFile), () -> "Lock holder failed: " + readLog());
            IOException failure = assertThrows(IOException.class, () -> AppInstanceLock.acquire(lockFile));
            assertEquals(AppInstanceLock.MESSAGE_ALREADY_RUNNING, failure.getMessage());
        } finally {
            holder.destroyForcibly();
            assertTrue(holder.waitFor(10, TimeUnit.SECONDS));
        }
        try (AppInstanceLock recovered = AppInstanceLock.acquire(lockFile)) {
            assertNotNull(recovered);
        }
    }

    private String readLog() {
        try {
            return Files.readString(folder.resolve("holder.log"));
        } catch (IOException exception) {
            return exception.toString();
        }
    }

    /** Holds a lock in a real child process until the parent terminates it. */
    public static class LockHolder {
        /** Signals successful acquisition before blocking until termination. */
        public static void main(String[] args) throws Exception {
            try (AppInstanceLock held = AppInstanceLock.acquire(Path.of(args[0]))) {
                if (held != null) {
                    Files.writeString(Path.of(args[1]), "ready");
                    System.in.read();
                }
            }
        }
    }
}

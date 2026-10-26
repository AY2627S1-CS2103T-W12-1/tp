package seedu.address.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import seedu.address.commons.util.FileUtil;

/** Prevents independent application sessions from overwriting the same roster with stale data. */
public final class AppInstanceLock implements AutoCloseable {
    public static final String FILE_NAME = ".trackcall.lock";
    public static final String MESSAGE_ALREADY_RUNNING =
            "TrackCall is already running from this folder. Use the existing window or close it before restarting.";

    private final FileChannel channel;
    private final FileLock lock;

    private AppInstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * Acquires an operating-system lock without waiting for another session to finish.
     * The lock file remains on disk; only a live process holding its lock prevents another launch.
     *
     * @param file Lock file inside the application's writable folder.
     * @return A lock that must remain open until the application exits.
     * @throws IOException If another session holds the lock or the file cannot be opened.
     */
    public static AppInstanceLock acquire(Path file) throws IOException {
        Path resolved = FileUtil.resolvePath(file);
        FileChannel channel = FileChannel.open(resolved, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException exception) {
                throw new IOException(MESSAGE_ALREADY_RUNNING, exception);
            }
            if (lock == null) {
                throw new IOException(MESSAGE_ALREADY_RUNNING);
            }
            return new AppInstanceLock(channel, lock);
        } catch (IOException | RuntimeException exception) {
            channel.close();
            throw exception;
        }
    }

    /** Releases this session's lock without deleting the shared lock file. */
    @Override
    public void close() throws IOException {
        try {
            if (lock.isValid()) {
                lock.release();
            }
        } finally {
            channel.close();
        }
    }
}

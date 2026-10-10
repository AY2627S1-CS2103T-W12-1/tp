package seedu.address.commons.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Resolves application paths and reads or writes files.
 */
public class FileUtil {

    private static final String CHARSET = "UTF-8";
    private static final Path CODE_LOCATION = getCodeLocation();
    private static final boolean IS_PACKAGED = !Files.isDirectory(CODE_LOCATION);
    private static final Path HOME_FOLDER = IS_PACKAGED
            ? CODE_LOCATION.getParent() : Path.of("").toAbsolutePath().normalize();

    /**
     * Returns the JAR folder, or the working directory for an unpackaged development run.
     */
    public static Path getHomeFolder() {
        return HOME_FOLDER;
    }

    /**
     * Resolves a released application's file inside its JAR folder.
     * Unpackaged runs retain their existing paths so development fixtures can use temporary folders.
     *
     * @throws IOException if a packaged path leaves the JAR folder or traverses a symbolic link.
     */
    public static Path resolvePath(Path file) throws IOException {
        if (!IS_PACKAGED) {
            return file;
        }
        Path target = HOME_FOLDER.resolve(file).normalize();
        if (!target.startsWith(HOME_FOLDER)) {
            throw new IOException("Files must stay inside the JAR folder: " + HOME_FOLDER);
        }
        Path current = HOME_FOLDER;
        for (Path component : HOME_FOLDER.relativize(target)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current) || (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !current.toRealPath().startsWith(HOME_FOLDER))) {
                throw new IOException("Symbolic links are not allowed in application file paths.");
            }
        }
        return target;
    }

    /** Returns the real location of the classes or JAR without depending on the launch directory. */
    private static Path getCodeLocation() {
        try {
            return Path.of(FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("Could not locate the application folder.", e);
        }
    }

    /**
     * Creates a file if it does not exist along with its missing parent directories.
     * @throws IOException if the file or directory cannot be created.
     */
    public static void createIfMissing(Path file) throws IOException {
        file = resolvePath(file);
        if (Files.exists(file)) {
            return;
        }

        createParentDirsOfFile(file);

        Files.createFile(file);
    }

    /**
     * Creates parent directories of file if it has a parent directory
     */
    private static void createParentDirsOfFile(Path file) throws IOException {
        Path parentDir = file.getParent();

        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
    }

    /**
     * Assumes file exists
     */
    public static String readFromFile(Path file) throws IOException {
        file = resolvePath(file);
        return new String(Files.readAllBytes(file), CHARSET);
    }

    /**
     * Writes UTF-8 content to a sibling temporary file and atomically replaces the target.
     * Creates missing parent directories. A write failure leaves an existing target untouched.
     *
     * @throws IOException if writing or atomic replacement fails, including unsupported file systems.
     */
    public static void writeToFile(Path file, String content) throws IOException {
        Path target = resolvePath(file).toAbsolutePath();
        createParentDirsOfFile(target);
        Path temporary = Files.createTempFile(target.getParent(), ".trackcall-", ".tmp");
        try {
            Files.writeString(temporary, content);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

}

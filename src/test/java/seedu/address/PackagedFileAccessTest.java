package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.FileUtil;

/** Tests the released application's file boundary and native preparation without launching a GUI. */
public class PackagedFileAccessTest {
    private static final List<Class<?>> PACKAGED_CLASSES = List.of(Main.class, MainApp.class,
            FileUtil.class, LogsCenter.class);
    private static final List<String> LIBRARIES = List.of("glass", "javafx_iio", "javafx_font", "prism_common",
            "prism_es2", "decora_sse", "prism_sw");
    private static final List<String> PROPERTY_NAMES = List.of("os.name", "os.arch", "javafx.cachedir",
            "java.io.tmpdir", "java.library.path");

    @TempDir
    public Path temporaryFolder;

    private final Map<String, String> originalProperties = new HashMap<>();
    private final Set<Handler> openedHandlers = new HashSet<>();
    private final Logger logger = Logger.getLogger("ab3");
    private Handler[] originalHandlers;
    private Level originalLevel;
    private boolean originalParentHandlers;

    @BeforeEach
    public void saveProcessSettings() {
        PROPERTY_NAMES.forEach(name -> originalProperties.put(name, System.getProperty(name)));
        originalHandlers = logger.getHandlers();
        originalLevel = logger.getLevel();
        originalParentHandlers = logger.getUseParentHandlers();
    }

    @AfterEach
    public void restoreProcessSettings() {
        originalProperties.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
        Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler);
        openedHandlers.stream().filter(handler -> !Arrays.asList(originalHandlers).contains(handler))
                .forEach(Handler::close);
        Arrays.stream(originalHandlers).forEach(logger::addHandler);
        logger.setLevel(originalLevel);
        logger.setUseParentHandlers(originalParentHandlers);
    }

    @Test
    public void packagedPaths_insideWorkAndOutsideAreRejected() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home")).toRealPath();
        try (URLClassLoader loader = packagedLoader(home, true)) {
            Class<?> utility = loader.loadClass(FileUtil.class.getName());
            assertEquals(home, utility.getMethod("getHomeFolder").invoke(null));
            Path target = resolve(utility, Path.of("data", "record.txt"));
            assertEquals(home.resolve("data/record.txt"), target);
            utility.getMethod("writeToFile", Path.class, String.class).invoke(null, Path.of("data/record.txt"), "keep");
            assertEquals("keep", utility.getMethod("readFromFile", Path.class)
                    .invoke(null, Path.of("data/record.txt")));
            utility.getMethod("createIfMissing", Path.class).invoke(null, Path.of("data/new.txt"));
            assertTrue(Files.isRegularFile(home.resolve("data/new.txt")));

            for (Path outside : List.of(temporaryFolder.resolve("outside.txt"), Path.of("..", "outside.txt"))) {
                InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> {
                    resolve(utility, outside);
                });
                assertTrue(error.getCause() instanceof IOException);
            }
            assertFalse(Files.exists(temporaryFolder.resolve("outside.txt")));
        }
    }

    @Test
    public void packagedPaths_symbolicLink_rejected() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home"));
        createLink(home.resolve("escape"), temporaryFolder);
        try (URLClassLoader loader = packagedLoader(home, true)) {
            InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> {
                resolve(loader.loadClass(FileUtil.class.getName()), Path.of("escape/outside.txt"));
            });
            assertTrue(error.getCause() instanceof IOException);
            assertFalse(Files.exists(temporaryFolder.resolve("outside.txt")));
        }
    }

    @Test
    public void startup_nonMac_cacheAndFallbackStayInsideHome() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home")).toRealPath();
        Path cache = Files.createDirectories(home.resolve(".javafx-cache/existing"));
        Files.writeString(cache.resolve("cached.txt"), "keep");
        System.setProperty("os.name", "Linux");
        try (URLClassLoader loader = packagedLoader(home, true)) {
            initialize(loader, Main.class.getName());
            assertEquals(home.resolve(".javafx-cache").toString(), System.getProperty("javafx.cachedir"));
            assertEquals(System.getProperty("javafx.cachedir"), System.getProperty("java.io.tmpdir"));
            assertEquals("keep", Files.readString(cache.resolve("cached.txt")));
        }
    }

    @Test
    public void startup_macArchitectures_extractMatchingLibraries() throws Exception {
        System.setProperty("os.name", "Mac OS X");
        for (String arch : List.of("x86_64", "aarch64", "arm64")) {
            System.setProperty("os.arch", arch);
            Path home = Files.createDirectory(temporaryFolder.resolve(arch)).toRealPath();
            String folder = arch.equals("x86_64") ? "mac-x64" : "mac-arm64";
            try (URLClassLoader loader = packagedLoader(home, true)) {
                initialize(loader, Main.class.getName());
                Path natives = home.resolve(".javafx-cache").resolve(folder);
                assertEquals(natives.toString(), System.getProperty("java.library.path"));
                for (String library : LIBRARIES) {
                    assertEquals(folder, Files.readString(natives.resolve("lib" + library + ".dylib")));
                }
            }
        }
    }

    @Test
    public void startup_defaultDataAndPreferences_useJarFolder() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home")).toRealPath();
        try (URLClassLoader loader = packagedLoader(home, true)) {
            initialize(loader, MainApp.class.getName());
            Class<?> application = loader.loadClass(MainApp.class.getName());
            for (String name : List.of("USER_PREFS_FILE_PATH", "ADDRESS_BOOK_FILE_PATH")) {
                Field field = application.getDeclaredField(name);
                field.setAccessible(true);
                Path expected = home.resolve(name.equals("USER_PREFS_FILE_PATH")
                        ? "preferences.json" : "data/addressbook.json");
                assertEquals(expected, field.get(null));
            }
        }
    }

    @Test
    public void startup_missingNativeResources_doesNotCopyUnrelatedFiles() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home"));
        System.setProperty("os.name", "Mac OS X");
        System.setProperty("os.arch", "x86_64");
        try (URLClassLoader loader = packagedLoader(home, false)) {
            initialize(loader, Main.class.getName());
            assertFalse(Files.exists(home.resolve(".javafx-cache/mac-x64")));
        }
    }

    @Test
    public void startup_cacheIsAFile_failsWithoutUsingOutsideTemporaryFolder() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home"));
        Files.writeString(home.resolve(".javafx-cache"), "keep");
        try (URLClassLoader loader = packagedLoader(home, true)) {
            assertThrows(ExceptionInInitializerError.class, () -> initialize(loader, Main.class.getName()));
            assertEquals(originalProperties.get("java.io.tmpdir"), System.getProperty("java.io.tmpdir"));
            assertEquals("keep", Files.readString(home.resolve(".javafx-cache")));
        }
    }

    @Test
    public void startup_cacheLink_rejectedBeforeExtraction() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home"));
        createLink(home.resolve(".javafx-cache"), temporaryFolder);
        try (URLClassLoader loader = packagedLoader(home, true)) {
            assertThrows(ExceptionInInitializerError.class, () -> initialize(loader, Main.class.getName()));
            assertFalse(Files.exists(temporaryFolder.resolve("mac-arm64")));
        }
    }

    @Test
    public void logging_symbolicLink_usesConsoleWithoutChangingOutsideFile() throws Exception {
        Path home = Files.createDirectory(temporaryFolder.resolve("home"));
        Path outside = temporaryFolder.resolve("outside.txt");
        Files.writeString(outside, "keep");
        createLink(home.resolve("addressbook.log.0"), outside);
        try (URLClassLoader loader = packagedLoader(home, true)) {
            initialize(loader, LogsCenter.class.getName());
            assertFalse(Arrays.stream(logger.getHandlers()).anyMatch(handler -> handler instanceof FileHandler));
            assertEquals("keep", Files.readString(outside));
        }
    }

    /** Initializes isolated startup code and tracks log handlers so Windows can remove temporary files afterwards. */
    private void initialize(ClassLoader loader, String name) throws ClassNotFoundException {
        try {
            Class.forName(name, true, loader);
        } finally {
            openedHandlers.addAll(Arrays.asList(logger.getHandlers()));
        }
    }

    private Path resolve(Class<?> utility, Path path) throws Exception {
        return (Path) utility.getMethod("resolvePath", Path.class).invoke(null, path);
    }

    /** Skips link-specific checks on systems where the test account cannot create symbolic links. */
    private void createLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links are unavailable: " + e.getMessage());
        }
    }

    /** Builds a tiny JAR with real startup classes and labelled native fixtures; no native code is executed. */
    private URLClassLoader packagedLoader(Path home, boolean includeNatives) throws IOException {
        Path jar = home.resolve("application.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            for (Class<?> type : PACKAGED_CLASSES) {
                String resource = type.getName().replace('.', '/') + ".class";
                output.putNextEntry(new JarEntry(resource));
                try (InputStream input = type.getResourceAsStream("/" + resource)) {
                    input.transferTo(output);
                }
                output.closeEntry();
            }
            if (includeNatives) {
                for (String folder : List.of("mac-arm64", "mac-x64")) {
                    for (String library : LIBRARIES) {
                        output.putNextEntry(new JarEntry("natives/" + folder + "/lib" + library + ".dylib"));
                        output.write(folder.getBytes(StandardCharsets.UTF_8));
                        output.closeEntry();
                    }
                }
            }
        }
        // Load startup classes from the JAR so FileUtil sees a real packaged code location.
        return new URLClassLoader(new URL[] {jar.toUri().toURL()}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean shouldResolve) throws ClassNotFoundException {
                if (PACKAGED_CLASSES.stream().noneMatch(type -> type.getName().equals(name))) {
                    return super.loadClass(name, shouldResolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        type = findClass(name);
                    }
                    if (shouldResolve) {
                        resolveClass(type);
                    }
                    return type;
                }
            }
        };
    }
}

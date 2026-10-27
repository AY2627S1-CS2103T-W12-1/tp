package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests complete-file replacement and cleanup when saving fails. */
public class FileUtilTest {

    @TempDir
    public Path testFolder;

    @Test
    public void createIfMissing_existingFile_preservesContents() throws IOException {
        Path target = testFolder.resolve("roster.json");
        Files.writeString(target, "Existing roster");
        FileUtil.createIfMissing(target);
        assertEquals("Existing roster", Files.readString(target));
        assertOnlyEntry(testFolder, target);
    }

    @Test
    public void writeAtomically_cleanupFails_keepsOriginalFailureAndPreviousFile() throws IOException {
        Path target = testFolder.resolve("roster.json");
        Files.writeString(target, "Existing roster");
        IOException failure = assertThrows(IOException.class, () -> FileUtil.writeAtomically(target, temporary -> {
            Files.delete(temporary);
            Files.createDirectory(temporary);
            Files.writeString(temporary.resolve("keep.txt"), "Blocks removal");
            throw new IOException("Write failed");
        }));

        assertEquals("Write failed", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertTrue(failure.getSuppressed()[0] instanceof IOException);
        assertEquals("Existing roster", Files.readString(target));
    }

    @Test
    public void writeToFile_missingParent_createsFileWithCompleteContent() throws IOException {
        Path target = testFolder.resolve("nested").resolve("roster.json");
        FileUtil.writeToFile(target, "New roster: José");
        assertEquals("New roster: José", Files.readString(target));
        assertOnlyEntry(target.getParent(), target);
    }

    @Test
    public void writeToFile_existingFile_replacesCompleteContent() throws IOException {
        Path target = testFolder.resolve("roster.json");
        Files.writeString(target, "Old saved roster that is longer than its replacement");
        FileUtil.writeToFile(target, "New roster");
        assertEquals("New roster", Files.readString(target));
        assertOnlyEntry(testFolder, target);
    }

    @Test
    public void writeToFile_replacementFails_preservesTargetAndRemovesTemporaryFile() throws IOException {
        Path target = Files.createDirectory(testFolder.resolve("roster.json"));
        Path sentinel = target.resolve("existing-data.txt");
        Files.writeString(sentinel, "Keep this data");

        assertThrows(IOException.class, () -> FileUtil.writeToFile(target, "New roster"));

        assertEquals("Keep this data", Files.readString(sentinel));
        assertOnlyEntry(testFolder, target);
        assertOnlyEntry(target, sentinel);
    }

    @Test
    public void writeAtomically_writerFailsAfterPartialOutput_preservesPreviousFile() throws IOException {
        Path target = testFolder.resolve("members.csv");
        Files.writeString(target, "Previous complete export");

        assertThrows(IOException.class, () -> FileUtil.writeAtomically(target, temporary -> {
            Files.writeString(temporary, "Partial replacement");
            throw new IOException("Simulated disk full");
        }));

        assertEquals("Previous complete export", Files.readString(target));
        assertOnlyEntry(testFolder, target);
    }

    @Test
    public void writeAtomically_existingPosixFile_preservesPermissions() throws IOException {
        assumeTrue(Files.getFileAttributeView(testFolder, PosixFileAttributeView.class) != null);
        Path target = testFolder.resolve("members.csv");
        for (String mode : List.of("rw-r--r--", "rw-rw----", "rw-------")) {
            Files.writeString(target, "Previous export");
            var permissions = PosixFilePermissions.fromString(mode);
            Files.setPosixFilePermissions(target, permissions);

            FileUtil.writeAtomically(target, temporary -> Files.writeString(temporary, "New export"));

            assertEquals("New export", Files.readString(target));
            assertEquals(permissions, Files.getPosixFilePermissions(target));
            assertOnlyEntry(testFolder, target);
        }
    }

    @Test
    public void writeAtomically_writerCopiesDifferentPermissions_preservesTargetPermissions() throws IOException {
        assumeTrue(Files.getFileAttributeView(testFolder, PosixFileAttributeView.class) != null);
        Path source = testFolder.resolve("source.csv");
        Path target = testFolder.resolve("members.csv");
        Files.writeString(source, "New export");
        Files.setPosixFilePermissions(source, PosixFilePermissions.fromString("rw-------"));
        Files.writeString(target, "Previous export");
        var permissions = PosixFilePermissions.fromString("rw-r--r--");
        Files.setPosixFilePermissions(target, permissions);

        FileUtil.writeAtomically(target,
                temporary -> Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING));

        assertEquals("New export", Files.readString(target));
        assertEquals(permissions, Files.getPosixFilePermissions(target));
    }

    @Test
    public void writeAtomically_writerFails_preservesPosixPermissionsAndContents() throws IOException {
        assumeTrue(Files.getFileAttributeView(testFolder, PosixFileAttributeView.class) != null);
        Path target = testFolder.resolve("members.csv");
        Files.writeString(target, "Previous export");
        var permissions = PosixFilePermissions.fromString("rw-r--r--");
        Files.setPosixFilePermissions(target, permissions);

        assertThrows(IOException.class, () -> FileUtil.writeAtomically(target, temporary -> {
            Files.writeString(temporary, "Partial export");
            throw new IOException("Simulated write failure");
        }));

        assertEquals("Previous export", Files.readString(target));
        assertEquals(permissions, Files.getPosixFilePermissions(target));
        assertOnlyEntry(testFolder, target);
    }

    /** Checks that saving did not leave a temporary file beside the target. */
    private void assertOnlyEntry(Path directory, Path expected) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            assertEquals(List.of(expected), entries.toList());
        }
    }
}

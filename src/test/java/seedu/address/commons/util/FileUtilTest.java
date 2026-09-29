package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests complete-file replacement and cleanup when saving fails. */
public class FileUtilTest {

    @TempDir
    public Path testFolder;

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

    /** Checks that saving did not leave a temporary file beside the target. */
    private void assertOnlyEntry(Path directory, Path expected) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            assertEquals(List.of(expected), entries.toList());
        }
    }
}

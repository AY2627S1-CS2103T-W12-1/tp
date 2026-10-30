package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.testutil.PersonBuilder;

/** Checks whether failed saves restore the access settings of an existing shared backup. */
public class JsonBackupPermissionsTest {
    @TempDir
    public Path directory;

    @Test
    public void save_failedActiveWrite_preservesExistingBackupPermissions() throws Exception {
        assumeTrue(Files.getFileAttributeView(directory, PosixFileAttributeView.class) != null);
        List<String> changedModes = new ArrayList<>();
        for (String mode : List.of("rw-------", "rw-r--r--", "rw-rw----", "rw-rw-rw-", "rw-r-----")) {
            Path home = Files.createDirectory(directory.resolve(mode));
            Path active = home.resolve("addressbook.json");
            Path backup = JsonAddressBookStorage.getBackupFilePath(active);
            JsonAddressBookStorage normal = new JsonAddressBookStorage(active);
            normal.saveAddressBook(roster("Previous Member"));
            normal.saveAddressBook(roster("Current Member"));
            Files.setPosixFilePermissions(backup, PosixFilePermissions.fromString(mode));
            byte[] original = Files.readAllBytes(active);
            byte[] previous = Files.readAllBytes(backup);
            JsonAddressBookStorage failing = new JsonAddressBookStorage(active) {
                @Override
                protected void writeActiveRoster(ReadOnlyAddressBook value, Path target) throws IOException {
                    throw new IOException("Simulated active-file write failure");
                }
            };
            assertThrows(IOException.class, () -> failing.saveAddressBook(roster("Updated Member")));
            assertArrayEquals(original, Files.readAllBytes(active));
            assertArrayEquals(previous, Files.readAllBytes(backup));
            String restored = PosixFilePermissions.toString(Files.getPosixFilePermissions(backup));
            System.out.println("Rollback mode: " + mode + " -> " + restored);
            if (!mode.equals(restored)) {
                changedModes.add(mode + " -> " + restored);
            }
            Files.setPosixFilePermissions(backup, PosixFilePermissions.fromString(mode));
            normal.saveAddressBook(roster("Updated Member"));
            assertEquals(mode, PosixFilePermissions.toString(Files.getPosixFilePermissions(backup)),
                    "Normal successful backup replacement control");
        }
        assertEquals(List.of(), changedModes, "Failed saves must preserve existing backup access settings");
    }

    private AddressBook roster(String name) {
        AddressBook result = new AddressBook();
        result.addPerson(new PersonBuilder().withName(name).build());
        return result;
    }
}

package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks that supported export paths round-trip and rejected paths leave existing files intact. */
public class ExportRoundTripTest {
    @TempDir
    public Path folder;

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    public void execute_exportQuotedFilename_preservesDestinationAndRoster() throws Exception {
        ModelManager model = new ModelManager();
        Logic logic = createLogic(model, "first");
        logic.execute("add n/Alice p/123 e/alice@example.com");
        Path existing = folder.resolve("members\"2026.csv");
        Files.writeString(existing, "Existing export");
        assertThrows(ParseException.class, () -> logic.execute("export " + existing));
        assertEquals("Existing export", Files.readString(existing));
        Path missing = folder.resolve("missing\"2026.csv");
        assertThrows(ParseException.class, () -> logic.execute("export " + missing));
        assertFalse(Files.exists(missing));
        assertEquals(1, model.getFilteredPersonList().size());
    }

    @Test
    public void execute_exportSupportedPaths_importsSameMembers() throws Exception {
        ModelManager model = new ModelManager();
        Logic logic = createLogic(model, "source");
        logic.execute("add n/Alice p/+65 123 e/alice@example.com a/Block A/12 t/year-1");
        for (String filename : List.of("members.csv", "member list.CsV", "O'Brien-é.csv")) {
            Path destination = folder.resolve(filename);
            logic.execute("export " + destination);
            ModelManager restored = new ModelManager();
            createLogic(restored, filename).execute("import p/\"" + destination + "\"");
            assertEquals(model.getAddressBook(), restored.getAddressBook());
        }
    }

    /** Creates independent storage for one synthetic session. */
    private Logic createLogic(ModelManager model, String name) {
        return new LogicManager(model, new StorageManager(
                new JsonAddressBookStorage(folder.resolve(name + ".json")),
                new JsonUserPrefsStorage(folder.resolve(name + "-prefs.json"))));
    }
}

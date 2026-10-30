package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks expanded email syntax across command and storage workflows. */
public class EmailValidationWorkflowTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void emailExpandedSyntax_addEditImportAndRoundTrip() throws Exception {
        Path folder = temporaryFolder.resolve("email");
        Session session = new Session(folder);
        String email = "alice__tan@xn--bcher-kva.example";
        session.logic.execute("add n/Alice p/123 e/" + email);
        assertEquals(email, session.saved().getFirst().getEmail().value);
        session.logic.execute("edit 1 e/_alice@example.com");
        assertEquals("_alice@example.com", session.saved().getFirst().getEmail().value);
        Path csv = folder.resolve("input.csv");
        writeCsv(csv, List.of(row("Bob", email)));
        session.logic.execute("import p/" + csv);
        Path exported = folder.resolve("export.csv");
        session.logic.execute("export " + exported);
        Session restored = new Session(folder.resolve("restored"));
        restored.logic.execute("import p/" + exported);
        assertEquals(session.saved(), restored.saved());
        byte[] saved = Files.readAllBytes(session.file);
        writeCsv(csv, List.of(row("Valid", email), row("Invalid", "alice..tan@example.com")));
        assertThrows(CommandException.class, () -> session.logic.execute("import p/" + csv));
        assertArrayEquals(saved, Files.readAllBytes(session.file));
    }

    @Test
    public void userSession_unicodeNamesAndJitteredCommands_areStable() throws Exception {
        Session session = new Session(temporaryFolder.resolve("unicode-session"));
        Random random = new Random(359356L);
        List<String> names = List.of("李明", "Αλέξανδρος", "Мария", "José\u0301 Tan", "नमस्ते सदस्य",
                "علي حسن", "हана", "Zoë", "山田太郎", "Élodie O'Connor");
        int index = 0;
        for (String name : names) {
            String email = "member_" + index + "@xn--bcher-kva.example";
            String gap = " ".repeat(1 + random.nextInt(4));
            session.logic.execute("add" + gap + "n/" + name + gap + "p/123" + index + gap + "e/" + email);
            assertEquals(name, session.saved().getLast().getName().fullName);
            assertEquals(email, session.saved().getLast().getEmail().value);
            index++;
        }
        for (String unicode : List.of("álîce@例子.example", "alice@例子.example", "álîce@example.com",
                "alice@ｅxample.com", "alice@xn--bücher.example", "😀@example.com")) {
            assertThrows(ParseException.class, () -> session.logic.execute(
                    "edit 1 e/" + unicode), unicode);
        }
        assertEquals(names, session.saved().stream().map(person -> person.getName().fullName).toList());
    }

    private static List<String> row(String name, String email) {
        return List.of(name, "123", email, "Block 1", "committee");
    }

    private static void writeCsv(Path file, List<List<String>> rows) throws IOException {
        StringWriter contents = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(contents, CSVFormat.RFC4180)) {
            printer.printRecord("name", "phone", "email", "address", "tags");
            printer.printRecords(rows);
        }
        Files.writeString(file, contents.toString());
    }

    /** A fresh application-logic instance using only disposable JSON and CSV files. */
    private static class Session {
        private final Path file;
        private final ModelManager model = new ModelManager();
        private final LogicManager logic;

        private Session(Path folder) throws IOException {
            Files.createDirectories(folder);
            file = folder.resolve("members.json");
            logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                    new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
        }

        private List<Person> saved() throws Exception {
            return new JsonAddressBookStorage(file).readAddressBook().orElseThrow().getPersonList();
        }
    }
}

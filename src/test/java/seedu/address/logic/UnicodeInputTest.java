package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Address;
import seedu.address.storage.CsvAddressBookImporter;
import seedu.address.storage.CsvImportException;

/** Checks pasted Unicode spaces at command, field-validation and CSV boundaries. */
public class UnicodeInputTest {
    @TempDir
    public Path directory;

    @Test
    public void search_unicodeSpacesBetweenKeywords_matchesBothMembers() throws Exception {
        Model model = new ModelManager();
        AddressBookParser parser = new AddressBookParser();
        parser.parseCommand("add n/Alice p/123 e/alice@example.com").execute(model);
        parser.parseCommand("add n/Bob p/456 e/bob@example.com").execute(model);
        for (String separator : List.of(" ", "\t", "\u00a0", "\u2007", "\u202f", "\u2003")) {
            for (String command : List.of("find Alice" + separator + "Bob",
                    "findemail alice@example.com" + separator + "bob@example.com")) {
                parser.parseCommand("list").execute(model);
                parser.parseCommand(command).execute(model);
                assertEquals(2, model.getFilteredPersonList().size(), command);
            }
        }
    }

    @Test
    public void search_onlyUnicodeSpaces_reportsMissingKeywords() {
        AddressBookParser parser = new AddressBookParser();
        for (String command : List.of("find", "findemail")) {
            assertThrows(ParseException.class, () -> parser.parseCommand(command + " \u00a0\u2007\u202f"));
        }
    }

    @Test
    public void address_onlyUnicodeSpaces_rejectsAddEditAndCsv() throws Exception {
        Model model = new ModelManager();
        AddressBookParser parser = new AddressBookParser();
        parser.parseCommand("add n/Alice p/123 e/alice@example.com a/原宿通り").execute(model);
        Path csv = directory.resolve("members.csv");
        for (String blank : List.of("\u00a0", "\u2007", "\u202f", "\u00a0 \u202f")) {
            assertThrows(ParseException.class, () -> parser.parseCommand(
                    "add n/Bob p/456 e/bob@example.com a/" + blank + " t/test"));
            assertThrows(ParseException.class, () -> parser.parseCommand("edit 1 a/" + blank));
            Files.writeString(csv, "name,phone,email,address,tags\nBob,456,bob@example.com," + blank + ",\n");
            assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(csv));
            assertEquals("原宿通り", model.getFilteredPersonList().getFirst().getAddress().value);
        }
    }

    @Test
    public void address_visibleUnicodeAndExplicitClear_remainSupported() throws Exception {
        Model model = new ModelManager();
        AddressBookParser parser = new AddressBookParser();
        String address = "日本\u00a0通り 12";
        parser.parseCommand("add n/Alice p/123 e/alice@example.com a/" + address).execute(model);
        assertEquals(address, model.getFilteredPersonList().getFirst().getAddress().value);
        parser.parseCommand("edit 1 a/").execute(model);
        assertEquals(Address.EMPTY_ADDRESS, model.getFilteredPersonList().getFirst().getAddress());
    }
}

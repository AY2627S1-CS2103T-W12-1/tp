package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.logic.parser.ParserUtil;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;

/** Shared validation at command, import, and saved-file boundaries. */
public class MemberFieldValidationTest {
    @TempDir
    public Path tempDir;

    @Test
    public void lengths_exactLimitAccepted_oneMoreRejected() {
        assertTrue(Name.isValidName("A".repeat(Name.MAX_LENGTH)));
        assertFalse(Name.isValidName("A".repeat(Name.MAX_LENGTH + 1)));
        assertTrue(Phone.isValidPhone("1".repeat(Phone.MAX_LENGTH)));
        assertFalse(Phone.isValidPhone("1".repeat(Phone.MAX_LENGTH + 1)));
        assertTrue(Email.isValidEmail("a".repeat(Email.MAX_LENGTH - 5) + "@x.co"));
        assertFalse(Email.isValidEmail("a".repeat(Email.MAX_LENGTH - 4) + "@x.co"));
        assertTrue(Address.isValidAddress("A".repeat(Address.MAX_LENGTH)));
        assertFalse(Address.isValidAddress("A".repeat(Address.MAX_LENGTH + 1)));
        assertTrue(Address.isValidAddress("\ud83d\ude00".repeat(Address.MAX_LENGTH / 2)));
        assertFalse(Address.isValidAddress("\ud83d\ude00".repeat(Address.MAX_LENGTH / 2) + "a"));
    }

    @Test
    public void oversizedFields_rejectedAcrossInputBoundaries() throws Exception {
        String[][] rows = {
            {"A".repeat(Name.MAX_LENGTH + 1), "123", "a@x.co", "Home", Name.MESSAGE_CONSTRAINTS},
            {"Alice", "1".repeat(Phone.MAX_LENGTH + 1), "a@x.co", "Home", Phone.MESSAGE_CONSTRAINTS},
            {"Alice", "123", "a".repeat(Email.MAX_LENGTH) + "@x.co", "Home", Email.MESSAGE_CONSTRAINTS},
            {"Alice", "123", "a@x.co", "A".repeat(Address.MAX_LENGTH + 1), Address.MESSAGE_CONSTRAINTS}
        };
        assertThrows(ParseException.class, () -> ParserUtil.parseName(rows[0][0]));
        assertThrows(ParseException.class, () -> ParserUtil.parsePhone(rows[1][1]));
        assertThrows(ParseException.class, () -> ParserUtil.parseEmail(rows[2][2]));
        assertThrows(ParseException.class, () -> ParserUtil.parseAddress(rows[3][3]));
        for (String[] row : rows) {
            assertRejectedByStorage(row[0], row[1], row[2], row[3], row[4]);
        }
    }

    @Test
    public void bidiControls_rejectedEverywhere_legitimateRtlRetained() throws Exception {
        int[] controls = {0x061c, 0x200e, 0x200f, 0x202a, 0x202b, 0x202c,
            0x202d, 0x202e, 0x2066, 0x2067, 0x2068, 0x2069};
        for (int control : controls) {
            String mark = Character.toString(control);
            for (String value : List.of(mark + "abc", "abc" + mark + "def", "abc" + mark)) {
                assertFalse(Address.isValidAddress(value));
                assertThrows(IllegalArgumentException.class, () -> new Address(value));
                assertThrows(ParseException.class, () -> ParserUtil.parseAddress(value));
                assertRejectedByStorage("Alice", "123", "a@x.co", value, Address.MESSAGE_CONSTRAINTS);
            }
        }
        assertTrue(Address.isValidAddress(""));
        assertEquals("شارع שלום 12", ParserUtil.parseAddress("شارع שלום 12").value);
        assertTrue(Address.isValidAddress("北京 12 \ud83d\ude00"));
    }

    private void assertRejectedByStorage(String name, String phone, String email, String address,
            String message) throws Exception {
        Path csv = tempDir.resolve("members.csv");
        Files.writeString(csv, "name,phone,email,address,tags\n"
                + String.join(",", name, phone, email, address, "") + "\n");
        CsvImportException csvError = assertThrows(CsvImportException.class, () -> CsvAddressBookImporter.read(csv));
        assertEquals("row 2: " + message, csvError.getMessage());
        Path json = tempDir.resolve("members.json");
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(List.of(
                new JsonAdaptedPerson(name, phone, email, address, List.of()))), json);
        String original = Files.readString(json);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(json);
        DataLoadingException error = assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertEquals(message, error.getCause().getMessage());
        assertEquals(original, Files.readString(json));
        Path preserved = storage.preserveDamagedFile();
        assertEquals(original, Files.readString(preserved));
        assertEquals(original, Files.readString(json));
    }
}

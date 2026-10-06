package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

/**
 * Reads member records from the CSV format accepted by the import command.
 */
public final class CsvAddressBookImporter {

    public static final String EXPECTED_HEADER = "name,phone,email,address,tags";
    public static final String MESSAGE_INVALID_HEADER = "invalid header. Expected " + EXPECTED_HEADER + ".";
    public static final String MESSAGE_INVALID_UTF8 = "invalid UTF-8 encoding.";
    public static final String MESSAGE_INVALID_RECORD_ENDING = "invalid CSV record ending.";
    public static final String MESSAGE_INVALID_CSV = "invalid CSV syntax.";
    public static final String MESSAGE_WRONG_FIELD_COUNT = "expected 5 fields but found %d.";

    private static final int FIELD_COUNT = 5;
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    private CsvAddressBookImporter() {
        // Utility class.
    }

    /**
     * Reads and validates every record in {@code filePath} without changing the source file.
     *
     * @throws IOException if the file cannot be read.
     * @throws CsvImportException if the file is not valid UTF-8 CSV or contains an invalid member.
     */
    public static List<Person> read(Path filePath) throws IOException, CsvImportException {
        requireNonNull(filePath);
        byte[] bytes = Files.readAllBytes(filePath);
        String contents = decodeUtf8(bytes);
        if (!contents.isEmpty() && contents.charAt(0) == BYTE_ORDER_MARK) {
            contents = contents.substring(1);
        }
        validateRecordEndings(contents);

        String[] records = contents.split("\\r?\\n", -1);
        if (!records[0].equals(EXPECTED_HEADER)) {
            throw new CsvImportException(MESSAGE_INVALID_HEADER);
        }

        List<Person> people = new ArrayList<>();
        for (int index = 1; index < records.length; index++) {
            String record = records[index];
            if (record.isEmpty()) {
                continue;
            }
            people.add(parsePerson(record, index + 1));
        }
        return List.copyOf(people);
    }

    private static String decodeUtf8(byte[] bytes) throws CsvImportException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new CsvImportException(MESSAGE_INVALID_UTF8, e);
        }
    }

    private static void validateRecordEndings(String contents) throws CsvImportException {
        for (int index = 0; index < contents.length(); index++) {
            if (contents.charAt(index) == '\r'
                    && (index + 1 == contents.length() || contents.charAt(index + 1) != '\n')) {
                throw new CsvImportException(MESSAGE_INVALID_RECORD_ENDING);
            }
        }
    }

    private static Person parsePerson(String record, int row) throws CsvImportException {
        List<String> fields;
        try {
            fields = parseFields(record);
        } catch (CsvImportException e) {
            throw rowError(row, e.getMessage());
        }
        if (fields.size() != FIELD_COUNT) {
            throw rowError(row, String.format(MESSAGE_WRONG_FIELD_COUNT, fields.size()));
        }

        String nameValue = fields.get(0).trim();
        String phoneValue = fields.get(1).trim();
        String emailValue = fields.get(2).trim();
        String addressValue = fields.get(3).trim();
        String tagsValue = fields.get(4).trim();

        if (!Name.isValidName(nameValue)) {
            throw rowError(row, Name.MESSAGE_CONSTRAINTS);
        }
        if (!Phone.isValidPhone(phoneValue)) {
            throw rowError(row, Phone.MESSAGE_CONSTRAINTS);
        }
        if (!Email.isValidEmail(emailValue)) {
            throw rowError(row, Email.MESSAGE_CONSTRAINTS);
        }
        if (!Address.isValidAddress(addressValue)) {
            throw rowError(row, Address.MESSAGE_CONSTRAINTS);
        }

        Set<Tag> tags = parseTags(tagsValue, row);
        return new Person(new Name(nameValue), new Phone(phoneValue), new Email(emailValue),
                new Address(addressValue), tags);
    }

    private static Set<Tag> parseTags(String tagsValue, int row) throws CsvImportException {
        Set<Tag> tags = new HashSet<>();
        if (tagsValue.isEmpty()) {
            return tags;
        }
        for (String tagValue : tagsValue.split(";", -1)) {
            String trimmedTag = tagValue.trim();
            if (!Tag.isValidTagName(trimmedTag)) {
                throw rowError(row, Tag.MESSAGE_CONSTRAINTS);
            }
            tags.add(new Tag(trimmedTag));
        }
        return tags;
    }

    /**
     * Parses one physical CSV record. Multiline values are intentionally unsupported.
     */
    private static List<String> parseFields(String record) throws CsvImportException {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        boolean quoteClosed = false;

        for (int index = 0; index < record.length(); index++) {
            char current = record.charAt(index);
            if (inQuotes) {
                if (current != '"') {
                    currentField.append(current);
                } else if (index + 1 < record.length() && record.charAt(index + 1) == '"') {
                    currentField.append('"');
                    index++;
                } else {
                    inQuotes = false;
                    quoteClosed = true;
                }
            } else if (quoteClosed) {
                if (current != ',') {
                    throw new CsvImportException(MESSAGE_INVALID_CSV);
                }
                fields.add(currentField.toString());
                currentField.setLength(0);
                quoteClosed = false;
            } else if (current == ',') {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else if (current == '"') {
                if (!currentField.isEmpty()) {
                    throw new CsvImportException(MESSAGE_INVALID_CSV);
                }
                inQuotes = true;
            } else {
                currentField.append(current);
            }
        }

        if (inQuotes) {
            throw new CsvImportException(MESSAGE_INVALID_CSV);
        }
        fields.add(currentField.toString());
        return fields;
    }

    private static CsvImportException rowError(int row, String reason) {
        return new CsvImportException("row " + row + ": " + reason);
    }
}

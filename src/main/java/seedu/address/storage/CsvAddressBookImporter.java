package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import seedu.address.commons.util.FileUtil;
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

    public static final String EXPECTED_HEADER = "name, phone, email, address, and tags";
    public static final String MESSAGE_INVALID_HEADER = "invalid header. Expected the columns " + EXPECTED_HEADER
            + " exactly once.";
    public static final String MESSAGE_INVALID_UTF8 = "invalid UTF-8 encoding.";
    public static final String MESSAGE_INVALID_RECORD_ENDING = "invalid CSV record ending.";
    public static final String MESSAGE_INVALID_CSV = "invalid CSV syntax.";
    public static final String MESSAGE_MULTILINE_FIELD = "line breaks inside fields are not supported.";
    public static final String MESSAGE_WRONG_FIELD_COUNT = "expected 5 fields but found %d.";

    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    public static final int MAX_MEMBER_ROWS = 10_000;
    public static final String MESSAGE_FILE_TOO_LARGE =
            "CSV file exceeds the 10 MiB limit. Split it into smaller CSV files.";
    public static final String MESSAGE_TOO_MANY_ROWS =
            "CSV file exceeds the 10,000 member-row limit. Split it into smaller CSV files.";

    private static final int FIELD_COUNT = CsvAddressBookCsv.HEADERS.size();
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    private CsvAddressBookImporter() {
        // Utility class.
    }

    /**
     * Reads and validates every record in {@code filePath} without changing the source file.
     *
     * @throws IOException if the file cannot be read.
     * @throws CsvImportException if the file exceeds the byte or row limits, is not valid UTF-8 CSV,
     *     or contains an invalid member.
     */
    public static List<Person> read(Path filePath) throws IOException, CsvImportException {
        requireNonNull(filePath);
        filePath = FileUtil.resolvePath(filePath);
        if (Files.size(filePath) > MAX_FILE_BYTES) {
            throw new CsvImportException(MESSAGE_FILE_TOO_LARGE);
        }
        byte[] bytes;
        try (InputStream input = Files.newInputStream(filePath)) {
            // Bound the read too: the file may grow after the size check.
            bytes = input.readNBytes(MAX_FILE_BYTES + 1);
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw new CsvImportException(MESSAGE_FILE_TOO_LARGE);
        }
        String contents = decodeUtf8(bytes);
        if (!contents.isEmpty() && contents.charAt(0) == BYTE_ORDER_MARK) {
            contents = contents.substring(1);
        }
        validateRecordEndings(contents);

        return parseRecords(new StringReader(contents));
    }

    static List<Person> parseRecords(Reader reader) throws CsvImportException {
        try (reader) {
            StringWriter contents = new StringWriter();
            reader.transferTo(contents);
            return parseContents(contents.toString());
        } catch (IOException e) {
            throw new CsvImportException(MESSAGE_INVALID_CSV, e);
        }
    }

    /** Keeps raw physical rows available so quote validation does not lose information during CSV decoding. */
    private static List<Person> parseContents(String contents) throws CsvImportException {
        try (BufferedReader rawRows = new BufferedReader(new StringReader(contents));
                CSVParser parser = CSVParser.parse(new StringReader(contents), CsvAddressBookCsv.FORMAT)) {
            Iterator<CSVRecord> records = parser.iterator();
            CSVRecord header = nextRecord(parser, records, rawRows);
            if (header == null) {
                throw new CsvImportException(MESSAGE_INVALID_HEADER);
            }

            Map<String, Integer> headerIndexes = parseHeaders(header);
            List<Person> people = new ArrayList<>();
            CSVRecord record;
            while ((record = nextRecord(parser, records, rawRows)) != null) {
                if (record.size() == 1 && record.get(0).trim().isEmpty()) {
                    continue;
                }
                if (people.size() >= MAX_MEMBER_ROWS) {
                    throw new CsvImportException(MESSAGE_TOO_MANY_ROWS);
                }
                people.add(parsePerson(record, headerIndexes));
            }
            return List.copyOf(people);
        } catch (IOException e) {
            throw new CsvImportException(MESSAGE_INVALID_CSV, e);
        }
    }

    /**
     * Reads one record, reporting syntax errors at its starting physical row.
     * Multiline fields are rejected before trimming so line breaks cannot disappear during normalisation.
     */
    private static CSVRecord nextRecord(CSVParser parser, Iterator<CSVRecord> records, BufferedReader rawRows)
            throws CsvImportException, IOException {
        long row = parser.getCurrentLineNumber() + 1;
        CSVRecord record;
        try {
            if (!records.hasNext()) {
                return null;
            }
            record = records.next();
        } catch (UncheckedIOException e) {
            throw new CsvImportException("row " + row + ": " + MESSAGE_INVALID_CSV, e.getCause());
        }
        for (String value : record) {
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw rowError(row, MESSAGE_MULTILINE_FIELD);
            }
        }
        validateQuotes(rawRows.readLine(), row);
        return record;
    }

    /**
     * Rejects quotes embedded in unquoted fields, which the CSV library otherwise accepts as literal text.
     * Quoted fields retain doubled-quote escaping and optional surrounding whitespace.
     */
    private static void validateQuotes(String record, long row) throws CsvImportException {
        boolean isQuoted = false;
        boolean isFieldStart = true;
        for (int index = 0; index < record.length(); index++) {
            char character = record.charAt(index);
            if (isQuoted) {
                if (character == '"') {
                    if (index + 1 < record.length() && record.charAt(index + 1) == '"') {
                        index++;
                    } else {
                        isQuoted = false;
                    }
                }
            } else if (character == ',') {
                isFieldStart = true;
            } else if (character == '"') {
                if (!isFieldStart) {
                    throw rowError(row, MESSAGE_INVALID_CSV);
                }
                isQuoted = true;
                isFieldStart = false;
            } else if (!Character.isWhitespace(character)) {
                isFieldStart = false;
            }
        }
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

    private static Map<String, Integer> parseHeaders(CSVRecord record) throws CsvImportException {
        Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < record.size(); index++) {
            String header = record.get(index);
            if (!CsvAddressBookCsv.HEADERS.contains(header) || indexes.putIfAbsent(header, index) != null) {
                throw new CsvImportException(MESSAGE_INVALID_HEADER);
            }
        }
        if (indexes.size() != FIELD_COUNT) {
            throw new CsvImportException(MESSAGE_INVALID_HEADER);
        }
        return indexes;
    }

    private static Person parsePerson(CSVRecord record, Map<String, Integer> headerIndexes) throws CsvImportException {
        long row = record.getRecordNumber();
        if (record.size() != FIELD_COUNT) {
            throw rowError(row, String.format(MESSAGE_WRONG_FIELD_COUNT, record.size()));
        }

        String nameValue = record.get(headerIndexes.get("name")).trim();
        String phoneValue = record.get(headerIndexes.get("phone")).trim();
        String emailValue = record.get(headerIndexes.get("email")).trim();
        String addressValue = record.get(headerIndexes.get("address")).trim();
        String tagsValue = record.get(headerIndexes.get("tags")).trim();

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

    private static Set<Tag> parseTags(String tagsValue, long row) throws CsvImportException {
        Set<Tag> tags = new HashSet<>();
        if (tagsValue.isEmpty()) {
            return tags;
        }
        for (String tagValue : tagsValue.split(";", -1)) {
            tagValue = tagValue.trim();
            if (!Tag.isValidTagName(tagValue)) {
                throw rowError(row, Tag.MESSAGE_CONSTRAINTS);
            }
            tags.add(new Tag(tagValue));
        }
        return tags;
    }

    private static CsvImportException rowError(long row, String reason) {
        return new CsvImportException("row " + row + ": " + reason);
    }
}

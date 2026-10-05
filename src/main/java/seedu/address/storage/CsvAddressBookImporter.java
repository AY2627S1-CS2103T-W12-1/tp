package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
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
 * Reads member records from a basic, unquoted CSV file.
 */
public final class CsvAddressBookImporter {

    public static final String EXPECTED_HEADER = "name,phone,email,address,tags";
    public static final String MESSAGE_INVALID_HEADER = "invalid header. Expected " + EXPECTED_HEADER + ".";
    public static final String MESSAGE_WRONG_FIELD_COUNT = "expected 5 fields but found %d.";

    private static final int FIELD_COUNT = 5;

    private CsvAddressBookImporter() {
        // Utility class.
    }

    /**
     * Reads a CSV containing the fixed header and five unquoted fields per member.
     *
     * @throws IOException if the file cannot be read.
     * @throws CsvImportException if its header, field count, or a member field is invalid.
     */
    public static List<Person> read(Path filePath) throws IOException, CsvImportException {
        requireNonNull(filePath);
        List<String> records = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        if (records.isEmpty() || !records.getFirst().equals(EXPECTED_HEADER)) {
            throw new CsvImportException(MESSAGE_INVALID_HEADER);
        }

        List<Person> people = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            people.add(parsePerson(records.get(index), index + 1));
        }
        return List.copyOf(people);
    }

    private static Person parsePerson(String record, int row) throws CsvImportException {
        String[] fields = record.split(",", -1);
        if (fields.length != FIELD_COUNT) {
            throw rowError(row, String.format(MESSAGE_WRONG_FIELD_COUNT, fields.length));
        }

        String nameValue = fields[0].trim();
        String phoneValue = fields[1].trim();
        String emailValue = fields[2].trim();
        String addressValue = fields[3].trim();
        String tagsValue = fields[4].trim();

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

    private static CsvImportException rowError(int row, String reason) {
        return new CsvImportException("row " + row + ": " + reason);
    }
}

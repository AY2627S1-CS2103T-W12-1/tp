package seedu.address.storage;

import java.util.List;

import org.apache.commons.csv.CSVFormat;

/**
 * Shared CSV format and column names for address book import and export.
 */
final class CsvAddressBookCsv {

    static final List<String> HEADERS = List.of("name", "phone", "email", "address", "tags");
    static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setIgnoreSurroundingSpaces(true)
            .get();

    private CsvAddressBookCsv() {
        // Utility class.
    }
}

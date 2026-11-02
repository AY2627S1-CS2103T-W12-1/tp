package seedu.address.storage;

import java.util.List;

import org.apache.commons.csv.CSVFormat;

/**
 * Shared CSV format and column names for address book import and export.
 */
final class CsvAddressBookCsv {

    static final List<String> HEADERS = List.of("name", "phone", "email", "address", "tags");
    static final String ENCODING_HEADER = "trackcall_encoding";
    static final String SPREADSHEET_ENCODING = "apostrophe-v1";
    static final List<String> ENCODED_HEADERS = List.of("name", "phone", "email", "address", "tags",
            ENCODING_HEADER);
    static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setIgnoreSurroundingSpaces(true)
            .get();
    private static final String FORMULA_PREFIXES = "=+-@";

    private CsvAddressBookCsv() {
        // Utility class.
    }

    /** Prefixes spreadsheet formula-like values, including values already starting with apostrophes. */
    static String escapeForSpreadsheet(String value) {
        return hasFormulaPrefix(value) ? "'" + value : value;
    }

    /** Reverses one export prefix in a file explicitly marked with {@link #SPREADSHEET_ENCODING}. */
    static String unescapeSpreadsheet(String value) {
        return value.startsWith("'") && hasFormulaPrefix(value.substring(1)) ? value.substring(1) : value;
    }

    private static boolean hasFormulaPrefix(String value) {
        int firstNonApostrophe = 0;
        while (firstNonApostrophe < value.length() && value.charAt(firstNonApostrophe) == '\'') {
            firstNonApostrophe++;
        }
        return firstNonApostrophe < value.length()
                && FORMULA_PREFIXES.indexOf(value.charAt(firstNonApostrophe)) >= 0;
    }
}

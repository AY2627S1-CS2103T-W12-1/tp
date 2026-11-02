package seedu.address.storage;

import java.util.List;

import org.apache.commons.csv.CSVFormat;

/**
 * Shared CSV format and column names for address book import and export.
 */
final class CsvAddressBookCsv {

    static final List<String> HEADERS = List.of("name", "phone", "email", "address", "tags");
    private static final String FORMULA_PREFIXES = "=+-@";
    static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setIgnoreSurroundingSpaces(true)
            .get();

    private CsvAddressBookCsv() {
        // Utility class.
    }

    /** Prefixes spreadsheet formula-like values, including values already starting with apostrophes. */
    static String escapeForSpreadsheet(String value) {
        return hasFormulaPrefix(value) ? "'" + value : value;
    }

    /** Reverses one export prefix; preserves any apostrophes that were part of the original value. */
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

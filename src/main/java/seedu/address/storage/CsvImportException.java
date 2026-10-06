package seedu.address.storage;

/**
 * Signals that a CSV file cannot be converted into valid address book records.
 */
public class CsvImportException extends Exception {

    public CsvImportException(String message) {
        super(message);
    }

    public CsvImportException(String message, Throwable cause) {
        super(message, cause);
    }
}

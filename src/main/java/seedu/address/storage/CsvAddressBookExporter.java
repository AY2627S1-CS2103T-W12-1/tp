package seedu.address.storage;

import static java.util.stream.Collectors.joining;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import seedu.address.model.person.Person;

/** Writes address book contacts to a CSV file. */
public final class CsvAddressBookExporter {

    private static final String[] HEADERS = {"Name", "Phone", "Email", "Address", "Tags"};

    private CsvAddressBookExporter() {
        // Utility class.
    }

    /** Writes the given contacts to {@code filePath} using UTF-8 and RFC 4180 CSV formatting. */
    public static void export(List<Person> people, Path filePath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180)) {
            csvPrinter.printRecord((Object[]) HEADERS);
            for (Person person : people) {
                String tags = person.getTags().stream()
                        .map(tag -> tag.tagName)
                        .sorted()
                        .collect(joining(";"));
                csvPrinter.printRecord(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(), tags);
            }
        }
    }
}

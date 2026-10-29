package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import seedu.address.testutil.PersonBuilder;

/** Documents literal substring boundaries independently of the matching implementation. */
public class PartialNameSearchEdgeTest {

    @TestFactory
    public Stream<DynamicTest> matching_literalAndUnicodeBoundaries() {
        Object[][] cases = {
            {"Alice Tan", "Alice", true},
            {"Alice Tan", "ali", true},
            {"Alice Tan", "LIC", true},
            {"Alice Tan", "ice", true},
            {"Alice Tan", "a", true},
            {"Alice Tan", "n", true},
            {"Alice Tan", "AliceTan", false},
            {"Alice Tan", "alce", false},
            {"Alice Tan", "Alicee", false},
            {"Alice Tan", "ceTa", false},
            {"Alice Tan", ".*", false},
            {"Alice Tan", "^Ali", false},
            {"Alice Tan", "[", false},
            {"Alice Tan", "\\", false},
            {"Alice Tan", "a".repeat(1000), false},
            {"Anne-Marie", "e-M", true},
            {"O'Connor", "'Con", true},
            {"O’Connor", "’con", true},
            {"O’Connor", "'con", false},
            {"José Garcia", "OSÉ", true},
            {"José Garcia", "Jose", false},
            {"José", "Jose\u0301", false},
            {"Jose\u0301", "José", false},
            {"Jose\u0301", "e\u0301", true},
            {"ΟΣ", "ος", true},
            {"ΟΣ", "οσ", true},
            {"张伟", "伟", true},
            {"Member 123", "23", true},
            {"Alice  Tan", "Tan", true}
        };
        return Stream.of(cases).map(row -> dynamicTest(row[0] + " / " + row[1], () -> {
            Person person = new PersonBuilder().withName((String) row[0]).build();
            assertEquals(row[2], new NameContainsKeywordsPredicate(List.of((String) row[1])).test(person));
        }));
    }
}

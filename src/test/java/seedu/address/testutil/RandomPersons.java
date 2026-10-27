package seedu.address.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import seedu.address.model.person.Person;

/** Generates reproducible valid rosters, including characters that need CSV escaping. */
public final class RandomPersons {
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String[] ADDRESS_PARTS = {
        "Road", ",", "\"", ";", "\\", "\t", "  ", "#01-23", "caf\u00e9", "\u4e2d\u6587", "\uD83C\uDFE0"
    };

    private RandomPersons() {
    }

    /** Returns unique members with no surrounding whitespace, so round trips should preserve every field. */
    public static List<Person> generate(Random random, int count) {
        List<Person> people = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String[] tags = new String[random.nextInt(7)];
            for (int tag = 0; tag < tags.length; tag++) {
                tags[tag] = word(random, 1 + random.nextInt(30));
            }
            StringBuilder address = new StringBuilder("Block ");
            int partCount = 1 + random.nextInt(20);
            for (int part = 0; part < partCount; part++) {
                address.append(ADDRESS_PARTS[random.nextInt(ADDRESS_PARTS.length)]);
            }
            address.append(" End");
            people.add(new PersonBuilder().withName(word(random, 1 + random.nextInt(30)) + "  " + index)
                    .withPhone("000" + random.nextInt(10000000))
                    .withEmail("Member" + index + "+" + word(random, 8) + "@example.com")
                    .withAddress(random.nextInt(5) == 0 ? "" : address.toString())
                    .withTags(tags).build());
        }
        return people;
    }

    private static String word(Random random, int length) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < length; index++) {
            result.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return result.toString();
    }
}
